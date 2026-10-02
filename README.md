# Consensus

Android app for planning a group trip. Everyone fills out a short survey about budget, interests and pace, then Gemini drafts a day-by-day itinerary that tries to give each person something they want. From there the group can vote on activities, swap out the ones nobody likes, split expenses and chat. Everything syncs live through Firestore.

Built in Java with Firebase (Auth, Firestore, Cloud Messaging, AI Logic) and the Google Maps/Places SDKs. The original requirements are in [docs/user-stories.md](docs/user-stories.md).

## Running it

You'll need Android Studio, Node, and a Google account.

### Firebase

1. Create a project in the [Firebase console](https://console.firebase.google.com).
2. Add an Android app with package name `com.abhinavpinisetti.consensus` and your debug SHA-1. To get the SHA-1:
   ```
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android | grep SHA1
   ```
3. Turn on **Authentication → Google**, create a **Firestore** database, and set up **AI Logic** with the Gemini Developer API.
4. Download `google-services.json` into `app/`. Do this after enabling Google sign-in and adding the SHA-1, otherwise sign-in fails (it just shows "Sign-in was cancelled").

### Rules and functions

```
npx firebase-tools login
npx firebase-tools use --add
npx firebase-tools deploy --only firestore:rules
```

Notifications and trip cleanup run as Cloud Functions, which need the Blaze plan. Skip this if you don't care about push notifications.

```
cd functions && npm install && cd ..
npx firebase-tools deploy --only functions
```

### Maps (optional)

Enable Maps SDK for Android and Places API (New) in Google Cloud for the same project, create a key restricted to the app's package and SHA-1, and add it to `local.properties`:

```
MAPS_API_KEY=AIza...
```

Without a key the app still works. You just type the destination instead of picking it, and the map view is disabled.

### Build

Open the folder in Android Studio and run it on a device or emulator that has Google Play and a signed-in Google account. Testing the group features needs a second device with a different account.

## Tests

```
./gradlew :app:testDebugUnitTest            # invite codes, settle-up math, AI response parsing
cd rules-tests && npm install && npm test   # Firestore rules, runs against the emulator
```

## How it's put together

Standard MVVM. Activities observe LiveData from ViewModels, and all Firebase, Gemini and Places calls live in `data/repo`. Real-time screens use small LiveData wrappers around Firestore snapshot listeners, so listeners are detached when a screen goes to the background. More detail on the layout and Firestore schema is in [CLAUDE.md](CLAUDE.md).

A few things that aren't obvious from the code:

- Invite codes are stored in their own `inviteCodes` collection. Non-members can't read trips, so joining needs somewhere to look up a code first.
- Member names and photos get copied onto the trip when someone joins. Rules only let you read your own user document.
- Gemini returns JSON against a schema, and the response is validated before anything is written. If the plan is malformed or incomplete, nothing gets saved. Regenerating only replaces AI-generated activities; ones people added by hand stay.
- Each activity is matched to a real place once and the result is stored, so the Places API isn't called on every screen load.
- Money is stored in cents. The settle-up calculation is plain Java in `util/SettleUp` with its own tests.
- The Gemini model name is `MODEL_NAME` in `AiRepository`. Google retires old models, so if generation starts failing, check Logcat for `AiRepository` and update it.
