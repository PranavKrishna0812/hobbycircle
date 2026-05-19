package com.example.hobbycircle.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
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
import com.example.hobbycircle.utils.PreferenceManager;
import com.example.hobbycircle.viewmodel.EventViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.example.hobbycircle.ui.BaseDrawerActivity;

public class NearbyEventsActivity extends BaseDrawerActivity implements EventAdapter.OnEventClickListener {

    private EditText etSearchEvents;
    private RecyclerView rvNearbyEvents;
    private View tvNearbyEmpty;

    private EventViewModel eventViewModel;
    private PreferenceManager preferenceManager;
    private EventAdapter adapter;

    private List<Event> fullEvents = new ArrayList<>();
    private String searchQuery = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        preferenceManager = new PreferenceManager(this);

        etSearchEvents = findViewById(R.id.etSearchEvents);
        rvNearbyEvents = findViewById(R.id.rvNearbyEvents);
        tvNearbyEmpty = findViewById(R.id.tvNearbyEmpty);

        rvNearbyEvents.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EventAdapter(new ArrayList<>(), this);
        rvNearbyEvents.setAdapter(adapter);

        eventViewModel = new ViewModelProvider(this).get(EventViewModel.class);

        eventViewModel.getEventsLiveData().observe(this, events -> {
            fullEvents = events != null ? events : new ArrayList<>();
            applyNearbyAndSearch();
        });

        eventViewModel.getMessageLiveData().observe(this, msg -> {
            if (msg != null && !msg.trim().isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
        });

        etSearchEvents.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s != null ? s.toString().trim() : "";
                applyNearbyAndSearch();
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        eventViewModel.loadEvents();
    }

    /**
     * Nearby = not created by me, not joined; optional match on profile location.
     * Search = filters title, description, hobbyId, location (case-insensitive).
     */
    private void applyNearbyAndSearch() {
        String myId = preferenceManager.getUserId();
        String myLoc = safe(preferenceManager.getUserLocation()).toLowerCase(Locale.getDefault());
        String q = searchQuery.toLowerCase(Locale.getDefault());

        List<Event> out = new ArrayList<>();
        for (Event e : fullEvents) {
            if (e == null) continue;

            if (safe(e.getCreatedByUserId()).equals(myId)) continue;

            List<String> joined = e.getJoinedUserIds() != null ? e.getJoinedUserIds() : new ArrayList<>();
            if (joined.contains(myId)) continue;

            String locCombined = (safe(e.getLocation()) + " " + safe(e.getMapQuery())).toLowerCase(Locale.getDefault());
            if (!myLoc.isEmpty() && !locCombined.contains(myLoc)) continue;

            if (!q.isEmpty()) {
                String hay = (safe(e.getTitle()) + " "
                        + safe(e.getDescription()) + " "
                        + safe(e.getHobbyId()) + " "
                        + locCombined).toLowerCase(Locale.getDefault());
                if (!hay.contains(q)) continue;
            }

            out.add(e);
        }

        adapter.submitList(out);
        tvNearbyEmpty.setVisibility(out.isEmpty() ? View.VISIBLE : View.GONE);
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

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    @Override
    protected int contentLayoutResId() {
        return R.layout.activity_nearby_events;
    }

    @androidx.annotation.NonNull
    @Override
    protected String getDefaultTitle() {
        return "Nearby Events";
    }
}