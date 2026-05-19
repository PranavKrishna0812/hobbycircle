package com.example.hobbycircle.ui.chat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hobbycircle.R;
import com.example.hobbycircle.data.model.ChatThread;
import com.example.hobbycircle.utils.Constants;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class EventChatsActivity extends AppCompatActivity implements ChatThreadAdapter.OnThreadClickListener {

    private Toolbar toolbarEventChats;
    private RecyclerView rvInquiries;
    private View layoutEmptyInquiries;

    private ChatThreadAdapter adapter;
    private final List<ChatThread> threadList = new ArrayList<>();

    private FirebaseFirestore db;
    private ListenerRegistration registration;

    private String eventId = "";
    private String eventTitle = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_chats);

        db = FirebaseFirestore.getInstance();

        initIntentData();
        initViews();
        setupList();
    }

    private void initIntentData() {
        if (getIntent() != null) {
            eventId = safe(getIntent().getStringExtra(Constants.EXTRA_EVENT_ID));
            eventTitle = safe(getIntent().getStringExtra(Constants.EXTRA_EVENT_TITLE));
        }

        if (eventId.isEmpty()) {
            Toast.makeText(this, "Event ID is missing.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initViews() {
        toolbarEventChats = findViewById(R.id.toolbarEventChats);
        rvInquiries = findViewById(R.id.rvInquiries);
        layoutEmptyInquiries = findViewById(R.id.layoutEmptyInquiries);

        setSupportActionBar(toolbarEventChats);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        toolbarEventChats.setTitle("Chat Inquiries");
        toolbarEventChats.setSubtitle(eventTitle.isEmpty() ? "Event Details" : eventTitle);

        toolbarEventChats.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupList() {
        rvInquiries.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatThreadAdapter(threadList, this);
        rvInquiries.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        listenForThreads();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (registration != null) {
            registration.remove();
        }
    }

    private void listenForThreads() {
        if (eventId.isEmpty()) return;

        registration = db.collection("chats")
                .whereEqualTo("eventId", eventId)
                .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Listen error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (value != null) {
                        List<ChatThread> list = value.toObjects(ChatThread.class);
                        threadList.clear();
                        threadList.addAll(list);
                        adapter.submitList(threadList);

                        layoutEmptyInquiries.setVisibility(threadList.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                });
    }

    @Override
    public void onThreadClick(ChatThread thread) {
        if (thread == null) return;

        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(Constants.EXTRA_EVENT_ID, thread.getEventId());
        intent.putExtra(Constants.EXTRA_EVENT_TITLE, thread.getEventTitle());
        intent.putExtra(Constants.EXTRA_USER_ID, thread.getUserId());
        intent.putExtra(Constants.EXTRA_USER_NAME, thread.getUserName());
        intent.putExtra(Constants.EXTRA_ORGANISER_ID, thread.getOrganiserId());
        intent.putExtra(Constants.EXTRA_ORGANISER_NAME, thread.getOrganiserName());
        startActivity(intent);
    }

    private String safe(String value) {
        return value != null ? value.trim() : "";
    }
}
