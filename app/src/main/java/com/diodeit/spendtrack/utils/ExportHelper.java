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
    // PDF REPORT
    // ================================================================
    public static boolean exportPdf(Context context,
                                    List<Expense> monthExpenses,
                                    double income,
                                    double expense,
                                    double balance,
                                    String monthLabel) {

        PdfDocument pdf = new PdfDocument();
        int pageWidth = 595;
        int pageHeight = 842;
        int pageNumber = 1;

        PdfDocument.PageInfo pageInfo =
                new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create();
        PdfDocument.Page page = pdf.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        TextPaint paint = new TextPaint();
        paint.setAntiAlias(true);

        int x = 40;
        int y = 60;

        // Title
        paint.setTextSize(20f);
        paint.setFakeBoldText(true);
        canvas.drawText("SpendTrack - " + monthLabel, x, y, paint);
        y += 30;

        paint.setTextSize(12f);
        paint.setFakeBoldText(false);
        canvas.drawText("Monthly Financial Report", x, y, paint);
        y += 30;

        // Summary
        paint.setTextSize(14f);
        paint.setFakeBoldText(true);
        canvas.drawText("Summary", x, y, paint);
        y += 22;

        paint.setTextSize(12f);
        paint.setFakeBoldText(false);
        canvas.drawText("Total Income : " + fmt(income), x, y, paint); y += 18;
        canvas.drawText("Total Expense: " + fmt(expense), x, y, paint); y += 18;
        canvas.drawText("Balance      : " + fmt(balance), x, y, paint); y += 30;

        // Divider
        paint.setStrokeWidth(1f);
        canvas.drawLine(x, y, pageWidth - x, y, paint);
        y += 20;

        // Table header
        paint.setFakeBoldText(true);
        paint.setTextSize(12f);
        canvas.drawText("Date",     x,       y, paint);
        canvas.drawText("Type",     x + 90,  y, paint);
        canvas.drawText("Category", x + 150, y, paint);
        canvas.drawText("Amount",   x + 300, y, paint);
        y += 8;
        canvas.drawLine(x, y, pageWidth - x, y, paint);
        y += 16;

        // Rows
        paint.setFakeBoldText(false);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy", Locale.US);
        if (monthExpenses != null) {
            for (Expense e : monthExpenses) {
                if (y > pageHeight - 60) {
                    pdf.finishPage(page);
                    pageNumber++;
                    pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create();
                    page = pdf.startPage(pageInfo);
                    canvas = page.getCanvas();
                    y = 60;
                }
                String type = e.isIncome() ? "Income" : "Expense";
                canvas.drawText(sdf.format(e.getDate()),              x,       y, paint);
                canvas.drawText(type,                                  x + 90,  y, paint);
                canvas.drawText(truncate(safe(e.getCategory()), 20),   x + 150, y, paint);
                canvas.drawText(fmt(e.getAmount()),                    x + 300, y, paint);
                y += 16;
            }
        }

        pdf.finishPage(page);

        String fileName = "spendtrack_report_"
                + new SimpleDateFormat("yyyy-MM", Locale.US)
                .format(Calendar.getInstance().getTime())
                + ".pdf";

        return writePdfToDownloads(context, fileName, pdf);
    }

    // ================================================================
    // WRITE HELPERS
    // ================================================================
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

    // ================================================================
    // UTILITIES
    // ================================================================
    private static String safe(String s) { return s == null ? "" : s; }

    private static String fmt(double v) {
        return String.format(Locale.US, "%,.2f", v);
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "...";
    }
}