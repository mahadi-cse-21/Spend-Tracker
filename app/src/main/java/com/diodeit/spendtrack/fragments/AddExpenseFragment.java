package com.diodeit.spendtrack.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;

import com.diodeit.spendtrack.MainActivity;
import com.diodeit.spendtrack.R;
import com.diodeit.spendtrack.adapters.AddedItemAdapter;
import com.diodeit.spendtrack.adapters.CategoryAdapter;
import com.diodeit.spendtrack.databases.DatabaseHelper;
import com.diodeit.spendtrack.models.Category;
import com.diodeit.spendtrack.models.Expense;
import com.diodeit.spendtrack.models.ExpenseItem;
import com.diodeit.spendtrack.utils.BengaliNumberConverter;
import com.diodeit.spendtrack.utils.DateUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AddExpenseFragment extends Fragment {

    private EditText etAmount, etName;
    private TextView tvSelectedDate, tvSelectedCategory, tvTotalAmount, tvAddedLabel;
    private View btnSelectDate, llTotal;
    private RecyclerView rvCategories, rvAddedItems;
    private MaterialButton btnAddItem, btnSave;
    private TabLayout tabType;
    private MaterialToolbar toolbar;

    private long selectedTimestamp;
    private String selectedCategory;
    private String selectedType = "expense";
    private final List<ExpenseItem> addedItems = new ArrayList<>();
    private AddedItemAdapter addedAdapter;

    private BengaliNumberConverter bn;
    private DatabaseHelper dbHelper;

    private boolean isConverting = false;
    private android.text.TextWatcher amountWatcher;

    private long editExpenseId = -1;
    private Expense editingExpense;

    public static AddExpenseFragment newInstanceForEdit(long expenseId) {
        AddExpenseFragment f = new AddExpenseFragment();
        Bundle args = new Bundle();
        args.putLong("expense_id", expenseId);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_expense, container, false);

        bn = new BengaliNumberConverter();
        dbHelper = new DatabaseHelper(requireContext());
        selectedTimestamp = System.currentTimeMillis();

        Bundle args = getArguments();
        if (args != null && args.getLong("expense_id", -1) > 0) {
            editExpenseId = args.getLong("expense_id", -1);
            editingExpense = dbHelper.getExpense(editExpenseId);
            if (editingExpense != null) {
                selectedType = editingExpense.getType() != null
                        ? editingExpense.getType() : "expense";
            }
        }

        if (editExpenseId <= 0 && args != null) {
            String dt = args.getString("default_type", "expense");
            selectedType = "income".equals(dt) ? "income" : "expense";
        }

        initViews(view);
        setupTypeToggle();
        setupCategoriesFor(selectedType);
        setupAddedList();
        setupDatePicker();
        setupAddButton();
        setupSaveButton();

        if (tabType != null) {
            int tabIndex = selectedType.equals("income") ? 1 : 0;
            TabLayout.Tab tab = tabType.getTabAt(tabIndex);
            if (tab != null) tab.select();
        }

        if (editingExpense != null) {
            prefillFromEditingExpense();
        } else {
            updateDateDisplay();
            updateTotalAndVisibility();
        }

        return view;
    }

    private void initViews(View view) {
        etAmount           = view.findViewById(R.id.et_amount);
        etName             = view.findViewById(R.id.et_name);
        tvSelectedDate     = view.findViewById(R.id.tv_selected_date);
        tvSelectedCategory = view.findViewById(R.id.tv_selected_category);
        tvTotalAmount      = view.findViewById(R.id.tv_total_amount);
        tvAddedLabel       = view.findViewById(R.id.tv_added_label);
        btnSelectDate      = view.findViewById(R.id.btn_select_date);
        llTotal            = view.findViewById(R.id.ll_total);
        rvCategories       = view.findViewById(R.id.rv_categories);
        rvAddedItems       = view.findViewById(R.id.rv_added_items);
        btnAddItem         = view.findViewById(R.id.btn_add_item);
        btnSave            = view.findViewById(R.id.btn_save_expense);
        tabType            = view.findViewById(R.id.tab_type);
        toolbar            = view.findViewById(R.id.toolbar);

        if (editExpenseId > 0 && toolbar != null) {
            toolbar.setTitle("এডিট করুন");
        }

        amountWatcher = new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (isConverting) return;
                String original = s.toString();
                String bengali = convertEnglishToBengali(original);
                if (!original.equals(bengali)) {
                    isConverting = true;
                    etAmount.setText(bengali);
                    etAmount.setSelection(bengali.length());
                    isConverting = false;
                }
            }
        };
        etAmount.addTextChangedListener(amountWatcher);

        final NestedScrollView scrollView = view.findViewById(R.id.scroll_view);
        View.OnFocusChangeListener scrollToFocused = (v, hasFocus) -> {
            if (hasFocus && scrollView != null) {
                v.postDelayed(() -> {
                    int y = v.getTop();
                    scrollView.smoothScrollTo(0, Math.max(0, y - 100));
                }, 250);
            }
        };
        etAmount.setOnFocusChangeListener(scrollToFocused);
        etName.setOnFocusChangeListener(scrollToFocused);
    }

    private void prefillFromEditingExpense() {
        if (editingExpense == null) return;

        selectedType = editingExpense.getType() != null ? editingExpense.getType() : "expense";
        if (tabType != null) {
            int tabIndex = selectedType.equals("income") ? 1 : 0;
            TabLayout.Tab tab = tabType.getTabAt(tabIndex);
            if (tab != null) tab.select();
        }

        setupCategoriesFor(selectedType);
        selectedCategory = editingExpense.getCategory();
        if (tvSelectedCategory != null) {
            tvSelectedCategory.setText("নির্বাচিত: " + editingExpense.getCategory());
        }

        selectedTimestamp = editingExpense.getDate();
        updateDateDisplay();

        addedItems.clear();
        if (editingExpense.getItems() != null && !editingExpense.getItems().isEmpty()) {
            addedItems.addAll(editingExpense.getItems());
        } else {
            addedItems.add(new ExpenseItem(
                    editingExpense.getNote() != null ? editingExpense.getNote() : "",
                    editingExpense.getAmount()));
        }
        if (addedAdapter != null) addedAdapter.notifyDataSetChanged();
        updateTotalAndVisibility();

        if (btnSave != null) {
            btnSave.setText(selectedType.equals("income")
                    ? "আয় আপডেট করুন" : "খরচ আপডেট করুন");
        }
    }

    private void setupTypeToggle() {
        if (tabType == null) return;
        tabType.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                selectedType = (tab.getPosition() == 0) ? "expense" : "income";

                if (editExpenseId <= 0) {
                    addedItems.clear();
                    if (addedAdapter != null) addedAdapter.notifyDataSetChanged();
                    updateTotalAndVisibility();
                }

                setupCategoriesFor(selectedType);

                if (btnSave != null) {
                    if (editExpenseId > 0) {
                        btnSave.setText(selectedType.equals("income")
                                ? "আয় আপডেট করুন" : "খরচ আপডেট করুন");
                    } else {
                        btnSave.setText(selectedType.equals("income")
                                ? "আয় সংরক্ষণ করুন" : "খরচ সংরক্ষণ করুন");
                    }
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupCategoriesFor(String type) {
        Category[] categories;
        if ("income".equals(type)) {
            categories = new Category[]{
                    new Category("বেতন", "বেতন", "💼"),
                    new Category("বোনাস", "বোনাস", "🎁"),
                    new Category("ফ্রিল্যান্স", "ফ্রিল্যান্স", "💻"),
                    new Category("ব্যবসা", "ব্যবসা", "🏢"),
                    new Category("উপহার", "উপহার", "🎀"),
                    new Category("সুদ", "সুদ", "💰"),
                    new Category("ভাড়া আয়", "ভাড়া", "🏘️"),
                    new Category("অন্যান্য আয়", "অন্যান্য", "⚙️")
            };
        } else {
            categories = new Category[]{
                    new Category(getString(R.string.cat_food),      getString(R.string.cat_food_short),      "🍔"),
                    new Category(getString(R.string.cat_grocery),   getString(R.string.cat_grocery_short),   "🛒"),
                    new Category(getString(R.string.cat_transport), getString(R.string.cat_transport_short), "🚗"),
                    new Category(getString(R.string.cat_rent),      getString(R.string.cat_rent_short),      "🏠"),
                    new Category(getString(R.string.cat_health),    getString(R.string.cat_health_short),    "💊"),
                    new Category(getString(R.string.cat_shopping),  getString(R.string.cat_shopping_short),  "🛍️"),
                    new Category(getString(R.string.cat_education), getString(R.string.cat_education_short), "📚"),
                    new Category(getString(R.string.cat_other),     getString(R.string.cat_other_short),     "⚙️")
            };
        }

        CategoryAdapter adapter = new CategoryAdapter(
                requireContext(), categories, category -> {
            selectedCategory = category.getName();
            tvSelectedCategory.setText("নির্বাচিত: " + category.getShortName());
        });

        rvCategories.setLayoutManager(new GridLayoutManager(requireContext(), 4));
        rvCategories.setAdapter(adapter);

        if (editExpenseId <= 0) {
            selectedCategory = categories[0].getName();
            tvSelectedCategory.setText("নির্বাচিত: " + categories[0].getShortName());
        }
    }

    private void setupAddedList() {
        addedAdapter = new AddedItemAdapter(
                requireContext(),
                addedItems,
                position -> {
                    if (position >= 0 && position < addedItems.size()) {
                        addedItems.remove(position);
                        addedAdapter.notifyItemRemoved(position);
                        addedAdapter.notifyItemRangeChanged(position, addedItems.size());
                        updateTotalAndVisibility();
                    }
                },
                position -> showEditItemDialog(position)
        );
        rvAddedItems.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvAddedItems.setAdapter(addedAdapter);
    }

    private void showEditItemDialog(int index) {
        if (index < 0 || index >= addedItems.size()) return;
        ExpenseItem item = addedItems.get(index);

        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad, pad, 0);

        EditText etNameD = new EditText(requireContext());
        etNameD.setHint("আইটেমের নাম");
        etNameD.setText(item.getName());
        container.addView(etNameD);

        EditText etAmountD = new EditText(requireContext());
        etAmountD.setHint("পরিমাণ");
        etAmountD.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
                | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etAmountD.setText(String.format(Locale.US, "%.0f", item.getAmount()));
        container.addView(etAmountD);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("আইটেম এডিট করুন")
                .setView(container)
                .setPositiveButton("সংরক্ষণ", (d, w) -> {
                    String newName = etNameD.getText().toString().trim();
                    String amtStr = etAmountD.getText().toString().trim();
                    if (newName.isEmpty() || amtStr.isEmpty()) {
                        Toast.makeText(requireContext(),
                                "নাম ও পরিমাণ দিন", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    double newAmount;
                    try {
                        newAmount = Double.parseDouble(bn.toEnglish(amtStr));
                    } catch (NumberFormatException ex) {
                        Toast.makeText(requireContext(),
                                "সঠিক পরিমাণ দিন", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    item.setName(newName);
                    item.setAmount(newAmount);
                    addedAdapter.notifyItemChanged(index);
                    updateTotalAndVisibility();
                    Toast.makeText(requireContext(),
                            "আইটেম আপডেট হয়েছে", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("বাতিল", null)
                .show();
    }

    private void setupDatePicker() {
        if (btnSelectDate == null) return;
        btnSelectDate.setOnClickListener(v -> {
            MaterialDatePicker<Long> picker =
                    MaterialDatePicker.Builder.datePicker()
                            .setTitleText("তারিখ নির্বাচন করুন")
                            .setSelection(selectedTimestamp)
                            .build();
            picker.addOnPositiveButtonClickListener(sel -> {
                selectedTimestamp = sel;
                updateDateDisplay();
            });
            picker.show(getParentFragmentManager(), "DATE_PICKER");
        });
    }

    private void updateDateDisplay() {
        if (tvSelectedDate == null) return;
        Calendar sel = Calendar.getInstance();
        sel.setTimeInMillis(selectedTimestamp);
        Calendar today = Calendar.getInstance();
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);

        boolean isToday = sel.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                sel.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR);
        boolean isYesterday = sel.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
                sel.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR);

        String dateStr;
        if (isToday) {
            dateStr = "আজ, " + DateUtils.getBengaliMonth(sel.get(Calendar.MONTH))
                    + " " + bn.toBengali(sel.get(Calendar.DAY_OF_MONTH));
        } else if (isYesterday) {
            dateStr = "গতকাল, " + DateUtils.getBengaliMonth(sel.get(Calendar.MONTH))
                    + " " + bn.toBengali(sel.get(Calendar.DAY_OF_MONTH));
        } else {
            dateStr = bn.toBengali(sel.get(Calendar.DAY_OF_MONTH)) + " "
                    + DateUtils.getBengaliMonth(sel.get(Calendar.MONTH)) + " "
                    + bn.toBengali(sel.get(Calendar.YEAR));
        }
        tvSelectedDate.setText(dateStr);
    }

    private void setupAddButton() {
        btnAddItem.setOnClickListener(v -> {
            String amountStr = etAmount.getText().toString().trim();
            String name = etName.getText().toString().trim();

            if (amountStr.isEmpty()) {
                Toast.makeText(requireContext(), "পরিমাণ লিখুন", Toast.LENGTH_SHORT).show();
                return;
            }
            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "নাম লিখুন", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount = parseAmount(amountStr);
            if (amount <= 0) {
                Toast.makeText(requireContext(),
                        "সঠিক পরিমাণ দিন", Toast.LENGTH_SHORT).show();
                return;
            }

            addedItems.add(new ExpenseItem(name, amount));
            addedAdapter.notifyItemInserted(addedItems.size() - 1);

            etAmount.setText("");
            etName.setText("");
            etAmount.requestFocus();

            updateTotalAndVisibility();
            Toast.makeText(requireContext(), "যোগ করা হয়েছে", Toast.LENGTH_SHORT).show();
        });
    }

    private double parseAmount(String input) {
        if (input == null) return 0;
        String english = bn.toEnglish(input.trim());
        try { return Double.parseDouble(english); }
        catch (NumberFormatException e) { return 0; }
    }

    private String convertEnglishToBengali(String input) {
        if (input == null || input.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (c >= '0' && c <= '9') sb.append((char) ('০' + (c - '0')));
            else sb.append(c);
        }
        return sb.toString();
    }

    private void setupSaveButton() {
        btnSave.setText(editExpenseId > 0 ? "খরচ আপডেট করুন" : "খরচ সংরক্ষণ করুন");

        btnSave.setOnClickListener(v -> {
            if (addedItems.isEmpty()) {
                String amountStr = etAmount.getText().toString().trim();
                String name = etName.getText().toString().trim();
                if (amountStr.isEmpty() || name.isEmpty()) {
                    Toast.makeText(requireContext(),
                            "কমপক্ষে একটি আইটেম যোগ করুন", Toast.LENGTH_SHORT).show();
                    return;
                }
                double amount = parseAmount(amountStr);
                if (amount <= 0) {
                    Toast.makeText(requireContext(),
                            "সঠিক পরিমাণ দিন", Toast.LENGTH_SHORT).show();
                    return;
                }
                addedItems.add(new ExpenseItem(name, amount));
            }

            double total = 0;
            for (ExpenseItem i : addedItems) total += i.getAmount();

            Expense expense = new Expense();
            expense.setAmount(total);
            expense.setCategory(selectedCategory);
            expense.setPaymentMethod("ক্যাশ");
            expense.setNote(addedItems.get(0).getName());
            expense.setDate(selectedTimestamp);
            expense.setItems(new ArrayList<>(addedItems));
            expense.setType(selectedType);

            if (editExpenseId > 0) {
                int rows = dbHelper.updateExpense(editExpenseId, expense);
                if (rows > 0) {
                    Toast.makeText(requireContext(),
                            "সফলভাবে আপডেট হয়েছে", Toast.LENGTH_SHORT).show();
                    requireActivity().onBackPressed();
                } else {
                    Toast.makeText(requireContext(),
                            "আপডেট ব্যর্থ হয়েছে", Toast.LENGTH_SHORT).show();
                }
            } else {
                long id = dbHelper.addExpense(expense);
                if (id > 0) {
                    String msg = selectedType.equals("income")
                            ? addedItems.size() + "টি আয় সংরক্ষিত হয়েছে"
                            : addedItems.size() + "টি খরচ সংরক্ষিত হয়েছে";
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();

                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) requireActivity()).navigateToTab("HOME");
                    }
                } else {
                    Toast.makeText(requireContext(),
                            "সংরক্ষণ ব্যর্থ হয়েছে", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateTotalAndVisibility() {
        boolean hasItems = !addedItems.isEmpty();
        if (tvAddedLabel != null) tvAddedLabel.setVisibility(hasItems ? View.VISIBLE : View.GONE);
        if (llTotal != null) llTotal.setVisibility(hasItems ? View.VISIBLE : View.GONE);

        double total = 0;
        for (ExpenseItem i : addedItems) total += i.getAmount();
        if (tvTotalAmount != null) {
            tvTotalAmount.setText("৳ " + bn.toBengali(
                    String.format(Locale.US, "%,.0f", total)));
        }
    }
}