package com.dormigo;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatConversationAdapter extends RecyclerView.Adapter<ChatConversationAdapter.ViewHolder> {

    public interface OnConversationClickListener {
        void onConversationClick(JSONObject chatItem, int position);
    }

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_ITEM = 1;

    private final Context context;
    private final List<JSONObject> conversationList;
    private final OnConversationClickListener listener;

    public ChatConversationAdapter(Context context, List<JSONObject> conversationList, OnConversationClickListener listener) {
        this.context = context;
        this.conversationList = conversationList;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        JSONObject item = conversationList.get(position);
        if (item.optBoolean("is_section_header", false)) {
            return VIEW_TYPE_HEADER;
        }
        return VIEW_TYPE_ITEM;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HEADER) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_chat_section_header, parent, false);
            return new ViewHolder(view, true);
        }
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat_conversation, parent, false);
        return new ViewHolder(view, false);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        JSONObject item = conversationList.get(position);

        if (holder.isHeader) {
            if (holder.textHeaderTitle != null) {
                holder.textHeaderTitle.setText(item.optString("header_title", ""));
            }
            return;
        }

        String name = item.optString("other_user_name", item.optString("full_name", "User"));
        String email = item.optString("email", "");
        String lastMsg = item.optString("message_text", email.isEmpty() ? "Tap to chat" : email);
        boolean isRead = item.optBoolean("is_read", true);
        String createdAt = item.optString("created_at", "");
        int unreadCount = item.optInt("unread_count", isRead ? 0 : 1);

        // Initials Avatar
        String initials = "U";
        if (name != null && !name.trim().isEmpty()) {
            String[] parts = name.trim().split("\\s+");
            if (parts.length >= 2) {
                initials = ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase(Locale.ROOT);
            } else if (parts[0].length() >= 2) {
                initials = parts[0].substring(0, 2).toUpperCase(Locale.ROOT);
            } else {
                initials = parts[0].toUpperCase(Locale.ROOT);
            }
        }
        if (holder.textAvatar != null) holder.textAvatar.setText(initials);

        // Name & Last Message
        if (holder.textContactName != null) holder.textContactName.setText(name);

        boolean isMuted = item.optBoolean("is_muted", false);
        if (holder.textLastMessage != null) {
            if (isMuted) {
                holder.textLastMessage.setText("🔕 Muted · " + lastMsg);
            } else {
                holder.textLastMessage.setText(lastMsg);
            }
        }

        if (holder.textRelativeTime != null) holder.textRelativeTime.setText(getRelativeTimeSpanString(createdAt));

        // Read vs Unread Styling
        if (holder.cardRoot != null) {
            if (!isRead) {
                holder.cardRoot.setBackgroundColor(Color.parseColor("#F2F9F7"));
                if (holder.textContactName != null) holder.textContactName.setTypeface(null, Typeface.BOLD);
                if (holder.textLastMessage != null) {
                    holder.textLastMessage.setTypeface(null, Typeface.BOLD);
                    holder.textLastMessage.setTextColor(Color.parseColor("#1B5E4C"));
                }

                if (holder.unreadGreenDot != null) holder.unreadGreenDot.setVisibility(View.VISIBLE);
                if (unreadCount > 0 && holder.textUnreadBadge != null) {
                    holder.textUnreadBadge.setText(unreadCount > 9 ? "9+" : String.valueOf(unreadCount));
                    holder.textUnreadBadge.setVisibility(View.VISIBLE);
                } else if (holder.textUnreadBadge != null) {
                    holder.textUnreadBadge.setVisibility(View.GONE);
                }
            } else {
                holder.cardRoot.setBackgroundResource(R.drawable.bg_card_rounded);
                if (holder.textContactName != null) holder.textContactName.setTypeface(null, Typeface.NORMAL);
                if (holder.textLastMessage != null) {
                    holder.textLastMessage.setTypeface(null, Typeface.NORMAL);
                    holder.textLastMessage.setTextColor(Color.parseColor("#6E6E73"));
                }

                if (holder.unreadGreenDot != null) holder.unreadGreenDot.setVisibility(View.GONE);
                if (holder.textUnreadBadge != null) holder.textUnreadBadge.setVisibility(View.GONE);
            }

            holder.cardRoot.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onConversationClick(item, holder.getBindingAdapterPosition());
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return conversationList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        boolean isHeader;
        TextView textHeaderTitle;

        View cardRoot;
        TextView textAvatar;
        View onlineIndicator;
        TextView textContactName;
        TextView textLastMessage;
        TextView textRelativeTime;
        TextView textUnreadBadge;
        View unreadGreenDot;

        public ViewHolder(@NonNull View itemView, boolean isHeader) {
            super(itemView);
            this.isHeader = isHeader;
            if (isHeader) {
                textHeaderTitle = itemView.findViewById(R.id.textHeaderTitle);
            } else {
                cardRoot = itemView.findViewById(R.id.cardRoot);
                textAvatar = itemView.findViewById(R.id.textAvatar);
                onlineIndicator = itemView.findViewById(R.id.onlineIndicator);
                textContactName = itemView.findViewById(R.id.textContactName);
                textLastMessage = itemView.findViewById(R.id.textLastMessage);
                textRelativeTime = itemView.findViewById(R.id.textRelativeTime);
                textUnreadBadge = itemView.findViewById(R.id.textUnreadBadge);
                unreadGreenDot = itemView.findViewById(R.id.unreadGreenDot);
            }
        }
    }

    public static String getRelativeTimeSpanString(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty() || dateStr.equals("null")) {
            return "";
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
                return diffMinutes + " min ago";
            } else if (diffHours < 24) {
                return diffHours + " hr ago";
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
