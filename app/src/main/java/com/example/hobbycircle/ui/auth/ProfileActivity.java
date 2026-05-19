package com.example.hobbycircle.ui.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hobbycircle.R;
import com.example.hobbycircle.data.model.Event;
import com.example.hobbycircle.ui.details.EventDetailActivity;
import com.example.hobbycircle.ui.events.EventAdapter;
import com.example.hobbycircle.utils.Constants;
import com.example.hobbycircle.utils.DrawerMenuHelper;
import com.example.hobbycircle.utils.PreferenceManager;
import com.example.hobbycircle.utils.UserRoleUtil;
import com.google.android.material.navigation.NavigationView;
import com.example.hobbycircle.viewmodel.EventViewModel;
import com.example.hobbycircle.viewmodel.UserViewModel;
import com.google.firebase.auth.FirebaseAuth;

import android.content.Intent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.example.hobbycircle.ui.BaseDrawerActivity;

public class ProfileActivity extends BaseDrawerActivity implements EventAdapter.OnEventClickListener {

    private EditText etName;
    private EditText etEmail;
    private EditText etLocation;
    private EditText etHobbies;
    private Button btnSaveProfile;
    private Button btnLogout;
    private TextView tvRoleBadge;
    private RecyclerView rvCreatedEvents;

    private UserViewModel userViewModel;
    private EventViewModel eventViewModel;
    private PreferenceManager preferenceManager;
    private EventAdapter createdEventsAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initViews();
        setupViewModels();
        prefillFromPreferences();
        setupClickListeners();
    }

    private void initViews() {
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etLocation = findViewById(R.id.etLocation);
        etHobbies = findViewById(R.id.etHobbies);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnLogout = findViewById(R.id.btnLogout);
        tvRoleBadge = findViewById(R.id.tvRoleBadge);
        rvCreatedEvents = findViewById(R.id.rvCreatedEvents);

        rvCreatedEvents.setLayoutManager(new LinearLayoutManager(this));
        createdEventsAdapter = new EventAdapter(new ArrayList<>(), this);
        rvCreatedEvents.setAdapter(createdEventsAdapter);
    }

    private void setupViewModels() {
        preferenceManager = new PreferenceManager(this);
        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        eventViewModel = new ViewModelProvider(this).get(EventViewModel.class);

        userViewModel.getMessageLiveData().observe(this, msg -> {
            if (msg != null && !msg.trim().isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
        });

        eventViewModel.getEventsLiveData().observe(this, events -> {
            String myUserId = preferenceManager.getUserId();
            List<Event> mine = new ArrayList<>();
            if (events != null) {
                for (Event e : events) {
                    if (e != null && safe(e.getCreatedByUserId()).equals(myUserId)) {
                        mine.add(e);
                    }
                }
            }
            createdEventsAdapter.submitList(mine);
        });

        eventViewModel.loadEvents();

        String userId = preferenceManager.getUserId();
        if (!userId.isEmpty()) {
            userViewModel.loadUserProfile(userId);
        }
        updateRoleBadge(preferenceManager.getUserRole());

        userViewModel.getUserLiveData().observe(this, user -> {
            if (user == null) return;
            String role = UserRoleUtil.resolveRole(this, user.getEmail(), user.getRole());
            preferenceManager.saveUserRole(role);
            updateRoleBadge(role);
        });
    }

    private void updateRoleBadge(String role) {
        if (tvRoleBadge == null) return;
        if (UserRoleUtil.isAdmin(role)) {
            tvRoleBadge.setText(R.string.role_admin);
        } else {
            tvRoleBadge.setText(R.string.role_user);
        }
    }

    private void prefillFromPreferences() {
        etName.setText(preferenceManager.getUserName());
        etEmail.setText(preferenceManager.getUserEmail());
        etLocation.setText(preferenceManager.getUserLocation());

        List<String> hobbies = preferenceManager.getSelectedHobbies();
        etHobbies.setText(hobbies.isEmpty() ? "" : TextUtils.join(", ", hobbies));
    }

    private void setupClickListeners() {
        btnSaveProfile.setOnClickListener(v -> saveProfile());
        btnLogout.setOnClickListener(v -> logout());
    }

    private void saveProfile() {
        String name = safe(textOf(etName));
        String email = safe(textOf(etEmail));
        String location = safe(textOf(etLocation));
        String hobbiesCsv = safe(textOf(etHobbies));

        if (name.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, R.string.error_name_email_required, Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = preferenceManager.getUserId();
        if (userId.isEmpty()) userId = UUID.randomUUID().toString();

        List<String> hobbies = parseCsv(hobbiesCsv);

        preferenceManager.saveUserId(userId);
        preferenceManager.saveUserName(name);
        preferenceManager.saveUserEmail(email);
        preferenceManager.saveUserLocation(location);
        preferenceManager.saveSelectedHobbies(hobbies);

        userViewModel.saveUserProfile(
                userId, name, email, location, hobbies, preferenceManager.getUserRole());
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        preferenceManager.clearAll();
        Intent intent = new Intent(this, AuthActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onEventClick(Event event) {
        if (event == null || safe(event.getId()).isEmpty()) return;
        Intent i = new Intent(this, EventDetailActivity.class);
        i.putExtra(Constants.EXTRA_EVENT_ID, event.getId());
        startActivity(i);
    }

    @Override
    protected void onResume() {
        super.onResume();
        NavigationView navigationView = findViewById(R.id.navigationView);
        DrawerMenuHelper.applyRoleVisibility(navigationView, preferenceManager.isAdmin());
        updateRoleBadge(preferenceManager.getUserRole());
        eventViewModel.loadEvents();
    }

    private String textOf(EditText e) {
        return e.getText() == null ? "" : e.getText().toString();
    }

    private List<String> parseCsv(String csv) {
        List<String> result = new ArrayList<>();
        if (csv == null || csv.trim().isEmpty()) return result;
        String[] parts = csv.split(",");
        for (String p : parts) {
            if (p != null && !p.trim().isEmpty()) result.add(p.trim());
        }
        return result;
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    @Override
    protected int contentLayoutResId() {
        return R.layout.activity_profile;
    }

    @androidx.annotation.NonNull
    @Override
    protected String getDefaultTitle() {
        return "Profile";
    }
}