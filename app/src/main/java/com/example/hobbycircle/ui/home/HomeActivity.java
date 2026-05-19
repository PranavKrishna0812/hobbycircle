package com.example.hobbycircle.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hobbycircle.R;
import com.example.hobbycircle.data.model.Event;
import com.example.hobbycircle.ui.auth.ProfileActivity;
import com.example.hobbycircle.ui.details.EventDetailActivity;
import com.example.hobbycircle.ui.events.CreateEventActivity;
import com.example.hobbycircle.ui.events.EventAdapter;
import com.example.hobbycircle.ui.admin.AdminEventsActivity;
import com.example.hobbycircle.ui.home.AcceptedEventsActivity;
import com.example.hobbycircle.utils.Constants;
import com.example.hobbycircle.utils.DrawerMenuHelper;
import com.example.hobbycircle.utils.PreferenceManager;
import com.example.hobbycircle.viewmodel.EventViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity implements
        EventAdapter.OnEventClickListener,
        NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageButton btnMenu;
    private TextView tvWelcome;
    private RecyclerView rvEventsHome;
    private FloatingActionButton fabCreateEvent;

    private PreferenceManager preferenceManager;
    private EventViewModel eventViewModel;
    private EventAdapter eventAdapter;

    private List<Event> fullEvents = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        preferenceManager = new PreferenceManager(this);

        initViews();
        setupRecycler();
        setupViewModel();
        setupClicks();
        renderHeader();

        eventViewModel.loadEvents();
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        btnMenu = findViewById(R.id.btnMenu);
        tvWelcome = findViewById(R.id.tvWelcome);
        rvEventsHome = findViewById(R.id.rvEventsHome);
        fabCreateEvent = findViewById(R.id.fabCreateEvent);

        navigationView.setNavigationItemSelectedListener(this);
        DrawerMenuHelper.applyRoleVisibility(navigationView, preferenceManager.isAdmin());
    }

    private void setupRecycler() {
        rvEventsHome.setLayoutManager(new LinearLayoutManager(this));
        eventAdapter = new EventAdapter(new ArrayList<>(), this);
        rvEventsHome.setAdapter(eventAdapter);
    }

    private void setupViewModel() {
        eventViewModel = new ViewModelProvider(this).get(EventViewModel.class);

        eventViewModel.getEventsLiveData().observe(this, events -> {
            fullEvents = events != null ? events : new ArrayList<>();
            showAcceptedOnly();
        });

        eventViewModel.getMessageLiveData().observe(this, msg -> {
            if (msg != null && !msg.trim().isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupClicks() {
        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        fabCreateEvent.setOnClickListener(v ->
                startActivity(new Intent(this, CreateEventActivity.class)));
    }

    private void renderHeader() {
        String name = preferenceManager.getUserName();
        tvWelcome.setText(name.isEmpty()
                ? getString(R.string.welcome_default)
                : getString(R.string.welcome_user, name));
    }

    /** Home feed: events current user has accepted (joined). */
    private void showAcceptedOnly() {
        String currentUserId = preferenceManager.getUserId();

        List<Event> filtered = new ArrayList<>();
        for (Event event : fullEvents) {
            if (event == null) continue;

            List<String> joined = event.getJoinedUserIds() != null ? event.getJoinedUserIds() : new ArrayList<>();
            if (joined.contains(currentUserId)) {
                filtered.add(event);
            }
        }

        eventAdapter.submitList(filtered);
    }

    @Override
    public void onEventClick(Event event) {
        if (event == null || safe(event.getId()).isEmpty()) return;

        Intent intent = new Intent(this, EventDetailActivity.class);
        intent.putExtra(Constants.EXTRA_EVENT_ID, event.getId());
        startActivity(intent);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull android.view.MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            // Already on home, just close drawer
        } else if (id == R.id.nav_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
        } else if (id == R.id.nav_accepted_events) {
            startActivity(new Intent(this, AcceptedEventsActivity.class));
        } else if (id == R.id.nav_nearby_events) {
            startActivity(new Intent(this, NearbyEventsActivity.class));
        } else if (id == R.id.nav_create_event) {
            startActivity(new Intent(this, CreateEventActivity.class));
        } else if (id == R.id.nav_manage_events) {
            if (preferenceManager.isAdmin()) {
                startActivity(new Intent(this, AdminEventsActivity.class));
            }
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderHeader();
        DrawerMenuHelper.applyRoleVisibility(navigationView, preferenceManager.isAdmin());
        eventViewModel.loadEvents();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }
}