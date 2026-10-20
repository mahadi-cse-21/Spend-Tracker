package com.diodeit.spendtrack.databases;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.diodeit.spendtrack.models.Budget;
import com.diodeit.spendtrack.models.Category;
import com.diodeit.spendtrack.models.CategoryBudget;
import com.diodeit.spendtrack.models.Expense;
import com.diodeit.spendtrack.models.ExpenseItem;
import com.diodeit.spendtrack.models.Loan;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "spendtrack.db";
    private static final int DB_VERSION = 8; // ★ Bumped version for categories table

    // ─── Expense table ─────────────────────────────────────────
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
    private static final String COL_LOAN_ID = "loan_id";

    // ─── Budget table ──────────────────────────────────────────
    private static final String TABLE_BUDGET = "budgets";
    private static final String COL_BUDGET_ID = "id";
    private static final String COL_BUDGET_MONTH = "month";
    private static final String COL_BUDGET_TOTAL = "total_amount";
    private static final String COL_BUDGET_CATEGORY = "category";
    private static final String COL_BUDGET_CAT_AMOUNT = "category_amount";
    private static final String COL_BUDGET_ALERT = "alert_percent";

    // ─── Loan table ────────────────────────────────────────────
    private static final String TABLE_LOAN = "loans";
    private static final String COL_LOAN_PK = "id";
    private static final String COL_LOAN_TYPE = "type";
    private static final String COL_LOAN_PERSON = "person_name";
    private static final String COL_LOAN_PRINCIPAL = "principal_amount";
    private static final String COL_LOAN_PAID = "paid_amount";
    private static final String COL_LOAN_DATE = "date";
    private static final String COL_LOAN_DUE_DATE = "due_date";
    private static final String COL_LOAN_NOTE = "note";
    private static final String COL_LOAN_CLOSED = "closed";
    private static final String COL_LOAN_LINKED_EXPENSE_ID = "linked_expense_id";

    // ─── Category table ────────────────────────────────────────
    private static final String TABLE_CATEGORY = "categories";
    private static final String COL_CAT_ID = "id";
    private static final String COL_CAT_NAME = "name";
    private static final String COL_CAT_TYPE = "type";
    private static final String COL_CAT_EMOJI = "emoji";

    private static final String CAT_TOTAL = "__TOTAL__";

    private final Gson gson = new Gson();
    private SQLiteDatabase cachedDb;

    private final Map<String, Object> cache = new HashMap<>();
    private final Map<String, List<Expense>> monthCache = new HashMap<>();
    private volatile int dataVersion = 0;

    private static volatile DatabaseHelper INSTANCE;

    public static DatabaseHelper get(Context context) {
        if (INSTANCE == null) {
            synchronized (DatabaseHelper.class) {
                if (INSTANCE == null) {
                    INSTANCE = new DatabaseHelper(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    private DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.enableWriteAheadLogging();
        db.setForeignKeyConstraintsEnabled(false);
    }

    private SQLiteDatabase db() {
        if (cachedDb == null || !cachedDb.isOpen()) {
            cachedDb = getWritableDatabase();
        }
        return cachedDb;
    }

    public void invalidateCache() {
        cache.clear();
        monthCache.clear();
        dataVersion++;
    }

    public int getDataVersion() {
        return dataVersion;
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
                COL_TYPE + " TEXT DEFAULT 'expense', " +
                COL_LOAN_ID + " INTEGER DEFAULT -1)");

        db.execSQL("CREATE TABLE " + TABLE_BUDGET + " (" +
                COL_BUDGET_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_BUDGET_MONTH + " TEXT, " +
                COL_BUDGET_TOTAL + " REAL, " +
                COL_BUDGET_CATEGORY + " TEXT, " +
                COL_BUDGET_CAT_AMOUNT + " REAL, " +
                COL_BUDGET_ALERT + " INTEGER DEFAULT 80, " +
                "UNIQUE(" + COL_BUDGET_MONTH + ", " + COL_BUDGET_CATEGORY + "))");

        db.execSQL("CREATE TABLE " + TABLE_LOAN + " (" +
                COL_LOAN_PK + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_LOAN_TYPE + " TEXT, " +
                COL_LOAN_PERSON + " TEXT, " +
                COL_LOAN_PRINCIPAL + " REAL, " +
                COL_LOAN_PAID + " REAL, " +
                COL_LOAN_DATE + " INTEGER, " +
                COL_LOAN_DUE_DATE + " INTEGER, " +
                COL_LOAN_NOTE + " TEXT, " +
                COL_LOAN_CLOSED + " INTEGER DEFAULT 0, " +
                COL_LOAN_LINKED_EXPENSE_ID + " INTEGER DEFAULT -1)");

        db.execSQL("CREATE TABLE " + TABLE_CATEGORY + " (" +
                COL_CAT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_CAT_NAME + " TEXT, " +
                COL_CAT_TYPE + " TEXT, " +
                COL_CAT_EMOJI + " TEXT, " +
                "UNIQUE(" + COL_CAT_NAME + ", " + COL_CAT_TYPE + "))");

        ensureIndexes(db);
        seedDefaultCategories(db); // Seed on first run
    }

    private void ensureIndexes(SQLiteDatabase db) {
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_exp_date ON " + TABLE_EXPENSE + "(" + COL_DATE + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_exp_type_date ON " + TABLE_EXPENSE + "(" + COL_TYPE + ", " + COL_DATE + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_exp_loan ON " + TABLE_EXPENSE + "(" + COL_LOAN_ID + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_exp_cat_date ON " + TABLE_EXPENSE + "(" + COL_CATEGORY + ", " + COL_DATE + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_loan_date ON " + TABLE_LOAN + "(" + COL_LOAN_DATE + ")");
    }

    public void ensureIndexes() {
        ensureIndexes(db());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) { try { db.execSQL("ALTER TABLE " + TABLE_EXPENSE + " ADD COLUMN " + COL_ITEMS_JSON + " TEXT"); } catch (Exception ignored) {} }
        if (oldVersion < 4) { try { db.execSQL("ALTER TABLE " + TABLE_EXPENSE + " ADD COLUMN " + COL_TYPE + " TEXT DEFAULT 'expense'"); } catch (Exception ignored) {} }
        if (oldVersion < 5) {
            try {
                db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_LOAN + " (" +
                        COL_LOAN_PK + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COL_LOAN_TYPE + " TEXT, " +
                        COL_LOAN_PERSON + " TEXT, " +
                        COL_LOAN_PRINCIPAL + " REAL, " +
                        COL_LOAN_PAID + " REAL, " +
                        COL_LOAN_DATE + " INTEGER, " +
                        COL_LOAN_DUE_DATE + " INTEGER, " +
                        COL_LOAN_NOTE + " TEXT, " +
                        COL_LOAN_CLOSED + " INTEGER DEFAULT 0)");
            } catch (Exception ignored) {}
        }
        if (oldVersion < 6) { try { db.execSQL("ALTER TABLE " + TABLE_LOAN + " ADD COLUMN " + COL_LOAN_LINKED_EXPENSE_ID + " INTEGER DEFAULT -1"); } catch (Exception ignored) {} }
        if (oldVersion < 7) { try { db.execSQL("ALTER TABLE " + TABLE_EXPENSE + " ADD COLUMN " + COL_LOAN_ID + " INTEGER DEFAULT -1"); } catch (Exception ignored) {} }
        if (oldVersion < 8) {
            try {
                db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_CATEGORY + " (" +
                        COL_CAT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COL_CAT_NAME + " TEXT, " +
                        COL_CAT_TYPE + " TEXT, " +
                        COL_CAT_EMOJI + " TEXT, " +
                        "UNIQUE(" + COL_CAT_NAME + ", " + COL_CAT_TYPE + "))");
                seedDefaultCategories(db);
            } catch (Exception ignored) {}
        }
        ensureIndexes(db);
    }

    private void seedDefaultCategories(SQLiteDatabase db) {
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_CATEGORY, null);
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();

        if (count == 0) {
            // Default Expense Categories
            addCategoryToDb(db, "খাবার", "expense", "🍔");
            addCategoryToDb(db, "বাজার", "expense", "🛒");
            addCategoryToDb(db, "যাতায়াত", "expense", "🚗");
            addCategoryToDb(db, "বিল", "expense", "🏠");
            addCategoryToDb(db, "স্বাস্থ্য", "expense", "💊");
            addCategoryToDb(db, "শপিং", "expense", "🛍️");
            addCategoryToDb(db, "শিক্ষা", "expense", "📚");
            addCategoryToDb(db, "অন্যান্য", "expense", "⚙️");

            // Default Income Categories
            addCategoryToDb(db, "বেতন", "income", "💼");
            addCategoryToDb(db, "বোনাস", "income", "🎁");
            addCategoryToDb(db, "ফ্রিল্যান্স", "income", "💻");
            addCategoryToDb(db, "ব্যবসা", "income", "🏢");
            addCategoryToDb(db, "অন্যান্য আয়", "income", "⚙️");
        }
    }

    private void addCategoryToDb(SQLiteDatabase db, String name, String type, String emoji) {
        ContentValues v = new ContentValues();
        v.put(COL_CAT_NAME, name);
        v.put(COL_CAT_TYPE, type);
        v.put(COL_CAT_EMOJI, emoji);
        db.insertWithOnConflict(TABLE_CATEGORY, null, v, SQLiteDatabase.CONFLICT_IGNORE);
    }

    // ═══════════════════════════════════════════════════════════
    // CATEGORIES API
    // ═══════════════════════════════════════════════════════════
    public List<Category> getCategories(String type) {
        List<Category> list = new ArrayList<>();
        Cursor c = db().query(TABLE_CATEGORY, null,
                COL_CAT_TYPE + "=?", new String[]{type},
                null, null, COL_CAT_ID + " ASC");

        if (c != null) {
            while (c.moveToNext()) {
                String name = c.getString(c.getColumnIndexOrThrow(COL_CAT_NAME));
                String emoji = c.getString(c.getColumnIndexOrThrow(COL_CAT_EMOJI));
                list.add(new Category(name, name, emoji));
            }
            c.close();
        }
        return list;
    }

    public boolean addCategory(String name, String type, String emoji) {
        ContentValues v = new ContentValues();
        v.put(COL_CAT_NAME, name);
        v.put(COL_CAT_TYPE, type);
        v.put(COL_CAT_EMOJI, emoji);

        long id = db().insertWithOnConflict(TABLE_CATEGORY, null, v, SQLiteDatabase.CONFLICT_IGNORE);
        if (id > 0) {
            invalidateCache();
            return true;
        }
        return false;
    }

    // ═══════════════════════════════════════════════════════════
    // EXPENSE WRITES / READS (Unchanged) ...
    // ═══════════════════════════════════════════════════════════
    public long addExpense(Expense e) {
        ContentValues v = new ContentValues();
        v.put(COL_AMOUNT, e.getAmount());
        v.put(COL_CATEGORY, e.getCategory());
        v.put(COL_PAYMENT, e.getPaymentMethod());
        v.put(COL_NOTE, e.getNote());
        v.put(COL_DATE, e.getDate());
        v.put(COL_RECEIPT, e.getReceiptPath());
        v.put(COL_WALLET, e.getWallet());
        v.put(COL_VENDOR, e.getVendor());
        v.put(COL_TAGS, e.getTags());
        v.put(COL_ITEMS_JSON, gson.toJson(e.getItems()));
        v.put(COL_TYPE, e.getType() != null ? e.getType() : "expense");
        v.put(COL_LOAN_ID, e.getLoanId());
        long id = db().insert(TABLE_EXPENSE, null, v);
        invalidateCache();
        return id;
    }

    public int updateExpense(long id, Expense e) {
        ContentValues v = new ContentValues();
        v.put(COL_AMOUNT, e.getAmount());
        v.put(COL_CATEGORY, e.getCategory());
        v.put(COL_PAYMENT, e.getPaymentMethod());
        v.put(COL_NOTE, e.getNote());
        v.put(COL_DATE, e.getDate());
        v.put(COL_RECEIPT, e.getReceiptPath());
        v.put(COL_WALLET, e.getWallet());
        v.put(COL_VENDOR, e.getVendor());
        v.put(COL_TAGS, e.getTags());
        v.put(COL_ITEMS_JSON, gson.toJson(e.getItems()));
        v.put(COL_TYPE, e.getType() != null ? e.getType() : "expense");
        v.put(COL_LOAN_ID, e.getLoanId());
        int rows = db().update(TABLE_EXPENSE, v, COL_ID + "=?", new String[]{String.valueOf(id)});
        invalidateCache();
        return rows;
    }

    public void deleteExpense(long id) {
        db().delete(TABLE_EXPENSE, COL_ID + "=?", new String[]{String.valueOf(id)});
        invalidateCache();
    }

    public Expense getExpense(long id) {
        Cursor c = db().query(TABLE_EXPENSE, null, COL_ID + "=?",
                new String[]{String.valueOf(id)}, null, null, null);
        Expense e = null;
        if (c != null && c.moveToFirst()) { e = cursorToExpense(c); c.close(); }
        return e;
    }

    public int getExpenseCount() {
        Object cached = cache.get("count");
        if (cached != null) return (int) cached;
        Cursor c = db().rawQuery("SELECT COUNT(*) FROM " + TABLE_EXPENSE, null);
        int n = 0;
        if (c.moveToFirst()) n = c.getInt(0);
        c.close();
        cache.put("count", n);
        return n;
    }

    public List<Expense> getTodayExpenses() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis();
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999);
        long end = cal.getTimeInMillis();
        return getExpensesByRange(start, end);
    }

    public List<Expense> getRecentExpenses(int limit) {
        Cursor c = db().query(TABLE_EXPENSE, null, null, null,
                null, null, COL_DATE + " DESC", String.valueOf(limit));
        List<Expense> list = new ArrayList<>();
        if (c != null) { while (c.moveToNext()) list.add(cursorToExpense(c)); c.close(); }
        return list;
    }

    public List<Expense> getExpensesByRange(long start, long end) {
        Cursor c = db().query(TABLE_EXPENSE, null, COL_DATE + " BETWEEN ? AND ?",
                new String[]{String.valueOf(start), String.valueOf(end)},
                null, null, COL_DATE + " DESC");
        List<Expense> list = new ArrayList<>();
        if (c != null) { while (c.moveToNext()) list.add(cursorToExpense(c)); c.close(); }
        return list;
    }

    public List<Expense> getMonthExpenses(int year, int month) {
        String key = "m_" + year + "_" + month;
        List<Expense> cached = monthCache.get(key);
        if (cached != null) return cached;

        Calendar cal = Calendar.getInstance();
        cal.set(year, month, 1, 0, 0, 0); cal.set(Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis();
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999);
        long end = cal.getTimeInMillis();

        List<Expense> list = getExpensesByRange(start, end);
        monthCache.put(key, list);
        return list;
    }

    public List<Expense> getExpensesByLoanId(long loanId) {
        Cursor c = db().query(TABLE_EXPENSE, null, COL_LOAN_ID + "=?",
                new String[]{String.valueOf(loanId)}, null, null, COL_DATE + " ASC");
        List<Expense> list = new ArrayList<>();
        if (c != null) { while (c.moveToNext()) list.add(cursorToExpense(c)); c.close(); }
        return list;
    }

    public List<Expense> getAllExpenses() {
        Cursor c = db().query(TABLE_EXPENSE, null, null, null, null, null, COL_DATE + " DESC");
        List<Expense> list = new ArrayList<>();
        if (c != null) { while (c.moveToNext()) list.add(cursorToExpense(c)); c.close(); }
        return list;
    }

    // ═══════════════════════════════════════════════════════════
    // TOTALS (cached) ...
    // ═══════════════════════════════════════════════════════════
    public double getTotalIncomeAllTime() {
        Object v = cache.get("incomeAll");
        if (v != null) return (double) v;
        double r = getTypeTotalAllTime("income");
        cache.put("incomeAll", r);
        return r;
    }

    public double getTotalExpenseAllTime() {
        Object v = cache.get("expenseAll");
        if (v != null) return (double) v;
        double r = getTypeTotalAllTime("expense");
        cache.put("expenseAll", r);
        return r;
    }

    public double getBalanceAllTime() { return getTotalIncomeAllTime() - getTotalExpenseAllTime(); }

    private double getTypeTotalAllTime(String type) {
        Cursor c = db().query(TABLE_EXPENSE,
                new String[]{"SUM(" + COL_AMOUNT + ")"},
                COL_TYPE + "=?", new String[]{type}, null, null, null);
        double t = 0;
        if (c != null && c.moveToFirst()) { t = c.getDouble(0); c.close(); }
        return t;
    }

    public double getCurrentMonthTotalIncome() {
        Calendar cal = Calendar.getInstance();
        return getTypeTotalForMonth("income", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH));
    }

    public double getCurrentMonthTotalExpense() {
        Calendar cal = Calendar.getInstance();
        return getTypeTotalForMonth("expense", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH));
    }

    private double getTypeTotalForMonth(String type, int year, int month) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, 1, 0, 0, 0); cal.set(Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis();
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999);
        long end = cal.getTimeInMillis();
        return getTypeTotalInRange(type, start, end);
    }

    public double getMonthIncome(int year, int month) {
        Calendar c = Calendar.getInstance();
        c.set(year, month, 1, 0, 0, 0); c.set(Calendar.MILLISECOND, 0);
        long s = c.getTimeInMillis();
        c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));
        c.set(Calendar.HOUR_OF_DAY, 23); c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59); c.set(Calendar.MILLISECOND, 999);
        return getTypeTotalInRange("income", s, c.getTimeInMillis());
    }

    public double getMonthExpense(int year, int month) {
        Calendar c = Calendar.getInstance();
        c.set(year, month, 1, 0, 0, 0); c.set(Calendar.MILLISECOND, 0);
        long s = c.getTimeInMillis();
        c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));
        c.set(Calendar.HOUR_OF_DAY, 23); c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59); c.set(Calendar.MILLISECOND, 999);
        return getTypeTotalInRange("expense", s, c.getTimeInMillis());
    }

    public double getCategoryRangeTotalExpense(String category, long start, long end) {
        Cursor c = db().query(TABLE_EXPENSE,
                new String[]{"SUM(" + COL_AMOUNT + ")"},
                COL_CATEGORY + "=? AND " + COL_TYPE + "='expense' AND " + COL_DATE + " BETWEEN ? AND ?",
                new String[]{category, String.valueOf(start), String.valueOf(end)},
                null, null, null);
        double t = 0;
        if (c != null && c.moveToFirst()) { t = c.getDouble(0); c.close(); }
        return t;
    }

    private double getTypeTotalInRange(String type, long start, long end) {
        Cursor c = db().query(TABLE_EXPENSE,
                new String[]{"SUM(" + COL_AMOUNT + ")"},
                COL_TYPE + "=? AND " + COL_DATE + " BETWEEN ? AND ?",
                new String[]{type, String.valueOf(start), String.valueOf(end)},
                null, null, null);
        double t = 0;
        if (c != null && c.moveToFirst()) { t = c.getDouble(0); c.close(); }
        return t;
    }

    // ═══════════════════════════════════════════════════════════
    // BUDGET / LOANS / CONVERTERS (Unchanged) ...
    // ═══════════════════════════════════════════════════════════
    public Budget getCurrentMonthBudget() {
        Calendar cal = Calendar.getInstance();
        String monthKey = cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) + 1);
        Cursor c = db().query(TABLE_BUDGET, null,
                COL_BUDGET_MONTH + "=? AND " + COL_BUDGET_CATEGORY + "=?",
                new String[]{monthKey, CAT_TOTAL}, null, null, null);
        Budget b = null;
        if (c != null && c.moveToFirst()) {
            b = new Budget();
            b.setId(c.getLong(c.getColumnIndexOrThrow(COL_BUDGET_ID)));
            b.setMonth(monthKey);
            b.setTotalAmount(c.getDouble(c.getColumnIndexOrThrow(COL_BUDGET_TOTAL)));
            b.setAlertPercent(c.getInt(c.getColumnIndexOrThrow(COL_BUDGET_ALERT)));
            c.close();
        }
        return b;
    }

    public List<CategoryBudget> getCategoryBudgets(String monthKey) {
        int year = Integer.parseInt(monthKey.split("-")[0]);
        int month = Integer.parseInt(monthKey.split("-")[1]) - 1;
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, 1, 0, 0, 0); cal.set(Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis();
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999);
        long end = cal.getTimeInMillis();

        Map<String, Double> spentMap = new HashMap<>();
        Cursor sc = db().rawQuery(
                "SELECT " + COL_CATEGORY + ", SUM(" + COL_AMOUNT + ") FROM " + TABLE_EXPENSE +
                        " WHERE " + COL_TYPE + "='expense' AND " + COL_DATE + " BETWEEN ? AND ?" +
                        " GROUP BY " + COL_CATEGORY,
                new String[]{String.valueOf(start), String.valueOf(end)});
        if (sc != null) { while (sc.moveToNext()) spentMap.put(sc.getString(0), sc.getDouble(1)); sc.close(); }

        Cursor c = db().query(TABLE_BUDGET, null,
                COL_BUDGET_MONTH + "=? AND " + COL_BUDGET_CATEGORY + "!=?",
                new String[]{monthKey, CAT_TOTAL}, null, null, null);
        List<CategoryBudget> list = new ArrayList<>();
        if (c != null) {
            while (c.moveToNext()) {
                String cat = c.getString(c.getColumnIndexOrThrow(COL_BUDGET_CATEGORY));
                double amt = c.getDouble(c.getColumnIndexOrThrow(COL_BUDGET_CAT_AMOUNT));
                double spent = spentMap.getOrDefault(cat, 0.0);
                CategoryBudget cb = new CategoryBudget(cat, amt, spent, 0);
                cb.setId(c.getLong(c.getColumnIndexOrThrow(COL_BUDGET_ID)));
                cb.setAlertEnabled(c.getInt(c.getColumnIndexOrThrow(COL_BUDGET_ALERT)) > 0);
                list.add(cb);
            }
            c.close();
        }
        return list;
    }

    public long saveBudget(Budget b) {
        ContentValues v = new ContentValues();
        v.put(COL_BUDGET_MONTH, b.getMonth());
        v.put(COL_BUDGET_CATEGORY, CAT_TOTAL);
        v.put(COL_BUDGET_TOTAL, b.getTotalAmount());
        v.put(COL_BUDGET_ALERT, b.getAlertPercent());
        long id = db().insertWithOnConflict(TABLE_BUDGET, null, v, SQLiteDatabase.CONFLICT_REPLACE);
        invalidateCache();
        return id;
    }

    public long saveCategoryBudget(String monthKey, CategoryBudget cb) {
        return saveCategoryBudget(monthKey, cb.getCategoryName(), cb.getBudgetAmount(), cb.isAlertEnabled());
    }
    public long saveCategoryBudget(String monthKey, String category, double amount) {
        return saveCategoryBudget(monthKey, category, amount, true);
    }
    public long saveCategoryBudget(String monthKey, String category, double amount, boolean alert) {
        ContentValues v = new ContentValues();
        v.put(COL_BUDGET_MONTH, monthKey);
        v.put(COL_BUDGET_CATEGORY, category);
        v.put(COL_BUDGET_CAT_AMOUNT, amount);
        v.put(COL_BUDGET_ALERT, alert ? 1 : 0);
        long id = db().insertWithOnConflict(TABLE_BUDGET, null, v, SQLiteDatabase.CONFLICT_REPLACE);
        invalidateCache();
        return id;
    }

    public long addLoan(Loan l) {
        ContentValues v = new ContentValues();
        v.put(COL_LOAN_TYPE, l.getType());
        v.put(COL_LOAN_PERSON, l.getPersonName());
        v.put(COL_LOAN_PRINCIPAL, l.getPrincipalAmount());
        v.put(COL_LOAN_PAID, l.getPaidAmount());
        v.put(COL_LOAN_DATE, l.getDate());
        v.put(COL_LOAN_DUE_DATE, l.getDueDate());
        v.put(COL_LOAN_NOTE, l.getNote());
        v.put(COL_LOAN_CLOSED, l.isClosed() ? 1 : 0);
        v.put(COL_LOAN_LINKED_EXPENSE_ID, l.getLinkedExpenseId());
        long id = db().insert(TABLE_LOAN, null, v);
        invalidateCache();
        return id;
    }

    public int updateLoan(Loan l) {
        ContentValues v = new ContentValues();
        v.put(COL_LOAN_TYPE, l.getType());
        v.put(COL_LOAN_PERSON, l.getPersonName());
        v.put(COL_LOAN_PRINCIPAL, l.getPrincipalAmount());
        v.put(COL_LOAN_PAID, l.getPaidAmount());
        v.put(COL_LOAN_DATE, l.getDate());
        v.put(COL_LOAN_DUE_DATE, l.getDueDate());
        v.put(COL_LOAN_NOTE, l.getNote());
        v.put(COL_LOAN_CLOSED, l.isClosed() ? 1 : 0);
        v.put(COL_LOAN_LINKED_EXPENSE_ID, l.getLinkedExpenseId());
        int rows = db().update(TABLE_LOAN, v, COL_LOAN_PK + "=?",
                new String[]{String.valueOf(l.getId())});
        invalidateCache();
        return rows;
    }

    public void deleteLoan(long id) {
        Loan loan = getLoan(id);
        SQLiteDatabase db = db();
        if (loan != null) {
            db.delete(TABLE_EXPENSE, COL_LOAN_ID + "=?", new String[]{String.valueOf(id)});
            String pTaken = "ঋণ পরিশোধ — " + loan.getPersonName();
            String pGiven = "ঋণ ফেরত পেয়েছি — " + loan.getPersonName();
            db.delete(TABLE_EXPENSE,
                    COL_NOTE + " IN (?, ?) AND " + COL_CATEGORY + "=?",
                    new String[]{pTaken, pGiven, "ঋণ পরিশোধ"});
        }
        db.delete(TABLE_LOAN, COL_LOAN_PK + "=?", new String[]{String.valueOf(id)});
        invalidateCache();
    }

    public Loan getLoan(long id) {
        Cursor c = db().query(TABLE_LOAN, null, COL_LOAN_PK + "=?",
                new String[]{String.valueOf(id)}, null, null, null);
        Loan l = null;
        if (c != null && c.moveToFirst()) { l = cursorToLoan(c); c.close(); }
        return l;
    }

    public List<Loan> getAllLoans() {
        Cursor c = db().query(TABLE_LOAN, null, null, null, null, null, COL_LOAN_DATE + " DESC");
        List<Loan> list = new ArrayList<>();
        if (c != null) { while (c.moveToNext()) list.add(cursorToLoan(c)); c.close(); }
        return list;
    }

    public double getTotalTakenRemaining() {
        double t = 0;
        for (Loan l : getAllLoans()) if (l.isTaken() && !l.isClosed()) t += l.getRemaining();
        return t;
    }

    public double getTotalGivenRemaining() {
        double t = 0;
        for (Loan l : getAllLoans()) if (l.isGiven() && !l.isClosed()) t += l.getRemaining();
        return t;
    }

    private Expense cursorToExpense(Cursor c) {
        Expense e = new Expense();
        e.setId(c.getLong(c.getColumnIndexOrThrow(COL_ID)));
        e.setAmount(c.getDouble(c.getColumnIndexOrThrow(COL_AMOUNT)));
        e.setCategory(c.getString(c.getColumnIndexOrThrow(COL_CATEGORY)));
        e.setPaymentMethod(c.getString(c.getColumnIndexOrThrow(COL_PAYMENT)));
        e.setNote(c.getString(c.getColumnIndexOrThrow(COL_NOTE)));
        e.setDate(c.getLong(c.getColumnIndexOrThrow(COL_DATE)));
        e.setReceiptPath(c.getString(c.getColumnIndexOrThrow(COL_RECEIPT)));
        e.setWallet(c.getString(c.getColumnIndexOrThrow(COL_WALLET)));
        e.setVendor(c.getString(c.getColumnIndexOrThrow(COL_VENDOR)));
        e.setTags(c.getString(c.getColumnIndexOrThrow(COL_TAGS)));
        int ti = c.getColumnIndex(COL_TYPE);
        if (ti >= 0) { String t = c.getString(ti); e.setType(t != null ? t : "expense"); }
        int ii = c.getColumnIndex(COL_ITEMS_JSON);
        if (ii >= 0) {
            String j = c.getString(ii);
            if (j != null && !j.isEmpty()) {
                Type type = new TypeToken<List<ExpenseItem>>(){}.getType();
                List<ExpenseItem> items = gson.fromJson(j, type);
                if (items != null) e.setItems(items);
            }
        }
        int li = c.getColumnIndex(COL_LOAN_ID);
        if (li >= 0) e.setLoanId(c.getLong(li));
        return e;
    }

    private Expense cursorToExpenseLight(Cursor c) {
        Expense e = new Expense();
        e.setId(c.getLong(c.getColumnIndexOrThrow(COL_ID)));
        e.setAmount(c.getDouble(c.getColumnIndexOrThrow(COL_AMOUNT)));
        e.setCategory(c.getString(c.getColumnIndexOrThrow(COL_CATEGORY)));
        e.setPaymentMethod(c.getString(c.getColumnIndexOrThrow(COL_PAYMENT)));
        e.setNote(c.getString(c.getColumnIndexOrThrow(COL_NOTE)));
        e.setDate(c.getLong(c.getColumnIndexOrThrow(COL_DATE)));
        String t = c.getString(c.getColumnIndexOrThrow(COL_TYPE));
        e.setType(t != null ? t : "expense");
        e.setLoanId(c.getLong(c.getColumnIndexOrThrow(COL_LOAN_ID)));
        return e;
    }

    private Loan cursorToLoan(Cursor c) {
        Loan l = new Loan();
        l.setId(c.getLong(c.getColumnIndexOrThrow(COL_LOAN_PK)));
        l.setType(c.getString(c.getColumnIndexOrThrow(COL_LOAN_TYPE)));
        l.setPersonName(c.getString(c.getColumnIndexOrThrow(COL_LOAN_PERSON)));
        l.setPrincipalAmount(c.getDouble(c.getColumnIndexOrThrow(COL_LOAN_PRINCIPAL)));
        l.setPaidAmount(c.getDouble(c.getColumnIndexOrThrow(COL_LOAN_PAID)));
        l.setDate(c.getLong(c.getColumnIndexOrThrow(COL_LOAN_DATE)));
        l.setDueDate(c.getLong(c.getColumnIndexOrThrow(COL_LOAN_DUE_DATE)));
        l.setNote(c.getString(c.getColumnIndexOrThrow(COL_LOAN_NOTE)));
        l.setClosed(c.getInt(c.getColumnIndexOrThrow(COL_LOAN_CLOSED)) == 1);
        int idx = c.getColumnIndex(COL_LOAN_LINKED_EXPENSE_ID);
        if (idx >= 0) l.setLinkedExpenseId(c.getLong(idx));
        return l;
    }
    public void clearAllData() {
        SQLiteDatabase db = db();
        db.delete(TABLE_EXPENSE, null, null);
        db.delete(TABLE_BUDGET, null, null);
        db.delete(TABLE_LOAN, null, null);
        // Note: We do NOT clear TABLE_CATEGORY so the user's custom categories stay!
        invalidateCache();
    }
}
