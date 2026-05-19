package com.example.hobbycircle.ui.home;

import android.content.Intent;
import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.List;

public class AcceptedEventsActivity extends BaseDrawerActivity implements EventAdapter.OnEventClickListener {

    private RecyclerView rvAcceptedEvents;
    private EventAdapter eventAdapter;
    private EventViewModel eventViewModel;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        preferenceManager = new PreferenceManager(this);
        rvAcceptedEvents = findViewById(R.id.rvAcceptedEvents);
        rvAcceptedEvents.setLayoutManager(new LinearLayoutManager(this));
        eventAdapter = new EventAdapter(new ArrayList<>(), this);
        rvAcceptedEvents.setAdapter(eventAdapter);

        eventViewModel = new ViewModelProvider(this).get(EventViewModel.class);
        eventViewModel.getEventsLiveData().observe(this, events -> {
            String userId = preferenceManager.getUserId();
            List<Event> accepted = new ArrayList<>();
            if (events != null) {
                for (Event e : events) {
                    if (e != null && e.getJoinedUserIds() != null && e.getJoinedUserIds().contains(userId)) {
                        accepted.add(e);
                    }
                }
            }
            eventAdapter.submitList(accepted);
        });
        eventViewModel.getMessageLiveData().observe(this, msg -> {
            if (msg != null && !msg.trim().isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
        });

        eventViewModel.loadEvents();
    }

    @Override
    public void onEventClick(Event event) {
        if (event == null || event.getId() == null || event.getId().trim().isEmpty()) return;
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
        return R.layout.activity_accepted_events;
    }

    @NonNull
    @Override
    protected String getDefaultTitle() {
        return getString(R.string.accepted_events_title);
    }
}
