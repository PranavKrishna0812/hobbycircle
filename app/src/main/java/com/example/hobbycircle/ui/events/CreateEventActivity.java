package com.example.hobbycircle.ui.events;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.hobbycircle.R;
import com.example.hobbycircle.data.model.Event;
import com.example.hobbycircle.ui.BaseDrawerActivity;
import com.example.hobbycircle.utils.Constants;
import com.example.hobbycircle.utils.PreferenceManager;
import com.example.hobbycircle.viewmodel.EventViewModel;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;


public class CreateEventActivity extends BaseDrawerActivity {

    private TextView tvCreateEventTitle;
    private EditText etTitle;
    private EditText etDescription;
    private EditText etHobbyId;
    private EditText etLocation;
    private EditText etEventTimeMillis;
    private Button btnCreateEvent;
    private ImageView ivEventImage;
    private Button btnAddImage;
    private Button btnPickLocation;

    private EventViewModel eventViewModel;
    private PreferenceManager preferenceManager;

    private byte[] selectedImageBytes = null;
    private String selectedMapQuery = "";
    private String existingImageUrl = "";

    private long selectedEventTimeMillis = 0L;
    private final Calendar selectedCalendar = Calendar.getInstance();

    private boolean editMode = false;
    private String editingEventId = "";
    private String draftEventId = "";
    private Event editingEvent = null;
    private boolean coverUploadReady = true;

    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    ivEventImage.setImageURI(uri);
                    try {
                        InputStream is = getContentResolver().openInputStream(uri);
                        if (is != null) {
                            ByteArrayOutputStream bos = new ByteArrayOutputStream();
                            byte[] buffer = new byte[1024];
                            int len;
                            while ((len = is.read(buffer)) != -1) {
                                bos.write(buffer, 0, len);
                            }
                            selectedImageBytes = bos.toByteArray();
                            is.close();
                            btnAddImage.setText(R.string.btn_change_cover_image);
                            coverUploadReady = false;
                            updateSubmitEnabled();
                            eventViewModel.uploadCoverImage(draftEventId, selectedImageBytes);
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, "Failed to read image: " + safe(e.getMessage()), Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private final ActivityResultLauncher<Intent> mapPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    double lat = data.getDoubleExtra("lat", 0);
                    double lng = data.getDoubleExtra("lng", 0);
                    String address = data.getStringExtra("address");

                    etLocation.setText(address != null ? address : "Selected Location");
                    selectedMapQuery = lat + "," + lng;
                }
            }
    );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        preferenceManager = new PreferenceManager(this);

        if (getIntent() != null) {
            editingEventId = safe(getIntent().getStringExtra(Constants.EXTRA_EVENT_ID));
            editMode = getIntent().getBooleanExtra(Constants.EXTRA_EDIT_EVENT, false)
                    || !editingEventId.isEmpty();
        }

        initViews();
        setupViewModel();
        setupTimePicker();
        btnCreateEvent.setOnClickListener(v -> saveEvent());

        draftEventId = editMode ? editingEventId : UUID.randomUUID().toString();

        if (editMode && !editingEventId.isEmpty()) {
            setDrawerTitle(getString(R.string.edit_meetup));
            if (tvCreateEventTitle != null) {
                tvCreateEventTitle.setText(R.string.edit_meetup);
            }
            btnCreateEvent.setText(R.string.save_meetup);
            eventViewModel.loadEventById(editingEventId);
        }
    }

    private void initViews() {
        tvCreateEventTitle = findViewById(R.id.tvCreateEventTitle);
        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        etHobbyId = findViewById(R.id.etHobbyId);
        etLocation = findViewById(R.id.etLocation);
        etEventTimeMillis = findViewById(R.id.etEventTimeMillis);
        btnCreateEvent = findViewById(R.id.btnCreateEvent);
        ivEventImage = findViewById(R.id.ivEventImage);
        btnAddImage = findViewById(R.id.btnAddImage);
        btnPickLocation = findViewById(R.id.btnPickLocation);

        btnAddImage.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        btnPickLocation.setOnClickListener(v -> {
            Intent intent = new Intent(this, MapPickerActivity.class);
            mapPickerLauncher.launch(intent);
        });
    }

    private void setupViewModel() {
        eventViewModel = new ViewModelProvider(this).get(EventViewModel.class);

        if (editMode) {
            eventViewModel.getSelectedEventLiveData().observe(this, event -> {
                if (event == null || safe(event.getId()).isEmpty()) {
                    return;
                }
                if (!canModifyEvent(event)) {
                    Toast.makeText(this, R.string.not_authorized, Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                editingEvent = event;
                populateForm(event);
            });
        }

        eventViewModel.getImageUploadingLiveData().observe(this, uploading -> {
            coverUploadReady = uploading == null || !uploading;
            if (Boolean.TRUE.equals(uploading)) {
                coverUploadReady = false;
            }
            updateSubmitEnabled();
        });

        eventViewModel.getImageUploadUrlLiveData().observe(this, url -> {
            if (url != null && !url.trim().isEmpty()) {
                existingImageUrl = url;
                coverUploadReady = true;
                updateSubmitEnabled();
            }
        });

        eventViewModel.getMessageLiveData().observe(this, message -> {
            if (message == null || message.trim().isEmpty()) {
                return;
            }
            Toast.makeText(CreateEventActivity.this, message, Toast.LENGTH_SHORT).show();
            if (message.equals(getString(R.string.msg_event_created))
                    || message.equals(getString(R.string.event_updated))) {
                finish();
            }
        });
    }

    private void populateForm(Event event) {
        etTitle.setText(event.getTitle());
        etDescription.setText(event.getDescription());
        etHobbyId.setText(event.getHobbyId());
        etLocation.setText(event.getLocation());
        selectedMapQuery = safe(event.getMapQuery());
        selectedEventTimeMillis = event.getEventTimeMillis();
        existingImageUrl = safe(event.getImageUrl());

        if (selectedEventTimeMillis > 0L) {
            selectedCalendar.setTimeInMillis(selectedEventTimeMillis);
            String formatted = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                    .format(new Date(selectedEventTimeMillis));
            etEventTimeMillis.setText(formatted);
        }

        if (!existingImageUrl.isEmpty()) {
            ivEventImage.setVisibility(View.VISIBLE);
            Glide.with(this).load(existingImageUrl).into(ivEventImage);
            btnAddImage.setText(R.string.btn_change_cover_image);
        }
    }

    private void updateSubmitEnabled() {
        boolean hasPendingImage = selectedImageBytes != null && selectedImageBytes.length > 0;
        btnCreateEvent.setEnabled(!hasPendingImage || coverUploadReady);
    }

    private boolean canModifyEvent(Event event) {
        if (event == null) {
            return false;
        }
        String userId = preferenceManager.getUserId();
        if (preferenceManager.isAdmin()) {
            return true;
        }
        return userId.equals(event.getCreatedByUserId());
    }

    private void setupTimePicker() {
        etEventTimeMillis.setOnClickListener(v -> openDatePicker());
    }

    private void openDatePicker() {
        Calendar now = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedCalendar.set(Calendar.YEAR, year);
                    selectedCalendar.set(Calendar.MONTH, month);
                    selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    openTimePicker();
                },
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH)
        );
        if (!editMode) {
            datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        }
        datePickerDialog.show();
    }

    private void openTimePicker() {
        Calendar now = Calendar.getInstance();
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    selectedCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    selectedCalendar.set(Calendar.MINUTE, minute);
                    selectedCalendar.set(Calendar.SECOND, 0);
                    selectedCalendar.set(Calendar.MILLISECOND, 0);

                    selectedEventTimeMillis = selectedCalendar.getTimeInMillis();
                    if (!editMode && selectedEventTimeMillis <= System.currentTimeMillis()) {
                        Toast.makeText(this, "Please select a future date/time.", Toast.LENGTH_SHORT).show();
                        selectedEventTimeMillis = 0L;
                        etEventTimeMillis.setText("");
                        return;
                    }

                    String formatted = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                            .format(selectedCalendar.getTime());
                    etEventTimeMillis.setText(formatted);
                },
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE),
                false
        );
        timePickerDialog.show();
    }

    private void saveEvent() {
        try {
            String title = safe(textOf(etTitle));
            String description = safe(textOf(etDescription));
            String hobbyId = safe(textOf(etHobbyId));
            String location = safe(textOf(etLocation));
            String mapQuery = safe(selectedMapQuery);

            if (title.isEmpty() || description.isEmpty() || hobbyId.isEmpty() || location.isEmpty()) {
                Toast.makeText(this, "Please fill all required fields.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedEventTimeMillis <= 0L) {
                Toast.makeText(this, "Please select a valid date/time.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!editMode && selectedEventTimeMillis <= System.currentTimeMillis()) {
                Toast.makeText(this, "Please select a valid future date/time.", Toast.LENGTH_SHORT).show();
                return;
            }

            String userId = preferenceManager.getUserId();
            if (userId.isEmpty()) {
                Toast.makeText(this, "Please complete profile first.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (editMode) {
                if (editingEvent == null) {
                    Toast.makeText(this, "Event is still loading.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!canModifyEvent(editingEvent)) {
                    Toast.makeText(this, R.string.not_authorized, Toast.LENGTH_SHORT).show();
                    return;
                }

                Event event = editingEvent;
                event.setTitle(title);
                event.setDescription(description);
                event.setHobbyId(hobbyId);
                event.setLocation(location);
                event.setMapQuery(mapQuery.isEmpty() ? (event.getMapQuery().isEmpty() ? location : event.getMapQuery()) : mapQuery);
                event.setEventTimeMillis(selectedEventTimeMillis);
                if (selectedImageBytes == null && !existingImageUrl.isEmpty()) {
                    event.setImageUrl(existingImageUrl);
                }
                eventViewModel.updateEvent(event, selectedImageBytes, preferenceManager.getUserId());
                return;
            }

            Event event = new Event();
            event.setId(draftEventId);
            event.setTitle(title);
            event.setDescription(description);
            event.setHobbyId(hobbyId);
            event.setLocation(location);
            event.setMapQuery(mapQuery.isEmpty() ? location : mapQuery);
            event.setEventTimeMillis(selectedEventTimeMillis);
            event.setCreatedByUserId(userId);
            event.setJoinedUserIds(new ArrayList<String>());
            if (!existingImageUrl.isEmpty()) {
                event.setImageUrl(existingImageUrl);
            }

            eventViewModel.createEvent(event, null);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to save event: " + safe(e.getMessage()), Toast.LENGTH_SHORT).show();
        }
    }

    private String textOf(EditText editText) {
        return editText.getText() != null ? editText.getText().toString() : "";
    }

    private String safe(String value) {
        return value != null ? value.trim() : "";
    }

    @Override
    protected int contentLayoutResId() {
        return R.layout.activity_create_event;
    }

    @androidx.annotation.NonNull
    @Override
    protected String getDefaultTitle() {
        return editMode ? getString(R.string.edit_meetup) : "Create Event";
    }
}
