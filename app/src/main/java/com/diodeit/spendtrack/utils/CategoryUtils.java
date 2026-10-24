package com.diodeit.spendtrack.utils;

import com.diodeit.spendtrack.R;

public class CategoryUtils {

    public static int getCategoryIcon(String category) {
        if (category == null) return R.drawable.ic_category;
        // also understand English names (user-made categories or translated names)
        String lower = category.toLowerCase();
        if (lower.contains("food") || lower.contains("restaurant")) return R.drawable.ic_restaurant;
        if (lower.contains("grocery") || lower.contains("grocer")) return R.drawable.ic_shopping_cart;
        if (lower.contains("transport") || lower.contains("travel")) return R.drawable.ic_directions_car;
        if (lower.contains("rent") || lower.contains("bill") || lower.contains("home")) return R.drawable.ic_home;
        if (lower.contains("health") || lower.contains("medic")) return R.drawable.ic_medication;
        if (lower.contains("shop")) return R.drawable.ic_shopping_bag;
        if (lower.contains("educat") || lower.contains("book")) return R.drawable.ic_auto_stories;
        if (lower.contains("salary") || lower.contains("bonus") || lower.contains("freelance")
                || lower.contains("business") || lower.contains("income")) return R.drawable.ic_payments;
        if (category.contains("খাবার")) return R.drawable.ic_restaurant;
        if (category.contains("বাজার")) return R.drawable.ic_shopping_cart;
        if (category.contains("যাতায়াত")) return R.drawable.ic_directions_car;
        if (category.contains("ভাড়া")) return R.drawable.ic_home;
        if (category.contains("স্বাস্থ্য")) return R.drawable.ic_medication;
        if (category.contains("শপিং")) return R.drawable.ic_shopping_bag;
        if (category.contains("শিক্ষা")) return R.drawable.ic_auto_stories;
        // Any income category → default to payments icon
        if (category.contains("বেতন") || category.contains("আয়") ||
                category.contains("বোনাস") || category.contains("ফ্রিল্যান্স") ||
                category.contains("ব্যবসা") || category.contains("উপর")) {
            return R.drawable.ic_payments;
        }
        return R.drawable.ic_category;
    }
}