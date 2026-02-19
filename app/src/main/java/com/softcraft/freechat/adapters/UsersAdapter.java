package com.softcraft.freechat.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.softcraft.freechat.R;
import com.softcraft.freechat.activities.ChatActivity;
import com.softcraft.freechat.models.User;

import java.util.List;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UserViewHolder> {

    private Context context;
    private List<User> userList;

    public UsersAdapter(Context context, List<User> userList) {
        this.context = context;
        this.userList = userList;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);

        holder.textViewName.setText(user.getName());

        // Set online status
        if ("online".equals(user.getStatus())) {
            holder.imageViewOnline.setVisibility(View.VISIBLE);
        } else {
            holder.imageViewOnline.setVisibility(View.GONE);
        }

        // Load avatar
        if (user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
            Glide.with(context)
                    .load(user.getPhotoUrl())
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .circleCrop()
                    .into(holder.imageViewAvatar);
        } else {
            holder.imageViewAvatar.setImageResource(R.drawable.ic_profile);
        }

        // Set click listener - DON'T pass chatId, let ChatActivity create it
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChatActivity.class);
            intent.putExtra("otherUserId", user.getUserId());
            intent.putExtra("otherUserName", user.getName() != null ? user.getName() : "User");
            intent.putExtra("otherUserPhoto", user.getPhotoUrl());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public void updateList(List<User> newList) {
        userList = newList;
        notifyDataSetChanged();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewAvatar;
        View imageViewOnline;
        TextView textViewName;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewAvatar = itemView.findViewById(R.id.imageViewAvatar);
            imageViewOnline = itemView.findViewById(R.id.imageViewOnline);
            textViewName = itemView.findViewById(R.id.textViewName);
        }
    }
}