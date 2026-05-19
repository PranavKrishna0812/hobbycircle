package com.example.hobbycircle.ui.details;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.hobbycircle.R;
import com.example.hobbycircle.data.model.Event;
import com.example.hobbycircle.data.model.Warning;
import com.example.hobbycircle.utils.Constants;
import com.example.hobbycircle.utils.NotificationHelper;
import com.example.hobbycircle.utils.PreferenceManager;
import com.example.hobbycircle.viewmodel.EventViewModel;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EventDetailActivity extends AppCompatActivity {

    private CollapsingToolbarLayout collapsingToolbar;
    private Toolbar toolbarDetail;
    private ImageView ivEventHero;
    private TextView tvDetailTitle;
    private Chip chipDetailHobby;
    private Chip chipAttendees;
    private TextView tvDetailDate;
    private TextView tvDetailLocation;
    private LinearLayout llLocationContainer;
    private TextView tvDetailCreator;
    private TextView tvDetailDescription;
    private MaterialButton btnJoinLeave;
    private MaterialButton btnEdit;
    private MaterialButton btnDelete;
    private MaterialButton btnChat;
    private MaterialButton btnViewChats;
    private MaterialButton btnWarn;

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
        collapsingToolbar = findViewById(R.id.collapsingToolbar);
        toolbarDetail = findViewById(R.id.toolbarDetail);
        ivEventHero = findViewById(R.id.ivEventHero);
        tvDetailTitle = findViewById(R.id.tvDetailTitle);
        chipDetailHobby = findViewById(R.id.chipDetailHobby);
        chipAttendees = findViewById(R.id.chipAttendees);
        tvDetailDate = findViewById(R.id.tvDetailDate);
        tvDetailLocation = findViewById(R.id.tvDetailLocation);
        llLocationContainer = findViewById(R.id.llLocationContainer);
        tvDetailCreator = findViewById(R.id.tvDetailCreator);
        tvDetailDescription = findViewById(R.id.tvDetailDescription);
        btnJoinLeave = findViewById(R.id.btnJoinLeave);
        btnEdit = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);
        btnChat = findViewById(R.id.btnChat);
        btnViewChats = findViewById(R.id.btnViewChats);
        btnWarn = findViewById(R.id.btnWarn);

        setSupportActionBar(toolbarDetail);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    private void initData() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = currentUser != null ? currentUser.getUid() : preferenceManager.getUserId();
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
        toolbarDetail.setNavigationOnClickListener(v -> onBackPressed());

        btnJoinLeave.setOnClickListener(v -> {
            if (currentUserId.isEmpty()) {
                Toast.makeText(this, R.string.error_complete_profile, Toast.LENGTH_SHORT).show();
                return;
            }
            if (eventId.isEmpty()) {
                Toast.makeText(this, R.string.error_invalid_event, Toast.LENGTH_SHORT).show();
                return;
            }

            if (currentEvent == null) {
                Toast.makeText(this, "Event details not loaded yet.", Toast.LENGTH_SHORT).show();
                return;
            }

            List<String> participants = currentEvent.getJoinedUserIds();
            boolean joined = participants != null && participants.contains(currentUserId);

            if (joined) {
                eventViewModel.leaveEvent(eventId, currentUserId);
                new NotificationHelper(this).cancelEventReminder(eventId);
                Toast.makeText(this, "Left event successfully.", Toast.LENGTH_SHORT).show();
            } else {
                boolean hasStarted = System.currentTimeMillis() >= currentEvent.getDateTime();
                if (hasStarted) {
                    Toast.makeText(this, "Cannot join an event that has already started.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (currentEvent.getAttendeeLimit() > 0 && participants != null && participants.size() >= currentEvent.getAttendeeLimit()) {
                    Toast.makeText(this, "This event has reached its maximum capacity of " + currentEvent.getAttendeeLimit() + " participants.", Toast.LENGTH_LONG).show();
                    return;
                }
                eventViewModel.joinEvent(eventId, currentUserId, currentEvent);
                new NotificationHelper(this).scheduleEventReminder(
                        eventId,
                        currentEvent.getTitle(),
                        currentEvent.getLocation(),
                        currentEvent.getEventTimeMillis()
                );
                Toast.makeText(this, "Joined event successfully! Reminder scheduled 1 hour before.", Toast.LENGTH_SHORT).show();
            }
        });

        llLocationContainer.setOnClickListener(v -> openMap());

        btnChat.setOnClickListener(v -> {
            if (currentUserId.isEmpty()) {
                Toast.makeText(this, R.string.error_complete_profile, Toast.LENGTH_SHORT).show();
                return;
            }
            if (currentEvent == null) {
                Toast.makeText(this, "Event details not loaded yet.", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(this, com.example.hobbycircle.ui.chat.ChatActivity.class);
            intent.putExtra(Constants.EXTRA_EVENT_ID, currentEvent.getId());
            intent.putExtra(Constants.EXTRA_EVENT_TITLE, currentEvent.getTitle());
            intent.putExtra(Constants.EXTRA_USER_ID, currentUserId);
            intent.putExtra(Constants.EXTRA_USER_NAME, preferenceManager.getUserName());
            intent.putExtra(Constants.EXTRA_ORGANISER_ID, currentEvent.getCreatedByUserId());
            intent.putExtra(Constants.EXTRA_ORGANISER_NAME, currentEvent.getCreatorName().isEmpty() ? "Organizer" : currentEvent.getCreatorName());
            startActivity(intent);
        });

        btnViewChats.setOnClickListener(v -> {
            if (currentEvent == null) {
                Toast.makeText(this, "Event details not loaded yet.", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(this, com.example.hobbycircle.ui.chat.EventChatsActivity.class);
            intent.putExtra(Constants.EXTRA_EVENT_ID, currentEvent.getId());
            intent.putExtra(Constants.EXTRA_EVENT_TITLE, currentEvent.getTitle());
            startActivity(intent);
        });

        btnDelete.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Event")
                .setMessage("Are you sure you want to delete this event?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (!eventId.isEmpty()) {
                        eventViewModel.deleteEvent(eventId);
                        Toast.makeText(this, "Event deleted successfully.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
        });

        btnEdit.setOnClickListener(v -> {
            if (currentEvent == null) {
                return;
            }
            Intent intent = new Intent(this, com.example.hobbycircle.ui.events.CreateEventActivity.class);
            intent.putExtra(Constants.EXTRA_EVENT_ID, currentEvent.getId());
            intent.putExtra(Constants.EXTRA_EDIT_EVENT, true);
            startActivity(intent);
        });

        btnWarn.setOnClickListener(v -> {
            if (currentEvent == null) {
                Toast.makeText(this, "Event details not loaded yet.", Toast.LENGTH_SHORT).show();
                return;
            }

            android.widget.EditText input = new android.widget.EditText(this);
            input.setHint("Type the warning message here...");
            input.setSingleLine(false);
            input.setMinLines(3);
            input.setGravity(android.view.Gravity.TOP);

            // Container layout to add padding around EditText
            android.widget.FrameLayout container = new android.widget.FrameLayout(this);
            android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
            int margin = (int) (16 * getResources().getDisplayMetrics().density);
            params.leftMargin = margin;
            params.rightMargin = margin;
            params.topMargin = margin;
            params.bottomMargin = margin;
            input.setLayoutParams(params);
            container.addView(input);

            new MaterialAlertDialogBuilder(this)
                .setTitle("Send Warning to Creator")
                .setMessage("This warning will be sent to the creator of this event: " + (currentEvent.getCreatorName().isEmpty() ? "Organizer" : currentEvent.getCreatorName()))
                .setView(container)
                .setPositiveButton("Send Warning", (dialog, which) -> {
                    String msg = input.getText().toString().trim();
                    if (msg.isEmpty()) {
                        Toast.makeText(this, "Warning message cannot be empty.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Warning w = new Warning(
                            null,
                            currentEvent.getId(),
                            currentEvent.getTitle(),
                            currentEvent.getCreatedByUserId(),
                            msg,
                            System.currentTimeMillis(),
                            false
                    );
                    eventViewModel.sendWarning(w);
                    Toast.makeText(this, "Warning message submitted.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
        });
    }

    private void bindEvent(Event event) {
        // Expand title text in collapsing toolbar layout
        collapsingToolbar.setTitle(nonEmpty(event.getTitle(), "Untitled Event"));
        
        tvDetailTitle.setText(nonEmpty(event.getTitle(), "Untitled Event"));
        tvDetailDescription.setText(nonEmpty(event.getDescription(), "No description available."));
        chipDetailHobby.setText(nonEmpty(event.getHobbyId(), "General"));
        tvDetailLocation.setText(nonEmpty(event.getLocation(), "N/A"));
        tvDetailDate.setText(formatTime(event.getEventTimeMillis()));
        tvDetailCreator.setText(nonEmpty(event.getCreatorName(), "Organizer"));

        List<String> participants = event.getJoinedUserIds();
        int count = participants != null ? participants.size() : 0;
        if (event.getAttendeeLimit() > 0) {
            chipAttendees.setText(String.format(Locale.getDefault(), "%d / %d going", count, event.getAttendeeLimit()));
        } else {
            chipAttendees.setText(String.format(Locale.getDefault(), "%d going", count));
        }

        boolean joined = participants != null && participants.contains(currentUserId);
        boolean hasStarted = System.currentTimeMillis() >= event.getDateTime();

        if (joined) {
            btnJoinLeave.setText("Leave Event");
            btnJoinLeave.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.hc_text_secondary)));
            btnJoinLeave.setEnabled(true);
        } else {
            if (hasStarted) {
                btnJoinLeave.setText("Event Started");
                btnJoinLeave.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.hc_text_secondary)));
                btnJoinLeave.setEnabled(false);
            } else {
                btnJoinLeave.setText("Join Event");
                btnJoinLeave.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.hc_accent)));
                btnJoinLeave.setEnabled(true);
            }
        }

        // Image Handling via Glide
        String imageUrl = safe(event.getImageUrl());
        Glide.with(this)
             .load(!imageUrl.isEmpty() ? imageUrl : R.drawable.ic_event_empty)
             .placeholder(R.drawable.ic_event_empty)
             .error(R.drawable.ic_event_empty)
             .into(ivEventHero);

        boolean isAdmin = preferenceManager.isAdmin();
        boolean canManage = canManageEvent(event);

        if (isAdmin) {
            btnJoinLeave.setVisibility(View.GONE);
            btnChat.setVisibility(View.GONE);
            btnViewChats.setVisibility(View.GONE);
            btnEdit.setVisibility(View.GONE);
            btnDelete.setVisibility(View.VISIBLE);
            btnWarn.setVisibility(View.VISIBLE);
        } else {
            btnWarn.setVisibility(View.GONE);
            btnDelete.setVisibility(canManage ? View.VISIBLE : View.GONE);
            btnEdit.setVisibility(canManage ? View.VISIBLE : View.GONE);

            if (canManage) {
                btnJoinLeave.setVisibility(View.GONE);
                btnChat.setVisibility(View.GONE);
                btnViewChats.setVisibility(View.VISIBLE);
            } else {
                btnJoinLeave.setVisibility(View.VISIBLE);
                btnChat.setVisibility(View.VISIBLE);
                btnViewChats.setVisibility(View.GONE);
            }
        }
    }

    private boolean canManageEvent(Event event) {
        if (event == null) {
            return false;
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