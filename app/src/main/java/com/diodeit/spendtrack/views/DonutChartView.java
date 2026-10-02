package com.diodeit.spendtrack.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;

public class DonutChartView extends View {

    private Paint paint;
    private RectF rectF;
    private float[] values = {};
    private int[] colors = {0xFF00513C, 0xFF006877, 0xFFBA1A1A, 0xFF406658, 0xFFBEC9C2, 0xFF7D5260, 0xFF6750A4};
    private float strokeWidth = 40f;

    public DonutChartView(Context context) {
        super(context);
        init();
    }

    public DonutChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(strokeWidth);
        paint.setStrokeCap(Paint.Cap.BUTT);
        rectF = new RectF();
    }

    public void setData(float[] values, int[] colors) {
        this.values = values;
        if (colors != null) {
            this.colors = colors;
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (values == null || values.length == 0) return;

        int width = getWidth();
        int height = getHeight();
        int size = Math.min(width, height);
        float padding = strokeWidth / 2 + 10;

        rectF.set(
                (width - size) / 2f + padding,
                (height - size) / 2f + padding,
                (width + size) / 2f - padding,
                (height + size) / 2f - padding
        );

        float total = 0;
        for (float v : values) total += v;
        if (total == 0) return;

        float startAngle = -90;
        for (int i = 0; i < values.length; i++) {
            paint.setColor(colors[i % colors.length]);
            float sweepAngle = (values[i] / total) * 360;
            if (sweepAngle > 0) {
                canvas.drawArc(rectF, startAngle, sweepAngle - (values.length > 1 ? 2 : 0), false, paint);
            }
            startAngle += sweepAngle;
        }
    }
}