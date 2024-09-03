package com.applovin.ramtool.cpu;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class Core2CoreLatView extends View {
    public Core2CoreLatView(Context context) {
        super(context);
        init();
    }

    public Core2CoreLatView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public Core2CoreLatView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public Core2CoreLatView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private Paint rectPaint;
    private Paint textPaint;

    private void init() {
        rectPaint = new Paint();
        rectPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(40);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    private volatile int[][] latData;
    private volatile int minData, maxData;

    private volatile int colorSelect;

    public void update(int[][] latData, int colorSelect) {
        this.latData = latData;
        this.colorSelect = colorSelect;
        if (latData != null) {
            minData = latData[0][0];
            maxData = latData[0][0];
            for (int i = 0; i < latData.length; i++) {
                for (int j = 0; j < latData[0].length; j++) {
                    minData = Math.min(minData, latData[i][j]);
                    maxData = Math.max(maxData, latData[i][j]);
                }
            }
        }
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int size = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (latData == null || latData.length == 0) {
            canvas.drawColor(Color.LTGRAY);
            canvas.drawText("unavailable",
                    getWidth() / 2,
                    getWidth() / 2,
                    textPaint);
            return;
        }

        int cellSize = getWidth() / latData.length;
        for (int i = 0; i < latData.length; i++) {
            for (int j = 0; j < latData[0].length; j++) {
                int left = j * cellSize;
                int right = (j + 1) * cellSize;
                int top = i * cellSize;
                int bottom = (i + 1) * cellSize;
                int value = latData[i][j];

                int color;
                if (value == 0) {
                    color = Color.rgb(170, 170, 170);
                } else {
                    int colorIntensity = (int) (255 * ((value * 1.d) / (maxData * 1.d)));
                    if (colorSelect == Color.RED) {
                        color = Color.rgb(255, 255 - colorIntensity, 255 - colorIntensity);
                    } else if (colorSelect == Color.GREEN) {
                        color = Color.rgb(255 - colorIntensity, 255, 255 - colorIntensity);
                    } else if (colorSelect == Color.BLUE) {
                        color = Color.rgb(255 - colorIntensity, 255 - colorIntensity, 255);
                    } else {
                        color = Color.rgb(170, 170, 170);
                    }
                }
                rectPaint.setColor(color);
                canvas.drawRect(left, top, right, bottom, rectPaint);

                float textX = left + cellSize / 2.0f;
                float textY = top + cellSize / 2.0f - (textPaint.ascent() + textPaint.descent()) / 2.0f;
                canvas.drawText(String.valueOf(value), textX, textY, textPaint);
            }
        }
    }
}
