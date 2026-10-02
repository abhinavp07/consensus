# Consensus

Android app where friends plan a trip together and AI drafts the itinerary. Full requirements: `docs/user-stories.md`.

## Stack

- **Language:** Java (not Kotlin), Android Studio + Gradle (Kotlin DSL build files). Min SDK 26, target/compile SDK 36.
- **Architecture:** MVVM with ViewModel + LiveData, one Activity per major screen (day tabs are Fragments).
- **Backend:** Firebase Authentication (Google sign-in via Credential Manager), Cloud Firestore, Firebase Cloud Messaging.
- **AI:** Gemini through Firebase AI Logic (`firebase-ai`), structured JSON output with a response schema.
- **Maps:** Google Maps SDK for Android + Places SDK (new Places API).
- **Secrets:** `MAPS_API_KEY` lives in `local.properties` (Secrets Gradle Plugin). `google-services.json` and `local.properties` are git-ignored.
- **Server:** Cloud Functions (Node 22) in `functions/` for notifications and recursive trip deletion.

## Layout

```
app/src/main/java/com/abhinavpinisetti/consensus/
  ai/            Pure Java: PromptBuilder, ItineraryParser (Gson), response schemas
  data/          QueryLiveData / DocumentLiveData (snapshot listeners tied to observer lifecycle), ErrorMessages
  data/model/    Firestore POJOs (public fields, @DocumentId)
  data/repo/     All Firebase, Gemini and Places calls live here — never in Activities/Fragments
  notifications/ FCM service + channel
  ui/            One package per screen: auth, trips, overview, preferences, itinerary, expenses, chat
  util/          Pure Java: InviteCodeGenerator, SettleUp, Money, DateUtils, Result, Event
firestore.rules  Security rules; tests in rules-tests/ (npm test, runs the emulator)
functions/       Cloud Functions
```

## Firestore structure

```
users/{userId}: name, email, photoUrl, fcmTokens[]
inviteCodes/{code}: tripId, ownerId                       # lookup for joining; get-only, not listable
trips/{tripId}: name, destination, destinationPlaceId, destinationLat, destinationLng,
                startDate, endDate (ISO "yyyy-MM-dd"), ownerId, inviteCode, memberIds[],
                memberProfiles{uid: {name, photoUrl}}, status, createdAt, generatedAt
  preferences/{userId}: budget, interests[], pace, mustDos, avoid, updatedAt
  days/{dayId = "day-01"}: date, dayNumber, note        # note = AI explanation of the day's balance
    activities/{activityId}: title, description, time (HH:mm), estimatedCost, votes{uid: 1|-1},
                             source ("ai"|"manual"), placeName, placeId, address, location (GeoPoint),
                             placeLookupDone
  expenses/{expenseId}: description, amountCents (int), paidBy, splitAmong[], createdAt
  messages/{messageId}: senderId, senderName, senderPhotoUrl, text, timestamp
```

Member names/photos are copied onto the trip (`memberProfiles`) and messages because rules only allow reading your own `users` doc.

## Conventions

- Keep Firebase and AI calls in repository classes. ViewModels call repositories; Activities observe LiveData.
- Every network call shows a loading state and every failure shows a friendly message (`ErrorMessages.from`).
- Real-time screens use `QueryLiveData`/`DocumentLiveData` — never one-time reads for live data.
- Logic that doesn't need Android goes in `util/` or `ai/` with JUnit tests in `app/src/test`.
- Never commit API keys, `google-services.json`, or `local.properties`.
- If you change `firestore.rules`, add/adjust tests in `rules-tests/test/` and run `npm test` there.
- After finishing a story, summarize what changed and how to test it manually.

## Commands

```
./gradlew :app:assembleDebug          # build
./gradlew :app:testDebugUnitTest      # JVM unit tests
./gradlew :app:lintDebug              # lint
cd rules-tests && npm install && npm test   # security rules tests (needs Java 11+)
firebase deploy --only firestore:rules,functions
```
