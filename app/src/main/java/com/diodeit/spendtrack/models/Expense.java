package com.diodeit.spendtrack.models;

import java.util.ArrayList;
import java.util.List;

public class Expense {
    private long id;
    private double amount;
    private String category;
    private String paymentMethod;
    private String note;
    private long date;
    private String receiptPath;
    private String wallet;
    private String vendor;
    private String tags;
    private String type = "expense";      // "expense" or "income"
    private List<ExpenseItem> items = new ArrayList<>();

    public Expense() {}

    public Expense(double amount, String category, String paymentMethod, long date) {
        this.amount = amount;
        this.category = category;
        this.paymentMethod = paymentMethod;
        this.date = date;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public long getDate() { return date; }
    public void setDate(long date) { this.date = date; }

    public String getReceiptPath() { return receiptPath; }
    public void setReceiptPath(String receiptPath) { this.receiptPath = receiptPath; }

    public String getWallet() { return wallet; }
    public void setWallet(String wallet) { this.wallet = wallet; }

    public String getVendor() { return vendor; }
    public void setVendor(String vendor) { this.vendor = vendor; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isIncome() { return "income".equals(type); }

    public List<ExpenseItem> getItems() { return items; }
    public void setItems(List<ExpenseItem> items) { this.items = items; }

    /** Human-readable summary of the items. */
    public String getItemsSummary() {
        if (items == null || items.isEmpty()) {
            return note != null ? note : "";
        }
        if (items.size() == 1) {
            return items.get(0).getName();
        }
        return items.get(0).getName() + " +" + (items.size() - 1);
    }
}