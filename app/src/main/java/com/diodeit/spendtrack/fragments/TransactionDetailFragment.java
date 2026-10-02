package com.diodeit.spendtrack.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.diodeit.spendtrack.MainActivity;
import com.diodeit.spendtrack.models.ExpenseItem;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.diodeit.spendtrack.R;
import com.diodeit.spendtrack.databases.DatabaseHelper;
import com.diodeit.spendtrack.models.Expense;
import com.diodeit.spendtrack.utils.BengaliNumberConverter;
import com.diodeit.spendtrack.utils.CategoryUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TransactionDetailFragment extends Fragment {

    private ImageView ivCategoryIcon;
    private TextView tvCategoryName, tvAmount, tvDatetime, tvReceiptId;
    private TextView tvPaymentMethod, tvWallet, tvVendor, tvNote;
    private LinearLayout llWalletRow, llVendorRow, llNoteRow, llTagsRow;
    private ChipGroup cgTags;
    private MaterialButton btnEdit, btnDownload;

    private BengaliNumberConverter bnConverter;
    private DatabaseHelper dbHelper;
    private Expense expense;
    private long expenseId = -1;

    public static TransactionDetailFragment newInstance(long expenseId) {
        TransactionDetailFragment fragment = new TransactionDetailFragment();
        Bundle args = new Bundle();
        args.putLong("expense_id", expenseId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            expenseId = getArguments().getLong("expense_id");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_transaction_detail, container, false);

        bnConverter = new BengaliNumberConverter();
        dbHelper = new DatabaseHelper(requireContext());

        if (expenseId > 0) {
            expense = dbHelper.getExpense(expenseId);
        }

        if (expense == null) {
            Toast.makeText(requireContext(),
                    "লেনদেন খুঁজে পাওয়া যায়নি", Toast.LENGTH_SHORT).show();
            requireActivity().onBackPressed();
            return view;
        }

        initViews(view);
        loadData(view);
        setupClickListeners();

        return view;
    }

    private void initViews(View view) {
        ivCategoryIcon  = view.findViewById(R.id.iv_category_icon);
        tvCategoryName  = view.findViewById(R.id.tv_category_name);
        tvAmount        = view.findViewById(R.id.tv_amount);
        tvDatetime      = view.findViewById(R.id.tv_datetime);
        tvReceiptId     = view.findViewById(R.id.tv_receipt_id);
        tvPaymentMethod = view.findViewById(R.id.tv_payment_method);
        tvWallet        = view.findViewById(R.id.tv_wallet);
        tvVendor        = view.findViewById(R.id.tv_vendor);
        tvNote          = view.findViewById(R.id.tv_note);

        llWalletRow     = view.findViewById(R.id.ll_wallet_row);
        llVendorRow     = view.findViewById(R.id.ll_vendor_row);
        llNoteRow       = view.findViewById(R.id.ll_note_row);
        llTagsRow       = view.findViewById(R.id.ll_tags_row);
        cgTags          = view.findViewById(R.id.cg_tags);

        btnEdit         = view.findViewById(R.id.btn_edit);

        androidx.appcompat.widget.Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());
        toolbar.inflateMenu(R.menu.menu_transaction_detail);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_delete) {
                confirmDelete();
                return true;
            }
            return false;
        });
    }

    private void loadData(View root) {
        boolean isIncome = expense.isIncome();

        tvCategoryName.setText(expense.getItemsSummary());

        String amountText = bnConverter.toBengali(
                String.format(Locale.US, "%,.0f", expense.getAmount()));
        if (isIncome) {
            tvAmount.setText("+ ৳ " + amountText);
            tvAmount.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary));
        } else {
            tvAmount.setText("- ৳ " + amountText);
            tvAmount.setTextColor(ContextCompat.getColor(requireContext(), R.color.error));
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy • hh:mm a",
                new Locale("bn", "BD"));
        tvDatetime.setText(bnConverter.toBengali(sdf.format(new Date(expense.getDate()))));

        tvReceiptId.setText("#TRX-" + bnConverter.toBengali(String.valueOf(expense.getId())));
        tvPaymentMethod.setText(expense.getPaymentMethod());

        ivCategoryIcon.setImageResource(CategoryUtils.getCategoryIcon(expense.getCategory()));

        TextView tvSectionTitle = root.findViewById(R.id.tv_items_section_title);
        if (tvSectionTitle != null) {
            tvSectionTitle.setText(isIncome ? "আয়ের বিবরণ" : "খরচের বিবরণ");
        }

        LinearLayout llItemsContainer = root.findViewById(R.id.ll_items_container);
        if (llItemsContainer != null) {
            llItemsContainer.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(requireContext());

            if (expense.getItems() != null && !expense.getItems().isEmpty()) {
                for (ExpenseItem item : expense.getItems()) {
                    View row = inflater.inflate(R.layout.item_detail_expense_row,
                            llItemsContainer, false);
                    TextView tvN = row.findViewById(R.id.tv_item_name);
                    TextView tvA = row.findViewById(R.id.tv_item_amount);
                    tvN.setText(item.getName());
                    tvA.setText("৳ " + bnConverter.toBengali(
                            String.format(Locale.US, "%,.0f", item.getAmount())));
                    llItemsContainer.addView(row);
                }
            } else if (expense.getNote() != null && !expense.getNote().isEmpty()) {
                View row = inflater.inflate(R.layout.item_detail_expense_row,
                        llItemsContainer, false);
                TextView tvN = row.findViewById(R.id.tv_item_name);
                TextView tvA = row.findViewById(R.id.tv_item_amount);
                tvN.setText(expense.getNote());
                tvA.setText("৳ " + bnConverter.toBengali(
                        String.format(Locale.US, "%,.0f", expense.getAmount())));
                llItemsContainer.addView(row);
            }
        }

        TextView tvDetailTotal = root.findViewById(R.id.tv_detail_total);
        if (tvDetailTotal != null) {
            tvDetailTotal.setText("৳ " + bnConverter.toBengali(
                    String.format(Locale.US, "%,.0f", expense.getAmount())));
        }

        if (expense.getWallet() != null && !expense.getWallet().isEmpty()) {
            llWalletRow.setVisibility(View.VISIBLE);
            tvWallet.setText(expense.getWallet());
        }
        if (expense.getVendor() != null && !expense.getVendor().isEmpty()) {
            llVendorRow.setVisibility(View.VISIBLE);
            tvVendor.setText(expense.getVendor());
        }
        if (expense.getNote() != null && !expense.getNote().isEmpty()) {
            llNoteRow.setVisibility(View.VISIBLE);
            tvNote.setText(expense.getNote());
        }
        if (expense.getTags() != null && !expense.getTags().isEmpty()) {
            llTagsRow.setVisibility(View.VISIBLE);
            cgTags.removeAllViews();
            String[] tags = expense.getTags().split(",");
            for (String tag : tags) {
                Chip chip = new Chip(requireContext());
                chip.setText(tag.trim());
                cgTags.addView(chip);
            }
        }
    }

    private void setupClickListeners() {
        btnEdit.setOnClickListener(v -> {
            AddExpenseFragment frag = AddExpenseFragment.newInstanceForEdit(expense.getId());
            ((MainActivity) requireActivity()).openDetailFragment(frag, "ADD_EDIT");
        });

    }

    private void confirmDelete() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("মুছে ফেলুন")
                .setMessage("আপনি কি এই লেনদেনটি মুছে ফেলতে চান? এটি ফিরিয়ে আনা যাবে না।")
                .setPositiveButton("মুছে ফেলুন", (d, w) -> {
                    dbHelper.deleteExpense(expense.getId());
                    Toast.makeText(requireContext(),
                            "মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show();
                    requireActivity().onBackPressed();
                })
                .setNegativeButton("বাতিল", null)
                .show();
    }
}