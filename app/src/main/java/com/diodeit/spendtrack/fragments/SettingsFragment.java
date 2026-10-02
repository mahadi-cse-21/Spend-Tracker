package com.diodeit.spendtrack.fragments;

import android.content.Intent;
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

    // ★ Replace these with your real contact info
    private static final String CONTACT_EMAIL = "mahadi.cse.21@gmail.com";
    private static final String CONTACT_PHONE = "+8801780689788";
    private static final String CONTACT_WHATSAPP = "+8801780689788"; // no + sign

    private MaterialSwitch switchDailyReminder;
    private LinearLayout rowBackup, rowRestore, rowClear, rowPrivacy, rowContact;

    private PreferenceManager prefManager;
    private DatabaseHelper dbHelper;

    private ActivityResultLauncher<String[]> csvPicker;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        csvPicker = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> { if (uri != null) performRestore(uri); });
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
        rowContact          = view.findViewById(R.id.row_contact);
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
        rowContact.setOnClickListener(v -> showContactDialog());
    }

    // ================================================================
    // CONTACT DIALOG
    // ================================================================
    private void showContactDialog() {
        String message =
                "ডেভেলপার: মেহেদী হাসান\n\n" +
                        "📧 ইমেইল: " + CONTACT_EMAIL + "\n" +
                        "📞 মোবাইল: " + CONTACT_PHONE + "\n" +
                        "💬 হোয়াটসঅ্যাপ: " + CONTACT_PHONE + "\n\n" +
                        "আপনার মতামত, সমস্যা বা পরামর্শ জানাতে চাইলে " +
                        "নিচের যেকোনো মাধ্যমে যোগাযোগ করুন।";

        final String[] options = {
                "📧  ইমেইল পাঠান",
                "💬  হোয়াটসঅ্যাপে মেসেজ",
                "📞  ফোন করুন",
                "⭐  প্লে স্টোরে রিভিউ দিন"
        };

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("যোগাযোগ করুন")
                .setMessage(message)
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: sendEmail();     break;
                        case 1: openWhatsApp();  break;
                        case 2: dialPhone();     break;
                        case 3: openPlayStore(); break;
                    }
                })
                .setNegativeButton("বন্ধ করুন", null)
                .show();
    }
    /** Opens the email app with the support address pre-filled. */
    private void sendEmail() {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:" + CONTACT_EMAIL));
        intent.putExtra(Intent.EXTRA_SUBJECT, "স্পেন্ডট্র্যাক — মতামত / সমস্যা");
        intent.putExtra(Intent.EXTRA_TEXT,
                "আপনার মতামত বা সমস্যা এখানে লিখুন:\n\n\n" +
                        "---\n" +
                        "অ্যাপ সংস্করণ: 1.0\n" +
                        "ডিভাইস: " + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL + "\n" +
                        "Android: " + android.os.Build.VERSION.RELEASE);
        try {
            startActivity(Intent.createChooser(intent, "ইমেইল পাঠান"));
        } catch (Exception e) {
            Toast.makeText(requireContext(),
                    "কোনো ইমেইল অ্যাপ পাওয়া যায়নি। সরাসরি ইমেইল করুন: " + CONTACT_EMAIL,
                    Toast.LENGTH_LONG).show();
        }
    }

    /** Opens WhatsApp with the support number. */
    private void openWhatsApp() {
        try {
            String url = "https://wa.me/" + CONTACT_WHATSAPP +
                    "?text=" + Uri.encode("স্পেন্ডট্র্যাক অ্যাপ সম্পর্কে:");
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(),
                    "হোয়াটসঅ্যাপ ইনস্টল করা নেই। ফোন করুন: " + CONTACT_PHONE,
                    Toast.LENGTH_LONG).show();
        }
    }

    /** Opens the phone dialer with the support number. */
    private void dialPhone() {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + CONTACT_PHONE));
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(),
                    "ফোন অ্যাপ পাওয়া যায়নি। ইমেইল করুন: " + CONTACT_EMAIL,
                    Toast.LENGTH_LONG).show();
        }
    }

    /** Opens the Play Store listing so users can leave a review. */
    private void openPlayStore() {
        try {
            // ★ Replace with your real package name
            String packageName = requireContext().getPackageName();
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + packageName));
            startActivity(intent);
        } catch (Exception e) {
            // Fallback to web browser if Play Store app not installed
            try {
                String packageName = requireContext().getPackageName();
                Intent intent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=" + packageName));
                startActivity(intent);
            } catch (Exception e2) {
                Toast.makeText(requireContext(),
                        "প্লে স্টোর খোলা যায়নি", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ================================================================
    // BACKUP / RESTORE / CLEAR / PRIVACY  (unchanged)
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

    private void openCsvPicker() {
        csvPicker.launch(new String[]{
                "text/csv", "text/comma-separated-values",
                "application/csv", "text/plain", "*/*"
        });
    }

    private void performRestore(Uri uri) {
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
                            "ফাইলে কোনো লেনদেন পাওয়া যায়নি", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(requireContext(),
                            "ইমপোর্ট ব্যর্থ হয়েছে", Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }

    private void confirmClear() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("সব ডেটা মুছুন")
                .setMessage("আপনি কি নিশ্চিতভাবে সব লেনদেন এবং বাজেট মুছে ফেলতে চান? " +
                        "এটি ফিরিয়ে আনা যাবে না।")
                .setPositiveButton("মুছে ফেলুন", (dialog, which) -> {
                    dbHelper.clearAllData();
                    Toast.makeText(requireContext(),
                            "সব ডেটা মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("বাতিল", null)
                .show();
    }

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
                        "যোগাযোগ: " + CONTACT_EMAIL;

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("গোপনীয়তা নীতি")
                .setMessage(policy)
                .setPositiveButton("বুঝেছি", null)
                .show();
    }
}