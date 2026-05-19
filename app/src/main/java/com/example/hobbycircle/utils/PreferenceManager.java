package com.example.hobbycircle.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PreferenceManager {

    private final SharedPreferences sharedPreferences;

    public PreferenceManager(Context context) {
        this.sharedPreferences = context.getApplicationContext()
                .getSharedPreferences(Constants.PREF_FILE_NAME, Context.MODE_PRIVATE);
    }

    public void saveUserId(String userId) {
        sharedPreferences.edit().putString(Constants.PREF_USER_ID, safe(userId)).apply();
    }

    public String getUserId() {
        return safe(sharedPreferences.getString(Constants.PREF_USER_ID, ""));
    }

    public void saveUserName(String userName) {
        sharedPreferences.edit().putString(Constants.PREF_USER_NAME, safe(userName)).apply();
    }

    public String getUserName() {
        return safe(sharedPreferences.getString(Constants.PREF_USER_NAME, ""));
    }

    public void saveUserEmail(String email) {
        sharedPreferences.edit().putString(Constants.PREF_USER_EMAIL, safe(email)).apply();
    }

    public String getUserEmail() {
        return safe(sharedPreferences.getString(Constants.PREF_USER_EMAIL, ""));
    }

    public void saveUserRole(String role) {
        sharedPreferences.edit()
                .putString(Constants.PREF_USER_ROLE, safe(role))
                .apply();
    }

    public String getUserRole() {
        return safe(sharedPreferences.getString(Constants.PREF_USER_ROLE, Constants.ROLE_USER));
    }

    public boolean isAdmin() {
        return Constants.ROLE_ADMIN.equals(getUserRole());
    }

    // added
    public void saveUserLocation(String location) {
        sharedPreferences.edit().putString(Constants.PREF_USER_LOCATION, safe(location)).apply();
    }

    // added
    public String getUserLocation() {
        return safe(sharedPreferences.getString(Constants.PREF_USER_LOCATION, ""));
    }

    public void saveSelectedHobbies(List<String> hobbies) {
        String csv = toCsv(hobbies);
        sharedPreferences.edit().putString(Constants.PREF_USER_HOBBIES, csv).apply();
    }

    public List<String> getSelectedHobbies() {
        String csv = sharedPreferences.getString(Constants.PREF_USER_HOBBIES, "");
        if (csv == null || csv.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<String> values = new ArrayList<>(Arrays.asList(csv.split(",")));
        List<String> cleaned = new ArrayList<>();
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                cleaned.add(value.trim());
            }
        }
        return cleaned;
    }

    public void clearAll() {
        sharedPreferences.edit().clear().apply();
    }

    private String toCsv(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String value : values) {
            if (value == null || value.trim().isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(value.trim());
        }
        return sb.toString();
    }

    private String safe(String value) {
        return value != null ? value : "";
    }
}