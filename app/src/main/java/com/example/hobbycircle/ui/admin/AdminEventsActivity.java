package com.example.hobbycircle.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
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
import com.example.hobbycircle.ui.events.EventAdapter;
import com.example.hobbycircle.utils.Constants;
import com.example.hobbycircle.utils.PreferenceManager;
import com.example.hobbycircle.viewmodel.EventViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminEventsActivity extends BaseDrawerActivity implements EventAdapter.OnEventClickListener {

    private EditText etSearchEvents;
    private RecyclerView rvAdminEvents;
    private View tvAdminEmpty;

    private EventViewModel eventViewModel;
    private PreferenceManager preferenceManager;
    private EventAdapter adapter;

    private List<Event> fullEvents = new ArrayList<>();
    private String searchQuery = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        preferenceManager = new PreferenceManager(this);
        if (!preferenceManager.isAdmin()) {
            Toast.makeText(this, R.string.not_authorized, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        etSearchEvents = findViewById(R.id.etSearchEvents);
        rvAdminEvents = findViewById(R.id.rvAdminEvents);
        tvAdminEmpty = findViewById(R.id.tvAdminEmpty);

        rvAdminEvents.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EventAdapter(new ArrayList<>(), this);
        adapter.setOnEventRatingChangeListener((event, rating) -> {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            String currentUserId = currentUser != null ? currentUser.getUid() : preferenceManager.getUserId();
            if (!currentUserId.isEmpty()) {
                eventViewModel.rateEvent(event.getId(), currentUserId, rating);
            }
        });
        rvAdminEvents.setAdapter(adapter);

        eventViewModel = new ViewModelProvider(this).get(EventViewModel.class);
        eventViewModel.getEventsLiveData().observe(this, events -> {
            fullEvents = events != null ? events : new ArrayList<>();
            applySearch();
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
                applySearch();
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        eventViewModel.loadEvents();
    }

    private void applySearch() {
        String q = searchQuery.toLowerCase(Locale.getDefault());
        List<Event> out = new ArrayList<>();
        for (Event e : fullEvents) {
            if (e == null) continue;
            if (!q.isEmpty()) {
                String hay = (safe(e.getTitle()) + " "
                        + safe(e.getDescription()) + " "
                        + safe(e.getHobbyId()) + " "
                        + safe(e.getLocation())).toLowerCase(Locale.getDefault());
                if (!hay.contains(q)) continue;
            }
            out.add(e);
        }
        adapter.submitList(out);
        tvAdminEmpty.setVisibility(out.isEmpty() ? View.VISIBLE : View.GONE);
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
        if (preferenceManager != null && preferenceManager.isAdmin()) {
            eventViewModel.loadEvents();
        }
    }

    @Override
    protected int contentLayoutResId() {
        return R.layout.activity_admin_events;
    }

    @NonNull
    @Override
    protected String getDefaultTitle() {
        return getString(R.string.admin_events_title);
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
