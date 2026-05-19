package com.example.hobbycircle.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.hobbycircle.R;
import com.example.hobbycircle.data.model.User;
import com.example.hobbycircle.data.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserViewModel extends AndroidViewModel {

    private final UserRepository userRepository;
    private final ExecutorService executorService;

    private final MutableLiveData<User> userLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<String> messageLiveData = new MutableLiveData<>();

    public UserViewModel(@NonNull Application application) {
        super(application);
        this.userRepository = new UserRepository(application.getApplicationContext());
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public LiveData<User> getUserLiveData() {
        return userLiveData;
    }

    public LiveData<Boolean> getLoadingLiveData() {
        return loadingLiveData;
    }

    public LiveData<String> getMessageLiveData() {
        return messageLiveData;
    }

    public void saveUserProfile(String userId, String name, String email, String location,
                              List<String> selectedHobbies, String role) {
        loadingLiveData.setValue(true);
        User user = new User(
                safe(userId),
                safe(name),
                safe(email),
                safe(role),
                safe(location),
                "",
                selectedHobbies != null ? selectedHobbies : new ArrayList<>()
        );

        userRepository.saveUserProfile(user, new UserRepository.ResultCallback<User>() {
            @Override
            public void onSuccess(User data) {
                loadingLiveData.postValue(false);
                userLiveData.postValue(data);
                messageLiveData.postValue(getApplication().getString(R.string.msg_profile_saved));
            }

            @Override
            public void onError(String message) {
                loadingLiveData.postValue(false);
                messageLiveData.postValue(safe(message));
            }
        });
    }

    public void loadUserProfile(String userId) {
        loadingLiveData.setValue(true);

        userRepository.loadUserFromCache(safe(userId), new UserRepository.ResultCallback<User>() {
            @Override
            public void onSuccess(User data) {
                userLiveData.postValue(data);
            }

            @Override
            public void onError(String message) {
                // Cache miss is acceptable; remote fetch will follow.
            }
        });

        userRepository.refreshUserFromRemote(safe(userId), new UserRepository.ResultCallback<User>() {
            @Override
            public void onSuccess(User data) {
                loadingLiveData.postValue(false);
                userLiveData.postValue(data);
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
