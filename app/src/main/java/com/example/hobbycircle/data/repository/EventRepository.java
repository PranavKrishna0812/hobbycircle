package com.example.hobbycircle.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.hobbycircle.data.local.AppDatabase;
import com.example.hobbycircle.data.local.EventDao;
import com.example.hobbycircle.data.local.EventEntity;
import com.example.hobbycircle.data.mapper.DataMappers;
import com.example.hobbycircle.data.model.Event;
import com.example.hobbycircle.data.remote.FirebaseRepository;
import com.example.hobbycircle.utils.NotificationHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Syncs event data between Firestore and Room. All network/DB work runs off the main thread.
 */
public class EventRepository {

    private final FirebaseRepository firebaseRepository;
    private final EventDao eventDao;
    private final NotificationHelper notificationHelper;
    private final ExecutorService executor;

    private final MutableLiveData<Boolean> uploadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<String> imageUploadUrlLiveData = new MutableLiveData<>();

    public EventRepository(@NonNull Context context) {
        Context app = context.getApplicationContext();
        this.firebaseRepository = new FirebaseRepository(app);
        this.eventDao = AppDatabase.getInstance(app).eventDao();
        this.notificationHelper = new NotificationHelper(app);
        this.executor = Executors.newSingleThreadExecutor();
    }

    public LiveData<Boolean> getUploadingLiveData() {
        return uploadingLiveData;
    }

    public LiveData<String> getImageUploadUrlLiveData() {
        return imageUploadUrlLiveData;
    }

    public interface ResultCallback<T> {
        void onSuccess(T data);
        void onError(String message);
    }

    public void loadEventsFromCache(@NonNull ResultCallback<List<Event>> callback) {
        executor.execute(() -> {
            try {
                List<EventEntity> entities = eventDao.getAllEvents();
                callback.onSuccess(DataMappers.fromEventEntities(entities));
            } catch (Exception e) {
                callback.onError("Failed to read local events: " + safe(e.getMessage()));
            }
        });
    }

    public void refreshEventsFromRemote(@NonNull ResultCallback<List<Event>> callback) {
        firebaseRepository.fetchAllEvents(new FirebaseRepository.RepositoryCallback<List<Event>>() {
            @Override
            public void onSuccess(List<Event> data) {
                List<Event> safeData = data != null ? data : new ArrayList<>();
                cacheEvents(safeData);
                callback.onSuccess(safeData);
            }

            @Override
            public void onError(String message) {
                callback.onError(safe(message));
            }
        });
    }

    public void loadEventById(String eventId, @NonNull ResultCallback<Event> callback) {
        executor.execute(() -> {
            try {
                EventEntity local = eventDao.getEventById(safe(eventId));
                if (local != null) {
                    callback.onSuccess(DataMappers.fromEventEntity(local));
                }
            } catch (Exception e) {
                callback.onError("Failed to read local event: " + safe(e.getMessage()));
            }
        });

        firebaseRepository.getEventById(safe(eventId), new FirebaseRepository.RepositoryCallback<Event>() {
            @Override
            public void onSuccess(Event data) {
                cacheEvent(data);
                callback.onSuccess(data);
            }

            @Override
            public void onError(String message) {
                callback.onError(safe(message));
            }
        });
    }

    public void createEvent(Event event, byte[] imageBytes, @NonNull ResultCallback<Event> callback) {
        if (imageBytes != null && imageBytes.length > 0) {
            uploadCoverThen(event, imageBytes,
                    () -> persistCreate(event, callback),
                    message -> {
                        // Fallback: Clear image URL and create event anyway
                        event.setImageUrl("");
                        persistCreate(event, callback);
                    });
        } else {
            persistCreate(event, callback);
        }
    }

    public void updateEvent(Event previous, Event updated, byte[] imageBytes, String currentUserId,
                            @NonNull ResultCallback<Event> callback) {
        if (imageBytes != null && imageBytes.length > 0) {
            uploadCoverThen(updated, imageBytes,
                    () -> persistUpdate(previous, updated, currentUserId, callback),
                    message -> {
                        // Fallback: Clear image URL and update event anyway
                        updated.setImageUrl("");
                        persistUpdate(previous, updated, currentUserId, callback);
                    });
        } else {
            persistUpdate(previous, updated, currentUserId, callback);
        }
    }

