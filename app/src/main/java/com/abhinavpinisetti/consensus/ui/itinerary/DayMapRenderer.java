package com.abhinavpinisetti.consensus.ui.itinerary;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.view.View;

import com.abhinavpinisetti.consensus.data.model.PlannedActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

/** Draws a day's activities as numbered pins in time order, joined by a line. */
final class DayMapRenderer {

    private DayMapRenderer() {}

    /** @return the number of pins drawn */
    static int render(Context context, GoogleMap map, View mapView, List<PlannedActivity> activities, int color) {
        map.clear();
        float density = context.getResources().getDisplayMetrics().density;
        List<LatLng> points = new ArrayList<>();

        for (PlannedActivity a : activities) { // already sorted by time
            if (a.location == null) continue;
            LatLng position = new LatLng(a.location.getLatitude(), a.location.getLongitude());
            points.add(position);
            int number = points.size();
            map.addMarker(new MarkerOptions()
                    .position(position)
                    .title(number + ". " + a.title)
                    .snippet(a.time)
                    .anchor(0.5f, 0.5f)
                    .icon(BitmapDescriptorFactory.fromBitmap(numberedPin(number, color, density))));
        }

        if (points.size() > 1) {
            map.addPolyline(new PolylineOptions().addAll(points).color(color).width(4 * density));
        }
        if (points.size() == 1) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(points.get(0), 14f));
        } else if (points.size() > 1) {
            LatLngBounds.Builder bounds = new LatLngBounds.Builder();
            for (LatLng p : points) bounds.include(p);
            int padding = (int) (48 * density);
            if (mapView != null && mapView.getWidth() > 0 && mapView.getHeight() > 0) {
                map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(),
                        mapView.getWidth(), mapView.getHeight(), padding));
            } else {
                map.setOnMapLoadedCallback(() ->
                        map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), padding)));
            }
        }
        return points.size();
    }

    private static Bitmap numberedPin(int number, int color, float density) {
        int size = (int) (32 * density);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        float r = size / 2f;

        Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        fill.setColor(Color.WHITE);
        canvas.drawCircle(r, r, r, fill);
        fill.setColor(color);
        canvas.drawCircle(r, r, r - 2 * density, fill);

        Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        text.setColor(Color.WHITE);
        text.setTextSize(14 * density);
        text.setTypeface(Typeface.DEFAULT_BOLD);
        text.setTextAlign(Paint.Align.CENTER);
        String label = String.valueOf(number);
        Rect textBounds = new Rect();
        text.getTextBounds(label, 0, label.length(), textBounds);
        canvas.drawText(label, r, r + textBounds.height() / 2f, text);
        return bitmap;
    }
}
