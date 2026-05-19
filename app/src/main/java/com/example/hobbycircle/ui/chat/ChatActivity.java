package com.example.hobbycircle.ui.chat;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hobbycircle.R;
import com.example.hobbycircle.data.model.ChatMessage;
import com.example.hobbycircle.data.model.ChatThread;
import com.example.hobbycircle.utils.Constants;
import com.example.hobbycircle.utils.PreferenceManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private Toolbar toolbarChat;
    private RecyclerView rvMessages;
    private View layoutEmptyChat;
    private EditText etChatMessage;
    private FloatingActionButton fabSend;

    private ChatMessageAdapter adapter;
    private final List<ChatMessage> messageList = new ArrayList<>();

    private PreferenceManager preferenceManager;
    private FirebaseFirestore db;
    private ListenerRegistration registration;

    private String currentUserId = "";
    private String currentUserName = "";

    // Intent extras
    private String eventId = "";
    private String eventTitle = "";
    private String userId = "";
    private String userName = "";
    private String organiserId = "";
    private String organiserName = "";

    private String chatThreadId = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        preferenceManager = new PreferenceManager(this);
        db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
            currentUserName = currentUser.getDisplayName() != null && !currentUser.getDisplayName().trim().isEmpty()
                    ? currentUser.getDisplayName().trim()
                    : (preferenceManager.getUserName().isEmpty() ? "User" : preferenceManager.getUserName());
        } else {
            currentUserId = preferenceManager.getUserId();
            currentUserName = preferenceManager.getUserName().isEmpty() ? "User" : preferenceManager.getUserName();
        }

        initIntentData();
        initViews();
        setupMessagesList();
        setupActions();
    }

    private void initIntentData() {
        if (getIntent() != null) {
            eventId = safe(getIntent().getStringExtra(Constants.EXTRA_EVENT_ID));
            eventTitle = safe(getIntent().getStringExtra(Constants.EXTRA_EVENT_TITLE));
            userId = safe(getIntent().getStringExtra(Constants.EXTRA_USER_ID));
            userName = safe(getIntent().getStringExtra(Constants.EXTRA_USER_NAME));
            organiserId = safe(getIntent().getStringExtra(Constants.EXTRA_ORGANISER_ID));
            organiserName = safe(getIntent().getStringExtra(Constants.EXTRA_ORGANISER_NAME));
        }

        if (eventId.isEmpty() || organiserId.isEmpty() || userId.isEmpty()) {
            Toast.makeText(this, "Failed to load chat thread.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // The unique conversation thread id is eventId_userId
        chatThreadId = eventId + "_" + userId;
    }

    private void initViews() {
        toolbarChat = findViewById(R.id.toolbarChat);
        rvMessages = findViewById(R.id.rvMessages);
        layoutEmptyChat = findViewById(R.id.layoutEmptyChat);
        etChatMessage = findViewById(R.id.etChatMessage);
        fabSend = findViewById(R.id.fabSend);

        // Customize toolbar
        setSupportActionBar(toolbarChat);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Toolbar title should show the other person's name
        String chatPartnerName = currentUserId.equals(organiserId) ? userName : organiserName;
        toolbarChat.setTitle(chatPartnerName.isEmpty() ? "Organizer Chat" : chatPartnerName);
        toolbarChat.setSubtitle("Regarding: " + (eventTitle.isEmpty() ? "Event Details" : eventTitle));
    }

    private void setupMessagesList() {
        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatMessageAdapter(messageList, currentUserId);
        rvMessages.setAdapter(adapter);
    }

    private void setupActions() {
        toolbarChat.setNavigationOnClickListener(v -> onBackPressed());
        fabSend.setOnClickListener(v -> sendMessage());
    }

    @Override
    protected void onStart() {
        super.onStart();
        listenForMessages();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (registration != null) {
            registration.remove();
        }
    }

    private void listenForMessages() {
        if (chatThreadId.isEmpty()) return;

        CollectionReference messagesRef = db.collection("chats")
                .document(chatThreadId)
                .collection("messages");

        registration = messagesRef.orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Listen error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (value != null) {
                        List<ChatMessage> list = value.toObjects(ChatMessage.class);
                        messageList.clear();
                        messageList.addAll(list);
                        adapter.submitList(messageList);

                        layoutEmptyChat.setVisibility(messageList.isEmpty() ? View.VISIBLE : View.GONE);

                        if (!messageList.isEmpty()) {
                            rvMessages.scrollToPosition(messageList.size() - 1);
                        }
                    }
                });
    }

    private void sendMessage() {
        String text = etChatMessage.getText() != null ? etChatMessage.getText().toString().trim() : "";
        if (text.isEmpty()) {
            return;
        }

        long timestamp = System.currentTimeMillis();
        ChatMessage message = new ChatMessage(currentUserId, currentUserName, text, timestamp);

        // Clear field immediately for premium feel
        etChatMessage.setText("");

        // 1. Write message to messages subcollection
        DocumentReference threadRef = db.collection("chats").document(chatThreadId);
        threadRef.collection("messages").add(message)
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to send message: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

        // 2. Update conversation thread summary for easy querying
        ChatThread summary = new ChatThread(
                chatThreadId,
                eventId,
                eventTitle,
                userId,
                userName,
                organiserId,
                organiserName,
                text,
                timestamp
        );
        threadRef.set(summary)
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update thread: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private String safe(String value) {
        return value != null ? value.trim() : "";
    }
}
