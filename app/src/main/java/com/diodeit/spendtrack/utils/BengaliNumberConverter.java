package com.diodeit.spendtrack.utils;

public class BengaliNumberConverter {

    private static final char[] BN_DIGITS = {'০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯'};
    private static final char[] EN_DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};

    public String toBengali(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (c >= '0' && c <= '9') sb.append(BN_DIGITS[c - '0']);
            else sb.append(c);
        }
        return sb.toString();
    }

    public String toBengali(double number) {
        return toBengali(String.format("%,.2f", number));
    }

    public String toBengali(int number) {
        return toBengali(String.valueOf(number));
    }

    public String toBengali(long number) {
        return toBengali(String.valueOf(number));
    }

    public String toEnglish(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            int index = -1;
            for (int i = 0; i < BN_DIGITS.length; i++) {
                if (c == BN_DIGITS[i]) { index = i; break; }
            }
            if (index >= 0) sb.append(EN_DIGITS[index]);
            else sb.append(c);
        }
        return sb.toString();
    }

    public String formatCurrency(double amount) {
        return "৳ " + toBengali(String.format("%,.0f", amount));
    }
}