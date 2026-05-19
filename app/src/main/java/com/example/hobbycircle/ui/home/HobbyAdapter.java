package com.example.hobbycircle.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hobbycircle.R;
import com.example.hobbycircle.data.model.Hobby;

import java.util.ArrayList;
import java.util.List;

public class HobbyAdapter extends RecyclerView.Adapter<HobbyAdapter.HobbyViewHolder> {

    private final List<Hobby> hobbies = new ArrayList<>();

    public HobbyAdapter(List<Hobby> initialList) {
        submitList(initialList);
    }

    public void submitList(List<Hobby> newList) {
        hobbies.clear();
        if (newList != null) {
            hobbies.addAll(newList);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HobbyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_hobby, parent, false);
        return new HobbyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HobbyViewHolder holder, int position) {
        Hobby hobby = hobbies.get(position);
        holder.bind(hobby);
    }

    @Override
    public int getItemCount() {
        return hobbies.size();
    }

    static class HobbyViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvHobbyName;
        private final TextView tvHobbyDescription;

        public HobbyViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHobbyName = itemView.findViewById(R.id.tvHobbyName);
            tvHobbyDescription = itemView.findViewById(R.id.tvHobbyDescription);
        }

        void bind(Hobby hobby) {
            if (hobby == null) {
                tvHobbyName.setText("Unknown Hobby");
                tvHobbyDescription.setText("No description available.");
                return;
            }

            String name = hobby.getName() != null && !hobby.getName().trim().isEmpty()
                    ? hobby.getName().trim()
                    : "Unnamed Hobby";

            String description = hobby.getDescription() != null && !hobby.getDescription().trim().isEmpty()
                    ? hobby.getDescription().trim()
                    : "No description available.";

            tvHobbyName.setText(name);
            tvHobbyDescription.setText(description);
        }
    }
}