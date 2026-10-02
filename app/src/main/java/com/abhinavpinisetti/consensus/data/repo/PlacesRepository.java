package com.abhinavpinisetti.consensus.data.repo;

import android.content.Context;
import android.util.Log;

import com.abhinavpinisetti.consensus.BuildConfig;
import com.abhinavpinisetti.consensus.data.model.Trip;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.CircularBounds;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.api.net.SearchByTextRequest;
import com.google.firebase.firestore.GeoPoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Matches AI place names to real places with Places text search. Results (or a "not found"
 * marker) are saved on the activity, so each activity is only ever looked up once.
 */
public class PlacesRepository {

    private static final String TAG = "PlacesRepository";
    private static final double BIAS_RADIUS_METERS = 30_000;
    private static final List<Place.Field> FIELDS = Arrays.asList(
            Place.Field.ID, Place.Field.DISPLAY_NAME, Place.Field.FORMATTED_ADDRESS, Place.Field.LOCATION);
    private static PlacesRepository instance;

    private PlacesClient client;
    /** Activities with a lookup in flight on this device, to avoid duplicate requests. */
    private final Set<String> inFlight = Collections.synchronizedSet(new HashSet<>());

    public static synchronized PlacesRepository get() {
        if (instance == null) instance = new PlacesRepository();
        return instance;
    }

    /** Initializes Places if a real API key was provided through local.properties. */
    public void init(Context context) {
        String key = BuildConfig.MAPS_API_KEY;
        if (key == null || key.isEmpty() || "DEFAULT_API_KEY".equals(key)) {
            Log.w(TAG, "MAPS_API_KEY not set in local.properties; Places features are disabled");
            return;
        }
        Places.initializeWithNewPlacesApiEnabled(context.getApplicationContext(), key);
        client = Places.createClient(context.getApplicationContext());
    }

    public boolean isAvailable() {
        return client != null;
    }

    /** Looks up every activity in {@code refs} and saves the matches. Never fails the caller. */
    public Task<Void> matchActivities(Trip trip, List<ItineraryRepository.ActivityRef> refs) {
        if (!isAvailable() || refs.isEmpty()) return Tasks.forResult(null);
        List<Task<Void>> tasks = new ArrayList<>();
        for (ItineraryRepository.ActivityRef ref : refs) {
            tasks.add(matchOne(trip, ref));
        }
        return Tasks.whenAll(tasks);
    }

    private Task<Void> matchOne(Trip trip, ItineraryRepository.ActivityRef ref) {
        String key = trip.id + "/" + ref.dayId + "/" + ref.activityId;
        if (ref.placeName == null || ref.placeName.trim().isEmpty() || !inFlight.add(key)) {
            return Tasks.forResult(null);
        }

        SearchByTextRequest.Builder request = SearchByTextRequest
                .builder(ref.placeName + ", " + trip.destination, FIELDS)
                .setMaxResultCount(1);
        if (trip.hasDestinationLocation()) {
            request.setLocationBias(CircularBounds.newInstance(
                    new LatLng(trip.destinationLat, trip.destinationLng), BIAS_RADIUS_METERS));
        }

        return client.searchByText(request.build()).continueWithTask(task -> {
            if (!task.isSuccessful()) {
                // Leave placeLookupDone false so a later visit can retry (e.g. after being offline).
                Log.w(TAG, "Places search failed for '" + ref.placeName + "'", task.getException());
                inFlight.remove(key);
                return Tasks.forResult(null);
            }
            Map<String, Object> fields = new HashMap<>();
            fields.put("placeLookupDone", true);
            List<Place> places = task.getResult().getPlaces();
            if (!places.isEmpty() && places.get(0).getLocation() != null) {
                Place place = places.get(0);
                fields.put("placeId", place.getId());
                fields.put("address", place.getFormattedAddress());
                fields.put("location", new GeoPoint(place.getLocation().latitude, place.getLocation().longitude));
            }
            return ItineraryRepository.get().savePlaceMatch(trip.id, ref, fields)
                    .continueWith(save -> {
                        inFlight.remove(key);
                        if (!save.isSuccessful()) Log.w(TAG, "Could not save place match", save.getException());
                        return null;
                    });
        });
    }
}
