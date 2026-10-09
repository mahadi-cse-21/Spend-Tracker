package com.diodeit.spendtrack.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.diodeit.spendtrack.R;
import com.diodeit.spendtrack.models.DateHeaderItem;
import com.diodeit.spendtrack.models.Expense;
import com.diodeit.spendtrack.models.ListItem;
import com.diodeit.spendtrack.models.TransactionItem;
import com.diodeit.spendtrack.utils.BengaliNumberConverter;
import com.diodeit.spendtrack.utils.CategoryUtils;
import com.diodeit.spendtrack.utils.DateUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TransactionGroupAdapter
        extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnTransactionClickListener {
        void onTransactionClick(Expense expense);
    }

    private final Context context;
    private final List<ListItem> items = new ArrayList<>();
    private final OnTransactionClickListener listener;
    private final BengaliNumberConverter bn = new BengaliNumberConverter();

    public TransactionGroupAdapter(Context context,
                                   OnTransactionClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    /** মূল এন্ট্রি পয়েন্ট — History/Home এখান থেকে ডেটা পাঠাবে */
    public void setExpenses(List<Expense> expenses) {
        items.clear();

        if (expenses == null || expenses.isEmpty()) {
            notifyDataSetChanged();
            return;
        }

        // তারিখ অনুযায়ী গ্রুপ। LinkedHashMap insertion order রক্ষা করে।
        // যেহেতু expenses date DESC সাজানো, তাই নতুন তারিখ আগে থাকবে।
        LinkedHashMap<Long, List<Expense>> grouped = new LinkedHashMap<>();

        for (Expense e : expenses) {
            long key = startOfDay(e.getDate());
            List<Expense> bucket = grouped.get(key);
            if (bucket == null) {
                bucket = new ArrayList<>();
                grouped.put(key, bucket);
            }
            bucket.add(e);
        }

        for (Map.Entry<Long, List<Expense>> entry : grouped.entrySet()) {
            long dayStart = entry.getKey();
            List<Expense> dayList = entry.getValue();

            double inc = 0, exp = 0;
            for (Expense e : dayList) {
                if (e.isIncome()) inc += e.getAmount();
                else              exp += e.getAmount();
            }

            // হেডার প্রথমে
            items.add(new DateHeaderItem(dayStart, inc, exp));
            // তারপর সেই দিনের সব লেনদেন
            for (Expense e : dayList) items.add(new TransactionItem(e));
        }

        notifyDataSetChanged();
    }

    private long startOfDay(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).getItemType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                      int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        if (viewType == ListItem.TYPE_HEADER) {
            View v = inflater.inflate(R.layout.item_date_header, parent, false);
            return new HeaderVH(v);
        } else {
            View v = inflater.inflate(R.layout.item_expense, parent, false);
            return new TransactionVH(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder,
                                 int position) {
        ListItem item = items.get(position);

        if (holder instanceof HeaderVH) {
            bindHeader((HeaderVH) holder, (DateHeaderItem) item);
        } else {
            bindTransaction((TransactionVH) holder, (TransactionItem) item);
        }
    }

    private void bindHeader(HeaderVH h, DateHeaderItem header) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(header.getDate());

        Calendar today = Calendar.getInstance();
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);

        boolean isToday =
                c.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                        c.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR);

        boolean isYesterday =
                c.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
                        c.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR);

        String dateStr;
        if (isToday) {
            dateStr = "আজ, " + bn.toBengali(c.get(Calendar.DAY_OF_MONTH)) + " "
                    + DateUtils.getBengaliMonth(c.get(Calendar.MONTH));
        } else if (isYesterday) {
            dateStr = "গতকাল, " + bn.toBengali(c.get(Calendar.DAY_OF_MONTH)) + " "
                    + DateUtils.getBengaliMonth(c.get(Calendar.MONTH));
        } else {
            dateStr = bn.toBengali(c.get(Calendar.DAY_OF_MONTH)) + " "
                    + DateUtils.getBengaliMonth(c.get(Calendar.MONTH)) + " "
                    + bn.toBengali(c.get(Calendar.YEAR));
        }
        h.tvDate.setText(dateStr);

        StringBuilder total = new StringBuilder();
        if (header.getIncome() > 0) {
            total.append("+").append(bn.toBengali(
                    String.format(Locale.US, "%,.0f", header.getIncome())));
        }
        if (header.getExpense() > 0) {
            if (total.length() > 0) total.append("  ");
            total.append("-").append(bn.toBengali(
                    String.format(Locale.US, "%,.0f", header.getExpense())));
        }
        if (total.length() == 0) total.append("—");
        h.tvDayTotal.setText(total.toString());
    }

    private void bindTransaction(TransactionVH h, TransactionItem item) {
        Expense e = item.getExpense();
        boolean isIncome = e.isIncome();

        String title = e.getItemsSummary();
        if (title == null || title.isEmpty()) title = e.getCategory();
        h.tvTitle.setText(title);

        if (e.getItems() != null && e.getItems().size() > 1) {
            String label = isIncome ? "টি আয়" : "টি খরচ";
            h.tvCategory.setText(e.getItems().size() + label);
        } else {
            h.tvCategory.setText(e.getCategory());
        }

        h.tvPayment.setText(e.getPaymentMethod());

        String amountStr = bn.toBengali(
                String.format(Locale.US, "%,.0f", e.getAmount()));

        if (isIncome) {
            h.tvAmount.setText("+৳" + amountStr);
            h.tvAmount.setTextColor(0xFF00513C);
        } else {
            h.tvAmount.setText("-৳" + amountStr);
            h.tvAmount.setTextColor(0xFFBA1A1A);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        h.tvTime.setText(sdf.format(new Date(e.getDate())));

        h.ivIcon.setImageResource(CategoryUtils.getCategoryIcon(e.getCategory()));

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onTransactionClick(e);
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class HeaderVH extends RecyclerView.ViewHolder {
        TextView tvDate, tvDayTotal;
        HeaderVH(@NonNull View v) {
            super(v);
            tvDate     = v.findViewById(R.id.tv_date_header);
            tvDayTotal = v.findViewById(R.id.tv_day_total);
        }
    }

    static class TransactionVH extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvTitle, tvCategory, tvPayment, tvAmount, tvTime;
        TransactionVH(@NonNull View v) {
            super(v);
            ivIcon     = v.findViewById(R.id.iv_category_icon);
            tvTitle    = v.findViewById(R.id.tv_expense_title);
            tvCategory = v.findViewById(R.id.tv_expense_category);
            tvPayment  = v.findViewById(R.id.tv_payment_method);
            tvAmount   = v.findViewById(R.id.tv_expense_amount);
            tvTime     = v.findViewById(R.id.tv_expense_time);
        }
    }
}