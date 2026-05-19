package com.example.hobbycircle.ui.auth;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hobbycircle.R;
import com.example.hobbycircle.data.model.Event;
import com.example.hobbycircle.data.model.User;
import com.example.hobbycircle.ui.details.EventDetailActivity;
import com.example.hobbycircle.utils.Constants;
import com.example.hobbycircle.utils.PreferenceManager;
import com.example.hobbycircle.viewmodel.ProfileViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

import java.util.ArrayList;
import java.util.List;

public class ProfileActivity extends AppCompatActivity implements CompactEventAdapter.OnCompactEventClickListener {

    private Toolbar toolbarProfile;
    private TextView tvAvatarInitial;
    private ImageView ivEditAvatar;
    private TextView tvProfileName;
    private TextView tvProfileEmail;

    private TextInputLayout tilProfileName;
    private TextInputEditText etName;
    private TextInputLayout tilProfileLocation;
    private TextInputEditText etLocation;

    private TextView tvHobbyCount;
    private ChipGroup chipGroupHobbies;
    private TextInputLayout tilAddHobby;
    private TextInputEditText etAddHobby;

    private TextView tvCreatedEventCount;
    private RecyclerView rvCreatedEvents;

    private MaterialButton btnLogout;
    private FloatingActionButton fabSaveProfile;

    private ProfileViewModel profileViewModel;
    private PreferenceManager preferenceManager;
    private CompactEventAdapter compactEventAdapter;

    private final List<String> currentHobbies = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        preferenceManager = new PreferenceManager(this);

        initViews();
        setupViewModel();
        setupActions();

