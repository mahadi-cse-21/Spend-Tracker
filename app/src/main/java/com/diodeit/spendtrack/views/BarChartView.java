package com.diodeit.spendtrack.views;

import com.diodeit.spendtrack.utils.Tr;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;

public class BarChartView extends View {

    private Paint barPaint;
    private Paint trackPaint;
    private float[] values = {0, 0, 0, 0};
    private String[] labels = {Tr.t("১ম"), Tr.t("২য়"), Tr.t("৩য়"), Tr.t("৪র্থ")};

    public BarChartView(Context context) {
        super(context);
        init();
    }

    public BarChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barPaint.setColor(androidx.core.content.ContextCompat.getColor(getContext(), com.diodeit.spendtrack.R.color.primary));
        barPaint.setStyle(Paint.Style.FILL);

        trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trackPaint.setColor(androidx.core.content.ContextCompat.getColor(getContext(), com.diodeit.spendtrack.R.color.surface_container_high));
        trackPaint.setStyle(Paint.Style.FILL);
    }

    public void setData(float[] values) {
        this.values = values;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        int barCount = values.length;
        if (barCount == 0) return;

        float barWidth = (width - 40) / (barCount * 2f);
        float gap = barWidth;
        float startX = 20;
        float maxHeight = height - 40;

        float maxValue = 0;
        for (float v : values) {
            if (v > maxValue) maxValue = v;
        }
        if (maxValue == 0) maxValue = 1; // Avoid division by zero

        for (int i = 0; i < barCount; i++) {
            float barHeight = (values[i] / maxValue) * maxHeight;
            float left = startX + i * (barWidth + gap);
            float right = left + barWidth;
            float top = height - 20 - barHeight;
            float bottom = height - 20;

            RectF trackRect = new RectF(left, 20, right, bottom);
            canvas.drawRoundRect(trackRect, 8, 8, trackPaint);

            RectF barRect = new RectF(left, top, right, bottom);
            canvas.drawRoundRect(barRect, 8, 8, barPaint);
        }
    }
}