package com.dormigo;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    public interface OnNotificationClickListener {
        void onNotificationClick(JSONObject notif, int position);
        void onNotificationDismissed(JSONObject notif, int position);
    }

    private final Context context;
    private final List<JSONObject> notificationList;
    private final OnNotificationClickListener listener;

    public NotificationAdapter(Context context, List<JSONObject> notificationList, OnNotificationClickListener listener) {
        this.context = context;
        this.notificationList = notificationList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_notification_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        JSONObject notif = notificationList.get(position);
        String title = notif.optString("title", "Notification");
        String message = notif.optString("message", "");
        String type = notif.optString("type", "SYSTEM").toUpperCase(Locale.ROOT);
        boolean isRead = notif.optBoolean("is_read", false);
        String createdAt = notif.optString("created_at", "");

        holder.textTitle.setText(title);
        holder.textMessage.setText(message);
        holder.textTime.setText(getRelativeTimeSpanString(createdAt));

        // Highlight unread notifications
        if (isRead) {
            holder.cardRoot.setBackgroundResource(R.drawable.bg_card_rounded);
            holder.unreadIndicator.setVisibility(View.GONE);
            holder.textTitle.setTypeface(null, Typeface.NORMAL);
        } else {
            holder.cardRoot.setBackgroundResource(R.drawable.bg_card_unread_rounded);
            holder.unreadIndicator.setVisibility(View.VISIBLE);
            holder.textTitle.setTypeface(null, Typeface.BOLD);
        }

        // Apply centralized notification style
        NotificationStyleManager.NotificationStyle style = NotificationStyleManager.getStyle(notif);
        holder.iconContainer.setBackgroundResource(style.containerBgRes);
        holder.iconImage.setImageResource(style.iconRes);
        holder.iconImage.setColorFilter(style.accentColor);

        holder.cardRoot.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNotificationClick(notif, holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    public void removeItem(int position) {
        if (position >= 0 && position < notificationList.size()) {
            JSONObject removedNotif = notificationList.remove(position);
            notifyItemRemoved(position);
            if (listener != null) {
                listener.onNotificationDismissed(removedNotif, position);
            }
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        View cardRoot;
        FrameLayout iconContainer;
        ImageView iconImage;
        TextView textTitle;
        TextView textMessage;
        TextView textTime;
        View unreadIndicator;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.cardRoot);
            iconContainer = itemView.findViewById(R.id.iconContainer);
            iconImage = itemView.findViewById(R.id.iconImage);
            textTitle = itemView.findViewById(R.id.textTitle);
            textMessage = itemView.findViewById(R.id.textMessage);
            textTime = itemView.findViewById(R.id.textTime);
            unreadIndicator = itemView.findViewById(R.id.unreadIndicator);
        }
    }

    public static String getRelativeTimeSpanString(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty() || dateStr.equals("null")) {
            return "Just now";
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            Date date = sdf.parse(dateStr);
            if (date == null) return dateStr;

            long diffMillis = System.currentTimeMillis() - date.getTime();
            long diffSeconds = diffMillis / 1000;
            long diffMinutes = diffSeconds / 60;
            long diffHours = diffMinutes / 60;
            long diffDays = diffHours / 24;

            if (diffSeconds < 60) {
                return "Just now";
            } else if (diffMinutes < 60) {
                return diffMinutes + " min" + (diffMinutes == 1 ? "" : "s") + " ago";
            } else if (diffHours < 24) {
                return diffHours + " hr" + (diffHours == 1 ? "" : "s") + " ago";
            } else if (diffDays == 1) {
                return "Yesterday";
            } else if (diffDays < 7) {
                return diffDays + " days ago";
            } else {
                SimpleDateFormat outFormat = new SimpleDateFormat("MMM d, yyyy", Locale.US);
                return outFormat.format(date);
            }
        } catch (Exception e) {
            return dateStr;
        }
    }
}
