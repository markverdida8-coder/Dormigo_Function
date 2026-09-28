package com.dormigo;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    public interface OnTransactionClickListener {
        void onTransactionClick(JSONObject tx, int position);
    }

    private final Context context;
    private final List<JSONObject> transactionList;
    private final OnTransactionClickListener listener;

    public TransactionAdapter(Context context, List<JSONObject> transactionList, OnTransactionClickListener listener) {
        this.context = context;
        this.transactionList = transactionList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_transaction_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        JSONObject tx = transactionList.get(position);

        String period = tx.optString("payment_description", tx.optString("payment_period", ""));
        if (period.isEmpty() || period.matches("\\d+")) {
            period = "Monthly Rent";
        }
        holder.textTitle.setText(period);

        double amt = tx.optDouble("amount", 0.0);
        holder.textAmount.setText("₱" + NumberFormat.getNumberInstance(Locale.US).format(amt));

        String houseName = tx.optString("house_name", "Boarding House");
        String roomNumber = tx.optString("room_number", "");
        String roomType = tx.optString("room_type", "");
        StringBuilder houseInfo = new StringBuilder(houseName);
        if (!roomNumber.isEmpty()) {
            houseInfo.append(" · Room ").append(roomNumber);
        }
        if (!roomType.isEmpty()) {
            houseInfo.append(" (").append(roomType).append(")");
        }
        holder.textHouseRoom.setText(houseInfo.toString());

        String dateStr = tx.optString("payment_date", tx.optString("due_date", ""));
        String method = tx.optString("payment_method", "Payment");
        holder.textDateMethod.setText(getRelativeTimeSpanString(dateStr) + " · " + method);

        String ref = tx.optString("transaction_ref", "");
        if (ref == null || ref.trim().isEmpty() || ref.equalsIgnoreCase("null")) {
            holder.textRef.setText("Ref: Reference unavailable");
            holder.textRef.setTextColor(Color.parseColor("#9A9A9E"));
        } else {
            holder.textRef.setText("Ref: " + ref.trim());
            holder.textRef.setTextColor(Color.parseColor("#1B5E4C"));
        }

        String status = tx.optString("status", "PENDING").toUpperCase(Locale.ROOT);
        holder.textStatusBadge.setText(status);

        if ("PAID".equals(status) || "CONFIRMED".equals(status)) {
            holder.iconContainer.setBackgroundResource(R.drawable.bg_circle_green_light);
            holder.iconImage.setImageResource(R.drawable.ic_receipt);
            holder.iconImage.setColorFilter(Color.parseColor("#1B5E4C"));

            holder.textStatusBadge.setBackgroundResource(R.drawable.bg_circle_green_light);
            holder.textStatusBadge.setTextColor(Color.parseColor("#1B5E4C"));
        } else if ("PENDING".equals(status)) {
            holder.iconContainer.setBackgroundResource(R.drawable.bg_circle_orange);
            holder.iconImage.setImageResource(R.drawable.ic_bell);
            holder.iconImage.setColorFilter(Color.parseColor("#FD7E14"));

            holder.textStatusBadge.setBackgroundResource(R.drawable.bg_circle_orange);
            holder.textStatusBadge.setTextColor(Color.parseColor("#FD7E14"));
        } else if ("SUBMITTED".equals(status)) {
            holder.textStatusBadge.setText("Awaiting Verification");
            holder.iconContainer.setBackgroundResource(R.drawable.bg_circle_orange);
            holder.iconImage.setImageResource(R.drawable.ic_bell);
            holder.iconImage.setColorFilter(Color.parseColor("#FD7E14"));

            holder.textStatusBadge.setBackgroundResource(R.drawable.bg_circle_orange);
            holder.textStatusBadge.setTextColor(Color.parseColor("#FD7E14"));
        } else {
            holder.iconContainer.setBackgroundResource(R.drawable.bg_circle_gray);
            holder.iconImage.setImageResource(R.drawable.ic_alert_circle);
            holder.iconImage.setColorFilter(Color.parseColor("#C53030"));

            holder.textStatusBadge.setBackgroundColor(Color.parseColor("#FEEAEA"));
            holder.textStatusBadge.setTextColor(Color.parseColor("#C53030"));
        }

        holder.cardRoot.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTransactionClick(tx, holder.getBindingAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return transactionList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        View cardRoot;
        FrameLayout iconContainer;
        ImageView iconImage;
        TextView textTitle;
        TextView textHouseRoom;
        TextView textAmount;
        TextView textDateMethod;
        TextView textRef;
        TextView textStatusBadge;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.cardRoot);
            iconContainer = itemView.findViewById(R.id.iconContainer);
            iconImage = itemView.findViewById(R.id.iconImage);
            textTitle = itemView.findViewById(R.id.textTitle);
            textHouseRoom = itemView.findViewById(R.id.textHouseRoom);
            textAmount = itemView.findViewById(R.id.textAmount);
            textDateMethod = itemView.findViewById(R.id.textDateMethod);
            textRef = itemView.findViewById(R.id.textRef);
            textStatusBadge = itemView.findViewById(R.id.textStatusBadge);
        }
    }

    public static String getRelativeTimeSpanString(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty() || dateStr.equalsIgnoreCase("null")) {
            return "Recent";
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date date = sdf.parse(dateStr);
            if (date == null) return dateStr;

            long diffMillis = System.currentTimeMillis() - date.getTime();
            long diffDays = diffMillis / (1000 * 60 * 60 * 24);

            if (diffDays <= 0) {
                return "Today";
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
