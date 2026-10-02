package com.abhinavpinisetti.consensus.ui.itinerary;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import com.abhinavpinisetti.consensus.data.model.PlannedActivity;

import java.util.Locale;

/** Opens Google Maps with directions to an activity (falls back to the browser). */
final class Directions {

    private static final String MAPS_PACKAGE = "com.google.android.apps.maps";

    private Directions() {}

    static void open(Context context, PlannedActivity activity, String tripDestination) {
        String name = activity.placeName != null ? activity.placeName : activity.title;
        Uri.Builder uri = Uri.parse("https://www.google.com/maps/dir/").buildUpon()
                .appendQueryParameter("api", "1");
        if (activity.placeId != null) {
            uri.appendQueryParameter("destination", name);
            uri.appendQueryParameter("destination_place_id", activity.placeId);
        } else if (activity.location != null) {
            uri.appendQueryParameter("destination", String.format(Locale.US, "%f,%f",
                    activity.location.getLatitude(), activity.location.getLongitude()));
        } else {
            uri.appendQueryParameter("destination", name + ", " + tripDestination);
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, uri.build()).setPackage(MAPS_PACKAGE);
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            try {
                context.startActivity(intent.setPackage(null));
            } catch (ActivityNotFoundException e2) {
                Toast.makeText(context, "No app can open maps", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
