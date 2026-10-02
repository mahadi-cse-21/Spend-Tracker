package com.diodeit.spendtrack.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.diodeit.spendtrack.R;
import com.diodeit.spendtrack.utils.BengaliNumberConverter;

public class CalculatorAdapter extends RecyclerView.Adapter<CalculatorAdapter.ViewHolder> {

    public interface OnCalculatorClickListener {
        void onButtonClick(String value);
    }

    private Context context;
    private String[] buttons;
    private OnCalculatorClickListener listener;
    private BengaliNumberConverter bnConverter;

    public CalculatorAdapter(Context context, String[] buttons, OnCalculatorClickListener listener) {
        this.context = context;
        this.buttons = buttons;
        this.listener = listener;
        this.bnConverter = new BengaliNumberConverter();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_calculator_button, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String button = buttons[position];
        holder.tvButton.setText(button);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onButtonClick(button);
            }
        });
    }

    @Override
    public int getItemCount() {
        return buttons.length;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvButton = itemView.findViewById(R.id.btn_calc);
        }
    }
}