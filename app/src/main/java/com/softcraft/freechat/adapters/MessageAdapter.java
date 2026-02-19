package com.softcraft.freechat.adapters;

import android.content.Context;
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
import com.softcraft.freechat.models.Message;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private Context context;
    private List<Message> messageList;
    private String currentUserId;
    private String otherUserPhoto;

    public MessageAdapter(Context context, List<Message> messageList, String otherUserPhoto) {
        this.context = context;
        this.messageList = messageList;
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        this.otherUserPhoto = otherUserPhoto;
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messageList.get(position);
        if (message.getSenderId().equals(currentUserId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SENT) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_message_sent, parent, false);
            return new SentMessageViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_message_received, parent, false);
            return new ReceivedMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messageList.get(position);

        if (holder.getItemViewType() == VIEW_TYPE_SENT) {
            ((SentMessageViewHolder) holder).bind(message);
        } else {
            ((ReceivedMessageViewHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    // ViewHolder for sent messages
    class SentMessageViewHolder extends RecyclerView.ViewHolder {
        TextView textViewMessage;
        TextView textViewTime;
        TextView textViewStatus;

        SentMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewMessage = itemView.findViewById(R.id.textViewMessage);
            textViewTime = itemView.findViewById(R.id.textViewTime);
            textViewStatus = itemView.findViewById(R.id.textViewStatus);
        }

        void bind(Message message) {
            textViewMessage.setText(message.getText());
            textViewTime.setText(formatTime(message.getTimestamp()));

            // Set message status with appropriate icons
            String status = message.getStatus();
            if (status != null) {
                switch (status) {
                    case "sending":
                        textViewStatus.setText("⏳"); // Hourglass for sending
                        textViewStatus.setTextColor(context.getColor(R.color.gray));
                        break;
                    case "sent":
                        textViewStatus.setText("✓"); // Single check for sent
                        textViewStatus.setTextColor(context.getColor(R.color.gray));
                        break;
                    case "delivered":
                        textViewStatus.setText("✓✓"); // Double check for delivered
                        textViewStatus.setTextColor(context.getColor(R.color.gray));
                        break;
                    case "read":
                        textViewStatus.setText("✓✓"); // Double check for read
                        textViewStatus.setTextColor(context.getColor(R.color.primary)); // Blue for read
                        break;
                    default:
                        textViewStatus.setText("");
                        break;
                }
            }

            // Also check readBy for more accurate read status
            Map<String, Boolean> readBy = message.getReadBy();
            if (readBy != null && readBy.size() > 1) { // More than just sender
                textViewStatus.setText("✓✓");
                textViewStatus.setTextColor(context.getColor(R.color.primary));
            }
        }
    }

    // ViewHolder for received messages
    class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewAvatar;
        TextView textViewSenderName;
        TextView textViewMessage;
        TextView textViewTime;

        ReceivedMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewAvatar = itemView.findViewById(R.id.imageViewAvatar);
            textViewSenderName = itemView.findViewById(R.id.textViewSenderName);
            textViewMessage = itemView.findViewById(R.id.textViewMessage);
            textViewTime = itemView.findViewById(R.id.textViewTime);
        }

        void bind(Message message) {
            textViewMessage.setText(message.getText());
            textViewSenderName.setText(message.getSenderName());
            textViewTime.setText(formatTime(message.getTimestamp()));

            // Load avatar
            if (otherUserPhoto != null && !otherUserPhoto.isEmpty()) {
                Glide.with(context)
                        .load(otherUserPhoto)
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .circleCrop()
                        .into(imageViewAvatar);
            } else {
                imageViewAvatar.setImageResource(R.drawable.ic_profile);
            }
        }
    }

    private String formatTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}