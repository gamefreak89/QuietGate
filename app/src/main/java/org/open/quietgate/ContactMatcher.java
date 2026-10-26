package org.open.quietgate;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

final class ContactMatcher {
    private ContactMatcher() {}

    static boolean isSavedContact(Context context, String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) return false;
        String value = identifier.trim();
        ContentResolver resolver = context.getContentResolver();
        boolean deviceMatch = value.contains("@") ? emailExists(resolver, value) : phoneExists(resolver, value);
        if (deviceMatch) return true;
        // Each edition supplies its own fallback. The FOSS build has no account integration.
        return Edition.matches(context, value);
    }

    private static boolean emailExists(ContentResolver resolver, String email) {
        Uri uri = Uri.withAppendedPath(ContactsContract.CommonDataKinds.Email.CONTENT_FILTER_URI,
                Uri.encode(email));
        try (Cursor c = resolver.query(uri,
                new String[]{ContactsContract.CommonDataKinds.Email.ADDRESS}, null, null, null)) {
            if (c == null) return false;
            String target = email.trim();
            while (c.moveToNext()) {
                String found = c.getString(0);
                if (found != null && found.trim().equalsIgnoreCase(target)) return true;
            }
        } catch (SecurityException ignored) { }
        return false;
    }

    private static boolean phoneExists(ContentResolver resolver, String phone) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phone));
        try (Cursor c = resolver.query(uri,
                new String[]{ContactsContract.PhoneLookup.NUMBER}, null, null, null)) {
            if (c == null) return false;
            while (c.moveToNext()) {
                String found = c.getString(0);
                if (found != null && normalizedDigits(phone).equals(normalizedDigits(found))) return true;
            }
        } catch (SecurityException ignored) { }
        return false;
    }

    private static String normalizedDigits(String value) {
        StringBuilder digits = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isDigit(c)) digits.append(c);
        }
        return digits.toString();
    }
}