        String userId = preferenceManager.getUserId();
        if (!userId.isEmpty()) {
            profileViewModel.loadUserProfile(userId);
        } else {
            FirebaseUser current = FirebaseAuth.getInstance().getCurrentUser();
            if (current != null) {
                profileViewModel.loadUserProfile(current.getUid());
            }
        }
    }

    private void initViews() {
        toolbarProfile = findViewById(R.id.toolbarProfile);
        tvAvatarInitial = findViewById(R.id.tvAvatarInitial);
        ivEditAvatar = findViewById(R.id.ivEditAvatar);
        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);

        tilProfileName = findViewById(R.id.tilProfileName);
        etName = findViewById(R.id.etName);
        tilProfileLocation = findViewById(R.id.tilProfileLocation);
        etLocation = findViewById(R.id.etLocation);

        tvHobbyCount = findViewById(R.id.tvHobbyCount);
        chipGroupHobbies = findViewById(R.id.chipGroupHobbies);
        tilAddHobby = findViewById(R.id.tilAddHobby);
        etAddHobby = findViewById(R.id.etAddHobby);

        tvCreatedEventCount = findViewById(R.id.tvCreatedEventCount);
        rvCreatedEvents = findViewById(R.id.rvCreatedEvents);

        btnLogout = findViewById(R.id.btnLogout);
        fabSaveProfile = findViewById(R.id.fabSaveProfile);

        setSupportActionBar(toolbarProfile);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        rvCreatedEvents.setLayoutManager(new LinearLayoutManager(this));
        compactEventAdapter = new CompactEventAdapter(new ArrayList<>(), this);
        rvCreatedEvents.setAdapter(compactEventAdapter);
    }

    private void setupViewModel() {
        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        profileViewModel.getUserLiveData().observe(this, this::bindUserProfile);

        profileViewModel.getCreatedEventsLiveData().observe(this, events -> {
            if (events != null) {
                compactEventAdapter.submitList(events);
                tvCreatedEventCount.setText(String.valueOf(events.size()));
            }
        });

        profileViewModel.getMessageLiveData().observe(this, message -> {
            if (message == null || message.trim().isEmpty()) {
                return;
            }
            if (message.equals("Profile saved ✓")) {
                Snackbar.make(fabSaveProfile, "Profile saved ✓", Snackbar.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupActions() {
        toolbarProfile.setNavigationOnClickListener(v -> onBackPressed());

        // Add hobby on Done IME action
        etAddHobby.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                addHobbyFromField();
                return true;
            }
            return false;
        });

        // Add hobby on endIcon click
        tilAddHobby.setEndIconOnClickListener(v -> addHobbyFromField());

        fabSaveProfile.setOnClickListener(v -> saveProfileData());

        btnLogout.setOnClickListener(v -> showLogoutConfirmation());
    }

    private void bindUserProfile(User user) {
        if (user == null) {
            return;
        }

        String name = safe(user.getName());
        etName.setText(name);
        tvProfileName.setText(name.isEmpty() ? "Anonymous" : name);

        String initial = name.isEmpty() ? "U" : name.substring(0, 1).toUpperCase();
        tvAvatarInitial.setText(initial);

        String email = safe(user.getEmail());
        tvProfileEmail.setText(email.isEmpty() ? "No email provided" : email);

        etLocation.setText(safe(user.getLocation()));

        currentHobbies.clear();
        chipGroupHobbies.removeAllViews();
        if (user.getSelectedHobbies() != null) {
            for (String hobby : user.getSelectedHobbies()) {
                if (hobby != null && !hobby.trim().isEmpty()) {
                    addHobbyChip(hobby.trim());
                }
            }
        }
        updateHobbyCount();
    }

    private void addHobbyFromField() {
        String hobby = etAddHobby.getText() != null ? etAddHobby.getText().toString().trim() : "";
        if (hobby.isEmpty()) {
            return;
        }
        if (currentHobbies.contains(hobby)) {
            Toast.makeText(this, "Hobby already added.", Toast.LENGTH_SHORT).show();
            return;
        }
        addHobbyChip(hobby);
        etAddHobby.setText("");
        updateHobbyCount();
    }

    private void addHobbyChip(String hobby) {
        currentHobbies.add(hobby);

        Chip chip = new Chip(this);
        chip.setText(hobby);
        chip.setCloseIconVisible(true);
        chip.setChipBackgroundColor(ColorStateList.valueOf(getResources().getColor(R.color.colorAccentSoft)));
        chip.setTextColor(getResources().getColor(R.color.hc_accent));
        chip.setCloseIconTint(ColorStateList.valueOf(getResources().getColor(R.color.hc_accent)));
        chip.setChipCornerRadius(50f);

        chip.setOnCloseIconClickListener(v -> {
            chipGroupHobbies.removeView(chip);
            currentHobbies.remove(hobby);
            updateHobbyCount();
        });

        chipGroupHobbies.addView(chip);
    }

    private void updateHobbyCount() {
        int count = currentHobbies.size();
        tvHobbyCount.setText(count == 1 ? "1 added" : count + " added");
    }

    private void saveProfileData() {
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        String location = etLocation.getText() != null ? etLocation.getText().toString().trim() : "";

        if (name.isEmpty()) {
            tilProfileName.setError("Name is required");
            return;
        } else {
            tilProfileName.setError(null);
        }

        // Save to Local SharedPreferences first for instant updates across the UI
        preferenceManager.saveUserName(name);
        preferenceManager.saveUserLocation(location);
        preferenceManager.saveSelectedHobbies(currentHobbies);

        // Update Firebase Auth Display Name
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            UserProfileChangeRequest request = new UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build();
            firebaseUser.updateProfile(request);
        }

        // Call ViewModel to write remote (Firestore) and cache (Room) updates
        profileViewModel.updateProfile(name, location, currentHobbies);
    }

    private void showLogoutConfirmation() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out of HobbyCircle?")
                .setPositiveButton("Log Out", (dialog, which) -> performLogout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performLogout() {
        FirebaseAuth.getInstance().signOut();
        preferenceManager.clearAll();

        Intent intent = new Intent(this, AuthActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onCompactEventClick(Event event) {
        if (event == null || safe(event.getId()).isEmpty()) {
            return;
        }
        Intent i = new Intent(this, EventDetailActivity.class);
        i.putExtra(Constants.EXTRA_EVENT_ID, event.getId());
        startActivity(i);
    }

    private String safe(String value) {
        return value != null ? value.trim() : "";
    }
}