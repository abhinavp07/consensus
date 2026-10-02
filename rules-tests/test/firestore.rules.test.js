/**
 * Security rules tests (US-9.1). Run with `npm test` in this folder; it starts the Firestore
 * emulator, runs these tests, and shuts it down.
 */
const fs = require("fs");
const path = require("path");
const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const {
  doc, getDoc, setDoc, updateDoc, deleteDoc, getDocs, collection, query, where,
  writeBatch, arrayUnion, arrayRemove, deleteField, serverTimestamp,
} = require("firebase/firestore");

let env;
const OWNER = "owner";
const MEMBER = "member";
const STRANGER = "stranger";

const db = (uid) => env.authenticatedContext(uid).firestore();

async function seed() {
  await env.withSecurityRulesDisabled(async (ctx) => {
    const admin = ctx.firestore();
    await setDoc(doc(admin, "trips/t1"), {
      name: "Chicago weekend",
      destination: "Chicago",
      startDate: "2026-10-09",
      endDate: "2026-10-11",
      ownerId: OWNER,
      inviteCode: "K9X4TP",
      status: "collecting_preferences",
      memberIds: [OWNER, MEMBER],
      memberProfiles: {[OWNER]: {name: "Olive"}, [MEMBER]: {name: "Max"}},
    });
    await setDoc(doc(admin, "inviteCodes/K9X4TP"), {tripId: "t1", ownerId: OWNER});
    await setDoc(doc(admin, "trips/t1/days/day-01"), {date: "2026-10-09", dayNumber: 1, note: ""});
    await setDoc(doc(admin, "trips/t1/days/day-01/activities/a1"), {
      title: "Art Institute", time: "10:00", estimatedCost: 32, source: "ai",
      votes: {[OWNER]: 1},
    });
    await setDoc(doc(admin, "trips/t1/preferences/" + MEMBER), {budget: "Low", pace: "Packed", interests: ["Food"]});
    await setDoc(doc(admin, "users/" + MEMBER), {name: "Max"});
  });
}

before(async () => {
  env = await initializeTestEnvironment({
    projectId: "demo-consensus",
    firestore: {rules: fs.readFileSync(path.join(__dirname, "../../firestore.rules"), "utf8")},
  });
});

beforeEach(async () => {
  await env.clearFirestore();
  await seed();
});

after(async () => {
  await env.cleanup();
});

describe("users", () => {
  it("lets users read and write their own document", async () => {
    await assertSucceeds(getDoc(doc(db(MEMBER), "users/" + MEMBER)));
    await assertSucceeds(setDoc(doc(db(MEMBER), "users/" + MEMBER), {name: "Maxine"}));
  });

  it("blocks other users' documents", async () => {
    await assertFails(getDoc(doc(db(OWNER), "users/" + MEMBER)));
    await assertFails(setDoc(doc(db(OWNER), "users/" + MEMBER), {name: "hacked"}));
  });

  it("blocks signed-out access", async () => {
    await assertFails(getDoc(doc(env.unauthenticatedContext().firestore(), "users/" + MEMBER)));
  });
});

describe("trips: read", () => {
  it("lets members read the trip and its subcollections", async () => {
    await assertSucceeds(getDoc(doc(db(MEMBER), "trips/t1")));
    await assertSucceeds(getDocs(collection(db(MEMBER), "trips/t1/days/day-01/activities")));
    await assertSucceeds(getDocs(collection(db(MEMBER), "trips/t1/preferences")));
  });

  it("allows the My Trips query", async () => {
    await assertSucceeds(getDocs(query(collection(db(MEMBER), "trips"), where("memberIds", "array-contains", MEMBER))));
  });

  it("hides trips and subcollections from non-members", async () => {
    await assertFails(getDoc(doc(db(STRANGER), "trips/t1")));
    await assertFails(getDocs(collection(db(STRANGER), "trips/t1/days")));
    await assertFails(getDocs(collection(db(STRANGER), "trips/t1/messages")));
    await assertFails(getDocs(collection(db(STRANGER), "trips")));
  });
});