    public void deleteEvent(String eventId, @NonNull ResultCallback<Void> callback) {
        notificationHelper.cancelEventReminder(safe(eventId));
        firebaseRepository.deleteEvent(safe(eventId), new FirebaseRepository.RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                executor.execute(() -> {
                    try {
                        eventDao.deleteById(safe(eventId));
                    } catch (Exception ignored) {
                    }
                });
                callback.onSuccess(null);
            }

            @Override
            public void onError(String message) {
                callback.onError(safe(message));
            }
        });
    }

    public void joinEvent(String eventId, String userId, Event eventForReminder,
                          @NonNull ResultCallback<Void> callback) {
        firebaseRepository.joinEvent(safe(eventId), safe(userId), new FirebaseRepository.RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                if (eventForReminder != null) {
                    notificationHelper.scheduleEventReminder(
                            eventForReminder.getId(),
                            eventForReminder.getTitle(),
                            eventForReminder.getLocation(),
                            eventForReminder.getEventTimeMillis()
                    );
                }
                callback.onSuccess(null);
            }

            @Override
            public void onError(String message) {
                callback.onError(safe(message));
            }
        });
    }

    public void leaveEvent(String eventId, String userId, @NonNull ResultCallback<Void> callback) {
        notificationHelper.cancelEventReminder(safe(eventId));
        firebaseRepository.leaveEvent(safe(eventId), safe(userId), new FirebaseRepository.RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                callback.onSuccess(null);
            }

            @Override
            public void onError(String message) {
                callback.onError(safe(message));
            }
        });
    }

    /** Upload cover immediately (e.g. on image pick) — exposes URL via {@link #getImageUploadUrlLiveData()}. */
    public void uploadCoverOnly(String eventId, byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            return;
        }
        uploadingLiveData.postValue(true);
        firebaseRepository.uploadEventCover(safe(eventId), imageBytes, new FirebaseRepository.RepositoryCallback<String>() {
            @Override
            public void onSuccess(String url) {
                uploadingLiveData.postValue(false);
                imageUploadUrlLiveData.postValue(url);
            }

            @Override
            public void onError(String message) {
                uploadingLiveData.postValue(false);
                imageUploadUrlLiveData.postValue(null);
            }
        });
    }

    private void uploadCoverThen(Event event, byte[] imageBytes, @NonNull Runnable onUploaded,
                                 @NonNull java.util.function.Consumer<String> onError) {
        uploadingLiveData.postValue(true);
        firebaseRepository.uploadEventCover(safe(event.getId()), imageBytes, new FirebaseRepository.RepositoryCallback<String>() {
            @Override
            public void onSuccess(String url) {
                event.setImageUrl(url);
                uploadingLiveData.postValue(false);
                imageUploadUrlLiveData.postValue(url);
                onUploaded.run();
            }

            @Override
            public void onError(String message) {
                uploadingLiveData.postValue(false);
                onError.accept(safe(message));
            }
        });
    }

    private void persistCreate(Event event, @NonNull ResultCallback<Event> callback) {
        firebaseRepository.createEvent(event, new FirebaseRepository.RepositoryCallback<Event>() {
            @Override
            public void onSuccess(Event data) {
                cacheEvent(data);
                callback.onSuccess(data);
            }

            @Override
            public void onError(String message) {
                callback.onError(safe(message));
            }
        });
    }

    private void persistUpdate(Event previous, Event updated, String currentUserId,
                               @NonNull ResultCallback<Event> callback) {
        firebaseRepository.updateEvent(updated, new FirebaseRepository.RepositoryCallback<Event>() {
            @Override
            public void onSuccess(Event data) {
                cacheEvent(data);
                rescheduleReminderIfNeeded(previous, updated, currentUserId);
                callback.onSuccess(data);
            }

            @Override
            public void onError(String message) {
                callback.onError(safe(message));
            }
        });
    }

    private void rescheduleReminderIfNeeded(Event previous, Event updated, String currentUserId) {
        if (previous == null || updated == null || currentUserId.isEmpty()) {
            return;
        }
        List<String> joined = updated.getJoinedUserIds();
        if (joined == null || !joined.contains(currentUserId)) {
            return;
        }
        if (previous.getEventTimeMillis() == updated.getEventTimeMillis()) {
            return;
        }
        notificationHelper.cancelEventReminder(updated.getId());
        notificationHelper.scheduleEventReminder(
                updated.getId(),
                updated.getTitle(),
                updated.getLocation(),
                updated.getEventTimeMillis()
        );
    }

    private void cacheEvent(Event event) {
        if (event == null) {
            return;
        }
        executor.execute(() -> {
            try {
                eventDao.insertOrUpdate(DataMappers.toEventEntity(event));
            } catch (Exception ignored) {
            }
        });
    }

    private void cacheEvents(List<Event> events) {
        executor.execute(() -> {
            try {
                List<EventEntity> entities = new ArrayList<>();
                for (Event event : events) {
                    entities.add(DataMappers.toEventEntity(event));
                }
                eventDao.clearAll();
                if (!entities.isEmpty()) {
                    eventDao.insertAll(entities);
                }
            } catch (Exception ignored) {
            }
        });
    }

    private String safe(String value) {
        return value != null ? value.trim() : "";
    }
}
