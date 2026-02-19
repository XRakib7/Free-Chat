package com.softcraft.freechat.adapters;

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
import com.softcraft.freechat.models.Message;
import com.softcraft.freechat.models.User;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SearchAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_USER = 1;
    private static final int TYPE_MESSAGE = 2;

    private android.content.Context context;
    private List<Object> items;

    public SearchAdapter(android.content.Context context, List<Object> items) {
        this.context = context;
        this.items = items;
    }

    @Override
    public int getItemViewType(int position) {
        if (items.get(position) instanceof User) {
            return TYPE_USER;
        } else {
            return TYPE_MESSAGE;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_USER) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_search_user, parent, false);
            return new UserViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_search_message, parent, false);
            return new MessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof UserViewHolder) {
            bindUser((UserViewHolder) holder, (User) items.get(position));
        } else {
            bindMessage((MessageViewHolder) holder, (Message) items.get(position));
        }
    }

    private void bindUser(UserViewHolder holder, User user) {
        holder.textViewName.setText(user.getName());
        holder.textViewEmail.setText(user.getEmail());

        if (user.getPhotoUrl() != null) {
            Glide.with(context)
                    .load(user.getPhotoUrl())
                    .circleCrop()
                    .into(holder.imageViewAvatar);
        }

        holder.itemView.setOnClickListener(v -> {
            // Start chat with this user
            Intent intent = new Intent(context, ChatActivity.class);
            intent.putExtra("otherUserId", user.getUserId());
            intent.putExtra("otherUserName", user.getName());
            intent.putExtra("otherUserPhoto", user.getPhotoUrl());
            context.startActivity(intent);
        });
    }

    private void bindMessage(MessageViewHolder holder, Message message) {
        holder.textViewMessage.setText(message.getText());
        holder.textViewSender.setText(message.getSenderName());

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault());
        holder.textViewTime.setText(sdf.format(new Date(message.getTimestamp())));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewAvatar;
        TextView textViewName;
        TextView textViewEmail;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewAvatar = itemView.findViewById(R.id.imageViewAvatar);
            textViewName = itemView.findViewById(R.id.textViewName);
            textViewEmail = itemView.findViewById(R.id.textViewEmail);
        }
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView textViewMessage;
        TextView textViewSender;
        TextView textViewTime;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewMessage = itemView.findViewById(R.id.textViewMessage);
            textViewSender = itemView.findViewById(R.id.textViewSender);
            textViewTime = itemView.findViewById(R.id.textViewTime);
        }
    }
}