describe("trips: owner-only changes", () => {
  it("lets the owner rename, re-date and change status", async () => {
    await assertSucceeds(updateDoc(doc(db(OWNER), "trips/t1"), {name: "New name", endDate: "2026-10-12", status: "planned"}));
  });

  it("blocks members from changing name, dates or destination", async () => {
    await assertFails(updateDoc(doc(db(MEMBER), "trips/t1"), {name: "Mine now"}));
    await assertFails(updateDoc(doc(db(MEMBER), "trips/t1"), {startDate: "2026-11-01"}));
    await assertFails(updateDoc(doc(db(MEMBER), "trips/t1"), {destination: "Paris"}));
  });

  it("only lets the owner delete", async () => {
    await assertFails(deleteDoc(doc(db(MEMBER), "trips/t1")));
    await assertSucceeds(deleteDoc(doc(db(OWNER), "trips/t1")));
  });
});

describe("trips: create", () => {
  it("allows creating a trip with invite code and days in one batch", async () => {
    const d = db(STRANGER);
    const batch = writeBatch(d);
    batch.set(doc(d, "trips/t2"), {
      name: "NYC", destination: "New York", startDate: "2026-12-01", endDate: "2026-12-02",
      ownerId: STRANGER, inviteCode: "ABCDEF", status: "collecting_preferences",
      memberIds: [STRANGER], memberProfiles: {[STRANGER]: {name: "S"}},
    });
    batch.set(doc(d, "inviteCodes/ABCDEF"), {tripId: "t2", ownerId: STRANGER});
    batch.set(doc(d, "trips/t2/days/day-01"), {date: "2026-12-01", dayNumber: 1, note: ""});
    await assertSucceeds(batch.commit());
  });

  it("rejects creating a trip owned by someone else or with extra members", async () => {
    const base = {
      name: "NYC", destination: "New York", startDate: "2026-12-01", endDate: "2026-12-02",
      inviteCode: "ABCDEF", status: "collecting_preferences", memberProfiles: {},
    };
    await assertFails(setDoc(doc(db(STRANGER), "trips/t3"), {...base, ownerId: OWNER, memberIds: [OWNER]}));
    await assertFails(setDoc(doc(db(STRANGER), "trips/t3"), {...base, ownerId: STRANGER, memberIds: [STRANGER, OWNER]}));
  });

  it("rejects an end date before the start date", async () => {
    await assertFails(setDoc(doc(db(STRANGER), "trips/t3"), {
      name: "NYC", destination: "New York", startDate: "2026-12-05", endDate: "2026-12-01",
      ownerId: STRANGER, inviteCode: "ABCDEF", status: "collecting_preferences",
      memberIds: [STRANGER], memberProfiles: {},
    }));
  });

  it("doesn't let someone claim an invite code for a trip they don't own", async () => {
    await assertFails(setDoc(doc(db(STRANGER), "inviteCodes/ZZZZZZ"), {tripId: "t1", ownerId: STRANGER}));
  });
});

