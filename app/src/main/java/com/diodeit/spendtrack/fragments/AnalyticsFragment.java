package com.diodeit.spendtrack.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.tabs.TabLayout;

import com.diodeit.spendtrack.R;
import com.diodeit.spendtrack.databases.DatabaseHelper;
import com.diodeit.spendtrack.models.CategoryBudget;
import com.diodeit.spendtrack.models.Expense;
import com.diodeit.spendtrack.utils.BengaliNumberConverter;
import com.diodeit.spendtrack.utils.DateUtils;
import com.diodeit.spendtrack.utils.ExportHelper;
import com.diodeit.spendtrack.views.BarChartView;
import com.diodeit.spendtrack.views.DonutChartView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AnalyticsFragment extends Fragment {

    private TextView tvMonthlyTotal, tvRemainingFund, tvDaysLeft, tvUsedPercent;
    private TextView tvHighestPercent, tvHighestCategory, tvAvgDaily, tvMaxExpense, tvChartMonth;
    private TextView tvBudgetAmountLabel, tvTotalBudgetLabel, tvCatCount;

    private LinearProgressIndicator progressOverall;
    private TabLayout tabPeriod;
    private DonutChartView donutChart;
    private BarChartView barChart;
    private LinearLayout llLegend, llBudgetHealth;
    private MaterialButton btnPdfExport, btnCsvExport;

    private BengaliNumberConverter bnConverter;
    private DatabaseHelper dbHelper;

    private final int[] chartColors = {
            0xFF00513C, 0xFF006877, 0xFFBA1A1A,
            0xFF406658, 0xFFBEC9C2, 0xFF7D5260,
            0xFF6750A4, 0xFF964B00
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_analytics, container, false);

        bnConverter = new BengaliNumberConverter();
        dbHelper = new DatabaseHelper(requireContext());

        initViews(view);
        setupClickListeners();
        loadData();

        return view;
    }

    private void initViews(View view) {
        tvMonthlyTotal      = view.findViewById(R.id.tv_monthly_total);
        tvRemainingFund     = view.findViewById(R.id.tv_remaining_fund);
        tvDaysLeft          = view.findViewById(R.id.tv_days_left);
        tvUsedPercent       = view.findViewById(R.id.tv_used_percent);
        tvHighestPercent    = view.findViewById(R.id.tv_highest_percent);
        tvHighestCategory   = view.findViewById(R.id.tv_highest_category);
        tvAvgDaily          = view.findViewById(R.id.tv_avg_daily);
        tvMaxExpense        = view.findViewById(R.id.tv_max_expense);
        tvChartMonth        = view.findViewById(R.id.tv_chart_month);
        tvBudgetAmountLabel = view.findViewById(R.id.tv_budget_amount_label);
        tvTotalBudgetLabel  = view.findViewById(R.id.tv_total_budget_label);
        tvCatCount          = view.findViewById(R.id.tv_category_count);

        progressOverall = view.findViewById(R.id.progress_overall);
        tabPeriod       = view.findViewById(R.id.tab_period);
        donutChart      = view.findViewById(R.id.donut_chart);
        barChart        = view.findViewById(R.id.bar_chart);
        llLegend        = view.findViewById(R.id.ll_legend);
        llBudgetHealth  = view.findViewById(R.id.ll_budget_health);
        btnPdfExport    = view.findViewById(R.id.btn_pdf_export);
        btnCsvExport    = view.findViewById(R.id.btn_csv_export);
    }

    private void loadData() {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);

        double income  = dbHelper.getCurrentMonthTotalIncome();
        double expense = dbHelper.getCurrentMonthTotalExpense();
        double balance = income - expense;

        String incomeStr  = bnConverter.toBengali(String.format(Locale.US, "%,.0f", income));
        String expenseStr = bnConverter.toBengali(String.format(Locale.US, "%,.0f", expense));
        String balanceStr = bnConverter.toBengali(String.format(Locale.US, "%,.0f", balance));

        tvMonthlyTotal.setText("৳ " + balanceStr);
        tvRemainingFund.setText("আয়: ৳" + incomeStr);
        tvBudgetAmountLabel.setText("আয়: ৳" + incomeStr);
        tvTotalBudgetLabel.setText("/ ব্যয় ৳" + expenseStr);

        int progress = income > 0 ? (int) ((expense / income) * 100) : 0;
        progressOverall.setProgress(Math.min(progress, 100));
        tvUsedPercent.setText(bnConverter.toBengali(progress) + "% ব্যয়");

        int daysLeft = DateUtils.getDaysRemainingInMonth();
        tvDaysLeft.setText("আর " + bnConverter.toBengali(daysLeft) + " দিন বাকি");

        String monthYear = DateUtils.getBengaliMonth(month) + " " + bnConverter.toBengali(year);
        tvChartMonth.setText(monthYear);

        loadCategoryBreakdown(year, month);
        loadWeeklyTrend(year, month);
        loadBudgetHealth();

        tabPeriod.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {}
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void loadCategoryBreakdown(int year, int month) {
        List<Expense> expenses = dbHelper.getMonthExpenses(year, month);
        if (expenses == null) expenses = new ArrayList<>();

        Map<String, Double> catMap = new HashMap<>();
        double total = 0;

        for (Expense e : expenses) {
            if (e == null) continue;
            if (e.isIncome()) continue;

            String cat = e.getCategory();
            if (cat == null || cat.trim().isEmpty()) cat = "Other";

            catMap.put(cat, catMap.getOrDefault(cat, 0.0) + e.getAmount());
            total += e.getAmount();
        }

        llLegend.removeAllViews();

        if (total <= 0) {
            donutChart.setData(new float[]{}, chartColors);
            tvHighestPercent.setText("০%");
            tvHighestCategory.setText("-");
            if (tvCatCount != null) tvCatCount.setText("০টি খাত");
            return;
        }

        List<Map.Entry<String, Double>> sortedCats = new ArrayList<>(catMap.entrySet());
        Collections.sort(sortedCats, (e1, e2) -> e2.getValue().compareTo(e1.getValue()));

        float[] values = new float[sortedCats.size()];
        String highestCat = "";
        double maxAmt = 0;

        LayoutInflater inflater = LayoutInflater.from(requireContext());

        for (int i = 0; i < sortedCats.size(); i++) {
            Map.Entry<String, Double> entry = sortedCats.get(i);
            values[i] = entry.getValue().floatValue();

            if (entry.getValue() > maxAmt) {
                maxAmt = entry.getValue();
                highestCat = entry.getKey();
            }

            View legendItem = inflater.inflate(R.layout.item_legend, llLegend, false);
            View colorDot = legendItem.findViewById(R.id.v_color_dot);
            TextView tvName = legendItem.findViewById(R.id.tv_cat_name);
            TextView tvAmountText = legendItem.findViewById(R.id.tv_cat_amount);
            TextView tvPercent = legendItem.findViewById(R.id.tv_cat_percent);

            int color = chartColors[i % chartColors.length];
            if (colorDot != null) colorDot.setBackgroundColor(color);
            if (tvName != null) tvName.setText(entry.getKey());
            if (tvAmountText != null) {
                tvAmountText.setText(bnConverter.formatCurrency(entry.getValue()));
            }
            int percent = (int) ((entry.getValue() / total) * 100);
            if (tvPercent != null) {
                tvPercent.setText(bnConverter.toBengali(percent) + "%");
            }
            llLegend.addView(legendItem);
        }

        donutChart.setData(values, chartColors);

        int highestPercent = (int) ((maxAmt / total) * 100);
        tvHighestPercent.setText(bnConverter.toBengali(highestPercent) + "%");
        tvHighestCategory.setText(highestCat);

        if (tvCatCount != null) {
            tvCatCount.setText(bnConverter.toBengali(catMap.size()) + "টি খাত");
        }
    }

    private void loadWeeklyTrend(int year, int month) {
        List<Expense> expenses = dbHelper.getMonthExpenses(year, month);
        if (expenses == null) expenses = new ArrayList<>();

        float[] weeklyValues = new float[4];
        double total = 0;
        double maxWeekly = 0;
        int maxWeekIdx = 0;

        Calendar cal = Calendar.getInstance();

        for (Expense e : expenses) {
            if (e == null) continue;
            if (e.isIncome()) continue;

            cal.setTimeInMillis(e.getDate());
            int week = (cal.get(Calendar.DAY_OF_MONTH) - 1) / 7;
            if (week >= 0 && week < 4) weeklyValues[week] += (float) e.getAmount();
            total += e.getAmount();
        }

        for (int i = 0; i < 4; i++) {
            if (weeklyValues[i] > maxWeekly) {
                maxWeekly = weeklyValues[i];
                maxWeekIdx = i;
            }
        }

        barChart.setData(weeklyValues);

        int currentDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
        double avg = currentDay > 0 ? total / currentDay : 0;

        tvAvgDaily.setText("গড় দৈনিক: ৳" + bnConverter.toBengali(
                String.format(Locale.US, "%,.0f", avg)));

        String[] weekNames = { "১ম সপ্তাহ", "২য় সপ্তাহ", "৩য় সপ্তাহ", "৪র্থ সপ্তাহ" };
        tvMaxExpense.setText("সর্বোচ্চ: " + weekNames[maxWeekIdx]);
    }

    private void loadBudgetHealth() {
        llBudgetHealth.removeAllViews();

        List<CategoryBudget> budgets = dbHelper.getCategoryBudgets(DateUtils.getMonthKey());
        if (budgets == null) budgets = new ArrayList<>();

        LayoutInflater inflater = LayoutInflater.from(requireContext());

        for (CategoryBudget cb : budgets) {
            if (cb == null) continue;

            View itemView = inflater.inflate(R.layout.item_budget_health, llBudgetHealth, false);

            TextView tvName = itemView.findViewById(R.id.tv_category_name);
            TextView tvStatus = itemView.findViewById(R.id.tv_status);
            TextView tvSpentVsBudget = itemView.findViewById(R.id.tv_spent_vs_budget);
            TextView tvRemaining = itemView.findViewById(R.id.tv_remaining);
            LinearProgressIndicator progressBar = itemView.findViewById(R.id.progress_bar);

            if (tvName == null || tvStatus == null || tvSpentVsBudget == null
                    || tvRemaining == null || progressBar == null) continue;

            tvName.setText(cb.getCategoryName());
            int percent = cb.getPercentSpent();
            progressBar.setProgress(Math.max(0, Math.min(percent, 100)));

            if (percent >= 90) {
                tvStatus.setText("সীমা ছাড়িয়েছে");
                tvStatus.setTextColor(0xFFBA1A1A);
                progressBar.setIndicatorColor(0xFFBA1A1A);
            } else if (percent >= 75) {
                tvStatus.setText("সীমার কাছাকাছি");
                tvStatus.setTextColor(0xFF964B00);
                progressBar.setIndicatorColor(0xFF964B00);
            } else {
                tvStatus.setText("নিয়ন্ত্রণে");
                tvStatus.setTextColor(0xFF00513C);
                progressBar.setIndicatorColor(0xFF00513C);
            }

            String spentStr = bnConverter.toBengali(
                    String.format(Locale.US, "%,.0f", cb.getSpentAmount()));
            String budgetStr = bnConverter.toBengali(
                    String.format(Locale.US, "%,.0f", cb.getBudgetAmount()));

            tvSpentVsBudget.setText("খরচ: ৳" + spentStr + " / ৳" + budgetStr);
            tvRemaining.setText("অবশিষ্ট: ৳" + bnConverter.toBengali(
                    String.format(Locale.US, "%,.0f", cb.getRemaining())));

            llBudgetHealth.addView(itemView);
        }
    }

    private void setupClickListeners() {
        btnPdfExport.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            int year = cal.get(Calendar.YEAR);
            int month = cal.get(Calendar.MONTH);

            List<Expense> monthExpenses = dbHelper.getMonthExpenses(year, month);
            double income  = dbHelper.getCurrentMonthTotalIncome();
            double expense = dbHelper.getCurrentMonthTotalExpense();
            double balance = income - expense;
            String monthLabel = DateUtils.getBengaliMonth(month) + " " + bnConverter.toBengali(year);

            boolean ok = ExportHelper.exportPdf(
                    requireContext(),
                    monthExpenses != null ? monthExpenses : new ArrayList<>(),
                    income, expense, balance, monthLabel);

            Toast.makeText(requireContext(),
                    ok ? "পিডিএফ ডাউনলোড সম্পন্ন — Downloads/SpendTrack"
                            : "পিডিএফ তৈরি ব্যর্থ",
                    Toast.LENGTH_LONG).show();
        });

        btnCsvExport.setOnClickListener(v -> {
            List<Expense> all = dbHelper.getAllExpenses();
            boolean ok = ExportHelper.exportCsv(requireContext(), all);
            Toast.makeText(requireContext(),
                    ok ? "ব্যাকআপ সম্পন্ন — Downloads/SpendTrack"
                            : "ব্যাকআপ তৈরি ব্যর্থ",
                    Toast.LENGTH_LONG).show();
        });
    }
}