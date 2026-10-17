package com.dormigo;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.util.Locale;

public class NotificationRouter {

    public static void route(Context context, JSONObject notification) {
        if (context == null || notification == null) return;

        SharedPreferences prefs = context.getSharedPreferences("DormigoPrefs", Context.MODE_PRIVATE);
        boolean isStudent = prefs.getBoolean("isStudent", true);
        boolean isLandlord = prefs.getBoolean("isLandlord", false);
        boolean isAdmin = prefs.getBoolean("isAdmin", false);
        String userType = prefs.getString("userType", prefs.getString("user_type", prefs.getString("role", isStudent ? "STUDENT" : "LANDLORD"))).toUpperCase(Locale.ROOT);

        boolean userIsAdmin = "ADMIN".equalsIgnoreCase(userType) || isAdmin;
        boolean userIsLandlord = "LANDLORD".equalsIgnoreCase(userType) || isLandlord || (!isStudent && !userIsAdmin);

        String type = notification.optString("type", "").toUpperCase(Locale.ROOT);
        int referenceId = notification.optInt("reference_id", 0);
        int notificationId = notification.optInt("notification_id", 0);
        String notifMsg = notification.optString("message", "");

        String senderName = parseSenderName(notifMsg, userIsLandlord ? "Tenant" : "Landlord");

        if (userIsAdmin) {
            routeAdmin(context, referenceId, notificationId);
        } else if (userIsLandlord) {
            routeLandlord(context, type, referenceId, notificationId, senderName);
        } else {
            routeStudent(context, type, referenceId, notificationId, senderName);
        }
    }

    private static void routeLandlord(Context context, String type, int referenceId, int notificationId, String senderName) {
        Intent intent;
        switch (type) {
            case "BOOKING":
            case "REQUEST":
                intent = new Intent(context, LandlordRequestsActivity.class);
                if (referenceId > 0) intent.putExtra("BOOKING_ID", referenceId);
                break;

            case "PAYMENT":
                intent = new Intent(context, LandlordTransactionHistoryActivity.class);
                if (referenceId > 0) intent.putExtra("PAYMENT_ID", referenceId);
                break;

            case "CHAT":
            case "MESSAGE":
                if (referenceId > 0) {
                    intent = new Intent(context, LandlordChatMessageActivity.class);
                    intent.putExtra("STUDENT_ID", referenceId);
                    intent.putExtra("STUDENT_NAME", senderName);
                    intent.putExtra("ROOM_INFO", "Tenant Renter");
                } else {
                    intent = new Intent(context, LandlordChatActivity.class);
                }
                break;

            case "REVIEW":
                if (referenceId > 0) {
                    intent = new Intent(context, LandlordReviewsActivity.class);
                    intent.putExtra("HOUSE_ID", referenceId);
                } else {
                    intent = new Intent(context, LandlordPropertiesActivity.class);
                }
                break;

            case "SYSTEM":
            case "VERIFICATION":
            default:
                if (referenceId > 0) {
                    intent = new Intent(context, LandlordPropertyDetailsActivity.class);
                    intent.putExtra("HOUSE_ID", referenceId);
                } else {
                    intent = new Intent(context, LandlordHomeActivity.class);
                }
                break;
        }
        if (notificationId > 0) intent.putExtra("NOTIFICATION_ID", notificationId);
        context.startActivity(intent);
    }

    private static void routeStudent(Context context, String type, int referenceId, int notificationId, String senderName) {
        Intent intent;
        switch (type) {
            case "BOOKING":
            case "REQUEST":
                intent = new Intent(context, BookingRequestsActivity.class);
                if (referenceId > 0) intent.putExtra("BOOKING_ID", referenceId);
                break;

            case "PAYMENT":
                intent = new Intent(context, TransactionHistoryActivity.class);
                if (referenceId > 0) intent.putExtra("PAYMENT_ID", referenceId);
                break;

            case "CHAT":
            case "MESSAGE":
                if (referenceId > 0) {
                    intent = new Intent(context, ChatMessageActivity.class);
                    intent.putExtra("LANDLORD_ID", referenceId);
                    intent.putExtra("LANDLORD_NAME", senderName);
                    intent.putExtra("HOUSE_NAME", "Boarding House");
                } else {
                    intent = new Intent(context, ChatHistoryActivity.class);
                }
                break;

            case "REVIEW":
                intent = new Intent(context, MyReviewsActivity.class);
                break;

            case "SYSTEM":
            case "VERIFICATION":
            default:
                intent = new Intent(context, ProfileActivity.class);
                break;
        }
        if (notificationId > 0) intent.putExtra("NOTIFICATION_ID", notificationId);
        context.startActivity(intent);
    }

    private static void routeAdmin(Context context, int referenceId, int notificationId) {
        Intent intent = new Intent(context, AdminDashboardActivity.class);
        if (referenceId > 0) intent.putExtra("REFERENCE_ID", referenceId);
        if (notificationId > 0) intent.putExtra("NOTIFICATION_ID", notificationId);
        context.startActivity(intent);
    }

    private static String parseSenderName(String message, String defaultName) {
        if (message == null || message.trim().isEmpty()) return defaultName;
        if (message.contains("from ")) {
            try {
                String namePart = message.substring(message.indexOf("from ") + 5).trim();
                String cleanName = namePart.endsWith(".") ? namePart.substring(0, namePart.length() - 1).trim() : namePart;
                if (!cleanName.isEmpty()) {
                    return cleanName;
                }
            } catch (Exception ignored) {}
        }
        return defaultName;
    }
}