describe("invite codes and joining", () => {
  it("lets anyone signed in look up a code, but not list codes", async () => {
    await assertSucceeds(getDoc(doc(db(STRANGER), "inviteCodes/K9X4TP")));
    await assertSucceeds(getDoc(doc(db(STRANGER), "inviteCodes/NOPE22")));
    await assertFails(getDocs(collection(db(STRANGER), "inviteCodes")));
  });

  it("lets a user add exactly their own ID", async () => {
    await assertSucceeds(updateDoc(doc(db(STRANGER), "trips/t1"), {
      memberIds: arrayUnion(STRANGER),
      [`memberProfiles.${STRANGER}`]: {name: "Sky"},
    }));
  });

  it("treats re-joining as a no-op", async () => {
    await assertSucceeds(updateDoc(doc(db(MEMBER), "trips/t1"), {
      memberIds: arrayUnion(MEMBER),
      [`memberProfiles.${MEMBER}`]: {name: "Max"},
    }));
  });

  it("blocks adding someone else", async () => {
    await assertFails(updateDoc(doc(db(STRANGER), "trips/t1"), {memberIds: arrayUnion("someone-else")}));
    await assertFails(updateDoc(doc(db(STRANGER), "trips/t1"), {memberIds: arrayUnion(STRANGER, "someone-else")}));
  });

  it("blocks changing other fields while joining", async () => {
    await assertFails(updateDoc(doc(db(STRANGER), "trips/t1"), {memberIds: arrayUnion(STRANGER), name: "Mine"}));
    await assertFails(updateDoc(doc(db(STRANGER), "trips/t1"), {
      memberIds: arrayUnion(STRANGER),
      [`memberProfiles.${OWNER}`]: {name: "Impostor"},
    }));
  });

  it("blocks removing other members", async () => {
    await assertFails(updateDoc(doc(db(STRANGER), "trips/t1"), {memberIds: [STRANGER]}));
    await assertFails(updateDoc(doc(db(MEMBER), "trips/t1"), {memberIds: arrayRemove(OWNER)}));
  });
});

describe("leaving", () => {
  it("lets a member leave and delete their preferences", async () => {
    const d = db(MEMBER);
    const batch = writeBatch(d);
    batch.delete(doc(d, "trips/t1/preferences/" + MEMBER));
    batch.update(doc(d, "trips/t1"), {memberIds: arrayRemove(MEMBER), [`memberProfiles.${MEMBER}`]: deleteField()});
    await assertSucceeds(batch.commit());
  });

  it("doesn't let the owner leave (they delete instead)", async () => {
    await assertFails(updateDoc(doc(db(OWNER), "trips/t1"), {memberIds: arrayRemove(OWNER)}));
  });
});

describe("preferences", () => {
  const prefs = {budget: "Medium", pace: "Balanced", interests: ["Art"], mustDos: "", avoid: ""};

  it("lets members write only their own preferences", async () => {
    await assertSucceeds(setDoc(doc(db(OWNER), "trips/t1/preferences/" + OWNER), prefs));
    await assertFails(setDoc(doc(db(OWNER), "trips/t1/preferences/" + MEMBER), prefs));
  });

  it("blocks non-members", async () => {
    await assertFails(setDoc(doc(db(STRANGER), "trips/t1/preferences/" + STRANGER), prefs));
    await assertFails(getDoc(doc(db(STRANGER), "trips/t1/preferences/" + MEMBER)));
  });

  it("validates answers", async () => {
    await assertFails(setDoc(doc(db(OWNER), "trips/t1/preferences/" + OWNER), {...prefs, budget: "Unlimited"}));
  });
});

