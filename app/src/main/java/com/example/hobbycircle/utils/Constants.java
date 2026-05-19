package com.example.hobbycircle.utils;

public final class Constants {

    private Constants() {
    }

    public static final String PREF_FILE_NAME = "hobby_circle_prefs";
    public static final String PREF_USER_ID = "pref_user_id";
    public static final String PREF_USER_NAME = "pref_user_name";
    public static final String PREF_USER_EMAIL = "pref_user_email";
    public static final String PREF_USER_HOBBIES = "pref_user_hobbies_csv";
    public static final String PREF_USER_LOCATION = "pref_user_location"; // added
    public static final String PREF_USER_ROLE = "pref_user_role";

    public static final String ROLE_USER = "user";
    public static final String ROLE_ADMIN = "admin";

    public static final String EXTRA_EVENT_ID = "extra_event_id";
    public static final String EXTRA_EDIT_EVENT = "extra_edit_event";
    public static final String EXTRA_EVENT_TITLE = "extra_event_title";
    public static final String EXTRA_EVENT_TIME = "extra_event_time";
    public static final String EXTRA_EVENT_LOCATION = "extra_event_location";

    public static final String NOTIFICATION_CHANNEL_ID = "hobby_circle_reminders";
    public static final String NOTIFICATION_CHANNEL_NAME = "HobbyCircle Reminders";
    public static final int NOTIFICATION_ID_EVENT_REMINDER = 1001;

    public static final String ACTION_EVENT_REMINDER = "com.example.hobbycircle.ACTION_EVENT_REMINDER";
}