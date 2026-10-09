package com.diodeit.spendtrack.models;

public class DateHeaderItem implements ListItem {
    private final long date;
    private final double income;
    private final double expense;

    public DateHeaderItem(long date, double income, double expense) {
        this.date = date;
        this.income = income;
        this.expense = expense;
    }

    public long getDate() { return date; }
    public double getIncome() { return income; }
    public double getExpense() { return expense; }

    @Override
    public int getItemType() { return TYPE_HEADER; }
}