package com.diodeit.spendtrack.utils;

import android.content.Context;
import android.content.res.Configuration;

import androidx.appcompat.app.AppCompatDelegate;

import java.util.Locale;

/**
 * One place for language + theme handling.
 *  - LocaleHelper.wrap(context)  : gives a context in the saved language (Bangla / English)
 *  - LocaleHelper.applyTheme(ctx): applies the saved theme (system / light / dark)
 */
public final class LocaleHelper {

    private LocaleHelper() {}

    public static Context wrap(Context base) {
        PreferenceManager prefs = new PreferenceManager(base);
        boolean english = "en".equals(prefs.getLanguage());

        // tell the helpers which language is active
        Tr.setEnglish(english);
        BengaliNumberConverter.setBengaliDigits(!english);

        Locale locale = english ? new Locale("en", "US") : new Locale("bn", "BD");
        Locale.setDefault(locale);

        Configuration config = new Configuration(base.getResources().getConfiguration());
        config.fontScale = 1.0f;
        config.setLocale(locale);
        config.setLayoutDirection(locale);
        return base.createConfigurationContext(config);
    }

    public static void applyTheme(Context context) {
        int mode;
        switch (new PreferenceManager(context).getThemeMode()) {
            case PreferenceManager.THEME_LIGHT: mode = AppCompatDelegate.MODE_NIGHT_NO; break;
            case PreferenceManager.THEME_DARK:  mode = AppCompatDelegate.MODE_NIGHT_YES; break;
            default:                            mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }
        AppCompatDelegate.setDefaultNightMode(mode);
    }
}
