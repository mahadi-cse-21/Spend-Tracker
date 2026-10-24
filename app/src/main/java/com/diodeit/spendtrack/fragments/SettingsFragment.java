package com.diodeit.spendtrack.fragments;

import com.diodeit.spendtrack.utils.Tr;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.diodeit.spendtrack.R;
import com.diodeit.spendtrack.databases.DatabaseHelper;
import com.diodeit.spendtrack.models.Expense;
import com.diodeit.spendtrack.utils.BengaliNumberConverter;
import com.diodeit.spendtrack.utils.CsvImporter;
import com.diodeit.spendtrack.utils.ExportHelper;
import com.diodeit.spendtrack.utils.LocaleHelper;
import com.diodeit.spendtrack.utils.PreferenceManager;
import com.diodeit.spendtrack.utils.ReminderScheduler;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.util.List;
import java.util.Locale;

public class SettingsFragment extends Fragment {

    private static final String CONTACT_EMAIL = "mahadi.cse.21@gmail.com";
    private static final String CONTACT_PHONE = "+8801780689788";
    private static final String CONTACT_WHATSAPP = "+8801780689788";

    private String appVersionName = "2.2.0";

    private MaterialSwitch switchDailyReminder;
    private LinearLayout rowDailyReminder, rowBackup, rowRestore, rowClear, rowPrivacy, rowContact;
    private TextView tvReminderTime;
    private LinearLayout rowLanguage, rowTheme;
    private TextView tvLanguageValue, tvThemeValue;

    private PreferenceManager prefManager;
    private DatabaseHelper dbHelper;

