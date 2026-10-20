package com.diodeit.spendtrack.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.diodeit.spendtrack.MainActivity;
import com.diodeit.spendtrack.R;
import com.diodeit.spendtrack.adapters.ExpenseAdapter;
import com.diodeit.spendtrack.adapters.TransactionGroupAdapter;
import com.diodeit.spendtrack.databases.DatabaseHelper;
import com.diodeit.spendtrack.models.Expense;
import com.diodeit.spendtrack.utils.BengaliNumberConverter;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private TextView tvTotalIncome, tvTotalExpense, tvTotalEntries;
    private TextView tvTodayExpense, tvTodayCount;
    private TextView tvSpentLabel, tvBudgetGoal, tvDailyAverage, tvSpendingPace;
    private TextView tvRemainingBudget, tvBudgetUsed;
    private LinearProgressIndicator budgetProgress;
    private RecyclerView rvTodayExpenses, rvRecentTransactions;

    private ExpenseAdapter todayAdapter;
    private TransactionGroupAdapter recentAdapter;

    private DatabaseHelper dbHelper;
    private BengaliNumberConverter bnConverter;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private int lastLoadedVersion = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        dbHelper = DatabaseHelper.get(requireContext());
        bnConverter = new BengaliNumberConverter();

        initViews(view);
        setupRecyclerViews();
        setupClickListeners(view);
        loadData();

        return view;
    }

    private void initViews(View view) {
        tvTotalIncome        = view.findViewById(R.id.tv_total_income);
        tvTotalExpense       = view.findViewById(R.id.tv_total_expense);
        tvTotalEntries       = view.findViewById(R.id.tv_total_transactions);
        tvTodayExpense       = view.findViewById(R.id.tv_today_expense);
        tvTodayCount         = view.findViewById(R.id.tv_today_count);
        tvSpentLabel         = view.findViewById(R.id.tv_spent_label);
        tvBudgetGoal         = view.findViewById(R.id.tv_budget_goal);
        tvDailyAverage       = view.findViewById(R.id.tv_daily_average);
        tvSpendingPace       = view.findViewById(R.id.tv_spending_pace);
        tvRemainingBudget    = view.findViewById(R.id.tv_remaining_budget);
        tvBudgetUsed         = view.findViewById(R.id.tv_budget_used);
        budgetProgress       = view.findViewById(R.id.budget_progress);
        rvTodayExpenses      = view.findViewById(R.id.rv_today_expenses);
        rvRecentTransactions = view.findViewById(R.id.rv_recent_transactions);
    }

    private void setupRecyclerViews() {
        if (rvTodayExpenses != null) {
            todayAdapter = new ExpenseAdapter(requireContext(), new ArrayList<>(), expense ->
                    ((MainActivity) requireActivity()).openDetailFragment(
                            TransactionDetailFragment.newInstance(expense.getId()),
                            "TRANSACTION_DETAIL"));
            rvTodayExpenses.setLayoutManager(new LinearLayoutManager(requireContext()));
            rvTodayExpenses.setAdapter(todayAdapter);
        }
        if (rvRecentTransactions != null) {
            recentAdapter = new TransactionGroupAdapter(requireContext(), expense ->
                    ((MainActivity) requireActivity()).openDetailFragment(
                            TransactionDetailFragment.newInstance(expense.getId()),
                            "TRANSACTION_DETAIL"));
            rvRecentTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
            rvRecentTransactions.setAdapter(recentAdapter);
        }
    }

    private void setupClickListeners(View view) {
        View b;
        if ((b = view.findViewById(R.id.btn_add_expense)) != null)
            b.setOnClickListener(v -> ((MainActivity) requireActivity()).navigateToTab("ADD"));
        if ((b = view.findViewById(R.id.btn_add_income)) != null)
            b.setOnClickListener(v -> ((MainActivity) requireActivity()).navigateToTab("ADD"));
        if ((b = view.findViewById(R.id.btn_view_analytics)) != null)
            b.setOnClickListener(v -> ((MainActivity) requireActivity()).navigateToTab("ANALYTICS"));
        if ((b = view.findViewById(R.id.btn_view_history)) != null)
            b.setOnClickListener(v -> ((MainActivity) requireActivity()).navigateToTab("HISTORY"));
        if ((b = view.findViewById(R.id.btn_open_loans)) != null)
            b.setOnClickListener(v -> ((MainActivity) requireActivity())
                    .openDetailFragment(new LoansFragment(), "LOANS"));
        if ((b = view.findViewById(R.id.btn_view_all)) != null)
            b.setOnClickListener(v -> ((MainActivity) requireActivity()).navigateToTab("HISTORY"));
        if ((b = view.findViewById(R.id.btn_add_expense_bottom)) != null)
            b.setOnClickListener(v -> ((MainActivity) requireActivity()).navigateToTab("ADD"));
    }

    private void loadData() {
        if (!isAdded()) return;

        // ★ Dirty check — skip DB work if nothing changed since last load
        int v = dbHelper.getDataVersion();
        if (v == lastLoadedVersion) return;
        lastLoadedVersion = v;

        final Calendar cal = Calendar.getInstance();
        final int year = cal.get(Calendar.YEAR);
        final int month = cal.get(Calendar.MONTH);
        final int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);

        MainActivity.io().execute(() -> {
            double income   = dbHelper.getTotalIncomeAllTime();
            double expense  = dbHelper.getTotalExpenseAllTime();
            int    count    = dbHelper.getExpenseCount();
            double monthIncome  = dbHelper.getMonthIncome(year, month);
            double monthExpense = dbHelper.getMonthExpense(year, month);
            List<Expense> todayList = dbHelper.getTodayExpenses();
            List<Expense> recentList = dbHelper.getRecentExpenses(30);

            mainHandler.post(() -> {
                if (!isAdded() || getView() == null) return;

                double balance  = income - expense;
                double dailyAvg = expense / Math.max(1, dayOfMonth);

                if (tvTotalIncome != null) tvTotalIncome.setText(bnConverter.formatCurrency(income));
                if (tvTotalExpense != null) tvTotalExpense.setText(bnConverter.formatCurrency(expense));
                if (tvTotalEntries != null) tvTotalEntries.setText(bnConverter.toBengali(count) + "টি এন্ট্রি");

                if (tvRemainingBudget != null) tvRemainingBudget.setText(bnConverter.toBengali(
                        String.format(Locale.US, "%,.0f", Math.max(balance, 0))));
                if (tvSpentLabel != null) tvSpentLabel.setText("আয়: ৳" + bnConverter.toBengali(
                        String.format(Locale.US, "%,.0f", income)));
                if (tvBudgetGoal != null) tvBudgetGoal.setText("ব্যয়: ৳" + bnConverter.toBengali(
                        String.format(Locale.US, "%,.0f", expense)));
                if (tvDailyAverage != null) tvDailyAverage.setText(bnConverter.formatCurrency(dailyAvg));

                int progress = income > 0
                        ? (int) Math.min((expense / income) * 100, 100) : 0;
                if (budgetProgress != null) budgetProgress.setProgress(progress);
                if (tvBudgetUsed != null) tvBudgetUsed.setText(
                        bnConverter.toBengali(progress) + "% ব্যয় হয়েছে");

                if (tvSpendingPace != null) {
                    if (monthIncome > 0 && monthExpense > monthIncome * 0.8) {
                        tvSpendingPace.setText("সতর্কতা");
                        tvSpendingPace.setBackgroundResource(R.drawable.bg_pill_error);
                        tvSpendingPace.setTextColor(ContextCompat.getColor(
                                requireContext(), R.color.on_error));
                    } else {
                        tvSpendingPace.setText("স্বাভাবিক");
                        tvSpendingPace.setBackgroundResource(R.drawable.bg_pill_primary_fixed);
                        tvSpendingPace.setTextColor(ContextCompat.getColor(
                                requireContext(), R.color.on_primary_fixed));
                    }
                }

                if (todayAdapter != null) todayAdapter.updateData(todayList);

                double todayExpense = 0, todayIncome = 0;
                for (Expense e : todayList) {
                    if (e.isIncome()) todayIncome += e.getAmount();
                    else              todayExpense += e.getAmount();
                }
                if (tvTodayExpense != null) {
                    if (todayIncome > 0 && todayExpense > 0) {
                        tvTodayExpense.setText(
                                "+" + bnConverter.formatCurrency(todayIncome)
                                        + " | -" + bnConverter.formatCurrency(todayExpense));
                    } else if (todayIncome > 0) {
                        tvTodayExpense.setText("+" + bnConverter.formatCurrency(todayIncome));
                    } else {
                        tvTodayExpense.setText(bnConverter.formatCurrency(todayExpense));
                    }
                }
                if (tvTodayCount != null) tvTodayCount.setText(
                        bnConverter.toBengali(todayList.size()) + "টি এন্ট্রি");

                if (recentAdapter != null) recentAdapter.setExpenses(recentList);
            });
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }
}