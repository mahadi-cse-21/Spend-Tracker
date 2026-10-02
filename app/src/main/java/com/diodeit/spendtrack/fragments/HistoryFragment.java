package com.diodeit.spendtrack.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import com.diodeit.spendtrack.MainActivity;
import com.diodeit.spendtrack.R;
import com.diodeit.spendtrack.adapters.TransactionHistoryAdapter;
import com.diodeit.spendtrack.databases.DatabaseHelper;
import com.diodeit.spendtrack.models.Expense;
import com.diodeit.spendtrack.utils.BengaliNumberConverter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class HistoryFragment extends Fragment {

    private TextView tvCurrentMonth, tvTotalExpense, tvTotalTransactions;
    private ImageView btnPrevMonth, btnNextMonth;
    private EditText etSearch;
    private ChipGroup cgFilters;
    private LinearLayout llEmptyState;
    private RecyclerView rvTransactions;

    private TransactionHistoryAdapter adapter;
    private DatabaseHelper dbHelper;
    private BengaliNumberConverter bnConverter;

    private Calendar currentCal;
    private SimpleDateFormat monthFormat;
    private List<Expense> allExpenses = new ArrayList<>();
    private String selectedFilter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        dbHelper = new DatabaseHelper(requireContext());
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
        adapter = new TransactionHistoryAdapter(requireContext(), new ArrayList<>(), expense -> {
            ((MainActivity) requireActivity()).openDetailFragment(
                    TransactionDetailFragment.newInstance(expense.getId()),
                    "TRANSACTION_DETAIL");
        });
        rvTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvTransactions.setAdapter(adapter);
    }

    private void setupFilterChips() {
        String[] filters = {
                getString(R.string.all_categories),
                "খরচ",
                "আয়",
                getString(R.string.pay_bkash_short),
                getString(R.string.pay_nagad_short),
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
                applyFilters();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void loadTransactions() {
        int year = currentCal.get(Calendar.YEAR);
        int month = currentCal.get(Calendar.MONTH);

        allExpenses = dbHelper.getMonthExpenses(year, month);

        tvCurrentMonth.setText(bnConverter.toBengali(monthFormat.format(currentCal.getTime())));

        applyFilters();
    }

    private void applyFilters() {
        String query = etSearch.getText().toString().toLowerCase();
        List<Expense> filtered = new ArrayList<>();
        double income = 0, expense = 0;

        for (Expense e : allExpenses) {
            String summary = e.getItemsSummary() != null ? e.getItemsSummary() : "";
            String note = e.getNote() != null ? e.getNote() : "";

            boolean matchesQuery =
                    e.getCategory().toLowerCase().contains(query) ||
                            summary.toLowerCase().contains(query) ||
                            note.toLowerCase().contains(query);

            boolean matchesFilter;
            if (selectedFilter.equals(getString(R.string.all_categories))) {
                matchesFilter = true;
            } else if (selectedFilter.equals("খরচ")) {
                matchesFilter = !e.isIncome();
            } else if (selectedFilter.equals("আয়")) {
                matchesFilter = e.isIncome();
            } else {
                matchesFilter = e.getCategory().contains(selectedFilter)
                        || e.getPaymentMethod().contains(selectedFilter);
            }

            if (matchesQuery && matchesFilter) {
                filtered.add(e);
                if (e.isIncome()) income += e.getAmount();
                else              expense += e.getAmount();
            }
        }

        adapter.updateData(filtered);
        tvTotalExpense.setText(
                "আয় " + bnConverter.formatCurrency(income)
                        + "  |  ব্যয় " + bnConverter.formatCurrency(expense));
        tvTotalTransactions.setText(
                getString(R.string.entries, bnConverter.toBengali(filtered.size())));
        toggleEmptyState(filtered.isEmpty());
    }

    private void toggleEmptyState(boolean show) {
        llEmptyState.setVisibility(show ? View.VISIBLE : View.GONE);
        rvTransactions.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void setupClickListeners(View view) {
        btnPrevMonth.setOnClickListener(v -> {
            currentCal.add(Calendar.MONTH, -1);
            loadTransactions();
        });
        btnNextMonth.setOnClickListener(v -> {
            currentCal.add(Calendar.MONTH, 1);
            loadTransactions();
        });
        view.findViewById(R.id.fab_export).setOnClickListener(v -> showExportSheet());
    }

    private void showExportSheet() {
        View sheetView = getLayoutInflater().inflate(R.layout.dialog_export, null);
        com.google.android.material.bottomsheet.BottomSheetDialog dialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(requireContext());
        dialog.setContentView(sheetView);
        sheetView.findViewById(R.id.btn_close_export).setOnClickListener(v -> dialog.dismiss());
        sheetView.findViewById(R.id.btn_export_pdf).setOnClickListener(v -> {
            android.widget.Toast.makeText(requireContext(),
                    R.string.export_success, android.widget.Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        sheetView.findViewById(R.id.btn_export_csv).setOnClickListener(v -> {
            android.widget.Toast.makeText(requireContext(),
                    R.string.export_success, android.widget.Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        dialog.show();
    }
}