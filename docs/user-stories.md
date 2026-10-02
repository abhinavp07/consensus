# Consensus – User Stories

Oct 1, 2026 · @Abhinav Pinisetti

## How to use this with Claude Code

Give Claude Code one epic at a time, in order. Each epic builds on the last, and Epics 1–5 make a complete, demo-able app.

1. Export this doc as Markdown and save it in your repo as `docs/user-stories.md`.
2. Copy the Project context section below into a `CLAUDE.md` file at the repo root. Claude Code reads it automatically every session.
3. Prompt one epic at a time, for example: *"Read docs/user-stories.md and implement Epic 2. Check each acceptance criterion, then tell me how to test it."*
4. Test on an emulator or phone, commit, then move to the next epic.

## Project context

- **App:** Consensus, an Android app where friends plan a trip together and AI drafts the itinerary.
- **Language:** Java (not Kotlin), built in Android Studio with Gradle.
- **Min SDK:** 26 (Android 8.0).
- **Architecture:** MVVM with ViewModel and LiveData; one Activity per major screen or a single Activity with Fragments.
- **Backend:** Firebase Authentication (Google sign-in), Cloud Firestore, Firebase Cloud Messaging.
- **AI:** Gemini through Firebase AI Logic, using structured JSON output.
- **Maps:** Google Maps SDK for Android and Places SDK.
- **Secrets:** API keys live in `local.properties`, never committed to Git.

### Firestore structure

```
users/{userId}: name, email, photoUrl
trips/{tripId}: name, destination, startDate, endDate, ownerId, inviteCode, memberIds[], status
  preferences/{userId}: budget, interests[], pace, mustDos, avoid
  days/{dayId}: date, dayNumber
    activities/{activityId}: title, description, time, estimatedCost, votes{}, placeId, address, location (GeoPoint)
  expenses/{expenseId}: description, amount, paidBy, splitAmong[]
  messages/{messageId}: senderId, text, timestamp
```

## Epic 1: Setup and sign-in

### US-1.1 Project setup

As a developer, I want the Android project connected to Firebase, so that every later feature has a working backend.

- [ ] New Android project in Java, min SDK 26, package `com.yourname.consensus`
- [ ] Firebase BoM, Auth, Firestore, and Google Sign-In dependencies added in Gradle
- [ ] `google-services.json` is in `.gitignore`
- [ ] App builds and launches to a blank screen with no errors

### US-1.2 Sign in with Google

As a user, I want to sign in with my Google account, so that my trips are saved to me.

- [ ] Login screen shows the app name and a "Sign in with Google" button
- [ ] Successful sign-in opens the My Trips screen
- [ ] On first sign-in, a `users/{userId}` document is created with name, email, and photo URL
- [ ] A failed or cancelled sign-in shows a friendly message and stays on the login screen

### US-1.3 Stay signed in and sign out

As a user, I want to stay signed in between app launches and sign out when I choose, so that I don't log in every time.

- [ ] A signed-in user skips the login screen on launch
- [ ] A sign-out option in the menu returns the user to the login screen

## Epic 2: Trips

### US-2.1 Create a trip

As a trip organizer, I want to create a trip with a name, destination, and dates, so that my friends have something to join.

- [ ] "Create trip" form has name, destination, start date, and end date (date pickers)
- [ ] End date cannot be before start date; all fields are required
- [ ] Saving creates a `trips/{tripId}` document with `ownerId`, `memberIds` containing the creator, and `status: "collecting_preferences"`
- [ ] A unique 6-character invite code is generated (letters and numbers, no look-alikes like 0/O or 1/I)
- [ ] After saving, the app opens the new trip's overview

### US-2.2 Share an invite code

As a trip organizer, I want to share the invite code, so that friends can join easily.

- [ ] Trip overview shows the invite code with a "Copy" button and a "Share" button
- [ ] "Share" opens the Android share sheet with a message like "Join my trip to Chicago! Code: K9X4TP"

### US-2.3 Join a trip with a code

As a friend, I want to enter an invite code, so that I can join the group's trip.

- [ ] "Join trip" dialog accepts a code (case-insensitive)
- [ ] A valid code adds my user ID to the trip's `memberIds` and opens the trip overview
- [ ] An invalid code shows "No trip found with that code"
- [ ] Joining a trip I'm already in just opens it, without adding me twice

### US-2.4 See my trips

As a user, I want to see all trips I created or joined, so that I can switch between them.

- [ ] My Trips lists every trip where `memberIds` contains my user ID, soonest start date first
- [ ] Each row shows name, destination, and dates
- [ ] The list updates in real time when I'm added to a trip
- [ ] An empty state says "No trips yet" with Create and Join buttons

### US-2.5 Trip overview

As a trip member, I want an overview of the trip, so that I can see who's in and what's next.

