package com.example.hobbycircle.data.remote;

/**
 * Canonical Firestore field names — keep reads/writes in sync with POJOs and Room entities.
 */
public final class FirestoreFields {

    private FirestoreFields() {
    }

    public static final String COLLECTION_USERS = "users";
    public static final String COLLECTION_EVENTS = "events";

    // User document fields
    public static final String USER_ID = "id";
    public static final String USER_NAME = "name";
    public static final String USER_EMAIL = "email";
    public static final String USER_ROLE = "role";
    public static final String USER_LOCATION = "location";
    public static final String USER_PHOTO_URL = "photoUrl";
    public static final String USER_SELECTED_HOBBIES = "selectedHobbies";
    public static final String USER_PROVIDER = "provider";
    public static final String USER_UPDATED_AT = "updatedAt";

    // Event document fields
    public static final String EVENT_ID = "id";
    public static final String EVENT_TITLE = "title";
    public static final String EVENT_DESCRIPTION = "description";
    public static final String EVENT_HOBBY_ID = "hobbyId";
    public static final String EVENT_LOCATION = "location";
    public static final String EVENT_MAP_QUERY = "mapQuery";
    public static final String EVENT_TIME_MILLIS = "eventTimeMillis";
    public static final String EVENT_CREATED_BY = "createdByUserId";
    public static final String EVENT_JOINED_USER_IDS = "joinedUserIds";
    public static final String EVENT_IMAGE_URL = "imageUrl";
    public static final String EVENT_UPDATED_AT = "updatedAt";
}
