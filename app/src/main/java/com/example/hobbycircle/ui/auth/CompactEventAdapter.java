package com.example.hobbycircle.ui.auth;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hobbycircle.R;
import com.example.hobbycircle.data.model.Event;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CompactEventAdapter extends RecyclerView.Adapter<CompactEventAdapter.ViewHolder> {

    private final List<Event> eventList;
    private final OnCompactEventClickListener listener;

    public interface OnCompactEventClickListener {
        void onCompactEventClick(Event event);
    }

    public CompactEventAdapter(List<Event> eventList, OnCompactEventClickListener listener) {
        this.eventList = eventList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event_compact, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Event event = eventList.get(position);
        if (event != null) {
            holder.bind(event, listener);
        }
    }

    @Override
    public int getItemCount() {
        return eventList != null ? eventList.size() : 0;
    }

    public void submitList(List<Event> newList) {
        this.eventList.clear();
        if (newList != null) {
            this.eventList.addAll(newList);
        }
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvCompactTitle;
        private final TextView tvCompactDate;
        private final TextView tvCompactAttendees;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCompactTitle = itemView.findViewById(R.id.tvCompactTitle);
            tvCompactDate = itemView.findViewById(R.id.tvCompactDate);
            tvCompactAttendees = itemView.findViewById(R.id.tvCompactAttendees);
        }

        public void bind(Event event, OnCompactEventClickListener listener) {
            tvCompactTitle.setText(event.getTitle());

            if (event.getEventTimeMillis() > 0L) {
                String formatted = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                        .format(new Date(event.getEventTimeMillis()));
                tvCompactDate.setText(formatted);
            } else {
                tvCompactDate.setText("Date & Time TBD");
            }

            int count = event.getJoinedUserIds() != null ? event.getJoinedUserIds().size() : 0;
            tvCompactAttendees.setText(String.valueOf(count));

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCompactEventClick(event);
                }
            });
        }
    }
}
