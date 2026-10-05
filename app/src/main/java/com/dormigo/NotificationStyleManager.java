package com.dormigo;

import android.graphics.Color;

import org.json.JSONObject;

import java.util.Locale;

public class NotificationStyleManager {

    public static class NotificationStyle {
        public int containerBgRes;
        public int iconRes;
        public int accentColor;

        public NotificationStyle(int containerBgRes, int iconRes, int accentColor) {
            this.containerBgRes = containerBgRes;
            this.iconRes = iconRes;
            this.accentColor = accentColor;
        }
    }

    public static NotificationStyle getStyle(JSONObject notification) {
        if (notification == null) {
            return new NotificationStyle(R.drawable.bg_circle_gray, R.drawable.ic_bell, Color.parseColor("#6C757D"));
        }

        String type = notification.optString("type", "").toUpperCase(Locale.ROOT);
        String title = notification.optString("title", "").toUpperCase(Locale.ROOT);
        String message = notification.optString("message", "").toUpperCase(Locale.ROOT);

        // 1. REJECTED / DECLINED / CANCELLED / FAILED (Red)
        if (title.contains("REJECT") || title.contains("DECLIN") || title.contains("CANCEL") || title.contains("FAIL")
                || message.contains("REJECT") || message.contains("DECLIN") || message.contains("CANCEL") || message.contains("FAIL")) {
            return new NotificationStyle(
                    R.drawable.bg_circle_red,
                    R.drawable.ic_close,
                    Color.parseColor("#C53030")
            );
        }

        // 2. PAYMENT_APPROVED / CONFIRMED / PAID (Green)
        if ("PAYMENT".equals(type) && (title.contains("APPROV") || title.contains("CONFIRM") || title.contains("PAID") || title.contains("SUCCESS")
                || message.contains("APPROV") || message.contains("CONFIRM") || message.contains("PAID") || message.contains("SUCCESS"))) {
            return new NotificationStyle(
                    R.drawable.bg_circle_green_light,
                    R.drawable.ic_verified,
                    Color.parseColor("#1B5E4C")
            );
        }

        // 3. BOOKING APPROVED (Green)
        if ("BOOKING".equals(type) && (title.contains("APPROV") || message.contains("APPROV"))) {
            return new NotificationStyle(
                    R.drawable.bg_circle_green_light,
                    R.drawable.ic_verified,
                    Color.parseColor("#1B5E4C")
            );
        }

        // 4. BOOKING_REQUEST / BOOKING PENDING (Amber / Orange)
        if ("BOOKING".equals(type) || "BOOKING_REQUEST".equals(type) || "REQUEST".equals(type)) {
            return new NotificationStyle(
                    R.drawable.bg_circle_orange,
                    R.drawable.ic_event_available,
                    Color.parseColor("#D97706")
            );
        }

        // 5. PAYMENT_SUBMITTED / PAYMENT PENDING (Blue)
        if ("PAYMENT".equals(type) || "PAYMENT_SUBMITTED".equals(type)) {
            return new NotificationStyle(
                    R.drawable.bg_circle_blue,
                    R.drawable.ic_receipt,
                    Color.parseColor("#0D6EFD")
            );
        }

        // 6. MESSAGE / CHAT (Purple)
        if ("CHAT".equals(type) || "MESSAGE".equals(type)) {
            return new NotificationStyle(
                    R.drawable.bg_circle_purple,
                    R.drawable.ic_message_square,
                    Color.parseColor("#892CDC")
            );
        }

        // 7. VERIFICATION / SHIELD (Teal)
        if ("VERIFICATION".equals(type) || title.contains("VERIF") || message.contains("VERIF")) {
            return new NotificationStyle(
                    R.drawable.bg_circle_green_light,
                    R.drawable.ic_shield,
                    Color.parseColor("#00897B")
            );
        }

        // 8. REMINDER (Gray)
        if ("REMINDER".equals(type) || title.contains("REMIND") || title.contains("DUE") || message.contains("DUE")) {
            return new NotificationStyle(
                    R.drawable.bg_circle_gray,
                    R.drawable.ic_bell,
                    Color.parseColor("#6C757D")
            );
        }

        // 9. SYSTEM / DEFAULT (Primary Material Green Light)
        return new NotificationStyle(
                R.drawable.bg_circle_green_light,
                R.drawable.ic_bell,
                Color.parseColor("#1B5E4C")
        );
    }
}
