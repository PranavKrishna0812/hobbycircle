package com.example.hobbycircle.ui.details;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import com.bumptech.glide.Glide;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.hobbycircle.R;
import com.example.hobbycircle.data.model.Event;
import com.example.hobbycircle.utils.Constants;
import com.example.hobbycircle.utils.PreferenceManager;
import com.example.hobbycircle.ui.events.CreateEventActivity;
import com.example.hobbycircle.viewmodel.EventViewModel;

import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EventDetailActivity extends AppCompatActivity {

    private TextView tvTitle;
    private TextView tvDescription;
    private TextView tvHobby;
    private TextView tvLocation;
    private TextView tvTime;
    private TextView tvParticipants;
    private Button btnJoin;
    private Button btnLeave;
    private Button btnOpenMap;
    private Button btnEditEvent;
    private Button btnDeleteEvent;
    private ImageView ivEventHeroImage;

    private EventViewModel eventViewModel;
    private PreferenceManager preferenceManager;
    private String eventId = "";
    private String currentUserId = "";
    private Event currentEvent;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        preferenceManager = new PreferenceManager(this);

        initViews();
        initData();
        setupViewModel();
        setupActions();
    }

    private void initViews() {
        tvTitle = findViewById(R.id.tvTitle);
        tvDescription = findViewById(R.id.tvDescription);
        tvHobby = findViewById(R.id.tvHobby);
        tvLocation = findViewById(R.id.tvLocation);
        tvTime = findViewById(R.id.tvTime);
        tvParticipants = findViewById(R.id.tvParticipants);
        btnJoin = findViewById(R.id.btnJoin);
        btnLeave = findViewById(R.id.btnLeave);
        btnOpenMap = findViewById(R.id.btnOpenMap);
        btnEditEvent = findViewById(R.id.btnEditEvent);
        btnDeleteEvent = findViewById(R.id.btnDeleteEvent);
        ivEventHeroImage = findViewById(R.id.ivEventHeroImage);
    }

    private void initData() {
        currentUserId = preferenceManager.getUserId();
        eventId = getIntent() != null ? safe(getIntent().getStringExtra(Constants.EXTRA_EVENT_ID)) : "";

        if (eventId.isEmpty()) {
            Toast.makeText(this, "Event ID is missing.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!eventId.isEmpty()) {
            eventViewModel.loadEventById(eventId);
        }
    }

    private void setupViewModel() {
        eventViewModel = new ViewModelProvider(this).get(EventViewModel.class);

        eventViewModel.getSelectedEventLiveData().observe(this, event -> {
            if (event == null) {
                return;
            }
            currentEvent = event;
            bindEvent(event);
        });

        eventViewModel.getMessageLiveData().observe(this, message -> {
            if (message != null && !message.trim().isEmpty()) {
                Toast.makeText(EventDetailActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });

        eventViewModel.loadEventById(eventId);
    }

    private void setupActions() {
        btnJoin.setOnClickListener(v -> {
            if (currentUserId.isEmpty()) {
                Toast.makeText(this, R.string.error_complete_profile, Toast.LENGTH_SHORT).show();
                return;
            }
            if (eventId.isEmpty()) {
                Toast.makeText(this, R.string.error_invalid_event, Toast.LENGTH_SHORT).show();
                return;
            }
            eventViewModel.joinEvent(eventId, currentUserId, currentEvent);
        });

        btnLeave.setOnClickListener(v -> {
            if (currentUserId.isEmpty()) {
                Toast.makeText(this, R.string.error_complete_profile, Toast.LENGTH_SHORT).show();
                return;
            }
            if (eventId.isEmpty()) {
                Toast.makeText(this, R.string.error_invalid_event, Toast.LENGTH_SHORT).show();
                return;
            }
            eventViewModel.leaveEvent(eventId, currentUserId);
        });

        btnOpenMap.setOnClickListener(v -> openMap());

        btnEditEvent.setOnClickListener(v -> {
            if (currentEvent == null || eventId.isEmpty()) {
                Toast.makeText(this, "Event not loaded yet.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!canManageEvent(currentEvent)) {
                Toast.makeText(this, R.string.not_authorized, Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, CreateEventActivity.class);
            intent.putExtra(Constants.EXTRA_EVENT_ID, eventId);
            intent.putExtra(Constants.EXTRA_EDIT_EVENT, true);
            startActivity(intent);
        });

        btnDeleteEvent.setOnClickListener(v -> {
            if (!eventId.isEmpty()) {
                eventViewModel.deleteEvent(eventId);
                Toast.makeText(this, "Deleting event...", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void bindEvent(Event event) {
        tvTitle.setText(nonEmpty(event.getTitle(), "Untitled Event"));
        tvDescription.setText(nonEmpty(event.getDescription(), "No description"));
        tvHobby.setText(String.format(Locale.getDefault(), "Hobby: %s", nonEmpty(event.getHobbyId(), "General")));
        tvLocation.setText(String.format(Locale.getDefault(), "Location: %s", nonEmpty(event.getLocation(), "N/A")));
        tvTime.setText(String.format(Locale.getDefault(), "Time: %s", formatTime(event.getEventTimeMillis())));

        List<String> participants = event.getJoinedUserIds();
        int count = participants != null ? participants.size() : 0;
        tvParticipants.setText(String.format(Locale.getDefault(), "Participants: %d", count));

        boolean joined = participants != null && participants.contains(currentUserId);
        btnJoin.setEnabled(!joined);
        btnLeave.setEnabled(joined);

        // Image Handling
        String imageUrl = safe(event.getImageUrl());
        if (!imageUrl.isEmpty()) {
            ivEventHeroImage.setVisibility(View.VISIBLE);
            Glide.with(this)
                 .load(imageUrl)
                 .placeholder(android.R.color.transparent)
                 .into(ivEventHeroImage);
        } else {
            ivEventHeroImage.setVisibility(View.GONE);
        }

        boolean canManage = canManageEvent(event);
        btnEditEvent.setVisibility(canManage ? View.VISIBLE : View.GONE);
        btnDeleteEvent.setVisibility(canManage ? View.VISIBLE : View.GONE);
        if (preferenceManager.isAdmin()) {
            btnDeleteEvent.setText(R.string.btn_delete_event_admin);
        }
    }

    private boolean canManageEvent(Event event) {
        if (event == null) {
            return false;
        }
        if (preferenceManager.isAdmin()) {
            return true;
        }
        return currentUserId.equals(event.getCreatedByUserId());
    }

    private void openMap() {
        if (currentEvent == null) {
            Toast.makeText(this, "Event not loaded yet.", Toast.LENGTH_SHORT).show();
            return;
        }

        String query = safe(currentEvent.getMapQuery());
        if (query.isEmpty()) {
            query = safe(currentEvent.getLocation());
        }
        if (query.isEmpty()) {
            Toast.makeText(this, "No map location available.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Uri gmmIntentUri;
            if (query.matches("-?\\d+\\.\\d+,-?\\d+\\.\\d+")) {
                // If it's pure coordinates, use them directly as the query to drop a pin
                gmmIntentUri = Uri.parse("geo:" + query + "?q=" + query);
            } else {
                gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(query));
            }
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");

            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                Intent fallback = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                if (fallback.resolveActivity(getPackageManager()) != null) {
                    startActivity(fallback);
                } else {
                    Toast.makeText(this, "No map app found.", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open maps.", Toast.LENGTH_SHORT).show();
        }
    }

    private String formatTime(long millis) {
        if (millis <= 0L) {
            return "Not set";
        }
        return DateFormat.format("dd MMM yyyy, hh:mm a", new Date(millis)).toString();
    }

    private String nonEmpty(String value, String fallback) {
        String safe = safe(value);
        return safe.isEmpty() ? fallback : safe;
    }

    private String safe(String value) {
        return value != null ? value.trim() : "";
    }
}