package com.dormigo;

import java.util.Locale;

public class AmenityHelper {

    public static String getAmenityIcon(String amenityName) {
        if (amenityName == null) return "✨";
        String lower = amenityName.toLowerCase(Locale.ROOT);

        if (lower.contains("wifi") || lower.contains("wi-fi") || lower.contains("internet")) return "📶";
        if (lower.contains("aircon") || lower.contains("air conditioning") || lower.contains("ac")) return "❄️";
        if (lower.contains("fan")) return "🌀";
        if (lower.contains("bed") || lower.contains("mattress")) return "🛏️";
        if (lower.contains("cabinet") || lower.contains("closet") || lower.contains("locker")) return "🚪";
        if (lower.contains("table") || lower.contains("desk") || lower.contains("study")) return "🛋️";
        if (lower.contains("chair")) return "🪑";
        if (lower.contains("parking") || lower.contains("garage")) return "🚗";
        if (lower.contains("laundry") || lower.contains("washing")) return "🧺";
        if (lower.contains("kitchen") || lower.contains("cooking")) return "🍳";
        if (lower.contains("refrigerator") || lower.contains("fridge")) return "🧊";
        if (lower.contains("water") || lower.contains("drinking")) return "🚰";
        if (lower.contains("private bath") || lower.contains("en suite")) return "🚿";
        if (lower.contains("shared bath") || lower.contains("bathroom") || lower.contains("toilet")) return "🚽";
        if (lower.contains("hot shower") || lower.contains("heater")) return "♨️";
        if (lower.contains("balcony") || lower.contains("terrace")) return "🏛️";
        if (lower.contains("visitor") || lower.contains("guest")) return "👥";
        if (lower.contains("pet")) return "🐾";
        if (lower.contains("cctv") || lower.contains("security") || lower.contains("guard")) return "🛡️";
        if (lower.contains("curfew")) return "⏰";

        return "✨";
    }

    public static String getAmenityCategory(String amenityName) {
        if (amenityName == null) return "Others";
        String lower = amenityName.toLowerCase(Locale.ROOT);

        if (lower.contains("wifi") || lower.contains("wi-fi") || lower.contains("internet")) return "Internet";
        if (lower.contains("bed") || lower.contains("mattress") || lower.contains("cabinet") || lower.contains("table") || lower.contains("chair") || lower.contains("fan") || lower.contains("aircon")) return "Bedroom";
        if (lower.contains("bath") || lower.contains("shower") || lower.contains("toilet") || lower.contains("heater")) return "Bathroom";
        if (lower.contains("kitchen") || lower.contains("refrigerator") || lower.contains("fridge") || lower.contains("cooking") || lower.contains("water")) return "Kitchen";
        if (lower.contains("cctv") || lower.contains("security") || lower.contains("guard") || lower.contains("curfew") || lower.contains("lock")) return "Security";
        if (lower.contains("parking") || lower.contains("garage")) return "Transportation";

        return "Others";
    }
}
