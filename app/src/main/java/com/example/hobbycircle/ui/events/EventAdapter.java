package com.example.hobbycircle.ui.events;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.RatingBar;

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

    public interface OnEventRatingChangeListener {
        void onEventRatingChanged(Event event, int rating);
    }

    private final List<Event> events = new ArrayList<>();
    private final OnEventClickListener listener;
    private OnEventRatingChangeListener ratingChangeListener;

    public void setOnEventRatingChangeListener(OnEventRatingChangeListener listener) {
        this.ratingChangeListener = listener;
    }

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
        holder.bind(events.get(position), listener, ratingChangeListener);
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {

        private final ImageView ivEventImage;
        private final com.google.android.material.chip.Chip chipHobby;
        private final TextView tvEventTitle;
        private final TextView tvEventDate;
        private final TextView tvEventLocation;
        private final View layoutAverageRating;
        private final TextView tvAverageRating;
        private final View layoutRatingContainer;
        private final RatingBar ratingBarEvent;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            ivEventImage = itemView.findViewById(R.id.ivEventImage);
            chipHobby = itemView.findViewById(R.id.chipHobby);
            tvEventTitle = itemView.findViewById(R.id.tvEventTitle);
            tvEventDate = itemView.findViewById(R.id.tvEventDate);
            tvEventLocation = itemView.findViewById(R.id.tvEventLocation);
            layoutAverageRating = itemView.findViewById(R.id.layoutAverageRating);
            tvAverageRating = itemView.findViewById(R.id.tvAverageRating);
            layoutRatingContainer = itemView.findViewById(R.id.layoutRatingContainer);
            ratingBarEvent = itemView.findViewById(R.id.ratingBarEvent);
        }

        void bind(Event event, OnEventClickListener listener, OnEventRatingChangeListener ratingChangeListener) {
            if (event == null) {
                bindCoverImage(null);
                if (chipHobby != null) {
                    chipHobby.setText("");
                }
                tvEventTitle.setText(itemView.getContext().getString(R.string.event_untitled));
                tvEventDate.setText(itemView.getContext().getString(R.string.event_time_not_set));
                tvEventLocation.setText(itemView.getContext().getString(R.string.event_location_na));
                if (layoutAverageRating != null) {
                    layoutAverageRating.setVisibility(View.GONE);
                }
                if (layoutRatingContainer != null) {
                    layoutRatingContainer.setVisibility(View.GONE);
                }
                itemView.setOnClickListener(null);
                return;
            }

            bindCoverImage(event.getImageUrl());

            String hobby = safe(event.getHobbyId(), "General");
            String title = safe(event.getTitle(), itemView.getContext().getString(R.string.event_untitled));
            String location = safe(event.getLocation(), itemView.getContext().getString(R.string.event_detail_location_na));
            String timeText = formatTime(event.getEventTimeMillis());

            if (chipHobby != null) {
                chipHobby.setText(hobby);
            }
            tvEventTitle.setText(title);
            tvEventDate.setText(timeText);
            tvEventLocation.setText(location);

            // Bind Average Rating
            if (layoutAverageRating != null && tvAverageRating != null) {
                if (event.getRatings() != null && !event.getRatings().isEmpty()) {
                    double sum = 0.0;
                    for (Long r : event.getRatings().values()) {
                        sum += r;
                    }
                    double avg = sum / event.getRatings().size();
                    tvAverageRating.setText(String.format(Locale.getDefault(), "%.1f (%d)", avg, event.getRatings().size()));
                    layoutAverageRating.setVisibility(View.VISIBLE);
                } else {
                    layoutAverageRating.setVisibility(View.GONE);
                }
            }

            // Bind Interactive Rating Bar
            if (layoutRatingContainer != null && ratingBarEvent != null) {
                com.google.firebase.auth.FirebaseUser currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
                String currentUserId = currentUser != null ? currentUser.getUid() : "";
                boolean hasJoined = event.getJoinedUserIds() != null && event.getJoinedUserIds().contains(currentUserId);
                boolean hasStarted = System.currentTimeMillis() >= event.getDateTime();

                if (hasJoined && hasStarted && !currentUserId.isEmpty()) {
                    layoutRatingContainer.setVisibility(View.VISIBLE);
                    ratingBarEvent.setOnRatingBarChangeListener(null);
                    Long userRating = event.getRatings() != null ? event.getRatings().get(currentUserId) : null;
                    ratingBarEvent.setRating(userRating != null ? userRating.floatValue() : 0.0f);
                    ratingBarEvent.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
                        if (fromUser && ratingChangeListener != null) {
                            ratingChangeListener.onEventRatingChanged(event, (int) rating);
                        }
                    });
                } else {
                    layoutRatingContainer.setVisibility(View.GONE);
                }
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEventClick(event);
                }
            });
        }

        private void bindCoverImage(String imageUrl) {
            if (ivEventImage == null) return;
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                ivEventImage.setImageResource(R.drawable.bg_event_cover_placeholder);
                return;
            }

            Glide.with(itemView.getContext())
                    .load(imageUrl.trim())
                    .placeholder(R.drawable.bg_event_cover_placeholder)
                    .error(R.drawable.bg_event_cover_placeholder)
                    .centerCrop()
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(ivEventImage);
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
