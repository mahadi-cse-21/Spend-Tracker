package com.diodeit.spendtrack.utils;

import android.content.Context;
import android.net.Uri;
import android.widget.Toast;

import com.diodeit.spendtrack.databases.DatabaseHelper;
import com.diodeit.spendtrack.models.Expense;
import com.diodeit.spendtrack.models.ExpenseItem;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CsvImporter {

    private CsvImporter() {}

    /**
     * Reads the CSV at the given Uri and inserts each row into the database.
     *
     * Expected header (matches ExportHelper.exportCsv output):
     *   ID,Type,Date,Category,Payment,Amount,Note,Items
     *
     * @return number of rows inserted, or -1 on failure
     */
    public static int importFromUri(Context context, Uri uri, DatabaseHelper db) {
        if (uri == null || db == null) return -1;

        int inserted = 0;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);

        try (InputStream is = context.getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(is, StandardCharsets.UTF_8))) {

            if (reader == null) {
                Toast.makeText(context, "ফাইল খোলা যায়নি", Toast.LENGTH_SHORT).show();
                return -1;
            }

            // Read the BOM-stripped header line
            String header = reader.readLine();
            if (header == null) {
                Toast.makeText(context, "ফাইল খালি", Toast.LENGTH_SHORT).show();
                return -1;
            }
            // Strip UTF-8 BOM if present
            if (header.startsWith("\uFEFF")) {
                header = header.substring(1);
            }
            // Sanity check
            if (!header.toLowerCase(Locale.US).startsWith("id,")) {
                Toast.makeText(context,
                        "সঠিক ব্যাকআপ ফাইল নয়", Toast.LENGTH_LONG).show();
                return -1;
            }

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                try {
                    Expense expense = parseLine(line, sdf);
                    if (expense != null) {
                        db.addExpense(expense);
                        inserted++;
                    }
                } catch (Exception ignored) {
                    // skip malformed line, keep going
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Toast.makeText(context, "ইমপোর্ট ব্যর্থ: " + ex.getMessage(),
                    Toast.LENGTH_LONG).show();
            return -1;
        }

        return inserted;
    }

    /**
     * Parses a single CSV line into an Expense object.
     *
     * Handles quoted fields with commas inside. Format:
     *   id,type,date,category,payment,amount,note,items
     */
    private static Expense parseLine(String line, SimpleDateFormat sdf) throws Exception {
        List<String> fields = splitCsv(line);
        if (fields.size() < 8) return null;

        // fields: 0=id, 1=type, 2=date, 3=category, 4=payment, 5=amount, 6=note, 7=items
        String type     = fields.get(1).trim();
        String dateStr  = fields.get(2).trim();
        String category = fields.get(3).trim();
        String payment  = fields.get(4).trim();
        String amountS  = fields.get(5).trim();
        String note     = fields.get(6).trim();
        String itemsS   = fields.get(7).trim();

        Expense e = new Expense();
        e.setType(type.equalsIgnoreCase("INCOME") ? "income" : "expense");
        e.setDate(sdf.parse(dateStr).getTime());
        e.setCategory(category);
        e.setPaymentMethod(payment);
        e.setAmount(Double.parseDouble(amountS));
        e.setNote(note);

        // Parse items — format "name=amount | name=amount"
        List<ExpenseItem> items = new ArrayList<>();
        if (!itemsS.isEmpty()) {
            String[] parts = itemsS.split("\\s*\\|\\s*");
            for (String part : parts) {
                int eq = part.lastIndexOf('=');
                if (eq > 0) {
                    String name = part.substring(0, eq).trim();
                    String amtS = part.substring(eq + 1).trim();
                    try {
                        items.add(new ExpenseItem(name, Double.parseDouble(amtS)));
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        if (items.isEmpty()) {
            // Fallback: single item from the note + amount
            items.add(new ExpenseItem(
                    note.isEmpty() ? category : note,
                    e.getAmount()));
        }
        e.setItems(items);

        return e;
    }

    /**
     * Splits one CSV row into fields. Supports double-quoted fields that
     * may contain commas. Skips the surrounding quotes.
     */
    private static List<String> splitCsv(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (inQuotes) {
                if (c == '"') {
                    // Doubled "" inside a quoted field → literal quote
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cur.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cur.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    fields.add(cur.toString());
                    cur.setLength(0);
                } else {
                    cur.append(c);
                }
            }
        }
        fields.add(cur.toString());
        return fields;
    }
}