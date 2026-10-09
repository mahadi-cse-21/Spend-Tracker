package com.diodeit.spendtrack.models;

public class Loan {
    private long id;

    /** "taken" = আমি ঋণ নিয়েছি (দিতে হবে) ; "given" = আমি ঋণ দিয়েছি (পাব) */
    private String type = "taken";

    /** কার সাথে লেনদেন */
    private String personName = "";

    /** মোট ঋণের পরিমাণ */
    private double principalAmount = 0;

    /** এখন পর্যন্ত পরিশোধিত / ফেরত পাওয়া পরিমাণ */
    private double paidAmount = 0;

    /** লেনদেনের তারিখ (মিলিসেকেন্ড) */
    private long date;

    /** প্রত্যাশিত ফেরত/পরিশোধের তারিখ (optional) */
    private long dueDate = 0;

    /** নোট / বিবরণ */
    private String note = "";

    /** সম্পূর্ণ পরিশোধিত কি না */
    private boolean closed = false;

    /** ★ Principal যুক্ত transaction (Expense/Income) এর ID */
    private long linkedExpenseId = -1;

    // ─────────────────────────────────────────────

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getPersonName() { return personName; }
    public void setPersonName(String personName) { this.personName = personName; }

    public double getPrincipalAmount() { return principalAmount; }
    public void setPrincipalAmount(double principalAmount) { this.principalAmount = principalAmount; }

    public double getPaidAmount() { return paidAmount; }
    public void setPaidAmount(double paidAmount) { this.paidAmount = paidAmount; }

    public long getDate() { return date; }
    public void setDate(long date) { this.date = date; }

    public long getDueDate() { return dueDate; }
    public void setDueDate(long dueDate) { this.dueDate = dueDate; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public boolean isClosed() { return closed; }
    public void setClosed(boolean closed) { this.closed = closed; }

    public long getLinkedExpenseId() { return linkedExpenseId; }
    public void setLinkedExpenseId(long linkedExpenseId) { this.linkedExpenseId = linkedExpenseId; }

    // ── Helpers ────────────────────────────────────

    public boolean isTaken()   { return "taken".equals(type); }
    public boolean isGiven()   { return "given".equals(type); }

    public double getRemaining() {
        return Math.max(0, principalAmount - paidAmount);
    }

    public int getProgressPercent() {
        if (principalAmount <= 0) return 0;
        int p = (int) ((paidAmount / principalAmount) * 100);
        return Math.max(0, Math.min(100, p));
    }
}