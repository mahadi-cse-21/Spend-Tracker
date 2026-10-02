package com.diodeit.spendtrack.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.diodeit.spendtrack.R;
import com.diodeit.spendtrack.models.Expense;
import com.diodeit.spendtrack.utils.BengaliNumberConverter;
import com.diodeit.spendtrack.utils.CategoryUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ViewHolder> {

    public interface OnExpenseClickListener {
        void onExpenseClick(Expense expense);
    }

    private Context context;
    private List<Expense> expenses;
    private OnExpenseClickListener listener;
    private BengaliNumberConverter bnConverter;

    public ExpenseAdapter(Context context, List<Expense> expenses, OnExpenseClickListener listener) {
        this.context = context;
        this.expenses = expenses;
        this.listener = listener;
        this.bnConverter = new BengaliNumberConverter();
    }

    public void updateData(List<Expense> newExpenses) {
        this.expenses = newExpenses;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_expense, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Expense expense = expenses.get(position);
        boolean isIncome = expense.isIncome();

        // Title
        String title = expense.getItemsSummary();
        if (title == null || title.isEmpty()) title = expense.getCategory();
        holder.tvTitle.setText(title);

        // Subtext
        if (expense.getItems() != null && expense.getItems().size() > 1) {
            String label = isIncome ? "টি আয়" : "টি খরচ";
            holder.tvCategory.setText(expense.getItems().size() + label);
        } else {
            holder.tvCategory.setText(expense.getCategory());
        }

        holder.tvPayment.setText(expense.getPaymentMethod());

        String amountStr = bnConverter.toBengali(
                String.format(Locale.US, "%,.0f", expense.getAmount()));

        if (isIncome) {
            holder.tvAmount.setText("+৳" + amountStr);
            holder.tvAmount.setTextColor(ContextCompat.getColor(context, R.color.primary));
        } else {
            holder.tvAmount.setText("-৳" + amountStr);
            holder.tvAmount.setTextColor(ContextCompat.getColor(context, R.color.error));
        }

        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        holder.tvTime.setText(sdf.format(new Date(expense.getDate())));

        holder.ivIcon.setImageResource(CategoryUtils.getCategoryIcon(expense.getCategory()));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onExpenseClick(expense);
        });
    }

    @Override
    public int getItemCount() { return expenses.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvTitle, tvCategory, tvPayment, tvAmount, tvTime;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon     = itemView.findViewById(R.id.iv_category_icon);
            tvTitle    = itemView.findViewById(R.id.tv_expense_title);
            tvCategory = itemView.findViewById(R.id.tv_expense_category);
            tvPayment  = itemView.findViewById(R.id.tv_payment_method);
            tvAmount   = itemView.findViewById(R.id.tv_expense_amount);
            tvTime     = itemView.findViewById(R.id.tv_expense_time);
        }
    }
}