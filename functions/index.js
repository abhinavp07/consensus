/**
 * Consensus Cloud Functions.
 *
 * - onTripUpdated: notifies members when someone joins and when the itinerary is (re)generated.
 * - onTripDeleted: deletes a trip's subcollections, which clients can't do recursively.
 *
 * Messages are data-only so the app's ConsensusMessagingService always builds the notification
 * and can deep-link to the trip.
 */
const {onDocumentUpdated, onDocumentDeleted} = require("firebase-functions/v2/firestore");
const logger = require("firebase-functions/logger");
const {initializeApp} = require("firebase-admin/app");
const {getFirestore, FieldValue} = require("firebase-admin/firestore");
const {getMessaging} = require("firebase-admin/messaging");

initializeApp();
const db = getFirestore();

const STALE_TOKEN_ERRORS = new Set([
  "messaging/registration-token-not-registered",
  "messaging/invalid-registration-token",
]);

exports.onTripUpdated = onDocumentUpdated("trips/{tripId}", async (event) => {
  const before = event.data.before.data();
  const after = event.data.after.data();
  const tripId = event.params.tripId;
  const members = after.memberIds || [];
  const tasks = [];

  const previous = new Set(before.memberIds || []);
  for (const uid of members.filter((id) => !previous.has(id))) {
    const name = after.memberProfiles?.[uid]?.name || "Someone";
    tasks.push(notify(members.filter((id) => id !== uid), {
      title: `${name} joined ${after.name}`,
      body: `${firstName(name)} is coming to ${after.destination}. Ask them to fill out their preferences!`,
      tripId,
    }));
  }

  const generatedBefore = before.generatedAt?.toMillis?.() ?? null;
  const generatedAfter = after.generatedAt?.toMillis?.() ?? null;
  if (generatedAfter && generatedAfter !== generatedBefore) {
    tasks.push(notify(members.filter((id) => id !== after.ownerId), {
      title: `Your ${after.destination} itinerary is ready`,
      body: "Take a look and vote on the activities.",
      tripId,
    }));
  }

  await Promise.all(tasks);
});

exports.onTripDeleted = onDocumentDeleted("trips/{tripId}", async (event) => {
  const tripRef = db.doc(`trips/${event.params.tripId}`);
  await db.recursiveDelete(tripRef);
  const code = event.data?.data()?.inviteCode;
  if (code) await db.doc(`inviteCodes/${code}`).delete().catch(() => {});
  logger.info("Deleted trip subcollections", {tripId: event.params.tripId});
});

/** Sends a data message to every device of the given users and prunes dead tokens. */
async function notify(userIds, data) {
  if (userIds.length === 0) return;
  const users = await db.getAll(...userIds.map((id) => db.doc(`users/${id}`)));
  const owners = [];
  const tokens = [];
  for (const user of users) {
    for (const token of user.get("fcmTokens") || []) {
      tokens.push(token);
      owners.push(user.ref);
    }
  }
  if (tokens.length === 0) return;

  const response = await getMessaging().sendEachForMulticast({
    tokens,
    data,
    android: {priority: "high"},
  });

  const cleanups = [];
  response.responses.forEach((result, i) => {
    if (!result.success && STALE_TOKEN_ERRORS.has(result.error?.code)) {
      cleanups.push(owners[i].update({fcmTokens: FieldValue.arrayRemove(tokens[i])}));
    }
  });
  await Promise.all(cleanups);
  logger.info("Sent notification", {title: data.title, sent: response.successCount, failed: response.failureCount});
}

function firstName(name) {
  return name.split(" ")[0];
}
