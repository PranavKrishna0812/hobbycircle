package com.example.hobbycircle.ui.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hobbycircle.R;
import com.example.hobbycircle.data.model.ChatThread;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatThreadAdapter extends RecyclerView.Adapter<ChatThreadAdapter.ViewHolder> {

    private final List<ChatThread> threadList;
    private final OnThreadClickListener listener;

    public interface OnThreadClickListener {
        void onThreadClick(ChatThread thread);
    }

    public ChatThreadAdapter(List<ChatThread> threadList, OnThreadClickListener listener) {
        this.threadList = threadList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_thread, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatThread thread = threadList.get(position);
        if (thread != null) {
            holder.bind(thread, listener);
        }
    }

    @Override
    public int getItemCount() {
        return threadList != null ? threadList.size() : 0;
    }

    public void submitList(List<ChatThread> newList) {
        this.threadList.clear();
        if (newList != null) {
            this.threadList.addAll(newList);
        }
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvThreadInitial;
        private final TextView tvThreadName;
        private final TextView tvThreadTime;
        private final TextView tvThreadLastMsg;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvThreadInitial = itemView.findViewById(R.id.tvThreadInitial);
            tvThreadName = itemView.findViewById(R.id.tvThreadName);
            tvThreadTime = itemView.findViewById(R.id.tvThreadTime);
            tvThreadLastMsg = itemView.findViewById(R.id.tvThreadLastMsg);
        }

        public void bind(ChatThread thread, OnThreadClickListener listener) {
            String name = thread.getUserName().isEmpty() ? "Anonymous User" : thread.getUserName();
            tvThreadName.setText(name);

            String initial = name.substring(0, 1).toUpperCase(Locale.getDefault());
            tvThreadInitial.setText(initial);

            tvThreadLastMsg.setText(thread.getLastMessage());

            if (thread.getLastMessageTimestamp() > 0L) {
                String timeFormatted = new SimpleDateFormat("hh:mm a", Locale.getDefault())
                        .format(new Date(thread.getLastMessageTimestamp()));
                tvThreadTime.setText(timeFormatted);
            } else {
                tvThreadTime.setText("");
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onThreadClick(thread);
                }
            });
        }
    }
}
