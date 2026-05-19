package com.example.hobbycircle.ui.events;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.hobbycircle.R;
import com.example.hobbycircle.data.model.Event;
import com.example.hobbycircle.ui.BaseDrawerActivity;
import com.example.hobbycircle.utils.Constants;
import com.example.hobbycircle.utils.PreferenceManager;
import com.example.hobbycircle.viewmodel.EventViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

public class CreateEventActivity extends BaseDrawerActivity {

    private LinearProgressIndicator progressStepper;

    private TextInputLayout tilEventTitle;
    private TextInputEditText etTitle;
    private TextInputLayout tilEventDescription;
    private TextInputEditText etDescription;
    private TextInputLayout tilHobbyTag;
    private TextInputEditText etHobbyId;
    private TextInputLayout tilAttendeeLimit;
    private TextInputEditText etAttendeeLimit;

    private MaterialCardView cardDatePicker;
    private TextView tvSelectedDate;
    private MaterialCardView cardTimePicker;
    private TextView tvSelectedTime;

    private TextInputLayout tilLocation;
    private TextInputEditText etLocation;
    private MaterialButton btnPickOnMap;
    private MaterialCardView cardMapPreview;
    private ImageView ivMapPreview;

    private MaterialCardView cardImagePicker;
    private LinearLayout layoutPickerDefault;
    private FrameLayout layoutPickerPreview;
    private ImageView ivCoverPreview;
    private MaterialButton btnChangeCover;

    private MaterialButton btnCancel;
    private MaterialButton btnCreateEvent;

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

    private static final String PRESET_SPORTS = "https://images.unsplash.com/photo-1461896836934-ffe607ba8211?q=80&w=600&auto=format&fit=crop";
    private static final String PRESET_MUSIC = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?q=80&w=600&auto=format&fit=crop";
    private static final String PRESET_GAMING = "https://images.unsplash.com/photo-1538481199705-c710c4e965fc?q=80&w=600&auto=format&fit=crop";
    private static final String PRESET_CREATIVE = "https://images.unsplash.com/photo-1460661419201-fd4cecdf8a8b?q=80&w=600&auto=format&fit=crop";
    private static final String PRESET_READING = "https://images.unsplash.com/photo-1481627834876-b7833e8f5570?q=80&w=600&auto=format&fit=crop";
    private static final String PRESET_SOCIAL = "https://images.unsplash.com/photo-1511632765486-a01980e01a18?q=80&w=600&auto=format&fit=crop";
    private static final String PRESET_NATURE = "https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?q=80&w=600&auto=format&fit=crop";

    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
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

                            layoutPickerDefault.setVisibility(View.GONE);
                            layoutPickerPreview.setVisibility(View.VISIBLE);
                            ivCoverPreview.setImageURI(uri);

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

                    loadMapPreview(lat, lng);
                    updateProgress();
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

        draftEventId = editMode ? editingEventId : UUID.randomUUID().toString();

        initViews();
        setupViewModel();
        setupActions();

        if (editMode && !editingEventId.isEmpty()) {
            setDrawerTitle("Edit Event");
            btnCreateEvent.setText("Save Event");
            eventViewModel.loadEventById(editingEventId);
        } else {
            setDrawerTitle("Create Event");
            btnCreateEvent.setText("Create Event");
        }

