package com.diodeit.spendtrack.models;

public class TransactionItem implements ListItem {
    private final Expense expense;

    public TransactionItem(Expense expense) {
        this.expense = expense;
    }

    public Expense getExpense() { return expense; }

    @Override
    public int getItemType() { return TYPE_TRANSACTION; }
}