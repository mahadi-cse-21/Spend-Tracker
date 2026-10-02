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
import com.diodeit.spendtrack.models.Loan;
import com.diodeit.spendtrack.utils.BengaliNumberConverter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.List;
import java.util.Locale;

public class LoanAdapter extends RecyclerView.Adapter<LoanAdapter.VH> {

    public interface OnLoanAction {
        void onEdit(Loan loan);
        void onPay(Loan loan);
        void onDelete(Loan loan);
    }

    private final Context ctx;
    private final List<Loan> loans;
    private final OnLoanAction listener;
    private final BengaliNumberConverter bn = new BengaliNumberConverter();

    public LoanAdapter(Context ctx, List<Loan> loans, OnLoanAction listener) {
        this.ctx = ctx;
        this.loans = loans;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx)
                .inflate(R.layout.item_loan, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Loan l = loans.get(position);

        h.tvPerson.setText(l.getPersonName());
        h.tvLoanType.setText(l.isTaken() ? "আমি ঋণ নিয়েছি" : "আমি ঋণ দিয়েছি");
        h.ivIcon.setImageResource(l.isTaken()
                ? R.drawable.ic_payments
                : R.drawable.ic_donate);

        String remainingStr = bn.toBengali(
                String.format(Locale.US, "%,.0f", l.getRemaining()));
        String paidStr = bn.toBengali(
                String.format(Locale.US, "%,.0f", l.getPaidAmount()));
        String totalStr = bn.toBengali(
                String.format(Locale.US, "%,.0f", l.getPrincipalAmount()));

        h.tvRemaining.setText("৳ " + remainingStr);
        h.tvPaid.setText("পরিশোধ: ৳ " + paidStr);
        h.tvTotal.setText("মোট: ৳ " + totalStr);

        if (l.isClosed()) {
            h.tvStatus.setText("সম্পূর্ণ পরিশোধিত");
            h.tvRemaining.setTextColor(0xFF00513C);   // green
            h.progressLoan.setIndicatorColor(0xFF00513C);
            h.btnPayLoan.setVisibility(View.GONE);
        } else {
            h.tvStatus.setText(l.isTaken() ? "বাকি" : "পাব");
            h.tvRemaining.setTextColor(0xFFBA1A1A);   // red
            h.progressLoan.setIndicatorColor(0xFFBA1A1A);
            h.btnPayLoan.setVisibility(View.VISIBLE);
        }

        h.progressLoan.setProgress(l.getProgressPercent());

        h.btnEditLoan.setOnClickListener(v -> listener.onEdit(l));
        h.btnPayLoan.setOnClickListener(v -> listener.onPay(l));
        h.btnDeleteLoan.setOnClickListener(v -> listener.onDelete(l));
    }

    @Override
    public int getItemCount() { return loans.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvPerson, tvLoanType, tvRemaining, tvStatus, tvPaid, tvTotal;
        LinearProgressIndicator progressLoan;
        MaterialButton btnEditLoan, btnPayLoan, btnDeleteLoan;

        VH(@NonNull View v) {
            super(v);
            ivIcon         = v.findViewById(R.id.iv_loan_icon);
            tvPerson       = v.findViewById(R.id.tv_person);
            tvLoanType     = v.findViewById(R.id.tv_loan_type);
            tvRemaining    = v.findViewById(R.id.tv_remaining);
            tvStatus       = v.findViewById(R.id.tv_status);
            tvPaid         = v.findViewById(R.id.tv_paid);
            tvTotal        = v.findViewById(R.id.tv_total);
            progressLoan   = v.findViewById(R.id.progress_loan);
            btnEditLoan    = v.findViewById(R.id.btn_edit_loan);
            btnPayLoan     = v.findViewById(R.id.btn_pay_loan);
            btnDeleteLoan  = v.findViewById(R.id.btn_delete_loan);
        }
    }
}