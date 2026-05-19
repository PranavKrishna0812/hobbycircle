package com.example.hobbycircle.ui.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hobbycircle.R;
import com.example.hobbycircle.data.model.ChatMessage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private final List<ChatMessage> messageList;
    private final String currentUserId;

    public ChatMessageAdapter(List<ChatMessage> messageList, String currentUserId) {
        this.messageList = messageList;
        this.currentUserId = currentUserId;
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = messageList.get(position);
        if (message.getSenderId().equals(currentUserId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SENT) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_msg_sent, parent, false);
            return new SentViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_msg_received, parent, false);
            return new ReceivedViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messageList.get(position);
        if (getItemViewType(position) == VIEW_TYPE_SENT) {
            ((SentViewHolder) holder).bind(message);
        } else {
            ((ReceivedViewHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() {
        return messageList != null ? messageList.size() : 0;
    }

    public void submitList(List<ChatMessage> newList) {
        this.messageList.clear();
        if (newList != null) {
            this.messageList.addAll(newList);
        }
        notifyDataSetChanged();
    }

    static class SentViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvSentMsg;
        private final TextView tvSentTime;

        public SentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSentMsg = itemView.findViewById(R.id.tvSentMsg);
            tvSentTime = itemView.findViewById(R.id.tvSentTime);
        }

        public void bind(ChatMessage message) {
            tvSentMsg.setText(message.getMessage());
            if (message.getTimestamp() > 0L) {
                String timeFormatted = new SimpleDateFormat("hh:mm a", Locale.getDefault())
                        .format(new Date(message.getTimestamp()));
                tvSentTime.setText(timeFormatted);
                tvSentTime.setVisibility(View.VISIBLE);
            } else {
                tvSentTime.setVisibility(View.GONE);
            }
        }
    }

    static class ReceivedViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvSenderLabel;
        private final TextView tvReceivedMsg;
        private final TextView tvReceivedTime;

        public ReceivedViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSenderLabel = itemView.findViewById(R.id.tvSenderLabel);
            tvReceivedMsg = itemView.findViewById(R.id.tvReceivedMsg);
            tvReceivedTime = itemView.findViewById(R.id.tvReceivedTime);
        }

        public void bind(ChatMessage message) {
            tvSenderLabel.setText(message.getSenderName());
            tvReceivedMsg.setText(message.getMessage());
            if (message.getTimestamp() > 0L) {
                String timeFormatted = new SimpleDateFormat("hh:mm a", Locale.getDefault())
                        .format(new Date(message.getTimestamp()));
                tvReceivedTime.setText(timeFormatted);
                tvReceivedTime.setVisibility(View.VISIBLE);
            } else {
                tvReceivedTime.setVisibility(View.GONE);
            }
        }
    }
}