- [ ] Shows destination, dates, and the member list with names and photos
- [ ] Each member has a check mark once they've submitted preferences
- [ ] Buttons lead to: My Preferences, Itinerary, Expenses, and Chat (later epics can show these as "coming soon")

### US-2.6 Leave or delete a trip

As a member, I want to leave a trip, and as the owner, I want to delete it, so that my list stays clean.

- [ ] Members can leave, which removes them from `memberIds`
- [ ] Only the owner sees "Delete trip", with a confirmation dialog

## Epic 3: Preference survey

### US-3.1 Fill out my preferences

As a trip member, I want to tell the app what I like, so that the itinerary fits me too.

- [ ] Budget: choice of Low, Medium, or High per day
- [ ] Interests: multi-select chips (Food, Museums, History, Nightlife, Outdoors, Shopping, Art, Sports, Relaxing)
- [ ] Pace: Relaxed, Balanced, or Packed
- [ ] Free-text fields for "Must-dos" and "Things to avoid" (dietary needs, mobility, etc.)
- [ ] Saving writes to `trips/{tripId}/preferences/{userId}` and returns to the overview

### US-3.2 Edit my preferences

As a trip member, I want to change my answers later, so that I can update my plans.

- [ ] Opening the survey again pre-fills my saved answers
- [ ] Saving overwrites my previous preferences

### US-3.3 See who's done

As a trip organizer, I want to see who has submitted preferences, so that I know when to generate the plan.

- [ ] Overview shows "3 of 5 ready" and updates in real time
- [ ] Members who haven't submitted are clearly marked

## Epic 4: Itinerary with live sync

Build this before the AI epic so the data model and real-time updates are proven first.

### US-4.1 View the itinerary by day

As a trip member, I want to see the plan day by day, so that I know what's happening when.

- [ ] Itinerary screen has a tab per day of the trip ("Day 1 · Fri Oct 9")
- [ ] Each day lists activity cards sorted by time, showing time, title, short description, and estimated cost
- [ ] Day documents are created automatically from the trip's start and end dates
- [ ] An empty day says "Nothing planned yet"

### US-4.2 Add an activity by hand

As a trip member, I want to add an activity myself, so that we can plan even without AI.

- [ ] "+" button opens a form: title, time, description, estimated cost
- [ ] Saving adds it under the selected day

### US-4.3 Edit and delete activities

As a trip member, I want to fix or remove an activity, so that the plan stays accurate.

- [ ] Tapping a card opens it for editing
- [ ] Delete asks for confirmation

### US-4.4 Real-time updates

As a trip member, I want changes from friends to appear instantly, so that we're all looking at the same plan.

- [ ] Itinerary uses Firestore snapshot listeners, not one-time reads
- [ ] An activity added on one phone appears on another within a few seconds without refreshing
- [ ] Listeners are removed when the screen closes, to avoid leaks and extra reads

## Epic 5: AI itinerary generation

### US-5.1 Generate the itinerary

As a trip organizer, I want the AI to draft a plan from everyone's preferences, so that we don't have to plan from scratch.

- [ ] "Generate itinerary" button on the overview, visible only to the owner
- [ ] Button is enabled once at least one member has submitted preferences; if some haven't, a dialog asks "2 people haven't answered yet. Generate anyway?"
- [ ] The app reads all preferences, builds a prompt with destination, dates, group size, and each member's answers, and calls Gemini through Firebase AI Logic
- [ ] The request uses structured output with a JSON schema: a list of days, each with activities containing `title`, `time` (HH:mm), `description`, `estimatedCost` (number), and `placeName`
- [ ] A loading state shows while generating ("Planning your trip…")
- [ ] Results are written to Firestore in a single batch write, and the trip `status` becomes `"planned"`
- [ ] Every member sees the new itinerary appear in real time

### US-5.2 Handle AI errors

As a user, I want a clear message if generation fails, so that I'm not stuck.

- [ ] Malformed or incomplete JSON is caught, nothing partial is saved, and the user sees "Couldn't generate a plan. Try again."
- [ ] Network errors and timeouts show the same retry option
- [ ] Errors are logged with enough detail to debug

### US-5.3 Regenerate

As a trip organizer, I want to regenerate the plan, so that I can get a better one.

- [ ] "Regenerate" warns that the current AI-generated activities will be replaced
- [ ] Activities members added by hand are kept (store a `source: "ai" | "manual"` field on each activity)

### US-5.4 Explain the balance

As a trip member, I want to know why the plan looks the way it does, so that the group feels it's fair.

- [ ] The AI also returns a short note per day, such as "Museum morning for Sam, food tour for Jordan"
- [ ] The note appears at the top of each day's tab

## Epic 6: Voting and AI replacements

### US-6.1 Vote on activities

As a trip member, I want to vote up or down on each activity, so that the group can show what it likes.

