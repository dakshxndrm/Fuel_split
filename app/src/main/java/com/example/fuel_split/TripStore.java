package com.example.fuel_split;

import android.content.Context;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TripStore {

    private static final String PREFS = "FuelSplitTrips";
    private static final String KEY   = "list";

    public static List<Trip> loadTrips(Context ctx) {
        String json = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY, null);
        List<Trip> list = new Gson().fromJson(json,
                new TypeToken<ArrayList<Trip>>() {}.getType());
        if (list == null) return new ArrayList<>();
        // Sort newest first by timestamp
        Collections.sort(list, (a, b) -> Long.compare(b.getTimestampMillis(), a.getTimestampMillis()));
        return list;
    }

    public static void saveTrips(Context ctx, List<Trip> trips) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY, new Gson().toJson(trips))
                .apply();
    }

    public static void addTrip(Context ctx, Trip trip) {
        List<Trip> trips = loadTrips(ctx);
        trips.add(0, trip);
        saveTrips(ctx, trips);
    }

    public static void updateTrip(Context ctx, Trip updated) {
        List<Trip> trips = loadTrips(ctx);
        for (int i = 0; i < trips.size(); i++) {
            Trip t = trips.get(i);
            if (t.getTimestampMillis() == updated.getTimestampMillis()
                    && t.getTripName().equals(updated.getTripName())) {
                trips.set(i, updated);
                break;
            }
        }
        saveTrips(ctx, trips);
    }
}
