package com.diodeit.spendtrack;

import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.diodeit.spendtrack.databases.DatabaseHelper;
import com.diodeit.spendtrack.fragments.AddExpenseFragment;
import com.diodeit.spendtrack.fragments.AnalyticsFragment;
import com.diodeit.spendtrack.fragments.HistoryFragment;
import com.diodeit.spendtrack.fragments.HomeFragment;
import com.diodeit.spendtrack.fragments.SettingsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG_HOME      = "HOME";
    private static final String TAG_HISTORY   = "HISTORY";
    private static final String TAG_ADD       = "ADD";
    private static final String TAG_ANALYTICS = "ANALYTICS";
    private static final String TAG_SETTINGS  = "SETTINGS";

    // ★ Shared IO pool — one place, no shutdown per fragment
    private static final ExecutorService IO = Executors.newFixedThreadPool(2);
    public static ExecutorService io() { return IO; }

    private BottomNavigationView bottomNav;
    private FragmentManager fragmentManager;
    private String currentTag = TAG_HOME;
    private int lastKeyboardState = -1;

    private DatabaseHelper dbHelper;
    private final Map<String, Fragment> tabFragments = new HashMap<>();
    private String activeTabTag = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fragmentManager = getSupportFragmentManager();
        dbHelper = DatabaseHelper.get(this);
        IO.execute(() -> dbHelper.ensureIndexes());

        initViews();
        setupBottomNavigation();
        setupKeyboardAwareNav();
        setupBackStackListener();

        if (savedInstanceState == null) {
            showTab(TAG_HOME);
            selectNavItem(TAG_HOME);
        } else {
            String top = readTopTag();
            currentTag = top != null ? top : TAG_HOME;
            activeTabTag = isTabTag(currentTag) ? currentTag : null;
            selectNavItem(currentTag);
        }
    }

    public DatabaseHelper getDb() {
        if (dbHelper == null) dbHelper = DatabaseHelper.get(this);
        return dbHelper;
    }

    private void initViews() {
        bottomNav = findViewById(R.id.bottom_navigation);
    }

    private void setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home)      { onTabSelected(TAG_HOME);      return true; }
            if (id == R.id.nav_history)   { onTabSelected(TAG_HISTORY);   return true; }
            if (id == R.id.nav_add)       { onTabSelected(TAG_ADD);       return true; }
            if (id == R.id.nav_analytics) { onTabSelected(TAG_ANALYTICS); return true; }
            if (id == R.id.nav_settings)  { onTabSelected(TAG_SETTINGS);  return true; }
            return false;
        });
    }

    private void onTabSelected(String tag) {
        if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStackImmediate(null,
                    FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
        if (tag.equals(activeTabTag)) {
            selectNavItem(tag);
            return;
        }
        showTab(tag);
        selectNavItem(tag);
    }

    private void showTab(String tag) {
        Fragment target = tabFragments.get(tag);
        FragmentTransaction tx = fragmentManager.beginTransaction();

        // ★ Removed tx.setCustomAnimations() here to make tab switching INSTANT.
        // Animations on tab switches often cause perceived lag.

        if (target == null) {
            target = createFragmentForTag(tag);
            if (target == null) return;
            tabFragments.put(tag, target);
            tx.add(R.id.fragment_container, target, tag);
        }
        if (activeTabTag != null && !activeTabTag.equals(tag)) {
            Fragment current = tabFragments.get(activeTabTag);
            if (current != null && current.isAdded()) tx.hide(current);
        }
        if (target.isAdded()) tx.show(target);

        tx.commitNow(); // commitNow applies the change immediately without waiting for the next frame
        activeTabTag = tag;
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

    private boolean isTabTag(String tag) {
        return TAG_HOME.equals(tag) || TAG_HISTORY.equals(tag) || TAG_ADD.equals(tag)
                || TAG_ANALYTICS.equals(tag) || TAG_SETTINGS.equals(tag);
    }

    public void openDetailFragment(Fragment fragment, String tag) {
        FragmentTransaction tx = fragmentManager.beginTransaction();
        tx.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
        tx.replace(R.id.fragment_container, fragment, tag);
        tx.addToBackStack(tag);
        tx.commit();
        currentTag = tag;
    }

    public void navigateToTab(String tag) {
        onTabSelected(tag);
    }

    private void setupBackStackListener() {
        fragmentManager.addOnBackStackChangedListener(() -> {
            String topTag = readTopTag();
            if (topTag != null) {
                currentTag = topTag;
                selectNavItem(currentTag);
            } else if (activeTabTag != null) {
                currentTag = activeTabTag;
                selectNavItem(activeTabTag);
            }
        });
    }

    private String readTopTag() {
        int count = fragmentManager.getBackStackEntryCount();
        if (count == 0) return null;
        return fragmentManager.getBackStackEntryAt(count - 1).getName();
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

    @Override
    public void onBackPressed() {
        if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    private void setupKeyboardAwareNav() {
        final View root = findViewById(R.id.fragment_container);
        if (root == null) return;

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            boolean imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime());
            setNavVisible(!imeVisible);
            return insets;
        });
        ViewCompat.requestApplyInsets(root);

        final View activityRoot = getWindow().getDecorView().getRootView();
        activityRoot.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        Rect visibleFrame = new Rect();
                        activityRoot.getWindowVisibleDisplayFrame(visibleFrame);
                        int screenHeight = activityRoot.getRootView().getHeight();
                        int heightDiff = screenHeight - visibleFrame.height();
                        boolean keyboardOpen = heightDiff > screenHeight * 0.15;
                        int newState = keyboardOpen ? 1 : 0;
                        if (newState != lastKeyboardState) {
                            lastKeyboardState = newState;
                            setNavVisible(!keyboardOpen);
                        }
                    }
                });
    }

    private void setNavVisible(boolean visible) {
        if (bottomNav == null) return;
        int target = visible ? View.VISIBLE : View.GONE;
        if (bottomNav.getVisibility() != target) bottomNav.setVisibility(target);
    }
}