describe("activities and votes", () => {
  it("lets members add, edit and delete activities", async () => {
    const d = db(MEMBER);
    await assertSucceeds(setDoc(doc(d, "trips/t1/days/day-01/activities/a2"),
        {title: "Pizza", time: "19:00", estimatedCost: 20, source: "manual", votes: {}}));
    await assertSucceeds(updateDoc(doc(d, "trips/t1/days/day-01/activities/a2"), {title: "Deep dish"}));
    await assertSucceeds(deleteDoc(doc(d, "trips/t1/days/day-01/activities/a2")));
  });

  it("rejects invalid activities and non-members", async () => {
    await assertFails(setDoc(doc(db(MEMBER), "trips/t1/days/day-01/activities/a3"),
        {title: "Pizza", time: "7pm", estimatedCost: 20}));
    await assertFails(setDoc(doc(db(STRANGER), "trips/t1/days/day-01/activities/a3"),
        {title: "Pizza", time: "19:00", estimatedCost: 20}));
  });

  it("lets a member set, change and remove only their own vote", async () => {
    const ref = doc(db(MEMBER), "trips/t1/days/day-01/activities/a1");
    await assertSucceeds(updateDoc(ref, {[`votes.${MEMBER}`]: -1}));
    await assertSucceeds(updateDoc(ref, {[`votes.${MEMBER}`]: 1}));
    await assertSucceeds(updateDoc(ref, {[`votes.${MEMBER}`]: deleteField()}));
  });

  it("blocks writing someone else's vote or invalid values", async () => {
    const ref = doc(db(MEMBER), "trips/t1/days/day-01/activities/a1");
    await assertFails(updateDoc(ref, {[`votes.${OWNER}`]: -1}));
    await assertFails(updateDoc(ref, {[`votes.${MEMBER}`]: 5}));
  });

  it("allows clearing all votes when swapping in an alternative", async () => {
    await assertSucceeds(updateDoc(doc(db(MEMBER), "trips/t1/days/day-01/activities/a1"),
        {title: "Navy Pier", votes: {}}));
  });

  it("lets the owner save a generated plan in one batch, keeping manual activities", async () => {
    const d = db(OWNER);
    const batch = writeBatch(d);
    batch.delete(doc(d, "trips/t1/days/day-01/activities/a1"));
    batch.set(doc(d, "trips/t1/days/day-01"), {dayNumber: 1, date: "2026-10-09", note: "Art for Olive"}, {merge: true});
    batch.set(doc(d, "trips/t1/days/day-01/activities/new1"), {
      title: "Millennium Park", time: "09:00", description: "", estimatedCost: 0, votes: {},
      source: "ai", placeName: "Millennium Park", placeId: null, address: null, location: null, placeLookupDone: false,
    });
    batch.update(doc(d, "trips/t1"), {status: "planned", generatedAt: serverTimestamp()});
    await assertSucceeds(batch.commit());
  });

  it("only lets the owner write day notes", async () => {
    await assertSucceeds(updateDoc(doc(db(OWNER), "trips/t1/days/day-01"), {note: "Art day"}));
    await assertFails(updateDoc(doc(db(MEMBER), "trips/t1/days/day-01"), {note: "Mine"}));
  });
});

describe("expenses and messages", () => {
  it("validates expenses", async () => {
    const d = db(MEMBER);
    await assertSucceeds(setDoc(doc(d, "trips/t1/expenses/e1"),
        {description: "Dinner", amountCents: 4250, paidBy: MEMBER, splitAmong: [OWNER, MEMBER]}));
    await assertFails(setDoc(doc(d, "trips/t1/expenses/e2"),
        {description: "Dinner", amountCents: 42.5, paidBy: MEMBER, splitAmong: [MEMBER]}));
    await assertFails(setDoc(doc(db(STRANGER), "trips/t1/expenses/e3"),
        {description: "Dinner", amountCents: 100, paidBy: STRANGER, splitAmong: [STRANGER]}));
  });

  it("only lets members post messages as themselves", async () => {
    await assertSucceeds(setDoc(doc(db(MEMBER), "trips/t1/messages/m1"), {senderId: MEMBER, text: "hi"}));
    await assertFails(setDoc(doc(db(MEMBER), "trips/t1/messages/m2"), {senderId: OWNER, text: "hi"}));
    await assertFails(setDoc(doc(db(STRANGER), "trips/t1/messages/m3"), {senderId: STRANGER, text: "hi"}));
  });

  it("makes messages append-only", async () => {
    await env.withSecurityRulesDisabled((ctx) =>
      setDoc(doc(ctx.firestore(), "trips/t1/messages/m1"), {senderId: MEMBER, text: "hi"}));
    await assertFails(updateDoc(doc(db(MEMBER), "trips/t1/messages/m1"), {text: "edited"}));
    await assertFails(deleteDoc(doc(db(MEMBER), "trips/t1/messages/m1")));
  });
});
