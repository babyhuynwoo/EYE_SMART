package com.example.eye_smart.gaze_utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import android.text.TextPaint;
import android.view.ViewGroup;

public class CalibrationView extends ViewGroup {

    private static final int DEFAULT_BACKGROUND_COLOR = Color.rgb(0x64, 0x5E, 0x5E);
    private static final int DEFAULT_TEXT_COLOR = Color.WHITE;
    private static final float DEFAULT_TEXT_SIZE_SP = 16f;

    private final int[] calibrationPointColors = {
            Color.parseColor("#EF5350"), // Red
            Color.parseColor("#AB47BC"), // Purple
            Color.parseColor("#FFA726"), // Orange
            Color.parseColor("#42A5F5"), // Blue
            Color.parseColor("#66BB6A"), // Green
            Color.parseColor("#CA9A00"), // Brown
            Color.parseColor("#FFFD00")  // Yellow
    };

    private int currentColorIndex = 0;
    private Paint backgroundPaint;
    private TextPaint messagePaint;
    private String instructionMessage = "Please stare at this point.";
    private boolean isMessageVisible = true;
    private CalibrationDot calibrationDot;

    private float offsetX = 0, offsetY = 0;

    public CalibrationView(Context context) {
        this(context, null);
    }

    public CalibrationView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CalibrationView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    private void initialize() {
        backgroundPaint = new Paint();
        backgroundPaint.setStyle(Paint.Style.FILL);
        backgroundPaint.setColor(DEFAULT_BACKGROUND_COLOR);

        messagePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        messagePaint.setColor(DEFAULT_TEXT_COLOR);
        messagePaint.setTextAlign(Paint.Align.CENTER);
        messagePaint.setTextSize(convertSpToPx(DEFAULT_TEXT_SIZE_SP));

        calibrationDot = new CalibrationDot(getContext());
        addView(calibrationDot);

        setWillNotDraw(false);
    }

    private float convertSpToPx(float sp) {
        return sp * getResources().getDisplayMetrics().scaledDensity;
    }

    public void setOffset(float x, float y) {
        offsetX = x;
        offsetY = y;
        requestLayout();
    }

    public void setMessageVisibility(boolean isVisible) {
        isMessageVisible = isVisible;
    }

    @Override
    public void setBackgroundColor(int color) {
        backgroundPaint.setColor(color);
        invalidate();
    }

    public void moveToNextDotColor() {
        currentColorIndex = (currentColorIndex + 1) % calibrationPointColors.length;
        calibrationDot.setDotColor(calibrationPointColors[currentColorIndex]);
        invalidate();
    }

    public void setDotPosition(float x, float y) {
        calibrationDot.setPosition(x - offsetX, y - offsetY);
        requestLayout();
    }

    public void setDotAnimationScale(float scale) {
        calibrationDot.setAnimationScale(scale);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        measureChild(calibrationDot, widthMeasureSpec, heightMeasureSpec);
        int width = resolveSize(calibrationDot.getMeasuredWidth(), widthMeasureSpec);
        int height = resolveSize(calibrationDot.getMeasuredHeight(), heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        float centerX = calibrationDot.getPositionX();
        float centerY = calibrationDot.getPositionY();
        float radius = calibrationDot.getRadius();

        int left = (int) (centerX - radius);
        int top = (int) (centerY - radius);
        int right = (int) (centerX + radius);
        int bottom = (int) (centerY + radius);

        calibrationDot.layout(left, top, right, bottom);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawRect(0, 0, getWidth(), getHeight(), backgroundPaint);
        if (isMessageVisible && instructionMessage != null && !instructionMessage.isEmpty()) {
            float x = getWidth() / 2f;
            float y = getHeight() / 2f - (messagePaint.descent() + messagePaint.ascent()) / 2f;
            canvas.drawText(instructionMessage, x, y, messagePaint);
        }
    }

    private static class CalibrationDot extends View {
        private static final float DEFAULT_RADIUS_DP = 30f;

        private final Paint outerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint innerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        private float positionX = 0f, positionY = 0f;
        private float radius;
        private float animationScale = 0f;

        public CalibrationDot(Context context) {
            super(context);
            initialize();
        }

        private void initialize() {
            radius = convertDpToPx(DEFAULT_RADIUS_DP);

            outerPaint.setStyle(Paint.Style.FILL);
            innerPaint.setStyle(Paint.Style.FILL);
        }

        private float convertDpToPx(float dp) {
            return dp * getResources().getDisplayMetrics().density;
        }

        public void setPosition(float x, float y) {
            positionX = x;
            positionY = y;
            requestLayout();
        }

        public float getPositionX() {
            return positionX;
        }

        public float getPositionY() {
            return positionY;
        }

        public void setDotColor(int color) {
            outerPaint.setColor(Color.argb(100, Color.red(color), Color.green(color), Color.blue(color)));
            innerPaint.setColor(color);
            invalidate();
        }

        public void setAnimationScale(float scale) {
            animationScale = scale;
            invalidate();
        }

        public float getRadius() {
            return radius;
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int size = (int) (2 * radius);
            setMeasuredDimension(size, size);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float animatedRadius = radius - ((radius / 2) * animationScale);
            float center = getWidth() / 2f;
            canvas.drawCircle(center, center, radius, outerPaint);
            canvas.drawCircle(center, center, animatedRadius, innerPaint);
        }
    }
}