- [ ] Each card has thumbs-up and thumbs-down buttons and shows the net score
- [ ] My vote is stored as `votes.{userId}: 1 or -1`; tapping the same button again removes it
- [ ] Each person gets one vote per activity
- [ ] Scores update live for everyone

### US-6.2 Flag unpopular activities

As a trip organizer, I want unpopular activities highlighted, so that I know what to change.

- [ ] Activities where more than half the members voted down show a "Needs a swap" badge

### US-6.3 Ask AI for alternatives

As a trip member, I want to ask the AI for replacement options, so that we can swap out activities we don't like.

- [ ] "Find alternatives" on a card sends the activity, its day's other activities, and the group's preferences to Gemini
- [ ] The AI returns 3 options in the same JSON shape as US-5.1
- [ ] A bottom sheet shows the options; picking one replaces the activity and clears its votes
- [ ] Cancelling changes nothing

## Epic 7: Google Maps

### US-7.1 Maps setup

As a developer, I want Maps and Places configured safely, so that the app can show real locations.

- [ ] Maps SDK for Android and Places SDK dependencies added
- [ ] API key loaded from `local.properties` through the Secrets Gradle Plugin, not hard-coded
- [ ] README notes that the key must be restricted to the app's package name and SHA-1 in Google Cloud Console

### US-7.2 Pick the destination with autocomplete

As a trip organizer, I want destination autocomplete, so that the destination is a real place.

- [ ] Destination field in Create Trip uses Places autocomplete (cities and regions)
- [ ] The selected place's ID and coordinates are saved on the trip

### US-7.3 Match AI suggestions to real places

As a trip member, I want each activity linked to a real place, so that addresses and map pins are accurate.

- [ ] After generation, each activity's `placeName` is looked up with Places text search, biased to the destination
- [ ] A match saves `placeId`, `address`, and `location` as a Firestore GeoPoint
- [ ] No match shows a small "Location not found" note on the card; the activity is kept
- [ ] Place details are saved once, not looked up again on every screen load

### US-7.4 Map of the day

As a trip member, I want to see the day's activities on a map, so that I understand the route.

- [ ] A List/Map toggle on each day's tab
- [ ] Map shows numbered pins in time order, connected by a line
- [ ] Tapping a pin shows the activity title and time
- [ ] Camera zooms to fit all pins

### US-7.5 Get directions

As a trip member, I want directions to an activity, so that I can get there.

- [ ] "Directions" on an activity opens the Google Maps app to that place

## Epic 8: Expenses, chat, and notifications

### US-8.1 Log an expense

As a trip member, I want to log what I paid for, so that costs get split fairly.

- [ ] Form: description, amount, who paid (defaults to me), and who it's split among (defaults to everyone)
- [ ] Amounts stored in cents as integers to avoid rounding errors
- [ ] Expenses list shows newest first, with a running total

### US-8.2 Settle up

As a trip member, I want to see who owes whom, so that we can pay each other back.

- [ ] Settle-up screen shows each person's balance (paid minus share)
- [ ] It suggests the fewest payments needed, such as "Alex pays Sam $42.50"
- [ ] The settle-up calculation is in its own class with unit tests

### US-8.3 Group chat

As a trip member, I want to chat with the group inside the trip, so that planning talk stays in one place.

- [ ] Messages show sender name, photo, text, and time, oldest at top, newest at bottom
- [ ] New messages appear live and the list scrolls to the newest
- [ ] Loads the latest 50 messages, with older ones loading on scroll up

### US-8.4 Notifications

As a trip member, I want notifications for big moments, so that I don't miss anything.

- [ ] Notify members when someone joins the trip and when the itinerary is generated
- [ ] Uses Firebase Cloud Messaging; device tokens saved on `users/{userId}`
- [ ] App asks for notification permission on Android 13+
- [ ] Note: sending notifications needs a server or Cloud Functions (Node.js or Python), so this story can be done last

## Requirements for every epic

### US-9.1 Firestore security rules

As a user, I want my trips visible only to members, so that strangers can't read or change them.

- [ ] Users can read and write only their own `users/{userId}` document
- [ ] Only users in a trip's `memberIds` can read the trip and its subcollections
- [ ] Only the owner can delete a trip or change its name, dates, or destination
- [ ] A member can write only their own preferences document and only their own entry in `votes`
- [ ] Joining is allowed only when a user adds exactly their own ID to `memberIds`
- [ ] Rules are saved in `firestore.rules` in the repo and tested in the Firebase Emulator

### General rules for Claude Code

- Keep Firebase and AI calls in repository classes, not in Activities or Fragments
- Show a loading state for every network call and a friendly message for every failure
- Never commit API keys, `google-services.json`, or `local.properties`
- Write unit tests for logic that doesn't need Android (invite codes, settle-up math, JSON parsing)
- After finishing each story, summarize what changed and how to test it manually
