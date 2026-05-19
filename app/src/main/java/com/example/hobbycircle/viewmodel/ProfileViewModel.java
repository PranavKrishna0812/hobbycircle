package com.example.hobbycircle.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.hobbycircle.R;
import com.example.hobbycircle.data.model.Event;
import com.example.hobbycircle.data.model.User;
import com.example.hobbycircle.data.repository.EventRepository;
import com.example.hobbycircle.data.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;

public class ProfileViewModel extends AndroidViewModel {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    private final MutableLiveData<User> userLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<Event>> createdEventsLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<String> messageLiveData = new MutableLiveData<>();

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        this.userRepository = new UserRepository(application.getApplicationContext());
        this.eventRepository = new EventRepository(application.getApplicationContext());
    }

    public LiveData<User> getUserLiveData() {
        return userLiveData;
    }

    public LiveData<List<Event>> getCreatedEventsLiveData() {
        return createdEventsLiveData;
    }

    public LiveData<Boolean> getLoadingLiveData() {
        return loadingLiveData;
    }

    public LiveData<String> getMessageLiveData() {
        return messageLiveData;
    }

    public void loadUserProfile(String userId) {
        loadingLiveData.setValue(true);

        userRepository.loadUserFromCache(safe(userId), new UserRepository.ResultCallback<User>() {
            @Override
            public void onSuccess(User data) {
                userLiveData.postValue(data);
                loadCreatedEvents(safe(userId));
            }

            @Override
            public void onError(String message) {}
        });

        userRepository.refreshUserFromRemote(safe(userId), new UserRepository.ResultCallback<User>() {
            @Override
            public void onSuccess(User data) {
                loadingLiveData.postValue(false);
                userLiveData.postValue(data);
                loadCreatedEvents(safe(userId));
            }

            @Override
            public void onError(String message) {
                loadingLiveData.postValue(false);
                messageLiveData.postValue(safe(message));
            }
        });
    }

    public void updateProfile(String name, String location, List<String> hobbies) {
        loadingLiveData.setValue(true);
        userRepository.updateProfile(name, location, hobbies, new UserRepository.ResultCallback<User>() {
            @Override
            public void onSuccess(User data) {
                loadingLiveData.postValue(false);
                userLiveData.postValue(data);
                messageLiveData.postValue("Profile saved ✓");
            }

            @Override
            public void onError(String message) {
                loadingLiveData.postValue(false);
                messageLiveData.postValue(safe(message));
            }
        });
    }

    public void loadCreatedEvents(String userId) {
        eventRepository.loadEventsFromCache(new EventRepository.ResultCallback<List<Event>>() {
            @Override
            public void onSuccess(List<Event> data) {
                filterAndPostCreatedEvents(data, userId);
            }

            @Override
            public void onError(String message) {}
        });

        eventRepository.refreshEventsFromRemote(new EventRepository.ResultCallback<List<Event>>() {
            @Override
            public void onSuccess(List<Event> data) {
                filterAndPostCreatedEvents(data, userId);
            }

            @Override
            public void onError(String message) {}
        });
    }

    private void filterAndPostCreatedEvents(List<Event> allEvents, String userId) {
        if (allEvents == null) {
            return;
        }
        List<Event> mine = new ArrayList<>();
        for (Event e : allEvents) {
            if (e != null && safe(e.getCreatedByUserId()).equals(userId)) {
                mine.add(e);
            }
        }
        createdEventsLiveData.postValue(mine);
    }

    private String safe(String value) {
        return value != null ? value.trim() : "";
    }
}
