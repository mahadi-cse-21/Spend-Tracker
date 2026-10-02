package com.diodeit.spendtrack.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateUtils {

    private static final String[] BENGALI_MONTHS = {
            "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
            "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
    };

    private static final String[] BENGALI_DAYS = {
            "রবিবার", "সোমবার", "মঙ্গলবার", "বুধবার", "বৃহস্পতিবার", "শুক্রবার", "শনিবার"
    };

    public static String getBengaliMonth(int month) {
        if (month >= 0 && month < 12) {
            return BENGALI_MONTHS[month];
        }
        return "";
    }

    public static String getBengaliDay(int dayOfWeek) {
        if (dayOfWeek >= 1 && dayOfWeek <= 7) {
            return BENGALI_DAYS[dayOfWeek - 1];
        }
        return "";
    }

    public static String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    public static String formatTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    public static String formatDateTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy • hh:mm a", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    public static String getMonthKey() {
        Calendar cal = Calendar.getInstance();
        return cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) + 1);
    }

    public static int getDaysInMonth() {
        Calendar cal = Calendar.getInstance();
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    public static int getDaysRemainingInMonth() {
        Calendar cal = Calendar.getInstance();
        int lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        return lastDay - cal.get(Calendar.DAY_OF_MONTH);
    }
}