package com.example.fuel_split;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.List;

public final class ContactStore {

    public static class Contact {
        public String code;
        public String displayName;
        public String address; // lowercase

        public Contact(String code, String displayName, String address) {
            this.code        = code;
            this.displayName = displayName;
            this.address     = address != null ? address.toLowerCase() : null;
        }
    }

    private static final String PREFS  = "FuelSplitContacts";
    private static final String KEY    = "contacts";

    private ContactStore() {}

    public static List<Contact> loadContacts(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY, "[]");
        List<Contact> list = new Gson().fromJson(json,
                new TypeToken<List<Contact>>(){}.getType());
        return list != null ? list : new ArrayList<>();
    }

    public static void saveContacts(Context ctx, List<Contact> contacts) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
           .edit().putString(KEY, new Gson().toJson(contacts)).apply();
    }

    /**
     * Add or update a contact by address (de-dup). Safe to call from any thread.
     * Also seeds NameResolver so names appear immediately in the UI.
     */
    public static void addContact(Context ctx, String code, String displayName, String address) {
        if (address == null || address.isEmpty()) return;
        String lowerAddr = address.toLowerCase();
        if (displayName != null && !displayName.isEmpty())
            NameResolver.seed(lowerAddr, displayName);

        List<Contact> contacts = loadContacts(ctx);
        for (Contact c : contacts) {
            if (lowerAddr.equals(c.address)) {
                c.displayName = displayName;
                c.code        = code;
                saveContacts(ctx, contacts);
                return;
            }
        }
        contacts.add(0, new Contact(code, displayName, lowerAddr));
        saveContacts(ctx, contacts);
    }

    /**
     * Seed NameResolver cache from all saved contacts.
     * Call this early (e.g. Activity.onCreate) so names appear without network.
     */
    public static void seedNameResolver(Context ctx) {
        for (Contact c : loadContacts(ctx)) {
            if (c.address != null && c.displayName != null && !c.displayName.isEmpty())
                NameResolver.seed(c.address, c.displayName);
        }
    }
}
