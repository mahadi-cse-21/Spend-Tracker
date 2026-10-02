package com.diodeit.spendtrack.fragments;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.diodeit.spendtrack.R;
import com.diodeit.spendtrack.databases.DatabaseHelper;
import com.diodeit.spendtrack.models.Expense;
import com.diodeit.spendtrack.utils.CsvImporter;
import com.diodeit.spendtrack.utils.ExportHelper;
import com.diodeit.spendtrack.utils.PreferenceManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.List;

public class SettingsFragment extends Fragment {

    private MaterialSwitch switchDailyReminder;
    private LinearLayout rowBackup, rowRestore, rowClear, rowPrivacy;

    private PreferenceManager prefManager;
    private DatabaseHelper dbHelper;

    /** File picker for CSV restore. Registered in onCreate. */
    private ActivityResultLauncher<String[]> csvPicker;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Register the file picker. We accept any MIME type because some
        // file managers report CSVs as "application/octet-stream" or "text/plain".
        csvPicker = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) performRestore(uri);
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        prefManager = new PreferenceManager(requireContext());
        dbHelper = new DatabaseHelper(requireContext());

        initViews(view);
        loadSettings();
        setupClickListeners();

        return view;
    }

    private void initViews(View view) {
        switchDailyReminder = view.findViewById(R.id.switch_daily_reminder);
        rowBackup           = view.findViewById(R.id.row_backup);
        rowRestore          = view.findViewById(R.id.row_restore);
        rowClear            = view.findViewById(R.id.row_clear);
        rowPrivacy          = view.findViewById(R.id.row_privacy);
    }

    private void loadSettings() {
        switchDailyReminder.setChecked(prefManager.isDailyReminderEnabled());
    }

    private void setupClickListeners() {
        switchDailyReminder.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefManager.setDailyReminder(isChecked));

        rowBackup.setOnClickListener(v -> exportBackup());
        rowRestore.setOnClickListener(v -> openCsvPicker());
        rowClear.setOnClickListener(v -> confirmClear());
        rowPrivacy.setOnClickListener(v -> showPrivacyPolicy());
    }

    // ================================================================
    // BACKUP
    // ================================================================
    private void exportBackup() {
        List<Expense> all = dbHelper.getAllExpenses();
        if (all == null || all.isEmpty()) {
            Toast.makeText(requireContext(), "কোনো ডেটা নেই", Toast.LENGTH_SHORT).show();
            return;
        }
        boolean ok = ExportHelper.exportCsv(requireContext(), all);
        Toast.makeText(requireContext(),
                ok ? "ব্যাকআপ সম্পন্ন — Downloads/SpendTrack"
                        : "ব্যাকআপ তৈরি ব্যর্থ",
                Toast.LENGTH_LONG).show();
    }

    // ================================================================
    // RESTORE
    // ================================================================
    private void openCsvPicker() {
        // Accept any MIME type because CSV files can be reported differently
        // by different file managers.
        csvPicker.launch(new String[]{
                "text/csv",
                "text/comma-separated-values",
                "application/csv",
                "text/plain",
                "*/*"
        });
    }

    private void performRestore(Uri uri) {
        // Confirm before importing on top of existing data
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("ডেটা রিস্টোর")
                .setMessage("আপনি কি নির্বাচিত CSV ফাইল থেকে সব লেনদেন ইমপোর্ট করতে চান?\n\n" +
                        "বর্তমান ডেটার সাথে যোগ হবে — কোনো ডেটা মুছে যাবে না।")
                .setPositiveButton("ইমপোর্ট করুন", (d, w) -> doImport(uri))
                .setNegativeButton("বাতিল", null)
                .show();
    }

    private void doImport(Uri uri) {
        Toast.makeText(requireContext(), "ইমপোর্ট হচ্ছে...", Toast.LENGTH_SHORT).show();

        // Run on a background thread to avoid blocking the UI
        new Thread(() -> {
            int imported = CsvImporter.importFromUri(requireContext(), uri, dbHelper);

            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                if (imported > 0) {
                    Toast.makeText(requireContext(),
                            imported + "টি লেনদেন সফলভাবে ইমপোর্ট হয়েছে",
                            Toast.LENGTH_LONG).show();
                } else if (imported == 0) {
                    Toast.makeText(requireContext(),
                            "ফাইলে কোনো লেনদেন পাওয়া যায়নি",
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(requireContext(),
                            "ইমপোর্ট ব্যর্থ হয়েছে",
                            Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }

    // ================================================================
    // CLEAR DATA
    // ================================================================
    private void confirmClear() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("সব ডেটা মুছুন")
                .setMessage("আপনি কি নিশ্চিতভাবে সব লেনদেন এবং বাজেট মুছে ফেলতে চান? এটি ফিরিয়ে আনা যাবে না।")
                .setPositiveButton("মুছে ফেলুন", (dialog, which) -> {
                    dbHelper.clearAllData();
                    Toast.makeText(requireContext(),
                            "সব ডেটা মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("বাতিল", null)
                .show();
    }

    // ================================================================
    // PRIVACY POLICY
    // ================================================================
    private void showPrivacyPolicy() {
        String policy =
                "স্পেন্ডট্র্যাক — গোপনীয়তা নীতি\n\n" +
                        "১. আপনার সব আর্থিক তথ্য (খরচ, আয়) শুধুমাত্র আপনার " +
                        "ডিভাইসের অভ্যন্তরীণ SQLite ডাটাবেসে সংরক্ষিত থাকে।\n\n" +
                        "২. অ্যাপটি সম্পূর্ণ অফলাইনে কাজ করে — কোনো তথ্য কোনো " +
                        "সার্ভারে পাঠানো হয় না।\n\n" +
                        "৩. কোনো তৃতীয় পক্ষের সাথে কোনো ব্যক্তিগত তথ্য শেয়ার করা হয় না।\n\n" +
                        "৪. অ্যাপটি কোনো ক্যামেরা বা ইন্টারনেট পারমিশন চায় না।\n\n" +
                        "৫. 'সব ডেটা মুছুন' অপশন ব্যবহার করে যেকোনো সময় সব তথ্য " +
                        "স্থায়ীভাবে মুছে ফেলা যায়।\n\n" +
                        "যদি আপনার কোনো প্রশ্ন থাকে, যোগাযোগ করুন: support@diodeit.com";

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("গোপনীয়তা নীতি")
                .setMessage(policy)
                .setPositiveButton("বুঝেছি", null)
                .show();
    }
}