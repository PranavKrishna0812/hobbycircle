package com.example.hobbycircle.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hobbycircle.R;
import com.example.hobbycircle.data.model.Event;
import com.example.hobbycircle.data.model.User;
import com.example.hobbycircle.data.model.Warning;
import com.example.hobbycircle.ui.BaseDrawerActivity;
import com.example.hobbycircle.ui.details.EventDetailActivity;
import com.example.hobbycircle.ui.events.CreateEventActivity;
import com.example.hobbycircle.ui.events.EventAdapter;
import com.example.hobbycircle.utils.Constants;
import com.example.hobbycircle.utils.PreferenceManager;
import com.example.hobbycircle.viewmodel.EventViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import android.widget.ScrollView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HomeActivity extends BaseDrawerActivity implements EventAdapter.OnEventClickListener {

    private TextView tvGreeting;
    private TextView tvEventCount;
    private RecyclerView rvJoinedEvents;
    private View emptyState;
    private Button btnBrowseNearby;
    private FloatingActionButton fabCreateEvent;

    private PreferenceManager preferenceManager;
    private EventViewModel eventViewModel;
    private EventAdapter eventAdapter;

    private ScrollView adminAnalyticsContainer;
    private TextView tvTotalUsers;
    private TextView tvTotalEvents;
    private RecyclerView rvCreatorRatings;
    private CreatorRatingAdapter creatorRatingAdapter;
    private List<User> allUsers = new ArrayList<>();
    private List<Event> allEvents = new ArrayList<>();

    private List<Event> fullEvents = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // BaseDrawerActivity will handle setContentView(R.layout.activity_base_drawer)
        // and inflating contentLayoutResId()

        preferenceManager = new PreferenceManager(this);

        initViews();
        setupRecycler();
        setupViewModel();
        setupClicks();
        renderHeaderAndGreeting();

        if (preferenceManager.isAdmin()) {
            adminAnalyticsContainer.setVisibility(View.VISIBLE);
            rvJoinedEvents.setVisibility(View.GONE);
            emptyState.setVisibility(View.GONE);
            fabCreateEvent.setVisibility(View.GONE);
            tvEventCount.setText("Admin Panel Analytics");
        } else {
            adminAnalyticsContainer.setVisibility(View.GONE);
            fabCreateEvent.setVisibility(View.VISIBLE);
        }

        loadData();
    }

    private void initViews() {
        tvGreeting = findViewById(R.id.tvGreeting);
        tvEventCount = findViewById(R.id.tvEventCount);
        rvJoinedEvents = findViewById(R.id.rvJoinedEvents);
        emptyState = findViewById(R.id.emptyState);
        btnBrowseNearby = findViewById(R.id.btnBrowseNearby);
        fabCreateEvent = findViewById(R.id.fabCreateEvent);

        adminAnalyticsContainer = findViewById(R.id.adminAnalyticsContainer);
        tvTotalUsers = findViewById(R.id.tvTotalUsers);
        tvTotalEvents = findViewById(R.id.tvTotalEvents);
        rvCreatorRatings = findViewById(R.id.rvCreatorRatings);
    }

    private void setupRecycler() {
        rvJoinedEvents.setLayoutManager(new LinearLayoutManager(this));
        eventAdapter = new EventAdapter(new ArrayList<>(), this);
        eventAdapter.setOnEventRatingChangeListener((event, rating) -> {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            String currentUserId = currentUser != null ? currentUser.getUid() : preferenceManager.getUserId();
            if (!currentUserId.isEmpty()) {
                eventViewModel.rateEvent(event.getId(), currentUserId, rating);
            }
        });
        rvJoinedEvents.setAdapter(eventAdapter);

        if (preferenceManager.isAdmin()) {
            rvCreatorRatings.setLayoutManager(new LinearLayoutManager(this));
            creatorRatingAdapter = new CreatorRatingAdapter(new ArrayList<>());
            rvCreatorRatings.setAdapter(creatorRatingAdapter);
        }
    }

    private void setupViewModel() {
        eventViewModel = new ViewModelProvider(this).get(EventViewModel.class);

        eventViewModel.getEventsLiveData().observe(this, events -> {
            fullEvents = events != null ? events : new ArrayList<>();
            allEvents = fullEvents;
            if (preferenceManager.isAdmin()) {
                updateAdminAnalytics();
            } else {
                showAcceptedOnly();
            }
        });

        eventViewModel.getUsersLiveData().observe(this, users -> {
            allUsers = users != null ? users : new ArrayList<>();
            if (preferenceManager.isAdmin()) {
                updateAdminAnalytics();
            }
        });

        eventViewModel.getWarningsLiveData().observe(this, warnings -> {
            if (warnings != null && !warnings.isEmpty()) {
                showWarningPopup(warnings);
            }
        });

        eventViewModel.getMessageLiveData().observe(this, msg -> {
            if (msg != null && !msg.trim().isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupClicks() {
        fabCreateEvent.setOnClickListener(v ->
                startActivity(new Intent(this, CreateEventActivity.class)));

        btnBrowseNearby.setOnClickListener(v ->
                startActivity(new Intent(this, NearbyEventsActivity.class)));
    }

    private void renderHeaderAndGreeting() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String name = "";
        String email = "";

        if (currentUser != null) {
            name = currentUser.getDisplayName();
            email = currentUser.getEmail();
        }
        if (name == null || name.trim().isEmpty()) {
            name = preferenceManager.getUserName();
        }
        if (email == null || email.trim().isEmpty()) {
            email = preferenceManager.getUserEmail();
        }

        String displayName = (name != null && !name.trim().isEmpty()) ? name.trim() : "User";
        String displayEmail = (email != null && !email.trim().isEmpty()) ? email.trim() : "user@example.com";

        tvGreeting.setText(String.format("Good morning, %s 👋", displayName));

        if (getNavigationView() != null) {
            View headerView = getNavigationView().getHeaderView(0);
            if (headerView != null) {
                TextView tvUserInitial = headerView.findViewById(R.id.tvUserInitial);
                TextView tvHeaderName = headerView.findViewById(R.id.tvHeaderName);
                TextView tvHeaderEmail = headerView.findViewById(R.id.tvHeaderEmail);

                if (tvHeaderName != null) tvHeaderName.setText(displayName);
                if (tvHeaderEmail != null) tvHeaderEmail.setText(displayEmail);
                if (tvUserInitial != null && !displayName.isEmpty()) {
                    tvUserInitial.setText(String.valueOf(displayName.charAt(0)).toUpperCase());
                }
            }
        }
    }

    private void showAcceptedOnly() {
        if (preferenceManager.isAdmin()) {
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String currentUserId = currentUser != null ? currentUser.getUid() : preferenceManager.getUserId();

        List<Event> filtered = new ArrayList<>();
        for (Event event : fullEvents) {
            if (event == null) continue;

            List<String> joined = event.getJoinedUserIds() != null ? event.getJoinedUserIds() : new ArrayList<>();
            if (joined.contains(currentUserId)) {
                filtered.add(event);
            }
        }

        int count = filtered.size();
        if (tvEventCount != null) {
            if (count == 1) {
                tvEventCount.setText("You have 1 upcoming event");
            } else {
                tvEventCount.setText(String.format(Locale.getDefault(), "You have %d upcoming events", count));
            }
        }

        if (emptyState != null && rvJoinedEvents != null) {
            if (count == 0) {
                emptyState.setVisibility(View.VISIBLE);
                rvJoinedEvents.setVisibility(View.GONE);
            } else {
                emptyState.setVisibility(View.GONE);
                rvJoinedEvents.setVisibility(View.VISIBLE);
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
    protected void onResume() {
        super.onResume();
        renderHeaderAndGreeting();
        loadData();
    }

    @Override
    protected int contentLayoutResId() {
        return R.layout.activity_home;
    }

    @NonNull
    @Override
    protected String getDefaultTitle() {
        return "Home";
    }

    private void loadData() {
        eventViewModel.loadEvents();
        if (preferenceManager.isAdmin()) {
            eventViewModel.loadAllUsers();
        } else {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            String currentUserId = currentUser != null ? currentUser.getUid() : preferenceManager.getUserId();
            if (!currentUserId.isEmpty()) {
                eventViewModel.fetchUnreadWarnings(currentUserId);
            }
        }
    }

    private void updateAdminAnalytics() {
        if (!preferenceManager.isAdmin()) return;

        int totalUsers = allUsers.size();
        int totalEvents = allEvents.size();

        tvTotalUsers.setText(String.valueOf(totalUsers));
        tvTotalEvents.setText(String.valueOf(totalEvents));

        List<CreatorRatingAdapter.CreatorStats> statsList = new ArrayList<>();
        for (User u : allUsers) {
            if (u == null) continue;
            // Admin user is not a creator, filter out if needed or include
            if (u.isAdmin()) continue;

            int count = 0;
            double ratingSum = 0.0;
            int ratingsCount = 0;

            for (Event e : allEvents) {
                if (e != null && safe(e.getCreatedByUserId()).equals(u.getId())) {
                    count++;
                    if (e.getRatings() != null && !e.getRatings().isEmpty()) {
                        for (Long val : e.getRatings().values()) {
                            ratingSum += val;
                            ratingsCount++;
                        }
                    }
                }
            }

            double avg = ratingsCount > 0 ? (ratingSum / ratingsCount) : -1.0;
            statsList.add(new CreatorRatingAdapter.CreatorStats(u, count, avg));
        }

        // Sort creators: highest rating first, then by count
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            statsList.sort((o1, o2) -> {
                if (o1.averageRating >= 0 && o2.averageRating < 0) return -1;
                if (o1.averageRating < 0 && o2.averageRating >= 0) return 1;
                if (o1.averageRating >= 0 && o2.averageRating >= 0) {
                    return Double.compare(o2.averageRating, o1.averageRating);
                }
                return Integer.compare(o2.eventsCreatedCount, o1.eventsCreatedCount);
            });
        }

        creatorRatingAdapter.submitList(statsList);
    }

    private void showWarningPopup(List<Warning> warnings) {
        if (warnings == null || warnings.isEmpty()) return;

        StringBuilder sb = new StringBuilder();
        sb.append("The administrator has issued the following warning(s) regarding your events:\n\n");
        for (int i = 0; i < warnings.size(); i++) {
            Warning w = warnings.get(i);
            sb.append(String.format(Locale.getDefault(), "%d. Event: \"%s\"\nReason: %s\n\n",
                    i + 1, w.getEventTitle(), w.getMessage()));
        }

        new MaterialAlertDialogBuilder(this)
            .setTitle("⚠️ Admin Warning")
            .setMessage(sb.toString())
            .setPositiveButton("I Understand", (dialog, which) -> {
                for (Warning w : warnings) {
                    eventViewModel.markWarningAsRead(w.getId());
                }
            })
            .setCancelable(false)
            .show();
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }
}