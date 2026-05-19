package com.example.hobbycircle.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hobbycircle.R;
import com.example.hobbycircle.data.model.User;

import java.util.List;
import java.util.Locale;

public class CreatorRatingAdapter extends RecyclerView.Adapter<CreatorRatingAdapter.ViewHolder> {

    public static class CreatorStats {
        public User user;
        public int eventsCreatedCount;
        public double averageRating;

        public CreatorStats(User user, int eventsCreatedCount, double averageRating) {
            this.user = user;
            this.eventsCreatedCount = eventsCreatedCount;
            this.averageRating = averageRating;
        }
    }

    private final List<CreatorStats> statsList;

    public CreatorRatingAdapter(List<CreatorStats> statsList) {
        this.statsList = statsList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_creator_rating, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CreatorStats stats = statsList.get(position);
        if (stats != null) {
            holder.bind(stats);
        }
    }

    @Override
    public int getItemCount() {
        return statsList != null ? statsList.size() : 0;
    }

    public void submitList(List<CreatorStats> newList) {
        this.statsList.clear();
        if (newList != null) {
            this.statsList.addAll(newList);
        }
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvCreatorInitial;
        private final TextView tvCreatorName;
        private final TextView tvCreatorEventsCount;
        private final TextView tvCreatorRatingText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCreatorInitial = itemView.findViewById(R.id.tvCreatorInitial);
            tvCreatorName = itemView.findViewById(R.id.tvCreatorName);
            tvCreatorEventsCount = itemView.findViewById(R.id.tvCreatorEventsCount);
            tvCreatorRatingText = itemView.findViewById(R.id.tvCreatorRatingText);
        }

        public void bind(CreatorStats stats) {
            User user = stats.user;
            String displayName = user.getName();
            if (displayName == null || displayName.trim().isEmpty()) {
                displayName = user.getEmail();
            }
            if (displayName == null || displayName.trim().isEmpty()) {
                displayName = "Creator";
            }
            tvCreatorName.setText(displayName);

            String initial = String.valueOf(displayName.charAt(0)).toUpperCase();
            tvCreatorInitial.setText(initial);

            tvCreatorEventsCount.setText(String.format(Locale.getDefault(), "%d event%s created", 
                    stats.eventsCreatedCount, stats.eventsCreatedCount == 1 ? "" : "s"));

            if (stats.averageRating >= 0) {
                tvCreatorRatingText.setText(String.format(Locale.getDefault(), "%.1f", stats.averageRating));
            } else {
                tvCreatorRatingText.setText("N/A");
            }
        }
    }
}
