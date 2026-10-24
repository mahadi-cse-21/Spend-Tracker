package com.diodeit.spendtrack;

import android.app.Application;

import com.diodeit.spendtrack.utils.LocaleHelper;

/** Runs once when the app process starts: applies the saved theme and language. */
public class SpendTrackApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        LocaleHelper.wrap(this);        // sets language flags (Tr / digits)
        LocaleHelper.applyTheme(this);  // light / dark / system
    }
}
