package com.example.eye_smart.gaze_utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;


import android.animation.ObjectAnimator;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

public class CalibrationView extends FrameLayout {
    private CalibrationPoint calibrationPoint;
    private final float radius = 30f;

    public CalibrationView(Context context) {
        super(context);
        init();
    }

    public CalibrationView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        calibrationPoint = new CalibrationPoint(getContext());
        int size = (int)(radius * 2);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size);
        calibrationPoint.setLayoutParams(params);
        addView(calibrationPoint);
    }

    // 점의 위치를 설정하는 메서드
    public void setPointPosition(float x, float y) {
        float left = x - radius;
        float top = y - radius;

        calibrationPoint.setX(left);
        calibrationPoint.setY(top);

        Log.d("CalibrationView", "setPointPosition: set calibrationPoint x=" + left + ", y=" + top);
    }

    public void startPointAnimation() {
        ObjectAnimator scaleXAnimator = ObjectAnimator.ofFloat(calibrationPoint, "scaleX", 1.0f, 1.5f, 1.0f);
        ObjectAnimator scaleYAnimator = ObjectAnimator.ofFloat(calibrationPoint, "scaleY", 1.0f, 1.5f, 1.0f);

        scaleXAnimator.setDuration(1000);
        scaleYAnimator.setDuration(1000);
        scaleXAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        scaleYAnimator.setRepeatCount(ObjectAnimator.INFINITE);

        scaleXAnimator.start();
        scaleYAnimator.start();
    }

    private static class CalibrationPoint extends View {
        private final Paint paint;
        private final float radius = 50f;

        public CalibrationPoint(Context context) {
            super(context);
            paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(Color.BLUE);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int size = (int)(radius * 2);
            setMeasuredDimension(size, size);
        }

        @Override
        protected void onDraw(@NonNull Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawCircle(radius, radius, radius, paint);
        }
    }
}
