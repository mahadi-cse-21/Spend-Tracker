package com.diodeit.spendtrack.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.diodeit.spendtrack.R;
import com.diodeit.spendtrack.adapters.BudgetCategoryAdapter;
import com.diodeit.spendtrack.databases.DatabaseHelper;
import com.diodeit.spendtrack.models.Budget;
import com.diodeit.spendtrack.models.CategoryBudget;
import com.diodeit.spendtrack.utils.BengaliNumberConverter;
import com.diodeit.spendtrack.utils.DateUtils;
import com.diodeit.spendtrack.utils.PreferenceManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class BudgetFragment extends Fragment {

    private TextView tvTotalBudget, tvAllocated, tvRemainingOpen;
    private LinearProgressIndicator progressAllocation;
    private RecyclerView rvCategoryBudgets;
    private MaterialButton btnAddCategory, btnSaveBudget, btnMonthSelector;
    private MaterialSwitch switchVibration, switchSafeSpend;
    private Toolbar toolbar;

    private BudgetCategoryAdapter adapter;
    private DatabaseHelper dbHelper;
    private BengaliNumberConverter bnConverter;
    private PreferenceManager prefManager;
    private String currentMonthKey;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_budget, container, false);

        dbHelper = new DatabaseHelper(requireContext());
        bnConverter = new BengaliNumberConverter();
        prefManager = new PreferenceManager(requireContext());
        currentMonthKey = DateUtils.getMonthKey();

        initViews(view);
        setupRecyclerView();
        loadBudgetData();
        setupClickListeners();

        return view;
    }

    private void initViews(View view) {
        toolbar = view.findViewById(R.id.toolbar);
        tvTotalBudget = view.findViewById(R.id.tv_total_budget);
        tvAllocated = view.findViewById(R.id.tv_allocated);
        tvRemainingOpen = view.findViewById(R.id.tv_remaining_open);
        progressAllocation = view.findViewById(R.id.progress_allocation);
        rvCategoryBudgets = view.findViewById(R.id.rv_category_budgets);
        btnAddCategory = view.findViewById(R.id.btn_add_category);
        btnSaveBudget = view.findViewById(R.id.btn_save_budget);
        btnMonthSelector = view.findViewById(R.id.btn_month_selector);
        switchVibration = view.findViewById(R.id.switch_vibration);
        switchSafeSpend = view.findViewById(R.id.switch_safe_spend);
    }

    private void setupRecyclerView() {
        adapter = new BudgetCategoryAdapter(requireContext(), new ArrayList<>(), category -> {
            dbHelper.saveCategoryBudget(currentMonthKey, category);
            loadBudgetData();
        });
        rvCategoryBudgets.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvCategoryBudgets.setAdapter(adapter);
    }

    private void loadBudgetData() {
        Budget mainBudget = dbHelper.getCurrentMonthBudget();
        List<CategoryBudget> categories = dbHelper.getCategoryBudgets(currentMonthKey);

        adapter.updateData(categories);

        double total = mainBudget != null ? mainBudget.getTotalAmount() : 0;
        double allocated = 0;
        for (CategoryBudget cb : categories) {
            allocated += cb.getBudgetAmount();
        }
        double remaining = Math.max(0, total - allocated);

        tvTotalBudget.setText(bnConverter.formatCurrency(total));
        tvAllocated.setText(bnConverter.formatCurrency(allocated));
        tvRemainingOpen.setText(bnConverter.formatCurrency(remaining));

        int progress = total > 0 ? (int) ((allocated / total) * 100) : 0;
        progressAllocation.setProgress(Math.min(progress, 100));
        
        TextView tvAllocPercent = getView().findViewById(R.id.tv_allocation_percent);
        if (tvAllocPercent != null) {
            tvAllocPercent.setText(getString(R.string.allocated_percent, bnConverter.toBengali(progress)));
        }

        switchVibration.setChecked(prefManager.isVibrationEnabled());
        switchSafeSpend.setChecked(prefManager.isSafeSpendEnabled());
        
        Calendar cal = Calendar.getInstance();
        btnMonthSelector.setText(DateUtils.getBengaliMonth(cal.get(Calendar.MONTH)) + " " + bnConverter.toBengali(cal.get(Calendar.YEAR)));
    }

    private void setupClickListeners() {
        btnAddCategory.setOnClickListener(v -> showAddCategoryDialog());

        btnSaveBudget.setOnClickListener(v -> {
            Toast.makeText(requireContext(), R.string.budget_saved, Toast.LENGTH_SHORT).show();
        });
        
        tvTotalBudget.setOnClickListener(v -> showEditMainBudgetDialog());

        switchVibration.setOnCheckedChangeListener((buttonView, isChecked) -> 
                prefManager.setVibration(isChecked));
        
        switchSafeSpend.setOnCheckedChangeListener((buttonView, isChecked) -> 
                prefManager.setSafeSpend(isChecked));
        
        toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());
    }

    private void showEditMainBudgetDialog() {
        Budget current = dbHelper.getCurrentMonthBudget();
        double currentAmount = current != null ? current.getTotalAmount() : 0;
        
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_category, null);
        dialogView.findViewById(R.id.tv_category_name_label).setVisibility(View.GONE);
        dialogView.findViewById(R.id.ll_category_name_container).setVisibility(View.GONE);

        EditText etAmount = dialogView.findViewById(R.id.et_amount);
        etAmount.setText(String.format(Locale.US, "%.0f", currentAmount));

        new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setTitle(R.string.total_budget_limit)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String amountStr = etAmount.getText().toString();
                    if (!amountStr.isEmpty()) {
                        double amount = Double.parseDouble(amountStr);
                        Budget budget = new Budget(currentMonthKey, amount);
                        dbHelper.saveBudget(budget);
                        loadBudgetData();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showAddCategoryDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_category, null);
        EditText etName = dialogView.findViewById(R.id.et_category_name);
        EditText etAmount = dialogView.findViewById(R.id.et_amount);

        new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setTitle(R.string.add_new_category)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String name = etName.getText().toString();
                    String amountStr = etAmount.getText().toString();
                    if (!name.isEmpty() && !amountStr.isEmpty()) {
                        double amount = Double.parseDouble(amountStr);
                        CategoryBudget cb = new CategoryBudget(name, amount, 0, 0);
                        dbHelper.saveCategoryBudget(currentMonthKey, cb);
                        loadBudgetData();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
