package com.diodeit.spendtrack.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

import com.diodeit.spendtrack.MainActivity;
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

    private static final String ARG_AUTO_EDIT_LOAN_ID = "auto_edit_loan_id";

    private TextView tvTotalTaken, tvTotalGiven;
    private RecyclerView rvLoans;
    private LinearLayout llEmpty;
    private MaterialButton btnAddLoan;
    private Toolbar toolbar;

    private LoanAdapter adapter;
    private DatabaseHelper db;
    private BengaliNumberConverter bn;

    private long autoEditLoanId = -1;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private int lastLoadedVersion = -1;

    public static LoansFragment newInstanceForEdit(long loanId) {
        LoansFragment f = new LoansFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_AUTO_EDIT_LOAN_ID, loanId);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            autoEditLoanId = getArguments().getLong(ARG_AUTO_EDIT_LOAN_ID, -1);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_loans, container, false);

        db = DatabaseHelper.get(requireContext());
        bn = new BengaliNumberConverter();

        initViews(view);
        setupRecycler();
        loadLoans();

        if (autoEditLoanId > 0) {
            view.post(() -> tryOpenEditorFor(autoEditLoanId));
        }
        return view;
    }

    private void tryOpenEditorFor(long loanId) {
        MainActivity.io().execute(() -> {
            Loan loan = db.getLoan(loanId);
            mainHandler.post(() -> {
                if (!isAdded()) return;
                if (loan != null) showAddLoanDialog(loan);
                else Toast.makeText(requireContext(), "ঋণ পাওয়া যায়নি", Toast.LENGTH_SHORT).show();
            });
        });
        autoEditLoanId = -1;
    }

    private void initViews(View v) {
        tvTotalTaken = v.findViewById(R.id.tv_total_taken);
        tvTotalGiven = v.findViewById(R.id.tv_total_given);
        rvLoans      = v.findViewById(R.id.rv_loans);
        llEmpty      = v.findViewById(R.id.ll_empty_state);
        btnAddLoan   = v.findViewById(R.id.btn_add_loan);
        toolbar      = v.findViewById(R.id.toolbar);

        if (toolbar != null) toolbar.setNavigationOnClickListener(x -> requireActivity().onBackPressed());
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
        int v = db.getDataVersion();
        if (v == lastLoadedVersion) return;
        lastLoadedVersion = v;

        MainActivity.io().execute(() -> {
            List<Loan> all = db.getAllLoans();
            double taken = 0, given = 0;
            for (Loan l : all) {
                if (l.isClosed()) continue;
                if (l.isTaken()) taken += l.getRemaining();
                else             given += l.getRemaining();
            }
            final double fTaken = taken, fGiven = given;

            mainHandler.post(() -> {
                if (!isAdded() || getView() == null) return;

                adapter = new LoanAdapter(requireContext(), all,
                        new LoanAdapter.OnLoanAction() {
                            @Override public void onEdit(Loan loan)   { showAddLoanDialog(loan); }
                            @Override public void onPay(Loan loan)    { showPayDialog(loan); }
                            @Override public void onDelete(Loan loan) { confirmDelete(loan); }
                        });
                rvLoans.setAdapter(adapter);

                llEmpty.setVisibility(all.isEmpty() ? View.VISIBLE : View.GONE);
                tvTotalTaken.setText("৳ " + bn.toBengali(String.format(Locale.US, "%,.0f", fTaken)));
                tvTotalGiven.setText("৳ " + bn.toBengali(String.format(Locale.US, "%,.0f", fGiven)));
            });
        });
    }

    private void showAddLoanDialog(@Nullable Loan existing) {
        View v = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_loan, null, false);

        RadioButton rbTaken = v.findViewById(R.id.rb_taken);
        RadioButton rbGiven = v.findViewById(R.id.rb_given);
        EditText etPerson   = v.findViewById(R.id.et_person);
        EditText etAmount   = v.findViewById(R.id.et_amount);
        EditText etNote     = v.findViewById(R.id.et_note);

        final boolean editing = (existing != null);

        if (editing) {
            if (existing.isTaken()) rbTaken.setChecked(true);
            else                    rbGiven.setChecked(true);
            etPerson.setText(existing.getPersonName());
            etAmount.setText(String.format(Locale.US, "%.0f", existing.getPrincipalAmount()));
            etNote.setText(existing.getNote() != null ? existing.getNote() : "");
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(editing ? "ঋণ এডিট করুন" : "নতুন ঋণ")
                .setView(v)
                .setPositiveButton(editing ? "আপডেট" : "সংরক্ষণ", (d, w) -> {
                    String person = etPerson.getText().toString().trim();
                    String amtStr = etAmount.getText().toString().trim();
                    String note   = etNote.getText().toString().trim();

                    if (person.isEmpty() || amtStr.isEmpty()) {
                        Toast.makeText(requireContext(), "নাম ও পরিমাণ দিন", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    double amount;
                    try { amount = Double.parseDouble(bn.toEnglish(amtStr)); }
                    catch (NumberFormatException ex) {
                        Toast.makeText(requireContext(), "সঠিক পরিমাণ দিন", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (amount <= 0) {
                        Toast.makeText(requireContext(), "পরিমাণ ০ এর চেয়ে বড় হতে হবে", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    final double fAmount = amount;
                    final String fPerson = person, fNote = note;
                    final boolean fTaken = rbTaken.isChecked();

                    MainActivity.io().execute(() -> {
                        if (editing) {
                            if (fAmount < existing.getPaidAmount()) {
                                mainHandler.post(() -> Toast.makeText(requireContext(),
                                        "পরিমাণ পরিশোধিত পরিমাণের চেয়ে কম হতে পারে না",
                                        Toast.LENGTH_LONG).show());
                                return;
                            }
                            existing.setType(fTaken ? "taken" : "given");
                            existing.setPersonName(fPerson);
                            existing.setNote(fNote);
                            existing.setPrincipalAmount(fAmount);
                            existing.setClosed(existing.getPaidAmount() >= fAmount);
                            db.updateLoan(existing);
                            syncAllLinkedExpenses(existing);
                            mainHandler.post(() -> {
                                if (!isAdded()) return;
                                lastLoadedVersion = -1;
                                Toast.makeText(requireContext(), "আপডেট হয়েছে", Toast.LENGTH_SHORT).show();
                                loadLoans();
                            });
                        } else {
                            Loan loan = new Loan();
                            loan.setType(fTaken ? "taken" : "given");
                            loan.setPersonName(fPerson);
                            loan.setPrincipalAmount(fAmount);
                            loan.setNote(fNote);
                            loan.setDate(System.currentTimeMillis());
                            loan.setPaidAmount(0);
                            loan.setClosed(false);

                            long loanId = db.addLoan(loan);
                            loan.setId(loanId);

                            Expense ledger = new Expense();
                            ledger.setAmount(loan.getPrincipalAmount());
                            ledger.setPaymentMethod("ক্যাশ");
                            ledger.setDate(loan.getDate());
                            ledger.setCategory("ঋণ");
                            ledger.setLoanId(loanId);

                            String action;
                            if (loan.isTaken()) {
                                ledger.setType("income");
                                action = "ঋণ নিয়েছি — " + loan.getPersonName();
                            } else {
                                ledger.setType("expense");
                                action = "ঋণ দিয়েছি — " + loan.getPersonName();
                            }
                            ledger.setNote(action);

                            List<ExpenseItem> items = new ArrayList<>();
                            items.add(new ExpenseItem(action, loan.getPrincipalAmount()));
                            ledger.setItems(items);

                            long expId = db.addExpense(ledger);
                            loan.setLinkedExpenseId(expId);
                            db.updateLoan(loan);

                            mainHandler.post(() -> {
                                if (!isAdded()) return;
                                lastLoadedVersion = -1;
                                Toast.makeText(requireContext(), "ঋণ যোগ করা হয়েছে", Toast.LENGTH_SHORT).show();
                                loadLoans();
                            });
                        }
                    });
                })
                .setNegativeButton("বাতিল", null)
                .show();
    }

    private void syncAllLinkedExpenses(Loan loan) {
        String principalNote, paymentNote;
        if (loan.isTaken()) {
            principalNote = "ঋণ নিয়েছি — " + loan.getPersonName();
            paymentNote   = "ঋণ পরিশোধ — " + loan.getPersonName();
        } else {
            principalNote = "ঋণ দিয়েছি — " + loan.getPersonName();
            paymentNote   = "ঋণ ফেরত পেয়েছি — " + loan.getPersonName();
        }

        Expense principal = loan.getLinkedExpenseId() > 0
                ? db.getExpense(loan.getLinkedExpenseId()) : null;
        if (principal == null) {
            for (Expense e : db.getExpensesByLoanId(loan.getId())) {
                if ("ঋণ".equals(e.getCategory())) { principal = e; break; }
            }
        }
        if (principal != null) {
            principal.setNote(principalNote);
            List<ExpenseItem> items = new ArrayList<>();
            items.add(new ExpenseItem(principalNote, loan.getPrincipalAmount()));
            principal.setItems(items);
            principal.setAmount(loan.getPrincipalAmount());
            principal.setCategory("ঋণ");
            principal.setType(loan.isTaken() ? "income" : "expense");
            principal.setLoanId(loan.getId());
            db.updateExpense(principal.getId(), principal);
            if (loan.getLinkedExpenseId() <= 0) {
                loan.setLinkedExpenseId(principal.getId());
                db.updateLoan(loan);
            }
        }

        for (Expense e : db.getExpensesByLoanId(loan.getId())) {
            if ("ঋণ".equals(e.getCategory())) continue;
            e.setNote(paymentNote);
            List<ExpenseItem> items = new ArrayList<>();
            items.add(new ExpenseItem(paymentNote, e.getAmount()));
            e.setItems(items);
            db.updateExpense(e.getId(), e);
        }
    }

    private void showPayDialog(Loan loan) {
        View v = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_loan_pay, null, false);

        TextView tvPerson    = v.findViewById(R.id.tv_pay_person);
        TextView tvRemaining = v.findViewById(R.id.tv_pay_remaining);
        EditText etAmount    = v.findViewById(R.id.et_pay_amount);

        tvPerson.setText(loan.getPersonName());
        tvRemaining.setText("বাকি: ৳ " + bn.toBengali(
                String.format(Locale.US, "%,.0f", loan.getRemaining())));
        etAmount.setText(String.format(Locale.US, "%.0f", loan.getRemaining()));

        String title = loan.isTaken() ? "ঋণ পরিশোধ করুন" : "ঋণ ফেরত পেয়েছি";

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(title)
                .setView(v)
                .setPositiveButton("নিশ্চিত", (d, w) -> {
                    String amtStr = etAmount.getText().toString().trim();
                    if (amtStr.isEmpty()) {
                        Toast.makeText(requireContext(), "পরিমাণ দিন", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    double pay;
                    try { pay = Double.parseDouble(bn.toEnglish(amtStr)); }
                    catch (NumberFormatException ex) {
                        Toast.makeText(requireContext(), "সঠিক পরিমাণ দিন", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (pay <= 0) {
                        Toast.makeText(requireContext(), "পরিমাণ ০ এর চেয়ে বড় হতে হবে", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    final double fPay = pay;
                    MainActivity.io().execute(() -> {
                        double newPaid = loan.getPaidAmount() + fPay;
                        if (newPaid > loan.getPrincipalAmount()) newPaid = loan.getPrincipalAmount();
                        loan.setPaidAmount(newPaid);
                        loan.setClosed(newPaid >= loan.getPrincipalAmount());
                        db.updateLoan(loan);

                        Expense payment = new Expense();
                        payment.setAmount(fPay);
                        payment.setPaymentMethod("ক্যাশ");
                        payment.setDate(System.currentTimeMillis());
                        payment.setLoanId(loan.getId());

                        String action;
                        if (loan.isTaken()) {
                            payment.setType("expense");
                            payment.setCategory("ঋণ পরিশোধ");
                            action = "ঋণ পরিশোধ — " + loan.getPersonName();
                        } else {
                            payment.setType("income");
                            payment.setCategory("ঋণ পরিশোধ");
                            action = "ঋণ ফেরত পেয়েছি — " + loan.getPersonName();
                        }
                        payment.setNote(action);

                        List<ExpenseItem> items = new ArrayList<>();
                        items.add(new ExpenseItem(action, fPay));
                        payment.setItems(items);
                        db.addExpense(payment);

                        mainHandler.post(() -> {
                            if (!isAdded()) return;
                            lastLoadedVersion = -1;
                            loadLoans();
                            Toast.makeText(requireContext(), "পরিশোধ রেকর্ড হয়েছে", Toast.LENGTH_SHORT).show();
                        });
                    });
                })
                .setNegativeButton("বাতিল", null)
                .show();
    }

    private void confirmDelete(Loan loan) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("ঋণ মুছুন")
                .setMessage(loan.getPersonName() + " এর ঋণ মুছে ফেলতে চান?\n\n" +
                        "এটি সংশ্লিষ্ট সব খরচ/আয়ের হিসাবও মুছে ফেলবে।")
                .setPositiveButton("মুছে ফেলুন", (d, w) -> {
                    MainActivity.io().execute(() -> {
                        db.deleteLoan(loan.getId());
                        mainHandler.post(() -> {
                            if (!isAdded()) return;
                            lastLoadedVersion = -1;
                            loadLoans();
                            Toast.makeText(requireContext(), "ঋণ মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show();
                        });
                    });
                })
                .setNegativeButton("বাতিল", null)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadLoans();
    }
}