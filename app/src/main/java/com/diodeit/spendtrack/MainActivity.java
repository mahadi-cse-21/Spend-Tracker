package com.diodeit.spendtrack;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.diodeit.spendtrack.fragments.AddExpenseFragment;
import com.diodeit.spendtrack.fragments.AnalyticsFragment;
import com.diodeit.spendtrack.fragments.HistoryFragment;
import com.diodeit.spendtrack.fragments.HomeFragment;
import com.diodeit.spendtrack.fragments.SettingsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private static final String TAG_HOME      = "HOME";
    private static final String TAG_HISTORY   = "HISTORY";
    private static final String TAG_ADD       = "ADD";
    private static final String TAG_ANALYTICS = "ANALYTICS";
    private static final String TAG_SETTINGS  = "SETTINGS";

    private BottomNavigationView bottomNav;
    private FragmentManager fragmentManager;

    private String currentTag = TAG_HOME;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fragmentManager = getSupportFragmentManager();

        initViews();
        setupBottomNavigation();
        setupKeyboardAwareNav();
        setupBackStackListener();

        if (savedInstanceState == null) {
            showFragment(TAG_HOME, false);
            selectNavItem(TAG_HOME);
        } else {
            // Restore after rotation / process death
            currentTag = readTopTag();
            selectNavItem(currentTag);
        }
    }

    private void initViews() {
        bottomNav = findViewById(R.id.bottom_navigation);
    }

    private void setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home)      { switchTab(TAG_HOME);      return true; }
            if (id == R.id.nav_history)   { switchTab(TAG_HISTORY);   return true; }
            if (id == R.id.nav_add)       { switchTab(TAG_ADD);       return true; }
            if (id == R.id.nav_analytics) { switchTab(TAG_ANALYTICS); return true; }
            if (id == R.id.nav_settings)  { switchTab(TAG_SETTINGS);  return true; }
            return false;
        });
    }

    private void setupKeyboardAwareNav() {
        final View root = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            boolean imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime());
            if (bottomNav != null) {
                bottomNav.setVisibility(imeVisible ? View.GONE : View.VISIBLE);
            }
            return insets;
        });
    }

    /**
     * Listens for back-stack pops (back press or popBackStack calls) and
     * updates the bottom nav highlight to match the currently visible fragment.
     */
    private void setupBackStackListener() {
        fragmentManager.addOnBackStackChangedListener(() -> {
            String topTag = readTopTag();
            if (topTag != null) {
                currentTag = topTag;
                selectNavItem(currentTag);
            } else {
                // Stack is empty → we're on the initial Home fragment
                currentTag = TAG_HOME;
                selectNavItem(TAG_HOME);
            }
        });
    }

    /** Reads the tag of the top back-stack entry, or null if the stack is empty. */
    private String readTopTag() {
        int count = fragmentManager.getBackStackEntryCount();
        if (count == 0) return null;
        return fragmentManager.getBackStackEntryAt(count - 1).getName();
    }

    // ================================================================
    // TAB SWITCHING
    // ================================================================
    private void switchTab(String tag) {
        if (tag.equals(currentTag) && fragmentManager.findFragmentByTag(tag) != null) {
            selectNavItem(tag);
            return;
        }
        showFragment(tag, true);
        // Nav item is synced by the back-stack listener; this call makes the
        // highlight immediate on the same frame as the tap.
        selectNavItem(tag);
    }

    private void showFragment(String tag, boolean addToBackStack) {
        Fragment fragment = createFragmentForTag(tag);
        if (fragment == null) return;

        FragmentTransaction tx = fragmentManager.beginTransaction();
        tx.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
        tx.replace(R.id.fragment_container, fragment, tag);
        if (addToBackStack) {
            tx.addToBackStack(tag);       // ← tag used as the entry name
        }
        tx.commit();
        currentTag = tag;
    }

    private Fragment createFragmentForTag(String tag) {
        switch (tag) {
            case TAG_HOME:      return new HomeFragment();
            case TAG_HISTORY:   return new HistoryFragment();
            case TAG_ADD:       return new AddExpenseFragment();
            case TAG_ANALYTICS: return new AnalyticsFragment();
            case TAG_SETTINGS:  return new SettingsFragment();
            default:            return null;
        }
    }

    private void selectNavItem(String tag) {
        if (bottomNav == null || tag == null) return;
        int itemId = navItemIdForTag(tag);
        if (itemId != -1 && bottomNav.getSelectedItemId() != itemId) {
            bottomNav.getMenu().findItem(itemId).setChecked(true);
        }
    }

    private int navItemIdForTag(String tag) {
        switch (tag) {
            case TAG_HOME:      return R.id.nav_home;
            case TAG_HISTORY:   return R.id.nav_history;
            case TAG_ADD:       return R.id.nav_add;
            case TAG_ANALYTICS: return R.id.nav_analytics;
            case TAG_SETTINGS:  return R.id.nav_settings;
            default:            return -1;
        }
    }

    // ================================================================
    // PUBLIC HELPERS
    // ================================================================
    public void navigateToTab(String tag) {
        switchTab(tag);
    }

    public void openDetailFragment(Fragment fragment, String tag) {
        FragmentTransaction tx = fragmentManager.beginTransaction();
        tx.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
        tx.replace(R.id.fragment_container, fragment, tag);
        tx.addToBackStack(tag);
        tx.commit();
    }

    // ================================================================
    // BACK PRESS
    // ================================================================
    @Override
    public void onBackPressed() {
        if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack();
            // The back-stack listener updates the nav highlight automatically.
            // No manual nav item sync needed.
        } else {
            super.onBackPressed();
        }
    }
}