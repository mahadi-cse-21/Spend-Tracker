package com.diodeit.spendtrack.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.diodeit.spendtrack.MainActivity;
import com.diodeit.spendtrack.R;
import com.diodeit.spendtrack.adapters.ExpenseAdapter;
import com.diodeit.spendtrack.databases.DatabaseHelper;
import com.diodeit.spendtrack.models.Expense;
import com.diodeit.spendtrack.utils.BengaliNumberConverter;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private TextView tvTotalExpense, tvTodayExpense, tvRemainingBudget, tvBudgetUsed;
    private TextView tvTodayCount, tvSpentLabel, tvBudgetGoal, tvDailyAverage, tvSpendingPace;
    private LinearProgressIndicator budgetProgress;
    private RecyclerView rvTodayExpenses, rvRecentTransactions;

    private ExpenseAdapter todayAdapter, recentAdapter;
    private DatabaseHelper dbHelper;
    private BengaliNumberConverter bnConverter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        dbHelper = new DatabaseHelper(requireContext());
        bnConverter = new BengaliNumberConverter();

        initViews(view);
        setupRecyclerViews();
        setupClickListeners(view);
        loadData();

        return view;
    }

    private void initViews(View view) {
        tvTotalExpense       = view.findViewById(R.id.tv_total_expense);
        tvTodayExpense       = view.findViewById(R.id.tv_today_expense);
        tvRemainingBudget    = view.findViewById(R.id.tv_remaining_budget);
        tvBudgetUsed         = view.findViewById(R.id.tv_budget_used);
        tvSpentLabel         = view.findViewById(R.id.tv_spent_label);
        tvTodayCount         = view.findViewById(R.id.tv_today_count);
        tvBudgetGoal         = view.findViewById(R.id.tv_budget_goal);
        tvDailyAverage       = view.findViewById(R.id.tv_daily_average);
        tvSpendingPace       = view.findViewById(R.id.tv_spending_pace);
        budgetProgress       = view.findViewById(R.id.budget_progress);
        rvTodayExpenses      = view.findViewById(R.id.rv_today_expenses);
        rvRecentTransactions = view.findViewById(R.id.rv_recent_transactions);
    }

    private void setupRecyclerViews() {
        todayAdapter = new ExpenseAdapter(requireContext(), new ArrayList<>(), expense -> {
            ((MainActivity) requireActivity()).openDetailFragment(
                    TransactionDetailFragment.newInstance(expense.getId()),
                    "TRANSACTION_DETAIL");
        });
        recentAdapter = new ExpenseAdapter(requireContext(), new ArrayList<>(), expense -> {
            ((MainActivity) requireActivity()).openDetailFragment(
                    TransactionDetailFragment.newInstance(expense.getId()),
                    "TRANSACTION_DETAIL");
        });
        rvTodayExpenses.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvTodayExpenses.setAdapter(todayAdapter);
        rvRecentTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvRecentTransactions.setAdapter(recentAdapter);
    }

    private void setupClickListeners(View view) {

        view.findViewById(R.id.btn_add_expense).setOnClickListener(v ->
                ((MainActivity) requireActivity()).navigateToTab("ADD"));

        view.findViewById(R.id.btn_add_income).setOnClickListener(v ->
                ((MainActivity) requireActivity()).navigateToTab("ADD"));

        view.findViewById(R.id.btn_view_analytics).setOnClickListener(v ->
                ((MainActivity) requireActivity()).navigateToTab("ANALYTICS"));

        view.findViewById(R.id.btn_view_history).setOnClickListener(v ->
                ((MainActivity) requireActivity()).navigateToTab("HISTORY"));

        view.findViewById(R.id.btn_open_loans).setOnClickListener(v ->
                ((MainActivity) requireActivity()).openDetailFragment(
                        new LoansFragment(), "LOANS"));

        view.findViewById(R.id.btn_view_all).setOnClickListener(v ->
                ((MainActivity) requireActivity()).navigateToTab("HISTORY"));

        view.findViewById(R.id.btn_add_expense_bottom).setOnClickListener(v ->
                ((MainActivity) requireActivity()).navigateToTab("ADD"));
    }

    private void loadData() {
        if (!isAdded()) return;

        Calendar cal = Calendar.getInstance();
        int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);

        // ★ All-time totals (since first use)
        double income   = dbHelper.getTotalIncomeAllTime();
        double expense  = dbHelper.getTotalExpenseAllTime();
        double balance  = income - expense;
        double dailyAvg = expense / Math.max(1, dayOfMonth);

        // Balance (big number)
        tvRemainingBudget.setText(bnConverter.toBengali(
                String.format(Locale.US, "%,.0f", balance)));

        // Income | Expense labels
        tvSpentLabel.setText("আয়: ৳" + bnConverter.toBengali(
                String.format(Locale.US, "%,.0f", income)));
        tvBudgetGoal.setText("ব্যয়: ৳" + bnConverter.toBengali(
                String.format(Locale.US, "%,.0f", expense)));

        // Total expense metric (right)
        tvTotalExpense.setText(bnConverter.toBengali(
                String.format(Locale.US, "%,.0f", expense)));

        // Daily average
        tvDailyAverage.setText(bnConverter.formatCurrency(dailyAvg));

        // Progress = expense / income (all-time)
        int progress = income > 0 ? (int) ((expense / income) * 100) : 0;
        budgetProgress.setProgress(Math.min(progress, 100));
        tvBudgetUsed.setText(bnConverter.toBengali(progress) + "% ব্যয় হয়েছে");

        // Pace indicator
        if (income > 0 && expense > income * 0.8) {
            tvSpendingPace.setText("সতর্কতা");
            tvSpendingPace.setBackgroundResource(R.drawable.bg_pill_error);
        } else {
            tvSpendingPace.setText("স্বাভাবিক");
            tvSpendingPace.setBackgroundResource(R.drawable.bg_pill_primary_fixed);
        }

        // Today's list
        List<Expense> todayExpenses = dbHelper.getTodayExpenses();
        todayAdapter.updateData(todayExpenses);

        double todayExpense = 0, todayIncome = 0;
        for (Expense e : todayExpenses) {
            if (e.isIncome()) todayIncome += e.getAmount();
            else              todayExpense += e.getAmount();
        }

        if (todayIncome > 0 && todayExpense > 0) {
            tvTodayExpense.setText(
                    "+" + bnConverter.formatCurrency(todayIncome)
                            + " | -" + bnConverter.formatCurrency(todayExpense));
        } else if (todayIncome > 0) {
            tvTodayExpense.setText("+" + bnConverter.formatCurrency(todayIncome));
        } else {
            tvTodayExpense.setText(bnConverter.formatCurrency(todayExpense));
        }
        tvTodayCount.setText(bnConverter.toBengali(todayExpenses.size()) + "টি এন্ট্রি");

        // Recent
        List<Expense> recentExpenses = dbHelper.getRecentExpenses(10);
        recentAdapter.updateData(recentExpenses);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }
}