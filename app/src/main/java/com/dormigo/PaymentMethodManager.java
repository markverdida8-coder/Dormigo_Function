package com.dormigo;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class PaymentMethodManager {

    public interface OnPaymentSettingsChangeListener {
        void onPaymentSettingsChanged(PaymentSettings settings);
    }

    public static class PaymentSettings {
        public int houseId;
        public boolean cashEnabled;
        public boolean gcashEnabled;
        public String gcashQrCode;

        public PaymentSettings(int houseId, boolean cashEnabled, boolean gcashEnabled, String gcashQrCode) {
            this.houseId = houseId;
            this.cashEnabled = cashEnabled;
            this.gcashEnabled = gcashEnabled;
            this.gcashQrCode = gcashQrCode != null ? gcashQrCode : "";
        }
    }

    private static PaymentMethodManager instance;
    private final ApiClient apiClient = new ApiClient();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private PaymentSettings currentSettings;

    public static synchronized PaymentMethodManager getInstance() {
        if (instance == null) {
            instance = new PaymentMethodManager();
        }
        return instance;
    }

    public PaymentSettings getCurrentSettings() {
        return currentSettings;
    }

    public boolean isCashEnabled() {
        return currentSettings != null && currentSettings.cashEnabled;
    }

    public boolean isGcashEnabled() {
        return currentSettings != null && currentSettings.gcashEnabled;
    }

    public String getQrCode() {
        return currentSettings != null ? currentSettings.gcashQrCode : "";
    }

    public void refreshPaymentSettings(int houseId, OnPaymentSettingsChangeListener listener) {
        if (houseId <= 0) {
            if (listener != null) {
                mainHandler.post(() -> listener.onPaymentSettingsChanged(new PaymentSettings(houseId, false, false, "")));
            }
            return;
        }

        apiClient.getBoardingHouses(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (listener != null) {
                    mainHandler.post(() -> listener.onPaymentSettingsChanged(
                            currentSettings != null ? currentSettings : new PaymentSettings(houseId, false, false, "")
                    ));
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (!response.isSuccessful() || response.body() == null) {
                    if (listener != null) {
                        mainHandler.post(() -> listener.onPaymentSettingsChanged(
                                currentSettings != null ? currentSettings : new PaymentSettings(houseId, false, false, "")
                        ));
                    }
                    return;
                }

                try {
                    String body = response.body().string();
                    JSONObject json = new JSONObject(body);
                    if (json.optBoolean("success", false)) {
                        JSONArray data = json.optJSONArray("data");
                        if (data != null) {
                            for (int idx = 0; idx < data.length(); idx++) {
                                JSONObject h = data.optJSONObject(idx);
                                if (h != null && h.optInt("house_id", 0) == houseId) {
                                    boolean cash = parseBooleanStrict(h, "cash_enabled", true);
                                    String qrPath = h.optString("gcash_qr_code", "");
                                    boolean hasQr = !qrPath.trim().isEmpty() && !"null".equalsIgnoreCase(qrPath.trim());

                                    currentSettings = new PaymentSettings(houseId, cash, hasQr, qrPath);
                                    if (listener != null) {
                                        mainHandler.post(() -> listener.onPaymentSettingsChanged(currentSettings));
                                    }
                                    return;
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {}

                if (listener != null) {
                    mainHandler.post(() -> listener.onPaymentSettingsChanged(
                            currentSettings != null ? currentSettings : new PaymentSettings(houseId, false, false, "")
                    ));
                }
            }
        });
    }

    public static boolean parseBooleanStrict(JSONObject json, String key, boolean defaultValue) {
        if (json == null || !json.has(key)) return defaultValue;
        Object val = json.opt(key);
        if (val == null || val == JSONObject.NULL) return defaultValue;
        if (val instanceof Boolean) return (Boolean) val;
        if (val instanceof Integer || val instanceof Long) return ((Number) val).intValue() != 0;
        String str = val.toString().trim();
        if ("1".equals(str) || "true".equalsIgnoreCase(str) || "t".equalsIgnoreCase(str)) return true;
        if ("0".equals(str) || "false".equalsIgnoreCase(str) || "f".equalsIgnoreCase(str)) return false;
        return defaultValue;
    }
}
