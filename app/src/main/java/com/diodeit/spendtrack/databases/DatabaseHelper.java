package com.diodeit.spendtrack.databases;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.diodeit.spendtrack.models.Budget;
import com.diodeit.spendtrack.models.CategoryBudget;
import com.diodeit.spendtrack.models.Expense;
import com.diodeit.spendtrack.models.ExpenseItem;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "spendtrack.db";
    private static final int DB_VERSION = 4;

    // Expense / income table
    private static final String TABLE_EXPENSE = "expenses";
    private static final String COL_ID = "id";
    private static final String COL_AMOUNT = "amount";
    private static final String COL_CATEGORY = "category";
    private static final String COL_PAYMENT = "payment_method";
    private static final String COL_NOTE = "note";
    private static final String COL_DATE = "date";
    private static final String COL_RECEIPT = "receipt_path";
    private static final String COL_WALLET = "wallet";
    private static final String COL_VENDOR = "vendor";
    private static final String COL_TAGS = "tags";
    private static final String COL_ITEMS_JSON = "items_json";
    private static final String COL_TYPE = "type";

    // Budget table
    private static final String TABLE_BUDGET = "budgets";
    private static final String COL_BUDGET_ID = "id";
    private static final String COL_BUDGET_MONTH = "month";
    private static final String COL_BUDGET_TOTAL = "total_amount";
    private static final String COL_BUDGET_CATEGORY = "category";
    private static final String COL_BUDGET_CAT_AMOUNT = "category_amount";
    private static final String COL_BUDGET_ALERT = "alert_percent";

    private static final String CAT_TOTAL = "__TOTAL__";

    private final Gson gson = new Gson();

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_EXPENSE + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_AMOUNT + " REAL NOT NULL, " +
                COL_CATEGORY + " TEXT, " +
                COL_PAYMENT + " TEXT, " +
                COL_NOTE + " TEXT, " +
                COL_DATE + " INTEGER, " +
                COL_RECEIPT + " TEXT, " +
                COL_WALLET + " TEXT, " +
                COL_VENDOR + " TEXT, " +
                COL_TAGS + " TEXT, " +
                COL_ITEMS_JSON + " TEXT, " +
                COL_TYPE + " TEXT DEFAULT 'expense')");

        db.execSQL("CREATE TABLE " + TABLE_BUDGET + " (" +
                COL_BUDGET_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_BUDGET_MONTH + " TEXT, " +
                COL_BUDGET_TOTAL + " REAL, " +
                COL_BUDGET_CATEGORY + " TEXT, " +
                COL_BUDGET_CAT_AMOUNT + " REAL, " +
                COL_BUDGET_ALERT + " INTEGER DEFAULT 80, " +
                "UNIQUE(" + COL_BUDGET_MONTH + ", " + COL_BUDGET_CATEGORY + "))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            try { db.execSQL("ALTER TABLE " + TABLE_EXPENSE + " ADD COLUMN " + COL_ITEMS_JSON + " TEXT"); } catch (Exception ignored) {}
        }
        if (oldVersion < 4) {
            try { db.execSQL("ALTER TABLE " + TABLE_EXPENSE + " ADD COLUMN " + COL_TYPE + " TEXT DEFAULT 'expense'"); } catch (Exception ignored) {}
        }
    }

    public void clearAllData() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_EXPENSE, null, null);
        db.delete(TABLE_BUDGET, null, null);
        db.close();
    }

    // ================================================================
    // ADD
    // ================================================================
    public long addExpense(Expense expense) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_AMOUNT, expense.getAmount());
        values.put(COL_CATEGORY, expense.getCategory());
        values.put(COL_PAYMENT, expense.getPaymentMethod());
        values.put(COL_NOTE, expense.getNote());
        values.put(COL_DATE, expense.getDate());
        values.put(COL_RECEIPT, expense.getReceiptPath());
        values.put(COL_WALLET, expense.getWallet());
        values.put(COL_VENDOR, expense.getVendor());
        values.put(COL_TAGS, expense.getTags());
        values.put(COL_ITEMS_JSON, gson.toJson(expense.getItems()));
        values.put(COL_TYPE, expense.getType() != null ? expense.getType() : "expense");
        long id = db.insert(TABLE_EXPENSE, null, values);
        db.close();
        return id;
    }

    public int updateExpense(long id, Expense expense) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_AMOUNT, expense.getAmount());
        values.put(COL_CATEGORY, expense.getCategory());
        values.put(COL_PAYMENT, expense.getPaymentMethod());
        values.put(COL_NOTE, expense.getNote());
        values.put(COL_DATE, expense.getDate());
        values.put(COL_RECEIPT, expense.getReceiptPath());
        values.put(COL_WALLET, expense.getWallet());
        values.put(COL_VENDOR, expense.getVendor());
        values.put(COL_TAGS, expense.getTags());
        values.put(COL_ITEMS_JSON, gson.toJson(expense.getItems()));
        values.put(COL_TYPE, expense.getType() != null ? expense.getType() : "expense");

        int rows = db.update(TABLE_EXPENSE, values, COL_ID + "=?",
                new String[]{String.valueOf(id)});
        db.close();
        return rows;
    }
    // ================================================================
    // READ
    // ================================================================
    public Expense getExpense(long id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_EXPENSE, null, COL_ID + "=?",
                new String[]{String.valueOf(id)}, null, null, null);
        Expense expense = null;
        if (cursor != null && cursor.moveToFirst()) {
            expense = cursorToExpense(cursor);
            cursor.close();
        }
        db.close();
        return expense;
    }

    public List<Expense> getTodayExpenses() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        long startOfDay = cal.getTimeInMillis();
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        long endOfDay = cal.getTimeInMillis();
        return getExpensesByRange(startOfDay, endOfDay);
    }

    public List<Expense> getRecentExpenses(int limit) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_EXPENSE, null, null, null,
                null, null, COL_DATE + " DESC", String.valueOf(limit));
        List<Expense> expenses = new ArrayList<>();
        if (cursor != null) {
            while (cursor.moveToNext()) expenses.add(cursorToExpense(cursor));
            cursor.close();
        }
        db.close();
        return expenses;
    }

    public List<Expense> getExpensesByRange(long start, long end) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_EXPENSE, null,
                COL_DATE + " BETWEEN ? AND ?",
                new String[]{String.valueOf(start), String.valueOf(end)},
                null, null, COL_DATE + " DESC");
        List<Expense> expenses = new ArrayList<>();
        if (cursor != null) {
            while (cursor.moveToNext()) expenses.add(cursorToExpense(cursor));
            cursor.close();
        }
        db.close();
        return expenses;
    }

    public List<Expense> getMonthExpenses(int year, int month) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startOfMonth = cal.getTimeInMillis();
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        long endOfMonth = cal.getTimeInMillis();
        return getExpensesByRange(startOfMonth, endOfMonth);
    }

    // ================================================================
    // TOTALS
    // ================================================================
    public double getCurrentMonthTotalIncome() {
        Calendar cal = Calendar.getInstance();
        return getTypeTotalForMonth("income", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH));
    }

    public double getCurrentMonthTotalExpense() {
        Calendar cal = Calendar.getInstance();
        return getTypeTotalForMonth("expense", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH));
    }

    public double getCurrentMonthBalance() {
        return getCurrentMonthTotalIncome() - getCurrentMonthTotalExpense();
    }

    private double getTypeTotalForMonth(String type, int year, int month) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, 1, 0, 0, 0);
        long startOfMonth = cal.getTimeInMillis();
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        long endOfMonth = cal.getTimeInMillis();

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_EXPENSE,
                new String[]{"SUM(" + COL_AMOUNT + ")"},
                COL_TYPE + "=? AND " + COL_DATE + " BETWEEN ? AND ?",
                new String[]{type, String.valueOf(startOfMonth), String.valueOf(endOfMonth)},
                null, null, null);
        double total = 0;
        if (cursor != null && cursor.moveToFirst()) {
            total = cursor.getDouble(0);
            cursor.close();
        }
        db.close();
        return total;
    }

    public double getCategoryRangeTotalExpense(String category, long start, long end) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_EXPENSE, new String[]{"SUM(" + COL_AMOUNT + ")"},
                COL_CATEGORY + "=? AND " + COL_DATE + " BETWEEN ? AND ?",
                new String[]{category, String.valueOf(start), String.valueOf(end)},
                null, null, null);
        double total = 0;
        if (cursor != null && cursor.moveToFirst()) {
            total = cursor.getDouble(0);
            cursor.close();
        }
        db.close();
        return total;
    }

    // ================================================================
    // BUDGET
    // ================================================================
    public Budget getCurrentMonthBudget() {
        Calendar cal = Calendar.getInstance();
        String monthKey = cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) + 1);
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_BUDGET, null,
                COL_BUDGET_MONTH + "=? AND " + COL_BUDGET_CATEGORY + "=?",
                new String[]{monthKey, CAT_TOTAL}, null, null, null);
        Budget budget = null;
        if (cursor != null && cursor.moveToFirst()) {
            budget = new Budget();
            budget.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COL_BUDGET_ID)));
            budget.setMonth(monthKey);
            budget.setTotalAmount(cursor.getDouble(cursor.getColumnIndexOrThrow(COL_BUDGET_TOTAL)));
            budget.setAlertPercent(cursor.getInt(cursor.getColumnIndexOrThrow(COL_BUDGET_ALERT)));
            cursor.close();
        }
        db.close();
        return budget;
    }

    public List<CategoryBudget> getCategoryBudgets(String monthKey) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_BUDGET, null,
                COL_BUDGET_MONTH + "=? AND " + COL_BUDGET_CATEGORY + "!=?",
                new String[]{monthKey, CAT_TOTAL}, null, null, null);
        List<CategoryBudget> budgets = new ArrayList<>();
        if (cursor != null) {
            int year = Integer.parseInt(monthKey.split("-")[0]);
            int month = Integer.parseInt(monthKey.split("-")[1]) - 1;
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, 1, 0, 0, 0);
            long startOfMonth = cal.getTimeInMillis();
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
            long endOfMonth = cal.getTimeInMillis();
            while (cursor.moveToNext()) {
                String category = cursor.getString(cursor.getColumnIndexOrThrow(COL_BUDGET_CATEGORY));
                double budgetAmount = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_BUDGET_CAT_AMOUNT));
                double spentAmount = getCategoryRangeTotalExpense(category, startOfMonth, endOfMonth);
                CategoryBudget cb = new CategoryBudget(category, budgetAmount, spentAmount, 0);
                cb.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COL_BUDGET_ID)));
                cb.setAlertEnabled(cursor.getInt(cursor.getColumnIndexOrThrow(COL_BUDGET_ALERT)) > 0);
                budgets.add(cb);
            }
            cursor.close();
        }
        db.close();
        return budgets;
    }

    public long saveBudget(Budget budget) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_BUDGET_MONTH, budget.getMonth());
        values.put(COL_BUDGET_CATEGORY, CAT_TOTAL);
        values.put(COL_BUDGET_TOTAL, budget.getTotalAmount());
        values.put(COL_BUDGET_ALERT, budget.getAlertPercent());
        long id = db.insertWithOnConflict(TABLE_BUDGET, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
        return id;
    }

    public long saveCategoryBudget(String monthKey, CategoryBudget cb) {
        return saveCategoryBudget(monthKey, cb.getCategoryName(), cb.getBudgetAmount(), cb.isAlertEnabled());
    }

    public long saveCategoryBudget(String monthKey, String category, double amount) {
        return saveCategoryBudget(monthKey, category, amount, true);
    }

    public long saveCategoryBudget(String monthKey, String category, double amount, boolean alertEnabled) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_BUDGET_MONTH, monthKey);
        values.put(COL_BUDGET_CATEGORY, category);
        values.put(COL_BUDGET_CAT_AMOUNT, amount);
        values.put(COL_BUDGET_ALERT, alertEnabled ? 1 : 0);
        long id = db.insertWithOnConflict(TABLE_BUDGET, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
        return id;
    }

    public void deleteExpense(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_EXPENSE, COL_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    private Expense cursorToExpense(Cursor cursor) {
        Expense expense = new Expense();
        expense.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)));
        expense.setAmount(cursor.getDouble(cursor.getColumnIndexOrThrow(COL_AMOUNT)));
        expense.setCategory(cursor.getString(cursor.getColumnIndexOrThrow(COL_CATEGORY)));
        expense.setPaymentMethod(cursor.getString(cursor.getColumnIndexOrThrow(COL_PAYMENT)));
        expense.setNote(cursor.getString(cursor.getColumnIndexOrThrow(COL_NOTE)));
        expense.setDate(cursor.getLong(cursor.getColumnIndexOrThrow(COL_DATE)));
        expense.setReceiptPath(cursor.getString(cursor.getColumnIndexOrThrow(COL_RECEIPT)));
        expense.setWallet(cursor.getString(cursor.getColumnIndexOrThrow(COL_WALLET)));
        expense.setVendor(cursor.getString(cursor.getColumnIndexOrThrow(COL_VENDOR)));
        expense.setTags(cursor.getString(cursor.getColumnIndexOrThrow(COL_TAGS)));

        int typeIdx = cursor.getColumnIndex(COL_TYPE);
        if (typeIdx >= 0) {
            String t = cursor.getString(typeIdx);
            expense.setType(t != null ? t : "expense");
        }

        int itemsIdx = cursor.getColumnIndex(COL_ITEMS_JSON);
        if (itemsIdx >= 0) {
            String itemsJson = cursor.getString(itemsIdx);
            if (itemsJson != null && !itemsJson.isEmpty()) {
                Type type = new TypeToken<List<ExpenseItem>>(){}.getType();
                List<ExpenseItem> items = gson.fromJson(itemsJson, type);
                if (items != null) expense.setItems(items);
            }
        }
        return expense;
    }

    /** Returns every row in the table. Used for backup export. */
    public List<Expense> getAllExpenses() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_EXPENSE, null, null, null,
                null, null, COL_DATE + " DESC");
        List<Expense> expenses = new ArrayList<>();
        if (cursor != null) {
            while (cursor.moveToNext()) expenses.add(cursorToExpense(cursor));
            cursor.close();
        }
        db.close();
        return expenses;
    }
}