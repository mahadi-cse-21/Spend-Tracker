package com.diodeit.spendtrack.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import com.diodeit.spendtrack.MainActivity;
import com.diodeit.spendtrack.R;
import com.diodeit.spendtrack.adapters.TransactionGroupAdapter;
import com.diodeit.spendtrack.databases.DatabaseHelper;
import com.diodeit.spendtrack.models.Expense;
import com.diodeit.spendtrack.utils.BengaliNumberConverter;
import com.diodeit.spendtrack.utils.ExportHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class HistoryFragment extends Fragment {

    private TextView tvCurrentMonth, tvTotalIncome, tvTotalExpense, tvTotalTransactions;
    private ImageView btnPrevMonth, btnNextMonth;
    private EditText etSearch;
    private ChipGroup cgFilters;
    private LinearLayout llEmptyState;
    private RecyclerView rvTransactions;

    private TransactionGroupAdapter adapter;

    private DatabaseHelper dbHelper;
    private BengaliNumberConverter bnConverter;

    private Calendar currentCal;
    private SimpleDateFormat monthFormat;
    private List<Expense> allExpenses = new ArrayList<>();
    private List<Expense> filteredExpenses = new ArrayList<>();
    private String selectedFilter;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    // ★ Dirty check variables
    private int lastLoadedVersion = -1;
    private int lastLoadedYear = -1;
    private int lastLoadedMonth = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        dbHelper = DatabaseHelper.get(requireContext());
        bnConverter = new BengaliNumberConverter();
        currentCal = Calendar.getInstance();
        monthFormat = new SimpleDateFormat("MMMM yyyy", new Locale("bn", "BD"));
        selectedFilter = getString(R.string.all_categories);

        initViews(view);
        setupRecyclerView();
        setupFilterChips();
        setupSearch();
        loadTransactions();
        setupClickListeners(view);

        return view;
    }

    private void initViews(View view) {
        tvCurrentMonth      = view.findViewById(R.id.tv_current_month);
        tvTotalIncome       = view.findViewById(R.id.tv_total_income);
        tvTotalExpense      = view.findViewById(R.id.tv_total_expense);
        tvTotalTransactions = view.findViewById(R.id.tv_total_transactions);
        btnPrevMonth        = view.findViewById(R.id.btn_prev_month);
        btnNextMonth        = view.findViewById(R.id.btn_next_month);
        etSearch            = view.findViewById(R.id.et_search);
        cgFilters           = view.findViewById(R.id.cg_filters);
        llEmptyState        = view.findViewById(R.id.ll_empty_state);
        rvTransactions      = view.findViewById(R.id.rv_transactions);
    }

    private void setupRecyclerView() {
        adapter = new TransactionGroupAdapter(requireContext(), expense ->
                ((MainActivity) requireActivity()).openDetailFragment(
                        TransactionDetailFragment.newInstance(expense.getId()),
                        "TRANSACTION_DETAIL"));
        rvTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvTransactions.setAdapter(adapter);
    }

    private void setupFilterChips() {
        String[] filters = {
                getString(R.string.all_categories),
                "খরচ", "আয়", "ঋণ",
                getString(R.string.pay_cash_short)
        };
        for (String filter : filters) {
            Chip chip = new Chip(requireContext());
            chip.setText(filter);
            chip.setCheckable(true);
            chip.setClickable(true);
            chip.setChecked(filter.equals(selectedFilter));
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedFilter = filter;
                    applyFilters();
                }
            });
            cgFilters.addView(chip);
        }
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) mainHandler.removeCallbacks(searchRunnable);
                searchRunnable = HistoryFragment.this::applyFilters;
                mainHandler.postDelayed(searchRunnable, 250); // Debounce
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void loadTransactions() {
        final int year  = currentCal.get(Calendar.YEAR);
        final int month = currentCal.get(Calendar.MONTH);

        // ★ Dirty check - do not load if nothing changed and same month
        int v = dbHelper.getDataVersion();
        if (v == lastLoadedVersion && year == lastLoadedYear && month == lastLoadedMonth) {
            return;
        }
        lastLoadedVersion = v;
        lastLoadedYear = year;
        lastLoadedMonth = month;

        tvCurrentMonth.setText(bnConverter.toBengali(monthFormat.format(currentCal.getTime())));

        // ★ Run DB query in background
        MainActivity.io().execute(() -> {
            // Uses the monthCache inside DatabaseHelper
            List<Expense> list = dbHelper.getMonthExpenses(year, month);
            mainHandler.post(() -> {
                if (!isAdded()) return;
                allExpenses = list;
                applyFilters();
            });
        });
    }

    private void applyFilters() {
        if (getView() == null) return;

        final String query = etSearch.getText().toString().toLowerCase();
        final String filter = selectedFilter;
        final List<Expense> source = allExpenses;
        final String allCategories = getString(R.string.all_categories);

        // ★ Run filtering in background
        MainActivity.io().execute(() -> {
            List<Expense> filtered = new ArrayList<>();
            double income = 0, expense = 0;

            for (Expense e : source) {
                String cat = e.getCategory() != null ? e.getCategory() : "";
                String summary = e.getItemsSummary() != null ? e.getItemsSummary() : "";
                String note = e.getNote() != null ? e.getNote() : "";
                String pay = e.getPaymentMethod() != null ? e.getPaymentMethod() : "";

                boolean matchesQuery =
                        cat.toLowerCase().contains(query) ||
                                summary.toLowerCase().contains(query) ||
                                note.toLowerCase().contains(query);

                boolean matchesFilter;
                if (filter.equals(allCategories)) {
                    matchesFilter = true;
                } else if (filter.equals("খরচ")) {
                    matchesFilter = !e.isIncome();
                } else if (filter.equals("আয়")) {
                    matchesFilter = e.isIncome();
                } else if (filter.equals("ঋণ")) {
                    matchesFilter = cat.equals("ঋণ") || cat.equals("ঋণ পরিশোধ");
                } else {
                    matchesFilter = cat.contains(filter) || pay.contains(filter);
                }

                if (matchesQuery && matchesFilter) {
                    filtered.add(e);
                    if (e.isIncome()) income += e.getAmount();
                    else              expense += e.getAmount();
                }
            }

            final List<Expense> result = filtered;
            final double fIncome = income, fExpense = expense;

            mainHandler.post(() -> {
                if (!isAdded() || getView() == null) return;
                filteredExpenses = result;

                // DiffUtil in adapter handles this extremely fast
                adapter.setExpenses(result);

                if (tvTotalIncome != null) tvTotalIncome.setText(bnConverter.formatCurrency(fIncome));
                if (tvTotalExpense != null) tvTotalExpense.setText(bnConverter.formatCurrency(fExpense));
                if (tvTotalTransactions != null) tvTotalTransactions.setText(
                        getString(R.string.entries, bnConverter.toBengali(result.size())));

                toggleEmptyState(result.isEmpty());
            });
        });
    }

    private void toggleEmptyState(boolean show) {
        llEmptyState.setVisibility(show ? View.VISIBLE : View.GONE);
        rvTransactions.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void setupClickListeners(View view) {
        btnPrevMonth.setOnClickListener(v -> {
            currentCal.add(Calendar.MONTH, -1);
            lastLoadedVersion = -1; // Force reload
            loadTransactions();
        });
        btnNextMonth.setOnClickListener(v -> {
            currentCal.add(Calendar.MONTH, 1);
            lastLoadedVersion = -1; // Force reload
            loadTransactions();
        });
        view.findViewById(R.id.fab_export).setOnClickListener(v -> showExportSheet());
    }

    private void showExportSheet() {
        View sheetView = getLayoutInflater().inflate(R.layout.dialog_export, null);
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        dialog.setContentView(sheetView);

        sheetView.findViewById(R.id.btn_close_export).setOnClickListener(v -> dialog.dismiss());

        sheetView.findViewById(R.id.btn_export_pdf).setOnClickListener(v -> {
            if (filteredExpenses == null || filteredExpenses.isEmpty()) {
                Toast.makeText(requireContext(), "কোনো লেনদেন নেই", Toast.LENGTH_SHORT).show();
                return;
            }
            double income = 0, expense = 0;
            for (Expense e : filteredExpenses) {
                if (e.isIncome()) income += e.getAmount();
                else              expense += e.getAmount();
            }
            double balance = income - expense;
            String label = bnConverter.toBengali(monthFormat.format(currentCal.getTime()));
            boolean ok = ExportHelper.exportPdf(requireContext(), filteredExpenses,
                    income, expense, balance, label);
            Toast.makeText(requireContext(),
                    ok ? "পিডিএফ ডাউনলোড সম্পন্ন — Downloads/SpendTrack" : "পিডিএফ তৈরি ব্যর্থ",
                    Toast.LENGTH_LONG).show();
            if (ok) dialog.dismiss();
        });

        sheetView.findViewById(R.id.btn_export_csv).setOnClickListener(v -> {
            if (filteredExpenses == null || filteredExpenses.isEmpty()) {
                Toast.makeText(requireContext(), "কোনো লেনদেন নেই", Toast.LENGTH_SHORT).show();
                return;
            }
            boolean ok = ExportHelper.exportCsv(requireContext(), filteredExpenses);
            Toast.makeText(requireContext(),
                    ok ? "ব্যাকআপ সম্পন্ন — Downloads/SpendTrack" : "ব্যাকআপ তৈরি ব্যর্থ",
                    Toast.LENGTH_LONG).show();
            if (ok) dialog.dismiss();
        });

        dialog.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTransactions();
    }
}