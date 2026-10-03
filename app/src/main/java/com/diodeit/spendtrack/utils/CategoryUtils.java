package com.diodeit.spendtrack.utils;

import com.diodeit.spendtrack.R;

public class CategoryUtils {

    public static int getCategoryIcon(String category) {
        if (category == null) return R.drawable.ic_category;
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