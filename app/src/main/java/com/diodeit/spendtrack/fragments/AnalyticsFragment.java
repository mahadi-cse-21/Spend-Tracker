package com.diodeit.spendtrack.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
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

    // ─── Views ────────────────────────────────────────────────
    private TextView tvMonthlyTotal, tvRemainingFund, tvDaysLeft, tvUsedPercent;
    private TextView tvIncomeStat, tvExpenseStat, tvRemainingStat;
    private TextView tvHighestPercent, tvHighestCategory, tvAvgDaily, tvMaxExpense, tvChartMonth;
    private TextView tvBudgetAmountLabel, tvTotalBudgetLabel, tvCatCount;
    private TextView tvSelectedPeriod, tvCurrentBadge;

    private ImageView btnPrevPeriod, btnNextPeriod;

    private LinearProgressIndicator progressOverall;
    private TabLayout tabPeriod;
    private DonutChartView donutChart;
    private BarChartView barChart;
    private LinearLayout llLegend, llBudgetHealth;
    private MaterialButton btnPdfExport, btnCsvExport;

    // ─── Helpers / state ──────────────────────────────────────
    private BengaliNumberConverter bnConverter;
    private DatabaseHelper dbHelper;

    /** 0 = Weekly, 1 = Monthly, 2 = Yearly */
    private int currentPeriod = 1;

    /** Anchor calendar for the currently selected period. */
    private final Calendar selectedCal = Calendar.getInstance();

    private final int[] chartColors = {
            0xFF00513C, 0xFF006877, 0xFFBA1A1A,
            0xFF406658, 0xFFBEC9C2, 0xFF7D5260,
            0xFF6750A4, 0xFF964B00
    };

    // ──────────────────────────────────────────────────────────
    // Lifecycle
    // ──────────────────────────────────────────────────────────
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_analytics, container, false);

        bnConverter = new BengaliNumberConverter();
        dbHelper = new DatabaseHelper(requireContext());

        initViews(view);
        setupPeriodSelector();
        setupTabListener();
        setupClickListeners();

        updatePeriodLabel();
        loadData();

        return view;
    }

    private void initViews(View view) {
        tvMonthlyTotal      = view.findViewById(R.id.tv_monthly_total);
        tvRemainingFund     = view.findViewById(R.id.tv_remaining_fund);
        tvDaysLeft          = view.findViewById(R.id.tv_days_left);
        tvUsedPercent       = view.findViewById(R.id.tv_used_percent);

        tvIncomeStat        = view.findViewById(R.id.tv_income_stat);
        tvExpenseStat       = view.findViewById(R.id.tv_expense_stat);
        tvRemainingStat     = view.findViewById(R.id.tv_remaining_stat);

        tvHighestPercent    = view.findViewById(R.id.tv_highest_percent);
        tvHighestCategory   = view.findViewById(R.id.tv_highest_category);
        tvAvgDaily          = view.findViewById(R.id.tv_avg_daily);
        tvMaxExpense        = view.findViewById(R.id.tv_max_expense);
        tvChartMonth        = view.findViewById(R.id.tv_chart_month);
        tvBudgetAmountLabel = view.findViewById(R.id.tv_budget_amount_label);
        tvTotalBudgetLabel  = view.findViewById(R.id.tv_total_budget_label);
        tvCatCount          = view.findViewById(R.id.tv_category_count);
        tvSelectedPeriod    = view.findViewById(R.id.tv_selected_period);
        tvCurrentBadge      = view.findViewById(R.id.tv_current_badge);

        btnPrevPeriod       = view.findViewById(R.id.btn_prev_period);
        btnNextPeriod       = view.findViewById(R.id.btn_next_period);

        progressOverall = view.findViewById(R.id.progress_overall);
        tabPeriod       = view.findViewById(R.id.tab_period);
        donutChart      = view.findViewById(R.id.donut_chart);
        barChart        = view.findViewById(R.id.bar_chart);
        llLegend        = view.findViewById(R.id.ll_legend);
        llBudgetHealth  = view.findViewById(R.id.ll_budget_health);
        btnPdfExport    = view.findViewById(R.id.btn_pdf_export);
        btnCsvExport    = view.findViewById(R.id.btn_csv_export);
    }

    // ═══════════════════════════════════════════════════════════
    // PERIOD SELECTOR
    // ═══════════════════════════════════════════════════════════
    private void setupPeriodSelector() {
        btnPrevPeriod.setOnClickListener(v -> {
            switch (currentPeriod) {
                case 0: selectedCal.add(Calendar.WEEK_OF_YEAR, -1); break;
                case 2: selectedCal.add(Calendar.YEAR, -1);         break;
                default: selectedCal.add(Calendar.MONTH, -1);       break;
            }
            updatePeriodLabel();
            loadData();
        });

        btnNextPeriod.setOnClickListener(v -> {
            if (isCurrentPeriod()) return;
            switch (currentPeriod) {
                case 0: selectedCal.add(Calendar.WEEK_OF_YEAR, 1); break;
                case 2: selectedCal.add(Calendar.YEAR, 1);         break;
                default: selectedCal.add(Calendar.MONTH, 1);       break;
            }
            updatePeriodLabel();
            loadData();
        });
    }

    private void updatePeriodLabel() {
        String label;
        int year  = selectedCal.get(Calendar.YEAR);
        int month = selectedCal.get(Calendar.MONTH);

        if (currentPeriod == 0) {
            Calendar start = (Calendar) selectedCal.clone();
            start.set(Calendar.DAY_OF_WEEK, start.getFirstDayOfWeek());
            Calendar end = (Calendar) start.clone();
            end.add(Calendar.DAY_OF_YEAR, 6);

            String startDay = bnConverter.toBengali(start.get(Calendar.DAY_OF_MONTH));
            String endDay   = bnConverter.toBengali(end.get(Calendar.DAY_OF_MONTH));
            String endMonth = DateUtils.getBengaliMonth(end.get(Calendar.MONTH));
            label = "সপ্তাহ: " + startDay + " – " + endDay + " " + endMonth;
        } else if (currentPeriod == 2) {
            label = "বছর " + bnConverter.toBengali(year);
        } else {
            label = DateUtils.getBengaliMonth(month) + " " + bnConverter.toBengali(year);
        }
        tvSelectedPeriod.setText(label);

        boolean current = isCurrentPeriod();
        tvCurrentBadge.setVisibility(current ? View.VISIBLE : View.GONE);
        btnNextPeriod.setEnabled(!current);
        btnNextPeriod.setAlpha(current ? 0.4f : 1f);
    }

    private boolean isCurrentPeriod() {
        Calendar now = Calendar.getInstance();
        if (currentPeriod == 0) {
            return selectedCal.get(Calendar.YEAR) == now.get(Calendar.YEAR)
                    && selectedCal.get(Calendar.WEEK_OF_YEAR) == now.get(Calendar.WEEK_OF_YEAR);
        } else if (currentPeriod == 2) {
            return selectedCal.get(Calendar.YEAR) == now.get(Calendar.YEAR);
        } else {
            return selectedCal.get(Calendar.YEAR) == now.get(Calendar.YEAR)
                    && selectedCal.get(Calendar.MONTH) == now.get(Calendar.MONTH);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // TABS
    // ═══════════════════════════════════════════════════════════
    private void setupTabListener() {
        if (tabPeriod == null) return;
        tabPeriod.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentPeriod = tab.getPosition();
                updatePeriodLabel();
                loadData();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
        TabLayout.Tab defaultTab = tabPeriod.getTabAt(1);
        if (defaultTab != null) defaultTab.select();
    }

    // ═══════════════════════════════════════════════════════════
    // LOAD DATA
    // ═══════════════════════════════════════════════════════════
    private void loadData() {
        int year  = selectedCal.get(Calendar.YEAR);
        int month = selectedCal.get(Calendar.MONTH);

        double income, expense;
        List<Expense> periodExpenses;
        String periodLabel;
        String periodShortLabel;
        String daysLeftText;

        switch (currentPeriod) {
            case 0: {
                long[] range = getWeekRange(selectedCal);
                income  = getTypeTotalInRange("income", range[0], range[1]);
                expense = getTypeTotalInRange("expense", range[0], range[1]);
                periodExpenses = dbHelper.getExpensesByRange(range[0], range[1]);
                periodLabel = "চলতি সপ্তাহের হিসাব";
                periodShortLabel = "সাপ্তাহিক";
                daysLeftText = getDaysLeftTextForWeek(selectedCal);
                break;
            }
            case 2: {
                income  = getYearIncome(year);
                expense = getYearExpense(year);
                periodExpenses = getYearExpenses(year);
                periodLabel = bnConverter.toBengali(year) + " সালের হিসাব";
                periodShortLabel = "বাৎসরিক";
                daysLeftText = getDaysLeftTextForYear(selectedCal);
                break;
            }
            default: {
                income  = getMonthIncome(year, month);
                expense = getMonthExpense(year, month);
                periodExpenses = dbHelper.getMonthExpenses(year, month);
                periodLabel = DateUtils.getBengaliMonth(month) + " "
                        + bnConverter.toBengali(year) + " মাসের হিসাব";
                periodShortLabel = "মাসিক";
                daysLeftText = getDaysLeftTextForMonth(selectedCal);
                break;
            }
        }

        double balance = income - expense;

        String incomeStr  = bnConverter.toBengali(String.format(Locale.US, "%,.0f", income));
        String expenseStr = bnConverter.toBengali(String.format(Locale.US, "%,.0f", expense));

        // Hero number: NEVER show a minus sign
        if (income <= 0) {
            tvMonthlyTotal.setText("৳ " + expenseStr);
            tvMonthlyTotal.setTextColor(ContextCompat.getColor(requireContext(), R.color.error));
            tvTotalBudgetLabel.setText("মোট ব্যয়");
        } else {
            double shown = Math.abs(balance);
            String balanceStr = bnConverter.toBengali(String.format(Locale.US, "%,.0f", shown));
            tvMonthlyTotal.setText("৳ " + balanceStr);
            if (balance >= 0) {
                tvMonthlyTotal.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.on_surface));
                tvTotalBudgetLabel.setText("বাকি");
            } else {
                tvMonthlyTotal.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.error));
                tvTotalBudgetLabel.setText("ঘাটতি");
            }
        }

        tvBudgetAmountLabel.setText(periodLabel);

        // Percent badge
        int percent;
        if (income > 0) {
            percent = (int) Math.min((expense / income) * 100, 100);
            tvUsedPercent.setText("ব্যয় " + bnConverter.toBengali(percent) + "%");
        } else if (expense > 0) {
            percent = 100;
            tvUsedPercent.setText("ব্যয় ১০০%");
        } else {
            percent = 0;
            tvUsedPercent.setText("ব্যয় —");
        }

        progressOverall.setProgressCompat(percent, true);

        if (percent >= 80) {
            tvUsedPercent.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.error));
        } else if (percent >= 50) {
            tvUsedPercent.setTextColor(0xFF964B00);
        } else {
            tvUsedPercent.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.on_surface_variant));
        }

        // 3 stat pills
        tvIncomeStat.setText("৳ " + incomeStr);
        tvExpenseStat.setText("৳ " + expenseStr);

        String remainingStr = bnConverter.toBengali(
                String.format(Locale.US, "%,.0f", Math.max(balance, 0)));
        tvRemainingStat.setText("৳ " + remainingStr);

        tvRemainingFund.setText("অবশিষ্ট: ৳" + remainingStr);
        tvDaysLeft.setText(daysLeftText);
        tvChartMonth.setText(periodShortLabel + " রিপোর্ট");

        loadCategoryBreakdown(periodExpenses);
        loadTrendChart(periodExpenses);
        loadBudgetHealth();
    }

    // ─── Ranges ───────────────────────────────────────────────
    private long[] getWeekRange(Calendar anchor) {
        Calendar start = (Calendar) anchor.clone();
        start.set(Calendar.DAY_OF_WEEK, start.getFirstDayOfWeek());
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);
        long s = start.getTimeInMillis();

        Calendar end = (Calendar) start.clone();
        end.add(Calendar.DAY_OF_YEAR, 6);
        end.set(Calendar.HOUR_OF_DAY, 23);
        end.set(Calendar.MINUTE, 59);
        end.set(Calendar.SECOND, 59);
        end.set(Calendar.MILLISECOND, 999);
        long e = end.getTimeInMillis();
        return new long[]{s, e};
    }

    private long[] getMonthRange(int year, int month) {
        Calendar c = Calendar.getInstance();
        c.set(year, month, 1, 0, 0, 0);
        c.set(Calendar.MILLISECOND, 0);
        long start = c.getTimeInMillis();
        c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);
        long end = c.getTimeInMillis();
        return new long[]{start, end};
    }

    private long[] getYearRange(int year) {
        Calendar start = Calendar.getInstance();
        start.set(year, Calendar.JANUARY, 1, 0, 0, 0);
        start.set(Calendar.MILLISECOND, 0);

        Calendar end = Calendar.getInstance();
        end.set(year, Calendar.DECEMBER, 31, 23, 59, 59);
        end.set(Calendar.MILLISECOND, 999);

        return new long[]{start.getTimeInMillis(), end.getTimeInMillis()};
    }

    private long startOfToday() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private long endOfToday() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);
        return c.getTimeInMillis();
    }

    // ─── Days-left helpers ────────────────────────────────────
    private String getDaysLeftTextForWeek(Calendar anchor) {
        long[] range = getWeekRange(anchor);
        long todayEnd = endOfToday();

        if (todayEnd < range[0]) return "৭ দিনের সপ্তাহ";
        if (todayEnd > range[1]) return "সম্পূর্ণ সপ্তাহ";

        long diffMs = range[1] - startOfToday();
        int daysLeft = (int) (diffMs / (24L * 60 * 60 * 1000)) + 1;
        if (daysLeft < 0) daysLeft = 0;
        return "আর " + bnConverter.toBengali(daysLeft) + " দিন বাকি";
    }

    private String getDaysLeftTextForMonth(Calendar anchor) {
        int year  = anchor.get(Calendar.YEAR);
        int month = anchor.get(Calendar.MONTH);
        long[] range = getMonthRange(year, month);
        long todayEnd = endOfToday();

        if (todayEnd < range[0]) {
            Calendar c = Calendar.getInstance();
            c.set(year, month, 1);
            int days = c.getActualMaximum(Calendar.DAY_OF_MONTH);
            return bnConverter.toBengali(days) + " দিনের মাস";
        }
        if (todayEnd > range[1]) return "সম্পূর্ণ মাস";

        Calendar now = Calendar.getInstance();
        int daysInMonth = now.getActualMaximum(Calendar.DAY_OF_MONTH);
        int daysLeft = daysInMonth - now.get(Calendar.DAY_OF_MONTH) + 1;
        if (daysLeft < 0) daysLeft = 0;
        return "আর " + bnConverter.toBengali(daysLeft) + " দিন বাকি";
    }

    private String getDaysLeftTextForYear(Calendar anchor) {
        int year = anchor.get(Calendar.YEAR);
        long[] range = getYearRange(year);
        long todayEnd = endOfToday();

        if (todayEnd < range[0]) {
            Calendar c = Calendar.getInstance();
            c.set(year, Calendar.JANUARY, 1);
            int days = c.getActualMaximum(Calendar.DAY_OF_YEAR);
            return bnConverter.toBengali(days) + " দিনের বছর";
        }
        if (todayEnd > range[1]) return "সম্পূর্ণ বছর";

        Calendar now = Calendar.getInstance();
        int totalDays = now.getActualMaximum(Calendar.DAY_OF_YEAR);
        int daysLeft = totalDays - now.get(Calendar.DAY_OF_YEAR) + 1;
        if (daysLeft < 0) daysLeft = 0;
        return "আর " + bnConverter.toBengali(daysLeft) + " দিন বাকি";
    }

    // ─── Totals ───────────────────────────────────────────────
    private double getTypeTotalInRange(String type, long start, long end) {
        List<Expense> list = dbHelper.getExpensesByRange(start, end);
        double total = 0;
        if (list == null) return 0;
        for (Expense e : list) {
            if (e == null) continue;
            if (type.equals("income") && e.isIncome()) total += e.getAmount();
            if (type.equals("expense") && !e.isIncome()) total += e.getAmount();
        }
        return total;
    }

    private double getMonthIncome(int year, int month) {
        long[] r = getMonthRange(year, month);
        return getTypeTotalInRange("income", r[0], r[1]);
    }

    private double getMonthExpense(int year, int month) {
        long[] r = getMonthRange(year, month);
        return getTypeTotalInRange("expense", r[0], r[1]);
    }

    private double getYearIncome(int year) {
        long[] r = getYearRange(year);
        return getTypeTotalInRange("income", r[0], r[1]);
    }

    private double getYearExpense(int year) {
        long[] r = getYearRange(year);
        return getTypeTotalInRange("expense", r[0], r[1]);
    }

    private List<Expense> getYearExpenses(int year) {
        long[] range = getYearRange(year);
        List<Expense> all = dbHelper.getExpensesByRange(range[0], range[1]);
        return all != null ? all : new ArrayList<>();
    }

    // ─────────────────────────────────────────────────────────
    // DONUT
    // ─────────────────────────────────────────────────────────
    private void loadCategoryBreakdown(List<Expense> expenses) {
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
            View colorDot         = legendItem.findViewById(R.id.v_color_dot);
            TextView tvName       = legendItem.findViewById(R.id.tv_cat_name);
            TextView tvAmountText = legendItem.findViewById(R.id.tv_cat_amount);
            TextView tvPercent    = legendItem.findViewById(R.id.tv_cat_percent);

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

    // ─────────────────────────────────────────────────────────
    // BAR CHART
    // ─────────────────────────────────────────────────────────
    private void loadTrendChart(List<Expense> expenses) {
        if (expenses == null) expenses = new ArrayList<>();

        int bucketCount = currentPeriod == 0 ? 7 : (currentPeriod == 2 ? 12 : 4);
        float[] values = new float[bucketCount];
        double total = 0;

        long weekStartMs = 0;
        if (currentPeriod == 0) {
            Calendar ws = (Calendar) selectedCal.clone();
            ws.set(Calendar.DAY_OF_WEEK, ws.getFirstDayOfWeek());
            ws.set(Calendar.HOUR_OF_DAY, 0);
            ws.set(Calendar.MINUTE, 0);
            ws.set(Calendar.SECOND, 0);
            ws.set(Calendar.MILLISECOND, 0);
            weekStartMs = ws.getTimeInMillis();
        }

        Calendar cal = Calendar.getInstance();
        for (Expense e : expenses) {
            if (e == null) continue;
            if (e.isIncome()) continue;

            cal.setTimeInMillis(e.getDate());
            int bucket;
            if (currentPeriod == 0) {
                long diff = cal.getTimeInMillis() - weekStartMs;
                bucket = (int) (diff / (24L * 60 * 60 * 1000));
            } else if (currentPeriod == 2) {
                bucket = cal.get(Calendar.MONTH);
            } else {
                bucket = (cal.get(Calendar.DAY_OF_MONTH) - 1) / 7;
                if (bucket > 3) bucket = 3;
            }
            if (bucket >= 0 && bucket < bucketCount) {
                values[bucket] += (float) e.getAmount();
            }
            total += e.getAmount();
        }

        barChart.setData(values);

        double avg;
        String maxLabel;
        if (currentPeriod == 0) {
            avg = total / 7.0;
            maxLabel = "সর্বোচ্চ দিন: " + maxDayLabel(values);
        } else if (currentPeriod == 2) {
            avg = total / 12.0;
            maxLabel = "সর্বোচ্চ মাস: " + maxMonthLabel(values);
        } else {
            int daysInMonth = selectedCal.getActualMaximum(Calendar.DAY_OF_MONTH);
            avg = total / Math.max(1, daysInMonth);
            String[] weekNames = { "১ম সপ্তাহ", "২য় সপ্তাহ", "৩য় সপ্তাহ", "৪র্থ সপ্তাহ" };
            maxLabel = "সর্বোচ্চ: " + weekNames[indexOfMax(values)];
        }

        tvAvgDaily.setText("গড়: ৳" + bnConverter.toBengali(
                String.format(Locale.US, "%,.0f", avg)));
        tvMaxExpense.setText(maxLabel);
    }

    private int indexOfMax(float[] arr) {
        int idx = 0;
        float max = -1;
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] > max) { max = arr[i]; idx = i; }
        }
        return idx;
    }

    private String maxDayLabel(float[] values) {
        Calendar ws = (Calendar) selectedCal.clone();
        ws.set(Calendar.DAY_OF_WEEK, ws.getFirstDayOfWeek());

        String[] dayNames = { "রবি", "সোম", "মঙ্গল", "বুধ", "বৃহঃ", "শুক্র", "শনি" };
        int idx = indexOfMax(values);
        if (idx < 0 || idx > 6) return "-";

        Calendar d = (Calendar) ws.clone();
        d.add(Calendar.DAY_OF_YEAR, idx);
        int dow = d.get(Calendar.DAY_OF_WEEK) - 1;
        return dayNames[dow];
    }

    private String maxMonthLabel(float[] values) {
        String[] months = {
                "জানু", "ফেব্রু", "মার্চ", "এপ্রিল", "মে", "জুন",
                "জুলাই", "আগস্ট", "সেপ্ট", "অক্টো", "নভে", "ডিসে"
        };
        int idx = indexOfMax(values);
        return (idx >= 0 && idx < months.length) ? months[idx] : "-";
    }

    // ─────────────────────────────────────────────────────────
    // BUDGET HEALTH
    // ─────────────────────────────────────────────────────────
    private void loadBudgetHealth() {
        llBudgetHealth.removeAllViews();

        String monthKey = selectedCal.get(Calendar.YEAR) + "-"
                + (selectedCal.get(Calendar.MONTH) + 1);

        List<CategoryBudget> budgets = dbHelper.getCategoryBudgets(monthKey);
        if (budgets == null) budgets = new ArrayList<>();

        View sectionHeader = getView() != null
                ? getView().findViewById(R.id.tv_budget_health_header)
                : null;

        if (budgets.isEmpty()) {
            if (sectionHeader != null) sectionHeader.setVisibility(View.GONE);
            llBudgetHealth.setVisibility(View.GONE);
            return;
        }
        llBudgetHealth.setVisibility(View.VISIBLE);
        if (sectionHeader != null) sectionHeader.setVisibility(View.VISIBLE);

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

    // ─────────────────────────────────────────────────────────
    // EXPORT
    // ─────────────────────────────────────────────────────────
    private void setupClickListeners() {
        btnPdfExport.setOnClickListener(v -> {
            int year  = selectedCal.get(Calendar.YEAR);
            int month = selectedCal.get(Calendar.MONTH);

            List<Expense> periodExpenses;
            double income, expense, balance;
            String periodLabel;

            if (currentPeriod == 0) {
                long[] range = getWeekRange(selectedCal);
                periodExpenses = dbHelper.getExpensesByRange(range[0], range[1]);
                income  = getTypeTotalInRange("income", range[0], range[1]);
                expense = getTypeTotalInRange("expense", range[0], range[1]);
                periodLabel = "সাপ্তাহিক রিপোর্ট";
            } else if (currentPeriod == 2) {
                periodExpenses = getYearExpenses(year);
                income  = getYearIncome(year);
                expense = getYearExpense(year);
                periodLabel = "বাৎসরিক রিপোর্ট " + bnConverter.toBengali(year);
            } else {
                periodExpenses = dbHelper.getMonthExpenses(year, month);
                income  = getMonthIncome(year, month);
                expense = getMonthExpense(year, month);
                periodLabel = DateUtils.getBengaliMonth(month) + " " + bnConverter.toBengali(year);
            }
            balance = income - expense;

            boolean ok = ExportHelper.exportPdf(
                    requireContext(),
                    periodExpenses != null ? periodExpenses : new ArrayList<>(),
                    income, expense, balance, periodLabel);

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