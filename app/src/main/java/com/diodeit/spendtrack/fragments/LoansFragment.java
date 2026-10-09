package com.diodeit.spendtrack.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.diodeit.spendtrack.R;
import com.diodeit.spendtrack.adapters.LoanAdapter;
import com.diodeit.spendtrack.databases.DatabaseHelper;
import com.diodeit.spendtrack.models.Expense;
import com.diodeit.spendtrack.models.ExpenseItem;
import com.diodeit.spendtrack.models.Loan;
import com.diodeit.spendtrack.utils.BengaliNumberConverter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LoansFragment extends Fragment {

    private TextView tvTotalTaken, tvTotalGiven;
    private RecyclerView rvLoans;
    private LinearLayout llEmpty;
    private MaterialButton btnAddLoan;
    private Toolbar toolbar;

    private LoanAdapter adapter;
    private DatabaseHelper db;
    private BengaliNumberConverter bn;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_loans, container, false);

        db = new DatabaseHelper(requireContext());
        bn = new BengaliNumberConverter();

        initViews(view);
        setupRecycler();
        loadLoans();

        return view;
    }

    private void initViews(View v) {
        tvTotalTaken = v.findViewById(R.id.tv_total_taken);
        tvTotalGiven = v.findViewById(R.id.tv_total_given);
        rvLoans      = v.findViewById(R.id.rv_loans);
        llEmpty      = v.findViewById(R.id.ll_empty_state);
        btnAddLoan   = v.findViewById(R.id.btn_add_loan);
        toolbar      = v.findViewById(R.id.toolbar);

        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(x -> requireActivity().onBackPressed());
        }

        btnAddLoan.setOnClickListener(x -> showAddLoanDialog(null));
    }

    private void setupRecycler() {
        adapter = new LoanAdapter(requireContext(), new ArrayList<>(),
                new LoanAdapter.OnLoanAction() {
                    @Override public void onEdit(Loan loan)   { showAddLoanDialog(loan); }
                    @Override public void onPay(Loan loan)    { showPayDialog(loan); }
                    @Override public void onDelete(Loan loan) { confirmDelete(loan); }
                });
        rvLoans.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvLoans.setAdapter(adapter);
    }

    private void loadLoans() {
        List<Loan> all = db.getAllLoans();
        adapter = new LoanAdapter(requireContext(), all, new LoanAdapter.OnLoanAction() {
            @Override public void onEdit(Loan loan)   { showAddLoanDialog(loan); }
            @Override public void onPay(Loan loan)    { showPayDialog(loan); }
            @Override public void onDelete(Loan loan) { confirmDelete(loan); }
        });
        rvLoans.setAdapter(adapter);

        llEmpty.setVisibility(all.isEmpty() ? View.VISIBLE : View.GONE);

        double taken = db.getTotalTakenRemaining();
        double given = db.getTotalGivenRemaining();

        tvTotalTaken.setText("৳ " + bn.toBengali(
                String.format(Locale.US, "%,.0f", taken)));
        tvTotalGiven.setText("৳ " + bn.toBengali(
                String.format(Locale.US, "%,.0f", given)));
    }

    // ═══════════════════════════════════════════════════════
    // ADD / EDIT
    // ═══════════════════════════════════════════════════════
    private void showAddLoanDialog(@Nullable Loan existing) {
        View v = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_loan, null, false);

        RadioButton rbTaken = v.findViewById(R.id.rb_taken);
        RadioButton rbGiven = v.findViewById(R.id.rb_given);
        EditText etPerson   = v.findViewById(R.id.et_person);
        EditText etAmount   = v.findViewById(R.id.et_amount);
        EditText etNote     = v.findViewById(R.id.et_note);

        boolean editing = existing != null;

        if (editing) {
            if (existing.isTaken()) rbTaken.setChecked(true);
            else                    rbGiven.setChecked(true);

            etPerson.setText(existing.getPersonName());
            etAmount.setText(String.format(Locale.US, "%.0f", existing.getPrincipalAmount()));
            etNote.setText(existing.getNote());

            etAmount.setEnabled(false);
            etAmount.setAlpha(0.6f);
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(editing ? "ঋণ এডিট করুন" : "নতুন ঋণ")
                .setView(v)
                .setPositiveButton(editing ? "আপডেট" : "সংরক্ষণ", (d, w) -> {
                    String person = etPerson.getText().toString().trim();
                    String amtStr = etAmount.getText().toString().trim();
                    String note   = etNote.getText().toString().trim();

                    if (person.isEmpty() || amtStr.isEmpty()) {
                        Toast.makeText(requireContext(),
                                "নাম ও পরিমাণ দিন", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    double amount;
                    try {
                        amount = Double.parseDouble(bn.toEnglish(amtStr));
                    } catch (NumberFormatException ex) {
                        Toast.makeText(requireContext(),
                                "সঠিক পরিমাণ দিন", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (amount <= 0) {
                        Toast.makeText(requireContext(),
                                "পরিমাণ ০ এর চেয়ে বড় হতে হবে", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Loan loan = editing ? existing : new Loan();
                    loan.setType(rbTaken.isChecked() ? "taken" : "given");
                    loan.setPersonName(person);
                    loan.setPrincipalAmount(amount);
                    loan.setNote(note);

                    if (editing) {
                        // ─── Edit mode ───
                        db.updateLoan(loan);
                        Toast.makeText(requireContext(),
                                "আপডেট হয়েছে", Toast.LENGTH_SHORT).show();
                    } else {
                        // ─── New loan ───
                        loan.setDate(System.currentTimeMillis());
                        loan.setPaidAmount(0);
                        loan.setClosed(false);

                        long loanId = db.addLoan(loan);
                        loan.setId(loanId);

                        // ★ শুধু "আমি ঋণ দিয়েছি" হলে টাকা কমবে → Expense তৈরি
                        //   "আমি ঋণ নিয়েছি" হলে কিছুই হবে না
                        if (loan.isGiven()) {
                            Expense ledger = new Expense();
                            ledger.setAmount(loan.getPrincipalAmount());
                            ledger.setCategory("ঋণ");
                            ledger.setPaymentMethod("ক্যাশ");
                            ledger.setDate(loan.getDate());
                            ledger.setType("expense");

                            String action = "ঋণ দিয়েছি — " + loan.getPersonName();
                            ledger.setNote(action);

                            List<ExpenseItem> items = new ArrayList<>();
                            items.add(new ExpenseItem(action, loan.getPrincipalAmount()));
                            ledger.setItems(items);

                            long expId = db.addExpense(ledger);

                            // link সেভ
                            loan.setLinkedExpenseId(expId);
                            db.updateLoan(loan);
                        }

                        Toast.makeText(requireContext(),
                                "ঋণ যোগ করা হয়েছে", Toast.LENGTH_SHORT).show();
                    }
                    loadLoans();
                })
                .setNegativeButton("বাতিল", null)
                .show();
    }

    // ═══════════════════════════════════════════════════════
    // PAY / RECEIVE
    // ═══════════════════════════════════════════════════════
    private void showPayDialog(Loan loan) {
        View v = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_loan, null, false);

        v.findViewById(R.id.rg_loan_type).setVisibility(View.GONE);
        v.findViewById(R.id.et_person).setVisibility(View.GONE);
        v.findViewById(R.id.et_note).setVisibility(View.GONE);

        EditText etAmount = v.findViewById(R.id.et_amount);
        etAmount.setHint("পরিশোধের পরিমাণ (৳)");
        etAmount.setText(String.format(Locale.US, "%.0f", loan.getRemaining()));

        String title = loan.isTaken()
                ? "ঋণ পরিশোধ করুন"
                : "ঋণ ফেরত পেয়েছি";

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(title)
                .setMessage(loan.getPersonName() + " — বাকি: ৳ "
                        + bn.toBengali(String.format(Locale.US, "%,.0f", loan.getRemaining())))
                .setView(v)
                .setPositiveButton("নিশ্চিত", (d, w) -> {
                    String amtStr = etAmount.getText().toString().trim();
                    if (amtStr.isEmpty()) {
                        Toast.makeText(requireContext(),
                                "পরিমাণ দিন", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    double pay;
                    try {
                        pay = Double.parseDouble(bn.toEnglish(amtStr));
                    } catch (NumberFormatException ex) {
                        Toast.makeText(requireContext(),
                                "সঠিক পরিমাণ দিন", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (pay <= 0) {
                        Toast.makeText(requireContext(),
                                "পরিমাণ ০ এর চেয়ে বড় হতে হবে", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double newPaid = loan.getPaidAmount() + pay;
                    if (newPaid > loan.getPrincipalAmount()) {
                        newPaid = loan.getPrincipalAmount();
                    }
                    loan.setPaidAmount(newPaid);
                    loan.setClosed(newPaid >= loan.getPrincipalAmount());

                    // ★ Loan আপডেট
                    db.updateLoan(loan);

                    // ★ শুধু "আমি ঋণ দিয়েছি" হলে ফেরত পেলে টাকা বাড়বে → Income তৈরি
                    //   "আমি ঋণ নিয়েছি" হলে পরিশোধ করলেও কিছুই হবে না
                    if (loan.isGiven()) {
                        Expense payment = new Expense();
                        payment.setAmount(pay);
                        payment.setCategory("ঋণ পরিশোধ");
                        payment.setPaymentMethod("ক্যাশ");
                        payment.setDate(System.currentTimeMillis());
                        payment.setType("income");

                        String action = "ঋণ ফেরত পেয়েছি — " + loan.getPersonName();
                        payment.setNote(action);

                        List<ExpenseItem> items = new ArrayList<>();
                        items.add(new ExpenseItem(action, pay));
                        payment.setItems(items);

                        db.addExpense(payment);
                    }

                    loadLoans();
                    Toast.makeText(requireContext(),
                            "পরিশোধ রেকর্ড হয়েছে", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("বাতিল", null)
                .show();
    }

    // ═══════════════════════════════════════════════════════
    // DELETE
    // ═══════════════════════════════════════════════════════
    private void confirmDelete(Loan loan) {
        String msg = loan.isGiven()
                ? loan.getPersonName() + " এর ঋণ মুছে ফেলতে চান?\n\nএটি সংশ্লিষ্ট সব খরচ/আয়ের হিসাবও মুছে ফেলবে।"
                : loan.getPersonName() + " এর ঋণ মুছে ফেলতে চান?";

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("ঋণ মুছুন")
                .setMessage(msg)
                .setPositiveButton("মুছে ফেলুন", (d, w) -> {
                    db.deleteLoan(loan.getId());
                    loadLoans();
                    Toast.makeText(requireContext(),
                            "ঋণ মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("বাতিল", null)
                .show();
    }
}