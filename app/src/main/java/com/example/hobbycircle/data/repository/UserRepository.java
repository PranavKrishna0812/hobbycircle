package com.example.hobbycircle.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.hobbycircle.data.local.AppDatabase;
import com.example.hobbycircle.data.local.UserDao;
import com.example.hobbycircle.data.local.UserEntity;
import com.example.hobbycircle.data.mapper.DataMappers;
import com.example.hobbycircle.data.model.User;
import com.example.hobbycircle.data.remote.FirebaseRepository;
import com.example.hobbycircle.data.remote.FirestoreFields;
import com.example.hobbycircle.utils.PreferenceManager;
import com.example.hobbycircle.utils.UserRoleUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Syncs user profile between Firestore, Room, SharedPreferences, and Firebase Auth display name.
 */
public class UserRepository {

    private final Context appContext;
    private final FirebaseRepository firebaseRepository;
    private final UserDao userDao;
    private final PreferenceManager preferenceManager;
    private final FirebaseFirestore firestore;
    private final ExecutorService executor;

    public UserRepository(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        this.firebaseRepository = new FirebaseRepository(appContext);
        this.userDao = AppDatabase.getInstance(appContext).userDao();
        this.preferenceManager = new PreferenceManager(appContext);
        this.firestore = FirebaseFirestore.getInstance();
        this.executor = Executors.newSingleThreadExecutor();
    }

    public interface ResultCallback<T> {
        void onSuccess(T data);
        void onError(String message);
    }

    public void loadUserFromCache(String userId, @NonNull ResultCallback<User> callback) {
        executor.execute(() -> {
            try {
                UserEntity entity = userDao.getUserById(safe(userId));
                if (entity != null) {
                    callback.onSuccess(applyRoleResolution(DataMappers.fromUserEntity(entity)));
                } else {
                    callback.onError("No cached profile.");
                }
            } catch (Exception e) {
                callback.onError(safe(e.getMessage()));
            }
        });
    }

    public void refreshUserFromRemote(String userId, @NonNull ResultCallback<User> callback) {
        firebaseRepository.getUserProfile(safe(userId), new FirebaseRepository.RepositoryCallback<User>() {
            @Override
            public void onSuccess(User data) {
                User resolved = applyRoleResolution(data);
                cacheAndPrefs(resolved);
                callback.onSuccess(resolved);
            }

            @Override
            public void onError(String message) {
                callback.onError(safe(message));
            }
        });
    }

    public void saveUserProfile(User user, @NonNull ResultCallback<User> callback) {
        User toSave = applyRoleResolution(user);
        if (toSave.getRole().isEmpty()) {
            toSave.setRole(preferenceManager.getUserRole());
        }
        firebaseRepository.saveUserProfile(toSave, new FirebaseRepository.RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                updateAuthDisplayName(toSave.getName());
                cacheAndPrefs(toSave);
                callback.onSuccess(toSave);
            }

            @Override
            public void onError(String message) {
                callback.onError(safe(message));
            }
        });
    }

    public String resolveAndPersistRole(FirebaseUser firebaseUser, String firestoreRole) {
        String email = firebaseUser != null ? safe(firebaseUser.getEmail()) : "";
        String role = UserRoleUtil.resolveRole(appContext, email, firestoreRole);
        preferenceManager.saveUserRole(role);
        if (firebaseUser != null) {
            preferenceManager.saveUserId(safe(firebaseUser.getUid()));
            preferenceManager.saveUserEmail(email);
            String name = safe(firebaseUser.getDisplayName());
            if (name.isEmpty() && email.contains("@")) {
                name = email.substring(0, email.indexOf("@"));
            }
            preferenceManager.saveUserName(name);
        }
        return role;
    }

    public void persistAuthUserDocument(FirebaseUser user, String provider, String role,
                                        @NonNull ResultCallback<Void> callback) {
        if (user == null) {
            callback.onError("User is null.");
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put(FirestoreFields.USER_ID, safe(user.getUid()));
        payload.put(FirestoreFields.USER_NAME, safe(user.getDisplayName()));
        payload.put(FirestoreFields.USER_EMAIL, safe(user.getEmail()));
        payload.put(FirestoreFields.USER_PROVIDER, provider);
        payload.put(FirestoreFields.USER_ROLE, UserRoleUtil.normalizeRole(role));
        payload.put(FirestoreFields.USER_UPDATED_AT, System.currentTimeMillis());

        firestore.collection(FirestoreFields.COLLECTION_USERS)
                .document(safe(user.getUid()))
                .set(payload, SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(safe(e.getMessage())));
    }

    public void fetchFirestoreRole(String uid, @NonNull ResultCallback<String> callback) {
        firestore.collection(FirestoreFields.COLLECTION_USERS)
                .document(safe(uid))
                .get()
                .addOnSuccessListener(snapshot -> {
                    String role = snapshot != null && snapshot.exists()
                            ? snapshot.getString(FirestoreFields.USER_ROLE)
                            : null;
                    callback.onSuccess(role != null ? role : "");
                })
                .addOnFailureListener(e -> callback.onError(safe(e.getMessage())));
    }

    private User applyRoleResolution(User user) {
        if (user == null) {
            return new User();
        }
        String role = UserRoleUtil.resolveRole(appContext, user.getEmail(), user.getRole());
        user.setRole(role);
        return user;
    }

    private void cacheAndPrefs(User user) {
        if (user == null) {
            return;
        }
        String role = UserRoleUtil.normalizeRole(user.getRole());
        user.setRole(role);
        preferenceManager.saveUserId(safe(user.getId()));
        preferenceManager.saveUserName(safe(user.getName()));
        preferenceManager.saveUserEmail(safe(user.getEmail()));
        preferenceManager.saveUserLocation(safe(user.getLocation()));
        preferenceManager.saveUserRole(role);
        preferenceManager.saveSelectedHobbies(user.getSelectedHobbies());

        executor.execute(() -> {
            try {
                userDao.insertOrUpdate(DataMappers.toUserEntity(user));
            } catch (Exception ignored) {
            }
        });
    }

    private void updateAuthDisplayName(String displayName) {
        FirebaseUser current = FirebaseAuth.getInstance().getCurrentUser();
        if (current == null || displayName == null || displayName.trim().isEmpty()) {
            return;
        }
        UserProfileChangeRequest request = new UserProfileChangeRequest.Builder()
                .setDisplayName(displayName.trim())
                .build();
        current.updateProfile(request);
    }

    private String safe(String value) {
        return value != null ? value.trim() : "";
    }
}
