package com.applovin.ramtool.ram.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;

import java.util.List;

public class LineChartView extends FramBase {
    private Paint mPointPaint;
    private int defaultPointColor = Color.RED;
    private int defaultDataLineColor = Color.RED;

    public LineChartView(Context context) {
        this(context, null);
    }

    public LineChartView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LineChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        initPaint();
    }

    private void initPaint() {
        mTitlePaint = new Paint();
        mTitlePaint.setAntiAlias(true);
        mTitlePaint.setColor(Color.GRAY);
        mTitlePaint.setStyle(Paint.Style.STROKE);
        mTitlePaint.setTextSize(mTitleTextSize);

        mBorderLinePaint = new Paint();
        mBorderLinePaint.setColor(defaultBorderColor);
        mBorderLinePaint.setStyle(Paint.Style.STROKE);
        mBorderLinePaint.setStrokeWidth(dp2px(1));
        mBorderLinePaint.setAntiAlias(true);

        mTextPaint = new Paint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setColor(Color.GRAY);
        mTextPaint.setStyle(Paint.Style.STROKE);
        mTextPaint.setTextSize(mLabelTextSize);

        mPointPaint = new Paint();
        mPointPaint.setAntiAlias(true);

        mPointPaint.setTextSize(mLabelTextSize);
        mPointPaint.setStrokeWidth(dp2px(2));

        mDataLinePaint = new Paint();
        mDataLinePaint.setAntiAlias(true);
        mDataLinePaint.setColor(defaultDataLineColor);
        mDataLinePaint.setStyle(Paint.Style.STROKE);
        mDataLinePaint.setStrokeWidth(dp2px(2));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawDataLines(canvas);
    }

    private void drawDataLines(Canvas canvas) {
        if (mTruelyDrawDatas.size() == 0) {
            return;
        }
        Path path = new Path();
        path.moveTo(0.5f * mBorderLandLength / showNum, (float) (mBorderVerticalLength * 0.95f / maxData * mTruelyDrawDatas.get(0)));
        for (int i = 0; i < (showNum > mTruelyDrawDatas.size() ? mTruelyDrawDatas.size() : showNum); i++) {
            float x = (i + 0.5f) * mBorderLandLength / showNum;
            float y = (float) (mBorderVerticalLength * 0.95f / maxData * mTruelyDrawDatas.get(i));
            path.lineTo(x, y);
        }
        canvas.drawPath(path, mDataLinePaint);
    }

    public void setDatas(List<Double> mDatas, List<String> mDesciption) {
        this.mDatas = mDatas;
        this.mDescription = mDesciption;

        if (showNum > mDatas.size()) {
            this.mTruelyDrawDatas.addAll(mDatas);
            this.mTruelyDescription.addAll(mDesciption);
        } else {
            this.mTruelyDrawDatas.addAll(mDatas.subList(0, showNum));
            this.mTruelyDescription.addAll(mDesciption.subList(0, showNum));
        }

//        animator.start();
        postInvalidate();
    }

    @Override
    public void setShowNum(int showNum) {
        this.showNum = showNum;
    }

}
