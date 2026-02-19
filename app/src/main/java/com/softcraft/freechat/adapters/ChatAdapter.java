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
import com.google.firebase.auth.FirebaseAuth;
import com.softcraft.freechat.R;
import com.softcraft.freechat.activities.ChatActivity;
import com.softcraft.freechat.models.Chat;
import com.softcraft.freechat.models.User;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private Context context;
    private List<Chat> chatList;
    private java.util.Map<String, User> userMap; // Cache for user details
    private String currentUserId;

    public ChatAdapter(Context context, List<Chat> chatList, java.util.Map<String, User> userMap) {
        this.context = context;
        this.chatList = chatList;
        this.userMap = userMap;
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        Chat chat = chatList.get(position);

        // Find the other participant (for private chats)
        String otherUserId = getOtherParticipantId(chat);
        User otherUser = userMap.get(otherUserId);

        if (otherUser != null) {
            // Set chat name
            holder.textViewChatName.setText(otherUser.getName());

            // Load avatar
            if (otherUser.getPhotoUrl() != null && !otherUser.getPhotoUrl().isEmpty()) {
                Glide.with(context)
                        .load(otherUser.getPhotoUrl())
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .circleCrop()
                        .into(holder.imageViewAvatar);
            } else {
                holder.imageViewAvatar.setImageResource(R.drawable.ic_profile);
            }
        }

        // Set last message
        if (chat.getLastMessage() != null && !chat.getLastMessage().isEmpty()) {
            holder.textViewLastMessage.setText(chat.getLastMessage());
        } else {
            holder.textViewLastMessage.setText("No messages yet");
        }

        // Set time
        if (chat.getLastMessageTime() > 0) {
            holder.textViewTime.setText(formatTime(chat.getLastMessageTime()));
        } else {
            holder.textViewTime.setText("");
        }

        // Handle unread count (you'll implement this later)
        holder.textViewUnreadCount.setVisibility(View.GONE);

        // Set click listener
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, ChatActivity.class);
                intent.putExtra("chatId", chat.getChatId());
                intent.putExtra("otherUserId", otherUserId);
                if (otherUser != null) {
                    intent.putExtra("otherUserName", otherUser.getName());
                    intent.putExtra("otherUserPhoto", otherUser.getPhotoUrl());
                }
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    private String getOtherParticipantId(Chat chat) {
        if (chat.getParticipantIds() != null) {
            for (String userId : chat.getParticipantIds()) {
                if (!userId.equals(currentUserId)) {
                    return userId;
                }
            }
        }
        return null;
    }

    private String formatTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewAvatar;
        TextView textViewChatName;
        TextView textViewLastMessage;
        TextView textViewTime;
        TextView textViewUnreadCount;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewAvatar = itemView.findViewById(R.id.imageViewAvatar);
            textViewChatName = itemView.findViewById(R.id.textViewChatName);
            textViewLastMessage = itemView.findViewById(R.id.textViewLastMessage);
            textViewTime = itemView.findViewById(R.id.textViewTime);
            textViewUnreadCount = itemView.findViewById(R.id.textViewUnreadCount);
        }
    }
}