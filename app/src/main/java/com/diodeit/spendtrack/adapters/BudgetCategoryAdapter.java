package com.diodeit.spendtrack.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.diodeit.spendtrack.R;
import com.diodeit.spendtrack.models.CategoryBudget;
import com.diodeit.spendtrack.utils.BengaliNumberConverter;
import com.diodeit.spendtrack.utils.CategoryUtils;
import java.util.List;
import java.util.Locale;

public class BudgetCategoryAdapter extends RecyclerView.Adapter<BudgetCategoryAdapter.ViewHolder> {

    public interface OnBudgetChangeListener {
        void onBudgetChanged(CategoryBudget category);
    }

    private Context context;
    private List<CategoryBudget> categories;
    private BengaliNumberConverter bnConverter;
    private OnBudgetChangeListener listener;

    public BudgetCategoryAdapter(Context context, List<CategoryBudget> categories, OnBudgetChangeListener listener) {
        this.context = context;
        this.categories = categories;
        this.bnConverter = new BengaliNumberConverter();
        this.listener = listener;
    }

    public void updateData(List<CategoryBudget> newCategories) {
        this.categories = newCategories;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_budget_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CategoryBudget category = categories.get(position);

        holder.tvCategoryName.setText(category.getCategoryName());
        
        String spentStr = bnConverter.toBengali(String.format(Locale.US, "%,.0f", category.getSpentAmount()));
        String budgetStr = bnConverter.toBengali(String.format(Locale.US, "%,.0f", category.getBudgetAmount()));
        
        // Fixed: changed current_expense to current_expense_label
        holder.tvCurrentExpense.setText(context.getString(R.string.current_expense_label, spentStr));
        holder.tvPercentBadge.setText(context.getString(R.string.spent_percent, bnConverter.toBengali(category.getPercentSpent())));
        
        holder.progressBudget.setProgress(Math.min(category.getPercentSpent(), 100));
        if (category.getPercentSpent() >= 90) {
            holder.progressBudget.setIndicatorColor(context.getColor(R.color.error));
        } else {
            holder.progressBudget.setIndicatorColor(context.getColor(R.color.primary));
        }

        holder.tvSpent.setText("৳ " + spentStr);
        holder.tvBudget.setText(context.getString(R.string.budget_label, budgetStr));
        holder.tvLimitAmount.setText("৳ " + budgetStr);

        holder.ivIcon.setImageResource(CategoryUtils.getCategoryIcon(category.getCategoryName()));

        holder.btnDecrease.setOnClickListener(v -> {
            if (category.getBudgetAmount() >= 500) {
                category.setBudgetAmount(category.getBudgetAmount() - 500);
                notifyItemChanged(holder.getAdapterPosition());
                if (listener != null) listener.onBudgetChanged(category);
            }
        });

        holder.btnIncrease.setOnClickListener(v -> {
            category.setBudgetAmount(category.getBudgetAmount() + 500);
            notifyItemChanged(holder.getAdapterPosition());
            if (listener != null) listener.onBudgetChanged(category);
        });
        
        holder.switchAlert.setOnCheckedChangeListener(null);
        holder.switchAlert.setChecked(category.isAlertEnabled());
        holder.switchAlert.setOnCheckedChangeListener((buttonView, isChecked) -> {
            category.setAlertEnabled(isChecked);
            if (listener != null) listener.onBudgetChanged(category);
        });
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon, btnDecrease, btnIncrease;
        TextView tvCategoryName, tvCurrentExpense, tvPercentBadge;
        TextView tvSpent, tvBudget, tvLimitAmount;
        LinearProgressIndicator progressBudget;
        MaterialSwitch switchAlert;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_icon);
            tvCategoryName = itemView.findViewById(R.id.tv_category_name);
            tvCurrentExpense = itemView.findViewById(R.id.tv_current_expense);
            tvPercentBadge = itemView.findViewById(R.id.tv_percent_badge);
            progressBudget = itemView.findViewById(R.id.progress_budget);
            tvSpent = itemView.findViewById(R.id.tv_spent);
            tvBudget = itemView.findViewById(R.id.tv_budget);
            tvLimitAmount = itemView.findViewById(R.id.tv_limit_amount);
            btnDecrease = itemView.findViewById(R.id.btn_decrease);
            btnIncrease = itemView.findViewById(R.id.btn_increase);
            switchAlert = itemView.findViewById(R.id.switch_alert);
        }
    }
}
