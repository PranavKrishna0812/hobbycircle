package com.example.hobbycircle.ui.events;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.hobbycircle.R;
import com.example.hobbycircle.data.model.Event;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    public interface OnEventClickListener {
        void onEventClick(Event event);
    }

    private final List<Event> events = new ArrayList<>();
    private final OnEventClickListener listener;

    public EventAdapter(List<Event> initialList, OnEventClickListener listener) {
        this.listener = listener;
        submitList(initialList);
    }

    public void submitList(List<Event> newList) {
        events.clear();
        if (newList != null) {
            events.addAll(newList);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        holder.bind(events.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {

        private final ImageView ivEventCover;
        private final TextView tvEventTitle;
        private final TextView tvEventDescription;
        private final TextView tvEventLocation;
        private final TextView tvEventTime;
        private final TextView tvEventParticipants;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            ivEventCover = itemView.findViewById(R.id.ivEventCover);
            tvEventTitle = itemView.findViewById(R.id.tvEventTitle);
            tvEventDescription = itemView.findViewById(R.id.tvEventDescription);
            tvEventLocation = itemView.findViewById(R.id.tvEventLocation);
            tvEventTime = itemView.findViewById(R.id.tvEventTime);
            tvEventParticipants = itemView.findViewById(R.id.tvEventParticipants);
        }

        void bind(Event event, OnEventClickListener listener) {
            if (event == null) {
                bindCoverImage(null);
                tvEventTitle.setText(itemView.getContext().getString(R.string.event_untitled));
                tvEventDescription.setText(itemView.getContext().getString(R.string.event_no_description));
                tvEventLocation.setText(itemView.getContext().getString(R.string.event_location_na));
                tvEventTime.setText(itemView.getContext().getString(R.string.event_time_not_set));
                tvEventParticipants.setText(itemView.getContext().getString(R.string.event_participants_count, 0));
                itemView.setOnClickListener(null);
                return;
            }

            bindCoverImage(event.getImageUrl());

            String title = safe(event.getTitle(), itemView.getContext().getString(R.string.event_untitled));
            String description = safe(event.getDescription(), itemView.getContext().getString(R.string.event_no_description));
            String location = safe(event.getLocation(), itemView.getContext().getString(R.string.event_detail_location_na));
            String timeText = formatTime(event.getEventTimeMillis());
            int participantCount = event.getJoinedUserIds() != null ? event.getJoinedUserIds().size() : 0;

            tvEventTitle.setText(title);
            tvEventDescription.setText(description);
            tvEventLocation.setText(itemView.getContext().getString(R.string.event_location_format, location));
            tvEventTime.setText(itemView.getContext().getString(R.string.event_time_format, timeText));
            tvEventParticipants.setText(itemView.getContext().getString(R.string.event_participants_count, participantCount));

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEventClick(event);
                }
            });
        }

        private void bindCoverImage(String imageUrl) {
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                ivEventCover.setVisibility(View.GONE);
                Glide.with(itemView.getContext()).clear(ivEventCover);
                return;
            }

            ivEventCover.setVisibility(View.VISIBLE);
            Glide.with(itemView.getContext())
                    .load(imageUrl.trim())
                    .placeholder(R.drawable.bg_event_cover_placeholder)
                    .error(R.drawable.bg_event_cover_placeholder)
                    .centerCrop()
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(ivEventCover);
        }

        private String formatTime(long eventTimeMillis) {
            if (eventTimeMillis <= 0L) {
                return itemView.getContext().getString(R.string.event_detail_time_not_set);
            }
            return DateFormat.format("dd MMM yyyy, hh:mm a", new Date(eventTimeMillis)).toString();
        }

        private String safe(String value, String fallback) {
            if (value == null) {
                return fallback;
            }
            String trimmed = value.trim();
            return trimmed.isEmpty() ? fallback : trimmed;
        }
    }
}
