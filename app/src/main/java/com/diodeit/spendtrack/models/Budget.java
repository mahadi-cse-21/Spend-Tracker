package com.diodeit.spendtrack.models;

public class Budget {
    private long id;
    private String month;
    private double totalAmount;
    private int alertPercent = 80;

    public Budget() {}

    public Budget(String month, double totalAmount) {
        this.month = month;
        this.totalAmount = totalAmount;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public int getAlertPercent() { return alertPercent; }
    public void setAlertPercent(int alertPercent) { this.alertPercent = alertPercent; }
}