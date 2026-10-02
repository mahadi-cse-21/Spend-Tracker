package com.diodeit.spendtrack.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.diodeit.spendtrack.R;
import com.google.android.material.chip.Chip;


public class PaymentAdapter extends RecyclerView.Adapter<PaymentAdapter.ViewHolder> {

    public interface OnPaymentClickListener {
        void onPaymentClick(String method);
    }

    private Context context;
    private String[] methods;
    private OnPaymentClickListener listener;
    private int selectedPosition = 0;

    public PaymentAdapter(Context context, String[] methods, OnPaymentClickListener listener) {
        this.context = context;
        this.methods = methods;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_payment_method, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String method = methods[position];
        holder.chip.setText(method);
        holder.chip.setChecked(position == selectedPosition);

        holder.chip.setOnClickListener(v -> {
            int oldPosition = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(oldPosition);
            notifyItemChanged(selectedPosition);

            if (listener != null) {
                listener.onPaymentClick(method);
            }
        });
    }

    @Override
    public int getItemCount() {
        return methods.length;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        Chip chip;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            chip = itemView.findViewById(R.id.chip_payment);
        }
    }
}