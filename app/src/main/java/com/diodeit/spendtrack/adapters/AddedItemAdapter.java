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
import com.diodeit.spendtrack.models.ExpenseItem;
import com.diodeit.spendtrack.utils.BengaliNumberConverter;

import java.util.List;
import java.util.Locale;

public class AddedItemAdapter extends RecyclerView.Adapter<AddedItemAdapter.VH> {

    public interface OnItemRemove { void onRemove(int position); }
    public interface OnItemEdit   { void onEdit(int position); }

    private final Context ctx;
    private final List<ExpenseItem> items;
    private final OnItemRemove removeListener;
    private final OnItemEdit editListener;
    private final BengaliNumberConverter bn = new BengaliNumberConverter();

    public AddedItemAdapter(Context ctx, List<ExpenseItem> items, OnItemRemove removeListener) {
        this(ctx, items, removeListener, null);
    }

    public AddedItemAdapter(Context ctx, List<ExpenseItem> items,
                            OnItemRemove removeListener, OnItemEdit editListener) {
        this.ctx = ctx;
        this.items = items;
        this.removeListener = removeListener;
        this.editListener = editListener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx)
                .inflate(R.layout.item_added_category, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        ExpenseItem item = items.get(position);
        h.tvName.setText(item.getName());
        h.tvAmount.setText("৳ " + bn.toBengali(
                String.format(Locale.US, "%,.0f", item.getAmount())));

        h.btnEdit.setOnClickListener(v -> {
            if (editListener != null) editListener.onEdit(h.getAdapterPosition());
        });
        h.btnDelete.setOnClickListener(v -> {
            if (removeListener != null) removeListener.onRemove(h.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivIcon, btnEdit, btnDelete;
        TextView tvName, tvAmount;

        VH(@NonNull View v) {
            super(v);
            ivIcon    = v.findViewById(R.id.iv_item_icon);
            tvName    = v.findViewById(R.id.tv_item_name);
            tvAmount  = v.findViewById(R.id.tv_item_amount);
            btnEdit   = v.findViewById(R.id.btn_edit_item);
            btnDelete = v.findViewById(R.id.btn_delete_item);
        }
    }
}