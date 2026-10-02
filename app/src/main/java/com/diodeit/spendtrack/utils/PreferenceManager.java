package com.diodeit.spendtrack.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceManager {

    private static final String PREF_NAME = "spendtrack_prefs";
    private static final String KEY_LANGUAGE = "language";
    private static final String KEY_CURRENCY = "currency";
    private static final String KEY_DAILY_REMINDER = "daily_reminder";
    private static final String KEY_BUDGET_ALERTS = "budget_alerts";
    private static final String KEY_VIBRATION = "vibration";
    private static final String KEY_SAFE_SPEND = "safe_spend";

    private final SharedPreferences prefs;

    public PreferenceManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public String getLanguage() { return prefs.getString(KEY_LANGUAGE, "bn"); }
    public void setLanguage(String lang) { prefs.edit().putString(KEY_LANGUAGE, lang).apply(); }

    public String getCurrency() { return prefs.getString(KEY_CURRENCY, "BDT"); }
    public void setCurrency(String currency) { prefs.edit().putString(KEY_CURRENCY, currency).apply(); }

    public boolean isDailyReminderEnabled() { return prefs.getBoolean(KEY_DAILY_REMINDER, true); }
    public void setDailyReminder(boolean enabled) { prefs.edit().putBoolean(KEY_DAILY_REMINDER, enabled).apply(); }

    public boolean isBudgetAlertsEnabled() { return prefs.getBoolean(KEY_BUDGET_ALERTS, true); }
    public void setBudgetAlerts(boolean enabled) { prefs.edit().putBoolean(KEY_BUDGET_ALERTS, enabled).apply(); }

    public boolean isVibrationEnabled() { return prefs.getBoolean(KEY_VIBRATION, true); }
    public void setVibration(boolean enabled) { prefs.edit().putBoolean(KEY_VIBRATION, enabled).apply(); }

    public boolean isSafeSpendEnabled() { return prefs.getBoolean(KEY_SAFE_SPEND, true); }
    public void setSafeSpend(boolean enabled) { prefs.edit().putBoolean(KEY_SAFE_SPEND, enabled).apply(); }
}