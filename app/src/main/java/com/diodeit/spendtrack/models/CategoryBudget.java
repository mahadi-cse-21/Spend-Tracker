package com.diodeit.spendtrack.models;

public class CategoryBudget {
    private long id;
    private String categoryName;
    private double budgetAmount;
    private double spentAmount;
    private int order;
    private boolean alertEnabled = true;

    public CategoryBudget(String categoryName, double budgetAmount, double spentAmount, int order) {
        this.categoryName = categoryName;
        this.budgetAmount = budgetAmount;
        this.spentAmount = spentAmount;
        this.order = order;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public double getBudgetAmount() { return budgetAmount; }
    public void setBudgetAmount(double budgetAmount) { this.budgetAmount = budgetAmount; }

    public double getSpentAmount() { return spentAmount; }
    public void setSpentAmount(double spentAmount) { this.spentAmount = spentAmount; }

    public int getOrder() { return order; }
    public void setOrder(int order) { this.order = order; }

    public boolean isAlertEnabled() { return alertEnabled; }
    public void setAlertEnabled(boolean alertEnabled) { this.alertEnabled = alertEnabled; }

    public int getPercentSpent() {
        if (budgetAmount <= 0) return 0;
        return (int) ((spentAmount / budgetAmount) * 100);
    }

    public double getRemaining() {
        return Math.max(0, budgetAmount - spentAmount);
    }
}