        updateProgress();
    }

    private void initViews() {
        progressStepper = findViewById(R.id.progressStepper);

        tilEventTitle = findViewById(R.id.tilEventTitle);
        etTitle = findViewById(R.id.etTitle);
        tilEventDescription = findViewById(R.id.tilEventDescription);
        etDescription = findViewById(R.id.etDescription);
        tilHobbyTag = findViewById(R.id.tilHobbyTag);
        etHobbyId = findViewById(R.id.etHobbyId);
        tilAttendeeLimit = findViewById(R.id.tilAttendeeLimit);
        etAttendeeLimit = findViewById(R.id.etAttendeeLimit);

        cardDatePicker = findViewById(R.id.cardDatePicker);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        cardTimePicker = findViewById(R.id.cardTimePicker);
        tvSelectedTime = findViewById(R.id.tvSelectedTime);

        tilLocation = findViewById(R.id.tilLocation);
        etLocation = findViewById(R.id.etLocation);
        btnPickOnMap = findViewById(R.id.btnPickOnMap);
        cardMapPreview = findViewById(R.id.cardMapPreview);
        ivMapPreview = findViewById(R.id.ivMapPreview);

        cardImagePicker = findViewById(R.id.cardImagePicker);
        layoutPickerDefault = findViewById(R.id.layoutPickerDefault);
        layoutPickerPreview = findViewById(R.id.layoutPickerPreview);
        ivCoverPreview = findViewById(R.id.ivCoverPreview);
        btnChangeCover = findViewById(R.id.btnChangeCover);

        btnCancel = findViewById(R.id.btnCancel);
        btnCreateEvent = findViewById(R.id.btnCreateEvent);

        // Bind preset drawables dynamically
        MaterialCardView presetSports = findViewById(R.id.presetSports);
        MaterialCardView presetMusic = findViewById(R.id.presetMusic);
        MaterialCardView presetGaming = findViewById(R.id.presetGaming);
        MaterialCardView presetCreative = findViewById(R.id.presetCreative);
        MaterialCardView presetReading = findViewById(R.id.presetReading);
        MaterialCardView presetSocial = findViewById(R.id.presetSocial);
        MaterialCardView presetNature = findViewById(R.id.presetNature);

        ImageView ivPresetSports = findViewById(R.id.ivPresetSports);
        ImageView ivPresetMusic = findViewById(R.id.ivPresetMusic);
        ImageView ivPresetGaming = findViewById(R.id.ivPresetGaming);
        ImageView ivPresetCreative = findViewById(R.id.ivPresetCreative);
        ImageView ivPresetReading = findViewById(R.id.ivPresetReading);
        ImageView ivPresetSocial = findViewById(R.id.ivPresetSocial);
        ImageView ivPresetNature = findViewById(R.id.ivPresetNature);

        // Load with Glide for stunning performance
        Glide.with(this).load(PRESET_SPORTS).into(ivPresetSports);
        Glide.with(this).load(PRESET_MUSIC).into(ivPresetMusic);
        Glide.with(this).load(PRESET_GAMING).into(ivPresetGaming);
        Glide.with(this).load(PRESET_CREATIVE).into(ivPresetCreative);
        Glide.with(this).load(PRESET_READING).into(ivPresetReading);
        Glide.with(this).load(PRESET_SOCIAL).into(ivPresetSocial);
        Glide.with(this).load(PRESET_NATURE).into(ivPresetNature);

        presetSports.setOnClickListener(v -> selectPresetCover(PRESET_SPORTS));
        presetMusic.setOnClickListener(v -> selectPresetCover(PRESET_MUSIC));
        presetGaming.setOnClickListener(v -> selectPresetCover(PRESET_GAMING));
        presetCreative.setOnClickListener(v -> selectPresetCover(PRESET_CREATIVE));
        presetReading.setOnClickListener(v -> selectPresetCover(PRESET_READING));
        presetSocial.setOnClickListener(v -> selectPresetCover(PRESET_SOCIAL));
        presetNature.setOnClickListener(v -> selectPresetCover(PRESET_NATURE));
    }

    private void setupViewModel() {
        eventViewModel = new ViewModelProvider(this).get(EventViewModel.class);

        if (editMode) {
            eventViewModel.getSelectedEventLiveData().observe(this, event -> {
                if (event == null || safe(event.getId()).isEmpty()) {
                    return;
                }
                if (!canModifyEvent(event)) {
                    Toast.makeText(this, "You are not authorized to modify this event.", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                editingEvent = event;
                populateForm(event);
            });
        }

        eventViewModel.getImageUploadingLiveData().observe(this, uploading -> {
            coverUploadReady = uploading == null || !uploading;
            updateSubmitEnabled();
        });

        eventViewModel.getImageUploadUrlLiveData().observe(this, url -> {
            if (url != null && !url.trim().isEmpty()) {
                existingImageUrl = url;
                coverUploadReady = true;
                updateSubmitEnabled();
                updateProgress();
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

    private void setupActions() {
        cardDatePicker.setOnClickListener(v -> openDatePicker());
        cardTimePicker.setOnClickListener(v -> openTimePicker());

        btnPickOnMap.setOnClickListener(v -> {
            Intent intent = new Intent(this, MapPickerActivity.class);
            mapPickerLauncher.launch(intent);
        });

        View.OnClickListener pickPhotoListener = v -> imagePickerLauncher.launch("image/*");
        cardImagePicker.setOnClickListener(pickPhotoListener);
        layoutPickerDefault.setOnClickListener(pickPhotoListener);
        btnChangeCover.setOnClickListener(pickPhotoListener);

        btnCancel.setOnClickListener(v -> finish());
        btnCreateEvent.setOnClickListener(v -> saveEvent());

        // Real-time Textwatchers for smooth stepper update
        TextWatcher stepperWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                updateProgress();
            }
        };

        etTitle.addTextChangedListener(stepperWatcher);
        etDescription.addTextChangedListener(stepperWatcher);
        etHobbyId.addTextChangedListener(stepperWatcher);
        etLocation.addTextChangedListener(stepperWatcher);
    }

    private void selectPresetCover(String url) {
        selectedImageBytes = null;
        existingImageUrl = url;
        coverUploadReady = true;

        layoutPickerDefault.setVisibility(View.GONE);
        layoutPickerPreview.setVisibility(View.VISIBLE);
        Glide.with(this).load(url).into(ivCoverPreview);

        updateSubmitEnabled();
        updateProgress();
    }

    private void populateForm(Event event) {
        etTitle.setText(event.getTitle());
        etDescription.setText(event.getDescription());
        etHobbyId.setText(event.getHobbyId());
        etLocation.setText(event.getLocation());
        if (event.getAttendeeLimit() > 0) {
            etAttendeeLimit.setText(String.valueOf(event.getAttendeeLimit()));
        } else {
            etAttendeeLimit.setText("");
        }
        selectedMapQuery = safe(event.getMapQuery());
        selectedEventTimeMillis = event.getEventTimeMillis();
        existingImageUrl = safe(event.getImageUrl());

        if (selectedEventTimeMillis > 0L) {
            selectedCalendar.setTimeInMillis(selectedEventTimeMillis);
            
            String dateFormatted = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date(selectedEventTimeMillis));
            tvSelectedDate.setText(dateFormatted);

            String timeFormatted = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date(selectedEventTimeMillis));
            tvSelectedTime.setText(timeFormatted);
        }

        if (!selectedMapQuery.isEmpty()) {
            try {
                String[] parts = selectedMapQuery.split(",");
                if (parts.length == 2) {
                    double lat = Double.parseDouble(parts[0].trim());
                    double lng = Double.parseDouble(parts[1].trim());
                    loadMapPreview(lat, lng);
                }
            } catch (Exception ignored) {}
        }

        if (!existingImageUrl.isEmpty()) {
            layoutPickerDefault.setVisibility(View.GONE);
            layoutPickerPreview.setVisibility(View.VISIBLE);
            Glide.with(this).load(existingImageUrl).into(ivCoverPreview);
        }

        updateProgress();
    }

    private void updateProgress() {
        int progress = 0;

        String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
        String desc = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";
        String hobby = etHobbyId.getText() != null ? etHobbyId.getText().toString().trim() : "";
        if (!title.isEmpty() && !desc.isEmpty() && !hobby.isEmpty()) {
            progress += 25;
        }

        String dateText = tvSelectedDate.getText().toString().trim();
        String timeText = tvSelectedTime.getText().toString().trim();
        if (!dateText.equalsIgnoreCase("Select Date") && !timeText.equalsIgnoreCase("Select Time") && selectedEventTimeMillis > 0L) {
            progress += 25;
        }

        String location = etLocation.getText() != null ? etLocation.getText().toString().trim() : "";
        if (!location.isEmpty()) {
            progress += 25;
        }

        if (selectedImageBytes != null || !existingImageUrl.isEmpty()) {
            progress += 25;
        }

        progressStepper.setProgress(Math.max(25, progress));
    }

    private void updateSubmitEnabled() {
        boolean hasPendingImage = selectedImageBytes != null && selectedImageBytes.length > 0;
        btnCreateEvent.setEnabled(!hasPendingImage || coverUploadReady);
    }

    private boolean canModifyEvent(Event event) {
        if (event == null) {
            return false;
        }
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String userId = currentUser != null ? currentUser.getUid() : preferenceManager.getUserId();
        if (preferenceManager.isAdmin()) {
            return true;
        }
        return userId.equals(event.getCreatedByUserId());
    }

    private void openDatePicker() {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Date")
                .setSelection(selectedEventTimeMillis > 0 ? selectedEventTimeMillis : MaterialDatePicker.todayInUtcMilliseconds())
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            utc.setTimeInMillis(selection);

            selectedCalendar.set(Calendar.YEAR, utc.get(Calendar.YEAR));
            selectedCalendar.set(Calendar.MONTH, utc.get(Calendar.MONTH));
            selectedCalendar.set(Calendar.DAY_OF_MONTH, utc.get(Calendar.DAY_OF_MONTH));

            selectedEventTimeMillis = selectedCalendar.getTimeInMillis();
            String formatted = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(selectedCalendar.getTime());
            tvSelectedDate.setText(formatted);
            updateProgress();
        });

        datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
    }

    private void openTimePicker() {
        MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                .setTimeFormat(android.text.format.DateFormat.is24HourFormat(this) ? TimeFormat.CLOCK_24H : TimeFormat.CLOCK_12H)
                .setHour(selectedCalendar.get(Calendar.HOUR_OF_DAY))
                .setMinute(selectedCalendar.get(Calendar.MINUTE))
                .setTitleText("Select Time")
                .build();

        timePicker.addOnPositiveButtonClickListener(v -> {
            selectedCalendar.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
            selectedCalendar.set(Calendar.MINUTE, timePicker.getMinute());
            selectedCalendar.set(Calendar.SECOND, 0);
            selectedCalendar.set(Calendar.MILLISECOND, 0);

            selectedEventTimeMillis = selectedCalendar.getTimeInMillis();
            String formatted = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(selectedCalendar.getTime());
            tvSelectedTime.setText(formatted);
            updateProgress();
        });

        timePicker.show(getSupportFragmentManager(), "TIME_PICKER");
    }

    private void loadMapPreview(double lat, double lng) {
        String key = getMapsApiKey();
        if (key.isEmpty()) {
            return;
        }
        String url = "https://maps.googleapis.com/maps/api/staticmap?center=" + lat + "," + lng + "&zoom=15&size=400x200&key=" + key;
        cardMapPreview.setVisibility(View.VISIBLE);
        Glide.with(this).load(url).placeholder(R.drawable.ic_event_empty).into(ivMapPreview);
    }

    private String getMapsApiKey() {
        try {
            android.content.pm.ApplicationInfo ai = getPackageManager().getApplicationInfo(getPackageName(), android.content.pm.PackageManager.GET_META_DATA);
            android.os.Bundle bundle = ai.metaData;
            if (bundle != null) {
                return safe(bundle.getString("com.google.android.geo.API_KEY", ""));
            }
        } catch (Exception ignored) {}
        return "";
    }

    private void saveEvent() {
        try {
            String title = safe(etTitle.getText() != null ? etTitle.getText().toString() : "");
            String description = safe(etDescription.getText() != null ? etDescription.getText().toString() : "");
            String hobbyId = safe(etHobbyId.getText() != null ? etHobbyId.getText().toString() : "");
            String location = safe(etLocation.getText() != null ? etLocation.getText().toString() : "");
            String limitStr = safe(etAttendeeLimit.getText() != null ? etAttendeeLimit.getText().toString() : "");
            int attendeeLimit = 0;
            if (!limitStr.isEmpty()) {
                try {
                    attendeeLimit = Integer.parseInt(limitStr);
                } catch (Exception ignored) {}
            }
            String mapQuery = safe(selectedMapQuery);

            boolean hasError = false;

            if (title.isEmpty()) {
                tilEventTitle.setError("Event title is required");
                hasError = true;
            } else {
                tilEventTitle.setError(null);
            }

            if (description.isEmpty()) {
                tilEventDescription.setError("Description is required");
                hasError = true;
            } else {
                tilEventDescription.setError(null);
            }

            if (hobbyId.isEmpty()) {
                tilHobbyTag.setError("Hobby tag is required");
                hasError = true;
            } else {
                tilHobbyTag.setError(null);
            }

            if (location.isEmpty()) {
                tilLocation.setError("Location is required");
                hasError = true;
            } else {
                tilLocation.setError(null);
            }

            if (selectedEventTimeMillis <= 0L) {
                Toast.makeText(this, "Please select a valid date and time.", Toast.LENGTH_SHORT).show();
                hasError = true;
            }

            if (!editMode && selectedEventTimeMillis <= System.currentTimeMillis()) {
                Toast.makeText(this, "Please select a valid future date and time.", Toast.LENGTH_SHORT).show();
                hasError = true;
            }

            if (hasError) {
                return;
            }

            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            String userId = currentUser != null ? currentUser.getUid() : preferenceManager.getUserId();
            if (userId.isEmpty()) {
                Toast.makeText(this, "Please complete your profile first.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (editMode) {
                if (editingEvent == null) {
                    Toast.makeText(this, "Event details not loaded yet.", Toast.LENGTH_SHORT).show();
                    return;
                }

                Event event = editingEvent;
                event.setTitle(title);
                event.setDescription(description);
                event.setHobbyId(hobbyId);
                event.setLocation(location);
                event.setMapQuery(mapQuery.isEmpty() ? (event.getMapQuery().isEmpty() ? location : event.getMapQuery()) : mapQuery);
                event.setDateTime(selectedEventTimeMillis);
                event.setCreatorId(userId);
                event.setCreatorName(preferenceManager.getUserName());
                event.setAttendeeLimit(attendeeLimit);
                if (selectedImageBytes == null && !existingImageUrl.isEmpty()) {
                    event.setImageUrl(existingImageUrl);
                }

                eventViewModel.updateEvent(event, selectedImageBytes, userId);
            } else {
                Event event = new Event();
                event.setId(draftEventId);
                event.setTitle(title);
                event.setDescription(description);
                event.setHobbyId(hobbyId);
                event.setLocation(location);
                event.setMapQuery(mapQuery.isEmpty() ? location : mapQuery);
                event.setDateTime(selectedEventTimeMillis);
                event.setCreatorId(userId);
                event.setCreatorName(preferenceManager.getUserName());
                event.setAttendeeLimit(attendeeLimit);
                event.setJoinedUserIds(new ArrayList<>());
                if (!existingImageUrl.isEmpty()) {
                    event.setImageUrl(existingImageUrl);
                }

                eventViewModel.createEvent(event, selectedImageBytes);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Failed to save event: " + safe(e.getMessage()), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected int contentLayoutResId() {
        return R.layout.activity_create_event;
    }

    @NonNull
    @Override
    protected String getDefaultTitle() {
        return "Create Event";
    }

    private String safe(String value) {
        return value != null ? value.trim() : "";
    }
}
