package com.example.fuel_split;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.ViewHolder> {

    public interface OnGroupClickListener {
        void onGroupClick(GroupItem group);
    }

    private final List<GroupItem> groups;
    private final OnGroupClickListener listener;

    public GroupAdapter(List<GroupItem> groups, OnGroupClickListener listener) {
        this.groups = groups;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_group, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        GroupItem g = groups.get(position);
        holder.tvGroupInitial.setText(String.valueOf(g.name.charAt(0)).toUpperCase());
        holder.tvGroupName.setText(g.name);
        holder.tvGroupMembers.setText(g.memberCount + " members · " + g.contractAddress.substring(0, 8) + "...");
        holder.itemView.setOnClickListener(v -> listener.onGroupClick(g));
    }

    @Override
    public int getItemCount() { return groups.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvGroupInitial, tvGroupName, tvGroupMembers;
        ViewHolder(@NonNull View v) {
            super(v);
            tvGroupInitial  = v.findViewById(R.id.tvGroupInitial);
            tvGroupName     = v.findViewById(R.id.tvGroupName);
            tvGroupMembers  = v.findViewById(R.id.tvGroupMembers);
        }
    }
}