package com.softcraft.freechat.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
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
import com.softcraft.freechat.activities.FullScreenImageActivity;
import com.softcraft.freechat.models.Message;
import com.softcraft.freechat.utils.ImageUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;
    private static final int VIEW_TYPE_SENT_IMAGE = 3;
    private static final int VIEW_TYPE_RECEIVED_IMAGE = 4;

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
            return message.isImage() ? VIEW_TYPE_SENT_IMAGE : VIEW_TYPE_SENT;
        } else {
            return message.isImage() ? VIEW_TYPE_RECEIVED_IMAGE : VIEW_TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SENT) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_message_sent, parent, false);
            return new SentMessageViewHolder(view);
        } else if (viewType == VIEW_TYPE_RECEIVED) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_message_received, parent, false);
            return new ReceivedMessageViewHolder(view);
        } else if (viewType == VIEW_TYPE_SENT_IMAGE) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_image_sent, parent, false);
            return new SentImageViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_image_received, parent, false);
            return new ReceivedImageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messageList.get(position);

        int viewType = getItemViewType(position);

        if (viewType == VIEW_TYPE_SENT) {
            ((SentMessageViewHolder) holder).bind(message);
        } else if (viewType == VIEW_TYPE_RECEIVED) {
            ((ReceivedMessageViewHolder) holder).bind(message);
        } else if (viewType == VIEW_TYPE_SENT_IMAGE) {
            ((SentImageViewHolder) holder).bind(message);
        } else if (viewType == VIEW_TYPE_RECEIVED_IMAGE) {
            ((ReceivedImageViewHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    // ViewHolder for sent text messages
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
                        textViewStatus.setText("⏳");
                        textViewStatus.setTextColor(context.getColor(R.color.gray));
                        break;
                    case "sent":
                        textViewStatus.setText("✓");
                        textViewStatus.setTextColor(context.getColor(R.color.gray));
                        break;
                    case "delivered":
                        textViewStatus.setText("✓✓");
                        textViewStatus.setTextColor(context.getColor(R.color.gray));
                        break;
                    case "read":
                        textViewStatus.setText("✓✓");
                        textViewStatus.setTextColor(context.getColor(R.color.primary));
                        break;
                    default:
                        textViewStatus.setText("");
                        break;
                }
            }
        }
    }

    // ViewHolder for received text messages
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

    // ViewHolder for sent images
    class SentImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewMessage;
        TextView textViewTime;
        TextView textViewStatus;

        SentImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewMessage = itemView.findViewById(R.id.imageViewMessage);
            textViewTime = itemView.findViewById(R.id.textViewTime);
            textViewStatus = itemView.findViewById(R.id.textViewStatus);
        }

        void bind(Message message) {
            // Load and display image
            if (message.getImageBase64() != null) {
                Bitmap bitmap = ImageUtils.base64ToBitmap(message.getImageBase64());
                if (bitmap != null) {
                    imageViewMessage.setImageBitmap(bitmap);
                    imageViewMessage.setVisibility(View.VISIBLE);
                }
            }

            textViewTime.setText(formatTime(message.getTimestamp()));

            // Set message status
            String status = message.getStatus();
            if (status != null) {
                switch (status) {
                    case "sending":
                        textViewStatus.setText("⏳");
                        textViewStatus.setTextColor(context.getColor(R.color.gray));
                        break;
                    case "sent":
                        textViewStatus.setText("✓");
                        textViewStatus.setTextColor(context.getColor(R.color.gray));
                        break;
                    case "delivered":
                        textViewStatus.setText("✓✓");
                        textViewStatus.setTextColor(context.getColor(R.color.gray));
                        break;
                    case "read":
                        textViewStatus.setText("✓✓");
                        textViewStatus.setTextColor(context.getColor(R.color.primary));
                        break;
                }
            }

            // Click to view full screen
            imageViewMessage.setOnClickListener(v -> {
                Intent intent = new Intent(context, FullScreenImageActivity.class);
                intent.putExtra("image_base64", message.getImageBase64());
                intent.putExtra("sender_name", message.getSenderName());
                intent.putExtra("timestamp", message.getTimestamp());
                context.startActivity(intent);
            });
        }
    }

    // ViewHolder for received images
    class ReceivedImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewAvatar;
        TextView textViewSenderName;
        ImageView imageViewMessage;
        TextView textViewTime;

        ReceivedImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewAvatar = itemView.findViewById(R.id.imageViewAvatar);
            textViewSenderName = itemView.findViewById(R.id.textViewSenderName);
            imageViewMessage = itemView.findViewById(R.id.imageViewMessage);
            textViewTime = itemView.findViewById(R.id.textViewTime);
        }

        void bind(Message message) {
            textViewSenderName.setText(message.getSenderName());

            // Load and display image
            if (message.getImageBase64() != null) {
                Bitmap bitmap = ImageUtils.base64ToBitmap(message.getImageBase64());
                if (bitmap != null) {
                    imageViewMessage.setImageBitmap(bitmap);
                    imageViewMessage.setVisibility(View.VISIBLE);
                }
            }

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

            // Click to view full screen
            imageViewMessage.setOnClickListener(v -> {
                Intent intent = new Intent(context, FullScreenImageActivity.class);
                intent.putExtra("image_base64", message.getImageBase64());
                intent.putExtra("sender_name", message.getSenderName());
                intent.putExtra("timestamp", message.getTimestamp());
                context.startActivity(intent);
            });
        }
    }

    private String formatTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}