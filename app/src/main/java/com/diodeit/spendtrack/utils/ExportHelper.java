package com.diodeit.spendtrack.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextPaint;
import android.widget.Toast;

import com.diodeit.spendtrack.models.Expense;
import com.diodeit.spendtrack.models.ExpenseItem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExportHelper {

    private ExportHelper() {}

    // ================================================================
    // CSV BACKUP
    // ================================================================
    public static boolean exportCsv(Context context, List<Expense> expenses) {
        if (expenses == null || expenses.isEmpty()) {
            Toast.makeText(context, "কোনো ডেটা নেই", Toast.LENGTH_SHORT).show();
            return false;
        }

        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF'); // UTF-8 BOM for Excel
        sb.append("ID,Type,Date,Category,Payment,Amount,Note,Items\n");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        for (Expense e : expenses) {
            sb.append(e.getId()).append(',');
            sb.append(e.isIncome() ? "INCOME" : "EXPENSE").append(',');
            sb.append('"').append(sdf.format(e.getDate())).append('"').append(',');
            sb.append('"').append(safe(e.getCategory())).append('"').append(',');
            sb.append('"').append(safe(e.getPaymentMethod())).append('"').append(',');
            sb.append(e.getAmount()).append(',');
            sb.append('"').append(safe(e.getNote())).append('"').append(',');

            StringBuilder items = new StringBuilder();
            if (e.getItems() != null) {
                for (int i = 0; i < e.getItems().size(); i++) {
                    ExpenseItem it = e.getItems().get(i);
                    if (i > 0) items.append(" | ");
                    items.append(safe(it.getName())).append("=").append(it.getAmount());
                }
            }
            sb.append('"').append(items.toString().replace("\"", "'")).append('"');
            sb.append('\n');
        }

        String fileName = "spendtrack_backup_"
                + new SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US)
                .format(Calendar.getInstance().getTime())
                + ".csv";

        return writeToDownloads(context, fileName, "text/csv", sb.toString().getBytes());
    }

    // ================================================================
    // PDF REPORT — English-only for Roboto compatibility
    // ================================================================
    public static boolean exportPdf(Context context,
                                    List<Expense> expenses,
                                    double income,
                                    double expense,
                                    double balance,
                                    String periodLabel) {

        PdfDocument pdf = new PdfDocument();
        final int pageWidth    = 595;
        final int pageHeight   = 842;
        final int marginX      = 40;
        final int marginTop    = 60;
        final int marginBottom = 60;

        // Column X positions
        final int colDate     = marginX;
        final int colType     = marginX + 75;
        final int colCategory = marginX + 135;
        final int colItems    = marginX + 250;
        final int colAmount   = pageWidth - marginX - 70;

        TextPaint paint = new TextPaint();
        paint.setAntiAlias(true);
        paint.setColor(0xFF191C1B);

        int pageNumber = 1;
        PdfDocument.PageInfo pageInfo =
                new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create();
        PdfDocument.Page page = pdf.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        int y = drawPageHeader(canvas, paint, pageWidth, marginX, marginTop,
                bnToEnPeriod(periodLabel));

        // Table header
        paint.setTextSize(11f);
        paint.setFakeBoldText(true);
        paint.setColor(0xFF6F7A73);
        canvas.drawText("Date",     colDate,     y, paint);
        canvas.drawText("Type",     colType,     y, paint);
        canvas.drawText("Category", colCategory, y, paint);
        canvas.drawText("Items",    colItems,    y, paint);
        canvas.drawText("Amount",   colAmount,   y, paint);
        y += 6;
        paint.setStrokeWidth(0.7f);
        paint.setColor(0xFFBEC9C2);
        canvas.drawLine(marginX, y, pageWidth - marginX, y, paint);
        y += 14;

        paint.setFakeBoldText(false);
        paint.setColor(0xFF191C1B);
        paint.setTextSize(10f);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy", Locale.US);

        if (expenses != null) {
            for (Expense e : expenses) {
                if (y > pageHeight - marginBottom) {
                    drawPageFooter(canvas, paint, pageWidth, pageHeight, marginX, pageNumber);
                    pdf.finishPage(page);

                    pageNumber++;
                    pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create();
                    page = pdf.startPage(pageInfo);
                    canvas = page.getCanvas();
                    y = drawPageHeader(canvas, paint, pageWidth, marginX, marginTop,
                            bnToEnPeriod(periodLabel));
                    y += 26;
                }

                boolean isIncome = e.isIncome();

                // Date
                paint.setColor(0xFF191C1B);
                canvas.drawText(sdf.format(new Date(e.getDate())), colDate, y, paint);

                // Type (colored)
                paint.setColor(isIncome ? 0xFF00513C : 0xFFBA1A1A);
                paint.setFakeBoldText(true);
                canvas.drawText(isIncome ? "INCOME" : "EXPENSE", colType, y, paint);
                paint.setFakeBoldText(false);

                // Category (translated to English)
                paint.setColor(0xFF191C1B);
                canvas.drawText(truncate(bnToEnSafe(e.getCategory()), 18),
                        colCategory, y, paint);

                // Items summary
                String items = describeItems(e);
                canvas.drawText(truncate(items, 22), colItems, y, paint);

                // Amount (right-aligned, colored)
                paint.setColor(isIncome ? 0xFF00513C : 0xFFBA1A1A);
                paint.setFakeBoldText(true);
                String amtStr = (isIncome ? "+" : "-") + fmt(e.getAmount());
                float amtWidth = paint.measureText(amtStr);
                canvas.drawText(amtStr, colAmount + 70 - amtWidth, y, paint);
                paint.setFakeBoldText(false);

                y += 14;

                paint.setColor(0xFFEDEEEC);
                paint.setStrokeWidth(0.3f);
                canvas.drawLine(marginX, y - 8, pageWidth - marginX, y - 8, paint);
                paint.setColor(0xFF191C1B);
            }
        }

        // Summary box at the end
        y += 20;
        if (y > pageHeight - 120) {
            drawPageFooter(canvas, paint, pageWidth, pageHeight, marginX, pageNumber);
            pdf.finishPage(page);
            pageNumber++;
            pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create();
            page = pdf.startPage(pageInfo);
            canvas = page.getCanvas();
            y = drawPageHeader(canvas, paint, pageWidth, marginX, marginTop,
                    bnToEnPeriod(periodLabel));
        }

        paint.setStrokeWidth(0.7f);
        paint.setColor(0xFFBEC9C2);
        canvas.drawLine(marginX, y, pageWidth - marginX, y, paint);
        y += 20;

        paint.setFakeBoldText(true);
        paint.setTextSize(13f);
        paint.setColor(0xFF191C1B);
        canvas.drawText("Summary", marginX, y, paint);
        y += 20;

        paint.setTextSize(11f);
        paint.setFakeBoldText(false);
        paint.setColor(0xFF00513C);
        canvas.drawText("Total Income  : " + fmt(income), marginX, y, paint);
        y += 16;
        paint.setColor(0xFFBA1A1A);
        canvas.drawText("Total Expense : " + fmt(expense), marginX, y, paint);
        y += 16;
        paint.setColor(0xFF191C1B);
        paint.setFakeBoldText(true);
        canvas.drawText("Balance       : " + fmt(balance), marginX, y, paint);

        drawPageFooter(canvas, paint, pageWidth, pageHeight, marginX, pageNumber);
        pdf.finishPage(page);

        String fileName = "spendtrack_report_"
                + new SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US)
                .format(Calendar.getInstance().getTime())
                + ".pdf";

        return writePdfToDownloads(context, fileName, pdf);
    }

    // ─── Header ─────────────────────────────────────────────────
    private static int drawPageHeader(Canvas canvas, TextPaint paint,
                                      int pageWidth, int marginX, int marginTop,
                                      String periodLabelEn) {
        int y = marginTop;

        paint.setColor(0xFF00513C);
        paint.setFakeBoldText(true);
        paint.setTextSize(22f);
        canvas.drawText("SpendTrack", marginX, y, paint);
        y += 24;

        paint.setColor(0xFF3F4944);
        paint.setFakeBoldText(false);
        paint.setTextSize(13f);
        canvas.drawText("Financial Report - " + periodLabelEn, marginX, y, paint);
        y += 20;

        paint.setColor(0xFF6F7A73);
        paint.setTextSize(9f);
        String generated = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US)
                .format(Calendar.getInstance().getTime());
        canvas.drawText("Generated: " + generated, marginX, y, paint);
        y += 18;

        paint.setColor(0xFFBEC9C2);
        paint.setStrokeWidth(0.8f);
        canvas.drawLine(marginX, y, pageWidth - marginX, y, paint);
        y += 16;

        return y;
    }

    // ─── Footer ─────────────────────────────────────────────────
    private static void drawPageFooter(Canvas canvas, TextPaint paint,
                                       int pageWidth, int pageHeight,
                                       int marginX, int pageNumber) {
        paint.setColor(0xFF6F7A73);
        paint.setTextSize(9f);
        paint.setFakeBoldText(false);
        String footer = "Page " + pageNumber + "  |  SpendTrack - Personal Expense Tracker";
        float w = paint.measureText(footer);
        canvas.drawText(footer, (pageWidth - w) / 2f, pageHeight - 30, paint);
    }

    // ─── Items summary ──────────────────────────────────────────
    private static String describeItems(Expense e) {
        if (e.getItems() == null || e.getItems().isEmpty()) {
            return bnToEnSafe(e.getNote());
        }
        if (e.getItems().size() == 1) {
            return bnToEnSafe(e.getItems().get(0).getName());
        }
        return bnToEnSafe(e.getItems().get(0).getName()) + " +" + (e.getItems().size() - 1);
    }

    // ─── Period label translation ───────────────────────────────
    private static String bnToEnPeriod(String label) {
        if (label == null) return "";
        if (label.contains("সাপ্তাহিক")) return "Weekly Report";
        if (label.contains("বাৎসরিক"))  return "Yearly Report";
        if (label.contains("মাসিক"))    return "Monthly Report";
        if (label.contains("মে"))       return "May Report";

        // Fallback: try month name translation
        return bnToEnSafe(label);
    }

    // ─── Bengali to English converter ───────────────────────────
    private static String bnToEnSafe(String bn) {
        if (bn == null) return "";
        String s = bn.trim();

        // Expense categories
        if (s.contains("খাবার") || s.contains("রেস্তোরাঁ") || s.contains("রেস্তোরাঁ")) return "Food";
        if (s.contains("বাজার") || s.contains("মুদি"))       return "Grocery";
        if (s.contains("যাতায়াত") || s.contains("ভাড়া"))   return "Transport";
        if (s.contains("বাড়ি") || s.contains("ইউটিলিটি"))  return "Utilities";
        if (s.contains("স্বাস্থ্য") || s.contains("ওষুধ"))  return "Health";
        if (s.contains("শপিং") || s.contains("বিনোদন"))     return "Shopping";
        if (s.contains("শিক্ষা") || s.contains("বই"))       return "Education";
        if (s.contains("অন্যান্য"))                          return "Other";

        // Income categories
        if (s.contains("বেতন"))       return "Salary";
        if (s.contains("বোনাস"))      return "Bonus";
        if (s.contains("ফ্রিল্যান্স")) return "Freelance";
        if (s.contains("ব্যবসা"))     return "Business";
        if (s.contains("উপহার"))      return "Gift";
        if (s.contains("সুদ"))        return "Interest";
        if (s.contains("ভাড়া আয়"))   return "Rent Income";

        // Loan categories
        if (s.contains("ঋণ পরিশোধ"))  return "Loan Payment";
        if (s.contains("ঋণ ফেরত"))    return "Loan Return";

        // Fallback: replace common Bengali words
        String out = s
                .replace("খাবার", "Food")
                .replace("বাজার", "Grocery")
                .replace("যাতায়াত", "Transport")
                .replace("ভাড়া", "Rent")
                .replace("স্বাস্থ্য", "Health")
                .replace("শপিং", "Shopping")
                .replace("শিক্ষা", "Education")
                .replace("অন্যান্য", "Other")
                .replace("বেতন", "Salary");

        // If still has Bengali chars, replace remaining with "Item"
        if (out.matches(".*[\\u0980-\\u09FF].*")) {
            return "Item";
        }
        return out;
    }

    // ─── File write helpers ─────────────────────────────────────
    private static boolean writeToDownloads(Context ctx,
                                            String fileName,
                                            String mimeType,
                                            byte[] data) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, mimeType);
                values.put(MediaStore.Downloads.RELATIVE_PATH,
                        Environment.DIRECTORY_DOWNLOADS + "/SpendTrack");
                values.put(MediaStore.Downloads.IS_PENDING, 1);

                ContentResolver resolver = ctx.getContentResolver();
                Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri == null) return false;

                try (OutputStream out = resolver.openOutputStream(uri)) {
                    if (out == null) return false;
                    out.write(data);
                }

                values.clear();
                values.put(MediaStore.Downloads.IS_PENDING, 0);
                resolver.update(uri, values, null, null);
                return true;
            } else {
                File dir = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS), "SpendTrack");
                if (!dir.exists()) dir.mkdirs();
                File f = new File(dir, fileName);
                try (FileOutputStream out = new FileOutputStream(f)) {
                    out.write(data);
                }
                return true;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private static boolean writePdfToDownloads(Context ctx, String fileName, PdfDocument pdf) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
                values.put(MediaStore.Downloads.RELATIVE_PATH,
                        Environment.DIRECTORY_DOWNLOADS + "/SpendTrack");
                values.put(MediaStore.Downloads.IS_PENDING, 1);

                ContentResolver resolver = ctx.getContentResolver();
                Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri == null) return false;

                try (OutputStream out = resolver.openOutputStream(uri)) {
                    if (out == null) return false;
                    pdf.writeTo(out);
                }

                values.clear();
                values.put(MediaStore.Downloads.IS_PENDING, 0);
                resolver.update(uri, values, null, null);
            } else {
                File dir = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS), "SpendTrack");
                if (!dir.exists()) dir.mkdirs();
                File f = new File(dir, fileName);
                try (FileOutputStream out = new FileOutputStream(f)) {
                    pdf.writeTo(out);
                }
            }
            pdf.close();
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            try { pdf.close(); } catch (Exception ignored) {}
            return false;
        }
    }

    // ─── Utilities ──────────────────────────────────────────────
    private static String safe(String s) { return s == null ? "" : s; }

    private static String fmt(double v) {
        return String.format(Locale.US, "%,.2f", v);
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}