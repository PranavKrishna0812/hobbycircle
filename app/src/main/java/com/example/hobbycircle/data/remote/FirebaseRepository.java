package com.example.hobbycircle.data.remote;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import androidx.annotation.NonNull;

import com.example.hobbycircle.data.model.Event;
import com.example.hobbycircle.data.model.User;
import com.example.hobbycircle.utils.UserRoleUtil;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FirebaseRepository {

    private final FirebaseFirestore firestore;
    private final Context appContext;

    public interface RepositoryCallback<T> {
        void onSuccess(T data);
        void onError(String message);
    }

    public FirebaseRepository(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        this.firestore = FirebaseFirestore.getInstance();
    }

    public void saveUserProfile(@NonNull User user, @NonNull RepositoryCallback<Void> callback) {
        if (!isInternetAvailable()) {
            callback.onError("No internet connection. Please try again.");
            return;
        }

        try {
            if (user.getId().trim().isEmpty()) {
                callback.onError("User ID is required.");
                return;
            }

            Map<String, Object> payload = mapUserToFirestore(user);
            payload.put(FirestoreFields.USER_UPDATED_AT, System.currentTimeMillis());

            firestore.collection(FirestoreFields.COLLECTION_USERS)
                    .document(user.getId())
                    .set(payload, SetOptions.merge())
                    .addOnSuccessListener(unused -> callback.onSuccess(null))
                    .addOnFailureListener(e -> callback.onError("Failed to save profile: " + safe(e.getMessage())));
        } catch (Exception e) {
            callback.onError("Unexpected error while saving profile: " + safe(e.getMessage()));
        }
    }

    public void getUserProfile(@NonNull String userId, @NonNull RepositoryCallback<User> callback) {
        if (!isInternetAvailable()) {
            callback.onError("No internet connection. Please try again.");
            return;
        }

        try {
            if (userId.trim().isEmpty()) {
                callback.onError("User ID cannot be empty.");
                return;
            }

            firestore.collection(FirestoreFields.COLLECTION_USERS)
                    .document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            callback.onSuccess(mapUser(documentSnapshot));
                        } else {
                            callback.onError("User profile not found.");
                        }
                    })
                    .addOnFailureListener(e -> callback.onError("Failed to fetch profile: " + safe(e.getMessage())));
        } catch (Exception e) {
            callback.onError("Unexpected error while fetching profile: " + safe(e.getMessage()));
        }
    }

    public void createEvent(@NonNull Event event, @NonNull RepositoryCallback<Event> callback) {
        if (!isInternetAvailable()) {
            callback.onError("No internet connection. Please try again.");
            return;
        }

        try {
            String eventId = event.getId();
            if (eventId.trim().isEmpty()) {
                eventId = UUID.randomUUID().toString();
                event.setId(eventId);
            }
            event.setUpdatedAtMillis(System.currentTimeMillis());

            firestore.collection(FirestoreFields.COLLECTION_EVENTS)
                    .document(eventId)
                    .set(mapEventToFirestore(event))
                    .addOnSuccessListener(unused -> callback.onSuccess(event))
                    .addOnFailureListener(e -> callback.onError("Failed to create event: " + safe(e.getMessage())));
        } catch (Exception e) {
            callback.onError("Unexpected error while creating event: " + safe(e.getMessage()));
        }
    }

    public void updateEvent(@NonNull Event event, @NonNull RepositoryCallback<Event> callback) {
        if (!isInternetAvailable()) {
            callback.onError("No internet connection. Please try again.");
            return;
        }

        try {
            String eventId = safe(event.getId());
            if (eventId.isEmpty()) {
                callback.onError("Event ID is required.");
                return;
            }
            event.setUpdatedAtMillis(System.currentTimeMillis());

            firestore.collection(FirestoreFields.COLLECTION_EVENTS)
                    .document(eventId)
                    .set(mapEventToFirestore(event))
                    .addOnSuccessListener(unused -> callback.onSuccess(event))
                    .addOnFailureListener(e -> callback.onError("Failed to update event: " + safe(e.getMessage())));
        } catch (Exception e) {
            callback.onError("Unexpected error while updating event: " + safe(e.getMessage()));
        }
    }

    public void patchEventImageUrl(@NonNull String eventId, @NonNull String imageUrl,
                                   @NonNull RepositoryCallback<String> callback) {
        if (!isInternetAvailable()) {
            callback.onError("No internet connection.");
            return;
        }
        if (eventId.trim().isEmpty()) {
            callback.onError("Event ID is required.");
            return;
        }

        Map<String, Object> patch = new HashMap<>();
        patch.put(FirestoreFields.EVENT_IMAGE_URL, safe(imageUrl));
        patch.put(FirestoreFields.EVENT_UPDATED_AT, System.currentTimeMillis());

        firestore.collection(FirestoreFields.COLLECTION_EVENTS)
                .document(eventId)
                .set(patch, SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onSuccess(imageUrl))
                .addOnFailureListener(e -> callback.onError("Failed to save image URL: " + safe(e.getMessage())));
    }

    public void deleteEvent(@NonNull String eventId, @NonNull RepositoryCallback<Void> callback) {
        if (!isInternetAvailable()) {
            callback.onError("No internet connection. Please try again.");
            return;
        }

        try {
            if (eventId.trim().isEmpty()) {
                callback.onError("Event ID is required.");
                return;
            }

            firestore.collection(FirestoreFields.COLLECTION_EVENTS)
                    .document(eventId)
                    .delete()
                    .addOnSuccessListener(unused -> callback.onSuccess(null))
                    .addOnFailureListener(e -> callback.onError("Failed to delete event: " + safe(e.getMessage())));
        } catch (Exception e) {
            callback.onError("Unexpected error while deleting event: " + safe(e.getMessage()));
        }
    }

    /**
     * Uploads event cover to {@link StoragePaths#eventCover(String)} and returns the download URL.
     */
    public void uploadEventCover(@NonNull String eventId, @NonNull byte[] imageBytes,
                                 @NonNull RepositoryCallback<String> callback) {
        if (!isInternetAvailable()) {
            callback.onError("No internet connection.");
            return;
        }
        if (eventId.trim().isEmpty()) {
            callback.onError("Invalid event ID for image upload.");
            return;
        }

        try {
            StorageReference imageRef = FirebaseStorage.getInstance()
                    .getReference()
                    .child(StoragePaths.eventCover(eventId));

            imageRef.putBytes(imageBytes)
                    .continueWithTask(task -> {
                        if (!task.isSuccessful()) {
                            Exception e = task.getException();
                            throw e != null ? e : new Exception("Upload failed without exception.");
                        }
                        return imageRef.getDownloadUrl();
                    })
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            String url = task.getResult().toString();
                            patchEventImageUrl(eventId, url, callback);
                        } else {
                            // Fallback to appspot.com bucket if the first one fails
                            try {
                                String projectId = FirebaseStorage.getInstance().getApp().getOptions().getProjectId();
                                String fallbackBucket = "gs://" + (projectId != null ? projectId : "hobbycircle-49094") + ".appspot.com";
                                StorageReference fallbackRef = FirebaseStorage.getInstance(fallbackBucket)
                                        .getReference()
                                        .child(StoragePaths.eventCover(eventId));

                                fallbackRef.putBytes(imageBytes)
                                        .continueWithTask(fallbackTask -> {
                                            if (!fallbackTask.isSuccessful()) {
                                                Exception e = fallbackTask.getException();
                                                throw e != null ? e : new Exception("Fallback upload failed.");
                                            }
                                            return fallbackRef.getDownloadUrl();
                                        })
                                        .addOnCompleteListener(fallbackTask -> {
                                            if (fallbackTask.isSuccessful() && fallbackTask.getResult() != null) {
                                                String url = fallbackTask.getResult().toString();
                                                patchEventImageUrl(eventId, url, callback);
                                            } else {
                                                String msg = fallbackTask.getException() != null
                                                        ? fallbackTask.getException().getMessage()
                                                        : "Unknown upload error";
                                                callback.onError("Upload failed: " + msg);
                                            }
                                        });
                            } catch (Exception ex) {
                                String msg = task.getException() != null
                                        ? task.getException().getMessage()
                                        : "Unknown upload error";
                                callback.onError("Upload failed: " + msg);
                            }
                        }
                    });
        } catch (Exception e) {
            callback.onError("Unexpected error: " + safe(e.getMessage()));
        }
    }

    public void fetchAllEvents(@NonNull RepositoryCallback<List<Event>> callback) {
        if (!isInternetAvailable()) {
            callback.onError("No internet connection. Showing offline data if available.");
            return;
        }

        try {
            firestore.collection(FirestoreFields.COLLECTION_EVENTS)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        List<Event> events = new ArrayList<>();
                        if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                            for (DocumentSnapshot snapshot : queryDocumentSnapshots.getDocuments()) {
                                events.add(mapEvent(snapshot));
                            }
                        }
                        callback.onSuccess(events);
                    })
                    .addOnFailureListener(e -> callback.onError("Failed to load events: " + safe(e.getMessage())));
        } catch (Exception e) {
            callback.onError("Unexpected error while loading events: " + safe(e.getMessage()));
        }
    }

    public void getEventById(@NonNull String eventId, @NonNull RepositoryCallback<Event> callback) {
        if (!isInternetAvailable()) {
            callback.onError("No internet connection. Please try again.");
            return;
        }

        try {
            if (eventId.trim().isEmpty()) {
                callback.onError("Event ID cannot be empty.");
                return;
            }

            firestore.collection(FirestoreFields.COLLECTION_EVENTS)
                    .document(eventId)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        if (snapshot != null && snapshot.exists()) {
                            callback.onSuccess(mapEvent(snapshot));
                        } else {
                            callback.onError("Event not found.");
                        }
                    })
                    .addOnFailureListener(e -> callback.onError("Failed to load event: " + safe(e.getMessage())));
        } catch (Exception e) {
            callback.onError("Unexpected error while loading event: " + safe(e.getMessage()));
        }
    }

    public void joinEvent(@NonNull String eventId, @NonNull String userId, @NonNull RepositoryCallback<Void> callback) {
        if (!isInternetAvailable()) {
            callback.onError("No internet connection. Please try again.");
            return;
        }

        try {
            if (eventId.trim().isEmpty() || userId.trim().isEmpty()) {
                callback.onError("Event ID and User ID are required.");
                return;
            }

            firestore.collection(FirestoreFields.COLLECTION_EVENTS)
                    .document(eventId)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        if (snapshot == null || !snapshot.exists()) {
                            callback.onError("Event not found.");
                            return;
                        }

                        List<String> joined = (List<String>) snapshot.get(FirestoreFields.EVENT_JOINED_USER_IDS);
                        if (joined != null && joined.contains(userId)) {
                            callback.onError("You have already joined this event.");
                            return;
                        }

                        Map<String, Object> patch = new HashMap<>();
                        patch.put(FirestoreFields.EVENT_JOINED_USER_IDS, FieldValue.arrayUnion(userId));
                        patch.put(FirestoreFields.EVENT_UPDATED_AT, System.currentTimeMillis());

                        firestore.collection(FirestoreFields.COLLECTION_EVENTS)
                                .document(eventId)
                                .update(patch)
                                .addOnSuccessListener(unused -> callback.onSuccess(null))
                                .addOnFailureListener(e -> callback.onError("Failed to join event: " + safe(e.getMessage())));
                    })
                    .addOnFailureListener(e -> callback.onError("Failed to validate event join: " + safe(e.getMessage())));
        } catch (Exception e) {
            callback.onError("Unexpected error while joining event: " + safe(e.getMessage()));
        }
    }

    public void leaveEvent(@NonNull String eventId, @NonNull String userId, @NonNull RepositoryCallback<Void> callback) {
        if (!isInternetAvailable()) {
            callback.onError("No internet connection. Please try again.");
            return;
        }

        try {
            if (eventId.trim().isEmpty() || userId.trim().isEmpty()) {
                callback.onError("Event ID and User ID are required.");
                return;
            }

            Map<String, Object> patch = new HashMap<>();
            patch.put(FirestoreFields.EVENT_JOINED_USER_IDS, FieldValue.arrayRemove(userId));
            patch.put(FirestoreFields.EVENT_UPDATED_AT, System.currentTimeMillis());

            firestore.collection(FirestoreFields.COLLECTION_EVENTS)
                    .document(eventId)
                    .update(patch)
                    .addOnSuccessListener(unused -> callback.onSuccess(null))
                    .addOnFailureListener(e -> callback.onError("Failed to leave event: " + safe(e.getMessage())));
        } catch (Exception e) {
            callback.onError("Unexpected error while leaving event: " + safe(e.getMessage()));
        }
    }

    private Map<String, Object> mapUserToFirestore(@NonNull User user) {
        Map<String, Object> map = new HashMap<>();
        map.put(FirestoreFields.USER_ID, safe(user.getId()));
        map.put(FirestoreFields.USER_NAME, safe(user.getName()));
        map.put(FirestoreFields.USER_EMAIL, safe(user.getEmail()));
        map.put(FirestoreFields.USER_ROLE, UserRoleUtil.normalizeRole(user.getRole()));
        map.put(FirestoreFields.USER_LOCATION, safe(user.getLocation()));
        map.put(FirestoreFields.USER_PHOTO_URL, safe(user.getPhotoUrl()));
        map.put(FirestoreFields.USER_SELECTED_HOBBIES,
                user.getSelectedHobbies() != null ? user.getSelectedHobbies() : new ArrayList<String>());
        return map;
    }

    private Map<String, Object> mapEventToFirestore(@NonNull Event event) {
        Map<String, Object> map = new HashMap<>();
        map.put(FirestoreFields.EVENT_ID, safe(event.getId()));
        map.put(FirestoreFields.EVENT_TITLE, safe(event.getTitle()));
        map.put(FirestoreFields.EVENT_DESCRIPTION, safe(event.getDescription()));
        map.put(FirestoreFields.EVENT_HOBBY_ID, safe(event.getHobbyId()));
        map.put(FirestoreFields.EVENT_LOCATION, safe(event.getLocation()));
        map.put(FirestoreFields.EVENT_MAP_QUERY, safe(event.getMapQuery()));
        map.put(FirestoreFields.EVENT_TIME_MILLIS, event.getEventTimeMillis());
        map.put(FirestoreFields.EVENT_CREATED_BY, safe(event.getCreatedByUserId()));
        map.put(FirestoreFields.EVENT_JOINED_USER_IDS,
                event.getJoinedUserIds() != null ? event.getJoinedUserIds() : new ArrayList<String>());
        map.put(FirestoreFields.EVENT_IMAGE_URL, safe(event.getImageUrl()));
        map.put(FirestoreFields.EVENT_UPDATED_AT,
                event.getUpdatedAtMillis() > 0L ? event.getUpdatedAtMillis() : System.currentTimeMillis());
        map.put("dateTime", event.getDateTime());
        map.put("creatorId", safe(event.getCreatorId()));
        map.put("creatorName", safe(event.getCreatorName()));
        return map;
    }

    private Event mapEvent(@NonNull DocumentSnapshot snapshot) {
        Event event = new Event();
        event.setId(safe(snapshot.getString(FirestoreFields.EVENT_ID)));
        if (event.getId().isEmpty() && snapshot.getId() != null) {
            event.setId(snapshot.getId());
        }
        event.setTitle(safe(snapshot.getString(FirestoreFields.EVENT_TITLE)));
        event.setDescription(safe(snapshot.getString(FirestoreFields.EVENT_DESCRIPTION)));
        event.setHobbyId(safe(snapshot.getString(FirestoreFields.EVENT_HOBBY_ID)));
        event.setLocation(safe(snapshot.getString(FirestoreFields.EVENT_LOCATION)));
        event.setMapQuery(safe(snapshot.getString(FirestoreFields.EVENT_MAP_QUERY)));

        Long time = snapshot.getLong(FirestoreFields.EVENT_TIME_MILLIS);
        event.setEventTimeMillis(time != null ? time : 0L);

        event.setCreatedByUserId(safe(snapshot.getString(FirestoreFields.EVENT_CREATED_BY)));
        event.setImageUrl(safe(snapshot.getString(FirestoreFields.EVENT_IMAGE_URL)));

        Long updatedAt = snapshot.getLong(FirestoreFields.EVENT_UPDATED_AT);
        event.setUpdatedAtMillis(updatedAt != null ? updatedAt : 0L);

        List<String> joined = (List<String>) snapshot.get(FirestoreFields.EVENT_JOINED_USER_IDS);
        event.setJoinedUserIds(joined != null ? joined : new ArrayList<>());

        Long dateTimeVal = snapshot.getLong("dateTime");
        event.setDateTime(dateTimeVal != null ? dateTimeVal : event.getEventTimeMillis());
        event.setCreatorId(safe(snapshot.getString("creatorId")));
        if (event.getCreatorId().isEmpty()) {
            event.setCreatorId(event.getCreatedByUserId());
        }
        event.setCreatorName(safe(snapshot.getString("creatorName")));
        return event;
    }

    private User mapUser(@NonNull DocumentSnapshot snapshot) {
        User user = new User();
        user.setId(safe(snapshot.getString(FirestoreFields.USER_ID)));
        if (user.getId().isEmpty() && snapshot.getId() != null) {
            user.setId(snapshot.getId());
        }
        user.setName(safe(snapshot.getString(FirestoreFields.USER_NAME)));
        user.setEmail(safe(snapshot.getString(FirestoreFields.USER_EMAIL)));
        user.setRole(UserRoleUtil.normalizeRole(snapshot.getString(FirestoreFields.USER_ROLE)));
        user.setLocation(safe(snapshot.getString(FirestoreFields.USER_LOCATION)));
        user.setPhotoUrl(safe(snapshot.getString(FirestoreFields.USER_PHOTO_URL)));

        Long updatedAt = snapshot.getLong(FirestoreFields.USER_UPDATED_AT);
        user.setUpdatedAtMillis(updatedAt != null ? updatedAt : 0L);

        List<String> selectedHobbies = (List<String>) snapshot.get(FirestoreFields.USER_SELECTED_HOBBIES);
        user.setSelectedHobbies(selectedHobbies != null ? selectedHobbies : new ArrayList<>());
        return user;
    }

    private boolean isInternetAvailable() {
        try {
            ConnectivityManager cm = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) {
                return false;
            }
            Network network = cm.getActiveNetwork();
            if (network == null) {
                return false;
            }
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
            return capabilities != null &&
                    (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                            || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                            || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        } catch (Exception e) {
            return false;
        }
    }

    private String safe(String value) {
        return value != null ? value : "";
    }
}
