package com.callaplace.call_a_place;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.provider.Settings;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class LocationReceiver extends BroadcastReceiver {

    private static final String TAG = "CAP/LocationReceiver";
    private static final Gson sGson = new GsonBuilder()
            .registerTypeAdapter(Location.class, new LocationUtil.Serializer())
            .create();

    private static boolean sLocationAvailable;
    private static Location sLastKnownLocation;

    public static PendingIntent createRequestIntent(Context context) {
        Intent intent = new Intent(context, LocationReceiver.class);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    public static LocationRequest createDefaultRequest() {
        return LocationRequest.create()
                .setInterval(300000)
                .setFastestInterval(10000);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (LocationAvailability.hasLocationAvailability(intent)) {
            final LocationAvailability availability = LocationAvailability.extractLocationAvailability(intent);
            sLocationAvailable = availability.isLocationAvailable();
            Log.d(TAG, "LocationAvailability: "+ Boolean.toString(sLocationAvailable));
        }
        if (LocationResult.hasResult(intent)) {
            final LocationResult result = LocationResult.extractResult(intent);
            sLastKnownLocation = result.getLastLocation();
            Log.d(TAG, "LocationResult: "+ sLastKnownLocation.toString());
        }
        if (sLocationAvailable && sLastKnownLocation != null) {
            final RequestQueue requestQueue = Volley.newRequestQueue(context);
            final String id = Settings.System.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            requestQueue.add(new LocationUpdate(context, id, sLastKnownLocation));
        }
    }

    private static class LocationUpdate extends GsonRequest<LocationUpdate.Body, Void> {
        private static final String LOCATION = "location";

        public LocationUpdate(Context context, String id, Location loc) {
            super(Method.POST, Url.get(context, LOCATION), sGson, new LocationUpdate.Body(id, loc), Void.TYPE);

            Log.d(TAG, "Sending location update: " + sGson.toJson(loc));
        }
        static class Body {
            private final String id;
            private final Location loc;
            Body(String id, Location loc) {
                this.id = id;
                this.loc = loc;
            }
        }
    }
}
