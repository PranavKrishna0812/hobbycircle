package com.example.hobbycircle.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hobbycircle.R;
import com.example.hobbycircle.data.model.Event;
import com.example.hobbycircle.ui.BaseDrawerActivity;
import com.example.hobbycircle.ui.details.EventDetailActivity;
import com.example.hobbycircle.ui.events.CreateEventActivity;
import com.example.hobbycircle.ui.events.EventAdapter;
import com.example.hobbycircle.utils.Constants;
import com.example.hobbycircle.utils.PreferenceManager;
import com.example.hobbycircle.viewmodel.EventViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NearbyEventsActivity extends BaseDrawerActivity implements EventAdapter.OnEventClickListener {

    private EditText etSearch;
    private ImageView ivClearSearch;
    private ChipGroup cgHobbies;
    private RecyclerView rvNearbyEvents;
    private View emptyState;
    private ExtendedFloatingActionButton fabCreate;

    private EventViewModel eventViewModel;
    private PreferenceManager preferenceManager;
    private EventAdapter adapter;

    private List<Event> fullEvents = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        preferenceManager = new PreferenceManager(this);

        initViews();
        setupRecycler();
        setupViewModel();
        setupListeners();

        eventViewModel.loadEvents();
    }

    private void initViews() {
        etSearch = findViewById(R.id.etSearch);
        ivClearSearch = findViewById(R.id.ivClearSearch);
        cgHobbies = findViewById(R.id.cgHobbies);
        rvNearbyEvents = findViewById(R.id.rvNearbyEvents);
        emptyState = findViewById(R.id.emptyState);
        fabCreate = findViewById(R.id.fabCreate);
    }

    private void setupRecycler() {
        rvNearbyEvents.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EventAdapter(new ArrayList<>(), this);
        rvNearbyEvents.setAdapter(adapter);
    }

    private void setupViewModel() {
        eventViewModel = new ViewModelProvider(this).get(EventViewModel.class);

        eventViewModel.getEventsLiveData().observe(this, events -> {
            fullEvents = events != null ? events : new ArrayList<>();
            applyFilters();
        });

        eventViewModel.getMessageLiveData().observe(this, msg -> {
            if (msg != null && !msg.trim().isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupListeners() {
        fabCreate.setOnClickListener(v ->
                startActivity(new Intent(this, CreateEventActivity.class)));

        ivClearSearch.setOnClickListener(v -> {
            etSearch.setText("");
            applyFilters();
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String q = s != null ? s.toString().trim() : "";
                ivClearSearch.setVisibility(q.isEmpty() ? View.GONE : View.VISIBLE);
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        cgHobbies.setOnCheckedChangeListener((group, checkedId) -> applyFilters());
    }

    private void applyFilters() {
        String myId = preferenceManager.getUserId();
        String myLoc = safe(preferenceManager.getUserLocation()).toLowerCase(Locale.getDefault());
        String query = etSearch.getText().toString().trim().toLowerCase(Locale.getDefault());

        // Get selected hobby name from chip
        int checkedChipId = cgHobbies.getCheckedChipId();
        String selectedHobby = "";
        if (checkedChipId != View.NO_ID) {
            Chip chip = findViewById(checkedChipId);
            if (chip != null) {
                selectedHobby = chip.getText().toString().trim();
            }
        }

        List<Event> filtered = new ArrayList<>();
        for (Event event : fullEvents) {
            if (event == null) continue;

            // Do not show my own events
            if (safe(event.getCreatedByUserId()).equals(myId)) continue;

            // Do not show events I have already joined
            List<String> joined = event.getJoinedUserIds() != null ? event.getJoinedUserIds() : new ArrayList<>();
            if (joined.contains(myId)) continue;

            // Filter by location (if user location is set)
            String locCombined = (safe(event.getLocation()) + " " + safe(event.getMapQuery())).toLowerCase(Locale.getDefault());
            if (!myLoc.isEmpty() && !locCombined.contains(myLoc)) continue;

            // Filter by hobby (if selected and not "All")
            if (!selectedHobby.isEmpty() && !selectedHobby.equalsIgnoreCase("All")) {
                if (!safe(event.getHobbyId()).equalsIgnoreCase(selectedHobby)) {
                    continue;
                }
            }

            // Filter by search query
            if (!query.isEmpty()) {
                String searchableText = (safe(event.getTitle()) + " "
                        + safe(event.getDescription()) + " "
                        + safe(event.getHobbyId()) + " "
                        + locCombined).toLowerCase(Locale.getDefault());
                if (!searchableText.contains(query)) {
                    continue;
                }
            }

            filtered.add(event);
        }

        adapter.submitList(filtered);
        emptyState.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
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
        eventViewModel.loadEvents();
    }

    @Override
    protected int contentLayoutResId() {
        return R.layout.activity_nearby_events;
    }

    @NonNull
    @Override
    protected String getDefaultTitle() {
        return "Discover Events";
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }
}