    private ActivityResultLauncher<String[]> csvPicker;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    TextView version_name;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        csvPicker = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> { if (uri != null) performRestore(uri); });

        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) {
                        ReminderScheduler.setDailyReminder(requireContext(), true);
                    } else {
                        Toast.makeText(requireContext(), Tr.t("নোটিফিকেশন পারমিশন ছাড়া রিমাইন্ডার কাজ করবে না"), Toast.LENGTH_LONG).show();
                        switchDailyReminder.setChecked(false);
                        prefManager.setDailyReminder(false);
                    }
                });

        readAppVersion();
    }

    private void readAppVersion() {
        try {
            appVersionName = requireContext()
                    .getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0)
                    .versionName;
        } catch (Exception e) {
            Log.e("SpendTrack_Version", "Failed to read version name", e);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        prefManager = new PreferenceManager(requireContext());
        dbHelper = DatabaseHelper.get(requireContext());
        version_name = view.findViewById(R.id.version_name);
        version_name.setText(appVersionName);

        initViews(view);
        loadSettings();
        setupClickListeners();

        return view;
    }

    private void initViews(View view) {
        switchDailyReminder = view.findViewById(R.id.switch_daily_reminder);
        rowDailyReminder    = view.findViewById(R.id.row_daily_reminder);
        tvReminderTime      = view.findViewById(R.id.tv_reminder_time);
        rowBackup           = view.findViewById(R.id.row_backup);
        rowRestore          = view.findViewById(R.id.row_restore);
        rowClear            = view.findViewById(R.id.row_clear);
        rowPrivacy          = view.findViewById(R.id.row_privacy);
        rowContact          = view.findViewById(R.id.row_contact);
        rowLanguage         = view.findViewById(R.id.row_language);
        rowTheme            = view.findViewById(R.id.row_theme);
        tvLanguageValue     = view.findViewById(R.id.tv_language_value);
        tvThemeValue        = view.findViewById(R.id.tv_theme_value);
    }

    private void loadSettings() {
        boolean isEnabled = prefManager.isDailyReminderEnabled();
        switchDailyReminder.setChecked(isEnabled);
        updateReminderTimeText();
        updateLanguageAndThemeText();
        ReminderScheduler.setDailyReminder(requireContext(), isEnabled);
    }

    private void updateReminderTimeText() {
        int hour = prefManager.getReminderHour();
        int minute = prefManager.getReminderMinute();

        String period = hour >= 12 ? Tr.t("বিকাল/রাত") : Tr.t("সকাল");
        int displayHour = hour % 12;
        if (displayHour == 0) displayHour = 12;

        BengaliNumberConverter bn = new BengaliNumberConverter();
        String timeStr = bn.toBengali(String.valueOf(displayHour)) + ":" +
                bn.toBengali(String.format(Locale.US, "%02d", minute));

        if (Tr.isEnglish()) {
            tvReminderTime.setText(timeStr + " " + period);
        } else {
            tvReminderTime.setText(period + " " + timeStr + " টা");
        }
    }

    private void setupClickListeners() {
        switchDailyReminder.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefManager.setDailyReminder(isChecked);

            if (isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                            != PackageManager.PERMISSION_GRANTED) {
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                        return;
                    }
                }
                ReminderScheduler.setDailyReminder(requireContext(), true);
            } else {
                ReminderScheduler.setDailyReminder(requireContext(), false);
                Toast.makeText(requireContext(), Tr.t("রিমাইন্ডার বন্ধ করা হয়েছে"), Toast.LENGTH_SHORT).show();
            }
        });

        rowDailyReminder.setOnClickListener(v -> {
            if (!switchDailyReminder.isChecked()) {
                switchDailyReminder.setChecked(true);
                return;
            }
            showTimePicker();
        });

        rowLanguage.setOnClickListener(v -> showLanguageDialog());
        rowTheme.setOnClickListener(v -> showThemeDialog());

        rowBackup.setOnClickListener(v -> exportBackup());
        rowRestore.setOnClickListener(v -> openCsvPicker());
        rowClear.setOnClickListener(v -> confirmClear());
        rowPrivacy.setOnClickListener(v -> showPrivacyPolicy());
        rowContact.setOnClickListener(v -> showContactDialog());
    }

    // ─── Language & Theme ────────────────────────────────────────────

    private void updateLanguageAndThemeText() {
        tvLanguageValue.setText("en".equals(prefManager.getLanguage())
                ? getString(R.string.english) : getString(R.string.bengali));

        String mode = prefManager.getThemeMode();
        if (PreferenceManager.THEME_LIGHT.equals(mode)) {
            tvThemeValue.setText(R.string.theme_light);
        } else if (PreferenceManager.THEME_DARK.equals(mode)) {
            tvThemeValue.setText(R.string.theme_dark);
        } else {
            tvThemeValue.setText(R.string.theme_system);
        }
    }

    private void showLanguageDialog() {
        String[] items = { getString(R.string.bengali), getString(R.string.english) };
        int checked = "en".equals(prefManager.getLanguage()) ? 1 : 0;

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.choose_language)
                .setSingleChoiceItems(items, checked, (dialog, which) -> {
                    String lang = (which == 1) ? "en" : "bn";
                    dialog.dismiss();
                    if (!lang.equals(prefManager.getLanguage())) {
                        prefManager.setLanguage(lang);
                        // restart the screen so every text uses the new language
                        requireActivity().recreate();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showThemeDialog() {
        String[] items = {
                getString(R.string.theme_system),
                getString(R.string.theme_light),
                getString(R.string.theme_dark)
        };
        String current = prefManager.getThemeMode();
        int checked = PreferenceManager.THEME_LIGHT.equals(current) ? 1
                : PreferenceManager.THEME_DARK.equals(current) ? 2 : 0;

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.choose_theme)
                .setSingleChoiceItems(items, checked, (dialog, which) -> {
                    String mode = (which == 1) ? PreferenceManager.THEME_LIGHT
                            : (which == 2) ? PreferenceManager.THEME_DARK
                            : PreferenceManager.THEME_SYSTEM;
                    dialog.dismiss();
                    if (!mode.equals(prefManager.getThemeMode())) {
                        prefManager.setThemeMode(mode);
                        // this also restarts the screen with the new colors
                        LocaleHelper.applyTheme(requireContext());
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showTimePicker() {
        int currentHour = prefManager.getReminderHour();
        int currentMinute = prefManager.getReminderMinute();

        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(currentHour)
                .setMinute(currentMinute)
                .setTitleText(Tr.t("রিমাইন্ডারের সময় নির্বাচন করুন"))
                .build();

        picker.addOnPositiveButtonClickListener(v -> {
            int newHour = picker.getHour();
            int newMinute = picker.getMinute();

            prefManager.setReminderTime(newHour, newMinute);
            updateReminderTimeText();
            ReminderScheduler.setDailyReminder(requireContext(), true);

            Toast.makeText(requireContext(), Tr.t("সময় পরিবর্তন করা হয়েছে"), Toast.LENGTH_SHORT).show();
        });

        picker.show(getParentFragmentManager(), "TIME_PICKER");
    }

    @Override
    public void onResume() {
        super.onResume();
        if (prefManager.isDailyReminderEnabled()) {
            ReminderScheduler.setDailyReminder(requireContext(), true);
        }
    }

    private void showContactDialog() {
        String message =
                Tr.t("ডেভেলপার: মেহেদী হাসান\n\n") +
                        Tr.t("📧 ইমেইল: ") + CONTACT_EMAIL + "\n" +
                        Tr.t("📞 মোবাইল: ") + CONTACT_PHONE + "\n" +
                        Tr.t("💬 হোয়াটসঅ্যাপ: ") + CONTACT_PHONE + "\n\n" +
                        Tr.t("আপনার মতামত, সমস্যা বা পরামর্শ জানাতে চাইলে ") +
                        Tr.t("নিচের যেকোনো মাধ্যমে যোগাযোগ করুন।");

        final String[] options = {
                Tr.t("📧  ইমেইল পাঠান"),
                Tr.t("💬  হোয়াটসঅ্যাপে মেসেজ"),
                Tr.t("📞  ফোন করুন"),
                Tr.t("⭐  প্লে স্টোরে রিভিউ দিন")
        };

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(Tr.t("যোগাযোগ করুন"))
                .setMessage(message)
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: sendEmail();     break;
                        case 1: openWhatsApp();  break;
                        case 2: dialPhone();     break;
                        case 3: openPlayStore(); break;
                    }
                })
                .setNegativeButton(Tr.t("বন্ধ করুন"), null)
                .show();
    }

    private void sendEmail() {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:" + CONTACT_EMAIL));
        intent.putExtra(Intent.EXTRA_SUBJECT, Tr.t("স্পেন্ডট্র্যাক — মতামত / সমস্যা"));
        intent.putExtra(Intent.EXTRA_TEXT,
                Tr.t("আপনার মতামত বা সমস্যা এখানে লিখুন:\n\n\n") +
                        "---\n" +
                        Tr.t("অ্যাপ সংস্করণ: ") + appVersionName + "\n" +
                        Tr.t("ডিভাইস: ") + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL + "\n" +
                        "Android: " + android.os.Build.VERSION.RELEASE);
        try {
            startActivity(Intent.createChooser(intent, Tr.t("ইমেইল পাঠান")));
        } catch (Exception e) {
            Toast.makeText(requireContext(),
                    Tr.t("কোনো ইমেইল অ্যাপ পাওয়া যায়নি। সরাসরি ইমেইল করুন: ") + CONTACT_EMAIL,
                    Toast.LENGTH_LONG).show();
        }
    }

    private void openWhatsApp() {
        try {
            String url = "https://wa.me/" + CONTACT_WHATSAPP +
                    "?text=" + Uri.encode(Tr.t("স্পেন্ডট্র্যাক অ্যাপ সম্পর্কে:"));
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(),
                    Tr.t("হোয়াটসঅ্যাপ ইনস্টল করা নেই। ফোন করুন: ") + CONTACT_PHONE,
                    Toast.LENGTH_LONG).show();
        }
    }

    private void dialPhone() {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + CONTACT_PHONE));
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(),
                    Tr.t("ফোন অ্যাপ পাওয়া যায়নি। ইমেইল করুন: ") + CONTACT_EMAIL,
                    Toast.LENGTH_LONG).show();
        }
    }

    private void openPlayStore() {
        try {
            String packageName = requireContext().getPackageName();
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + packageName));
            startActivity(intent);
        } catch (Exception e) {
            try {
                String packageName = requireContext().getPackageName();
                Intent intent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=" + packageName));
                startActivity(intent);
            } catch (Exception e2) {
                Toast.makeText(requireContext(),
                        Tr.t("প্লে স্টোর খোলা যায়নি"), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void exportBackup() {
        List<Expense> all = dbHelper.getAllExpenses();
        if (all == null || all.isEmpty()) {
            Toast.makeText(requireContext(), Tr.t("কোনো ডেটা নেই"), Toast.LENGTH_SHORT).show();
            return;
        }
        boolean ok = ExportHelper.exportCsv(requireContext(), all);
        Toast.makeText(requireContext(),
                ok ? Tr.t("ব্যাকআপ সম্পন্ন — Downloads/SpendTrack")
                        : Tr.t("ব্যাকআপ তৈরি ব্যর্থ"),
                Toast.LENGTH_LONG).show();
    }

    private void openCsvPicker() {
        csvPicker.launch(new String[]{
                "text/csv", "text/comma-separated-values",
                "application/csv", "text/plain", "*/*"
        });
    }

    private void performRestore(Uri uri) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(Tr.t("ডেটা রিস্টোর"))
                .setMessage(Tr.t("আপনি কি নির্বাচিত CSV ফাইল থেকে সব লেনদেন ইমপোর্ট করতে চান?\n\n") +
                        Tr.t("বর্তমান ডেটার সাথে যোগ হবে — কোনো ডেটা মুছে যাবে না।"))
                .setPositiveButton(Tr.t("ইমপোর্ট করুন"), (d, w) -> doImport(uri))
                .setNegativeButton(Tr.t("বাতিল"), null)
                .show();
    }

    private void doImport(Uri uri) {
        Toast.makeText(requireContext(), Tr.t("ইমপোর্ট হচ্ছে..."), Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            int imported = CsvImporter.importFromUri(requireContext(), uri, dbHelper);
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                if (imported > 0) {
                    Toast.makeText(requireContext(),
                            imported + Tr.t("টি লেনদেন সফলভাবে ইমপোর্ট হয়েছে"),
                            Toast.LENGTH_LONG).show();
                } else if (imported == 0) {
                    Toast.makeText(requireContext(),
                            Tr.t("ফাইলে কোনো লেনদেন পাওয়া যায়নি"), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(requireContext(),
                            Tr.t("ইমপোর্ট ব্যর্থ হয়েছে"), Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }

    private void confirmClear() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(Tr.t("সব ডেটা মুছুন"))
                .setMessage(Tr.t("আপনি কি নিশ্চিতভাবে সব লেনদেন এবং বাজেট মুছে ফেলতে চান? ") +
                        Tr.t("এটি ফিরিয়ে আনা যাবে না।"))
                .setPositiveButton(Tr.t("মুছে ফেলুন"), (dialog, which) -> {
                    dbHelper.clearAllData();
                    Toast.makeText(requireContext(),
                            Tr.t("সব ডেটা মুছে ফেলা হয়েছে"), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(Tr.t("বাতিল"), null)
                .show();
    }

    private void showPrivacyPolicy() {
        String policy =
                Tr.t("স্পেন্ডট্র্যাক — গোপনীয়তা নীতি\n\n") +
                        Tr.t("১. আপনার সব আর্থিক তথ্য (খরচ, আয়) শুধুমাত্র আপনার ") +
                        Tr.t("ডিভাইসের অভ্যন্তরীণ SQLite ডাটাবেসে সংরক্ষিত থাকে।\n\n") +
                        Tr.t("২. অ্যাপটি সম্পূর্ণ অফলাইনে কাজ করে — কোনো তথ্য কোনো ") +
                        Tr.t("সার্ভারে পাঠানো হয় না।\n\n") +
                        Tr.t("৩. কোনো তৃতীয় পক্ষের সাথে কোনো ব্যক্তিগত তথ্য শেয়ার করা হয় না।\n\n") +
                        Tr.t("৪. অ্যাপটি কোনো ক্যামেরা বা ইন্টারনেট পারমিশন চায় না।\n\n") +
                        Tr.t("৫. 'সব ডেটা মুছুন' অপশন ব্যবহার করে যেকোনো সময় সব তথ্য ") +
                        Tr.t("স্থায়ীভাবে মুছে ফেলা যায়।\n\n") +
                        Tr.t("যোগাযোগ: ") + CONTACT_EMAIL;

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(Tr.t("গোপনীয়তা নীতি"))
                .setMessage(policy)
                .setPositiveButton(Tr.t("বুঝেছি"), null)
                .show();
    }
}