package com.example.hobbycircle.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.hobbycircle.R;
import com.example.hobbycircle.data.model.Event;
import com.example.hobbycircle.data.model.User;
import com.example.hobbycircle.data.model.Warning;
import com.example.hobbycircle.data.repository.EventRepository;
import com.example.hobbycircle.data.repository.UserRepository;
import com.example.hobbycircle.data.remote.FirebaseRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EventViewModel extends AndroidViewModel {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final ExecutorService executorService;

    private final MutableLiveData<List<Event>> eventsLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Event> selectedEventLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<String> messageLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<User>> usersLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Warning>> warningsLiveData = new MutableLiveData<>(new ArrayList<>());

    private Event lastLoadedEventForUpdate;

    public EventViewModel(@NonNull Application application) {
        super(application);
        this.eventRepository = new EventRepository(application.getApplicationContext());
        this.userRepository = new UserRepository(application.getApplicationContext());
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<User>> getUsersLiveData() {
        return usersLiveData;
    }

    public LiveData<List<Warning>> getWarningsLiveData() {
        return warningsLiveData;
    }

    public void loadAllUsers() {
        userRepository.fetchAllUsers(new UserRepository.ResultCallback<List<User>>() {
            @Override
            public void onSuccess(List<User> data) {
                usersLiveData.postValue(data);
            }

            @Override
            public void onError(String message) {
                messageLiveData.postValue(message);
            }
        });
    }

    public void sendWarning(Warning warning) {
        new FirebaseRepository(getApplication()).sendWarning(warning, new FirebaseRepository.RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                messageLiveData.postValue("Warning sent successfully.");
            }

            @Override
            public void onError(String message) {
                messageLiveData.postValue(message);
            }
        });
    }

    public void fetchUnreadWarnings(String userId) {
        new FirebaseRepository(getApplication()).fetchUnreadWarnings(userId, new FirebaseRepository.RepositoryCallback<List<Warning>>() {
            @Override
            public void onSuccess(List<Warning> data) {
                warningsLiveData.postValue(data);
            }

            @Override
            public void onError(String message) {
                messageLiveData.postValue(message);
            }
        });
    }

    public void markWarningAsRead(String warningId) {
        new FirebaseRepository(getApplication()).markWarningAsRead(warningId, new FirebaseRepository.RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                // Warning successfully dismissed
            }

            @Override
            public void onError(String message) {
                messageLiveData.postValue(message);
            }
        });
    }

    public LiveData<List<Event>> getEventsLiveData() {
        return eventsLiveData;
    }

    public LiveData<Event> getSelectedEventLiveData() {
        return selectedEventLiveData;
    }

    public LiveData<Boolean> getLoadingLiveData() {
        return loadingLiveData;
    }

    public LiveData<String> getMessageLiveData() {
        return messageLiveData;
    }

    public LiveData<Boolean> getImageUploadingLiveData() {
        return eventRepository.getUploadingLiveData();
    }

    public LiveData<String> getImageUploadUrlLiveData() {
        return eventRepository.getImageUploadUrlLiveData();
    }

    public void uploadCoverImage(String eventId, byte[] imageBytes) {
        eventRepository.uploadCoverOnly(eventId, imageBytes);
    }

    public void createEvent(Event event, byte[] imageBytes) {
        loadingLiveData.setValue(true);
        eventRepository.createEvent(event, imageBytes, new EventRepository.ResultCallback<Event>() {
            @Override
            public void onSuccess(Event data) {
                loadingLiveData.postValue(false);
                messageLiveData.postValue(getApplication().getString(R.string.msg_event_created));
                loadEvents();
            }

            @Override
            public void onError(String message) {
                loadingLiveData.postValue(false);
                messageLiveData.postValue(safe(message));
            }
        });
    }

    public void updateEvent(Event updated, byte[] imageBytes, String currentUserId) {
        loadingLiveData.setValue(true);
        Event previous = lastLoadedEventForUpdate != null
                ? lastLoadedEventForUpdate
                : selectedEventLiveData.getValue();
        eventRepository.updateEvent(previous, updated, imageBytes, safe(currentUserId),
                new EventRepository.ResultCallback<Event>() {
                    @Override
                    public void onSuccess(Event data) {
                        loadingLiveData.postValue(false);
                        selectedEventLiveData.postValue(data);
                        messageLiveData.postValue(getApplication().getString(R.string.event_updated));
                        loadEvents();
                    }

                    @Override
                    public void onError(String message) {
                        loadingLiveData.postValue(false);
                        messageLiveData.postValue(safe(message));
                    }
                });
    }

    public void loadEvents() {
        loadingLiveData.setValue(true);
        eventRepository.loadEventsFromCache(new EventRepository.ResultCallback<List<Event>>() {
            @Override
            public void onSuccess(List<Event> data) {
                eventsLiveData.postValue(data);
            }

            @Override
            public void onError(String message) {
                messageLiveData.postValue(safe(message));
            }
        });

        eventRepository.refreshEventsFromRemote(new EventRepository.ResultCallback<List<Event>>() {
            @Override
            public void onSuccess(List<Event> data) {
                eventsLiveData.postValue(data);
                loadingLiveData.postValue(false);
            }

            @Override
            public void onError(String message) {
                loadingLiveData.postValue(false);
                messageLiveData.postValue(safe(message));
            }
        });
    }

    public void loadEventById(String eventId) {
        loadingLiveData.setValue(true);
        eventRepository.loadEventById(safe(eventId), new EventRepository.ResultCallback<Event>() {
            @Override
            public void onSuccess(Event data) {
                lastLoadedEventForUpdate = data;
                selectedEventLiveData.postValue(data);
                loadingLiveData.postValue(false);
            }

            @Override
            public void onError(String message) {
                loadingLiveData.postValue(false);
                messageLiveData.postValue(safe(message));
            }
        });
    }

    public void joinEvent(String eventId, String userId, Event eventForReminder) {
        loadingLiveData.setValue(true);
        eventRepository.joinEvent(safe(eventId), safe(userId), eventForReminder,
                new EventRepository.ResultCallback<Void>() {
                    @Override
                    public void onSuccess(Void data) {
                        loadingLiveData.postValue(false);
                        messageLiveData.postValue(getApplication().getString(R.string.msg_joined_event));
                        loadEventById(eventId);
                        loadEvents();
                    }

                    @Override
                    public void onError(String message) {
                        loadingLiveData.postValue(false);
                        messageLiveData.postValue(safe(message));
                    }
                });
    }

    public void leaveEvent(String eventId, String userId) {
        loadingLiveData.setValue(true);
        eventRepository.leaveEvent(safe(eventId), safe(userId), new EventRepository.ResultCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                loadingLiveData.postValue(false);
                messageLiveData.postValue(getApplication().getString(R.string.msg_left_event));
                loadEventById(eventId);
                loadEvents();
            }

            @Override
            public void onError(String message) {
                loadingLiveData.postValue(false);
                messageLiveData.postValue(safe(message));
            }
        });
    }

    public void deleteEvent(String eventId) {
        loadingLiveData.setValue(true);
        eventRepository.deleteEvent(safe(eventId), new EventRepository.ResultCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                loadingLiveData.postValue(false);
                messageLiveData.postValue(getApplication().getString(R.string.msg_event_deleted));
                loadEvents();
            }

            @Override
            public void onError(String message) {
                loadingLiveData.postValue(false);
                messageLiveData.postValue(safe(message));
            }
        });
    }

    public void rateEvent(String eventId, String userId, int rating) {
        loadingLiveData.setValue(true);
        eventRepository.rateEvent(safe(eventId), safe(userId), rating, new EventRepository.ResultCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                loadingLiveData.postValue(false);
                messageLiveData.postValue("Rating submitted successfully!");
                loadEvents();
            }

            @Override
            public void onError(String message) {
                loadingLiveData.postValue(false);
                messageLiveData.postValue(safe(message));
            }
        });
    }

    private String safe(String value) {
        return value != null ? value.trim() : "";
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }
}
