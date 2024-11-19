package com.example.eye_smart.gaze_utils;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import android.text.TextPaint;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;

public class CalibrationView extends ViewGroup {

    private static final int DEFAULT_TEXT_COLOR = Color.WHITE;
    private static final float DEFAULT_TEXT_SIZE_SP = 24f;

    // 시선 보정 점의 색상 배열
    private final int[] calibrationPointColors = {
            Color.parseColor("#ffe400"), // 노란색
            //Color.parseColor("#F7F478"), // 빨강색
            //Color.parseColor("#AB47BC"), // 보라색
            //Color.parseColor("#42A5F5"), // 파랑색
            //Color.parseColor("#66BB6A"), // 초록색
            //Color.parseColor("#CA9A00"), // 갈색
    };

    private int currentColorIndex = 0;
    private TextPaint messagePaint;
    private boolean isMessageVisible = true;
    private CalibrationDot calibrationDot;
    private Drawable backgroundDrawable;

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

    // 초기 설정
    private void initialize() {
        setupMessagePaint();
        setupCalibrationDot();
        setWillNotDraw(false);
    }

    // 메시지 페인트 설정
    private void setupMessagePaint() {
        messagePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        messagePaint.setColor(DEFAULT_TEXT_COLOR);
        messagePaint.setTextAlign(Paint.Align.CENTER);
        messagePaint.setTextSize(dpToPx()); // 텍스트 크기 설정
        messagePaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD_ITALIC));
        messagePaint.setShadowLayer(10, 0, 0, Color.BLACK); // 텍스트 그림자 추가
    }

    // 보정 점 설정
    private void setupCalibrationDot() {
        calibrationDot = new CalibrationDot(getContext());
        addView(calibrationDot);
    }

    // SP 단위를 픽셀로 변환
    private float dpToPx() {
        return CalibrationView.DEFAULT_TEXT_SIZE_SP * getResources().getDisplayMetrics().scaledDensity;
    }

    // 보정 뷰 오프셋 설정
    public void setOffset(float x, float y) {
        offsetX = x;
        offsetY = y;
        requestLayout();
    }

    // 메시지 가시성 설정
    public void setMessageVisibility(boolean isVisible) {
        isMessageVisible = isVisible;
    }

    // 보정 점 색상 변경
    public void moveToNextDotColor() {
        currentColorIndex = (currentColorIndex + 1) % calibrationPointColors.length;
        calibrationDot.setDotColor(calibrationPointColors[currentColorIndex]);
        calibrationDot.animateDot(100); // 애니메이션 설정
        invalidate();
    }

    // 보정 점 위치 설정
    public void setDotPosition(float x, float y) {
        calibrationDot.setPosition(x - offsetX, y - offsetY);
        requestLayout();
    }

    // 보정 점 애니메이션 스케일 설정
    public void setDotAnimationScale(float progress) {
        calibrationDot.setProgress(progress);
    }

    // 측정 메서드 재정의
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        measureChild(calibrationDot, widthMeasureSpec, heightMeasureSpec);
        int width = resolveSize(calibrationDot.getMeasuredWidth(), widthMeasureSpec);
        int height = resolveSize(calibrationDot.getMeasuredHeight(), heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    // 레이아웃 설정
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

    // 배경과 텍스트 그리기
    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        // 배경을 먼저 그리기
        if (backgroundDrawable != null) {
            backgroundDrawable.setBounds(0, 0, getWidth(), getHeight());
            backgroundDrawable.draw(canvas);
        }

        // 기존 메시지 및 다른 요소 그리기
        if (isMessageVisible) {
            String instructionMessage = "잠시 뒤 나오는 점들을 응시해주세요!";
            float x = getWidth() / 2f;
            float y = getHeight() / 2f - (messagePaint.descent() + messagePaint.ascent()) / 2f;
            canvas.drawText(instructionMessage, x, y, messagePaint);
        }
    }
    // 보정 점 클래스
    private static class CalibrationDot extends View {
        private static final float DEFAULT_RADIUS_DP = 30f;

        private final Paint outerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint innerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        private float positionX = 0f, positionY = 0f;
        private float radius;
        private float progress = 0f;

        public CalibrationDot(Context context) {
            super(context);
            initialize();
        }

        // 초기화 설정
        private void initialize() {
            radius = dpToPx();
            outerPaint.setStyle(Paint.Style.STROKE); // 외곽선 스타일
            outerPaint.setStrokeWidth(1f);           // 외곽선 두께
            outerPaint.setColor(Color.WHITE);  // 외곽선 색상 설정
            outerPaint.setStyle(Paint.Style.FILL);   // 외부 페인트 스타일
            innerPaint.setStyle(Paint.Style.FILL);   // 내부 페인트 스타일
        }

        // DP 단위를 픽셀로 변환
        private float dpToPx() {
            return CalibrationDot.DEFAULT_RADIUS_DP * getResources().getDisplayMetrics().density;
        }

        // 애니메이션 진행 정도 설정
        public void setProgress(float progress) {
            this.progress = progress;
            invalidate();
        }

        // 위치 설정
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

        // 점의 색상 설정
        public void setDotColor(int color) {
            innerPaint.setColor(color);
            invalidate();
        }

        public float getRadius() {
            return radius;
        }

        // 점 애니메이션 실행
        public void animateDot(long duration) {
            ValueAnimator animator = ValueAnimator.ofFloat(0, 360);
            animator.setDuration(duration);
            animator.setInterpolator(new DecelerateInterpolator());
            animator.addUpdateListener(animation -> {
                animation.getAnimatedValue();
                invalidate();
            });
            animator.start();
        }

        // 측정 메서드 재정의
        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int size = (int) (2 * radius);
            setMeasuredDimension(size, size);
        }

        // 점 그리기
        @Override
        protected void onDraw(Canvas canvas) {
            float center = getWidth() / 2f;

            canvas.drawCircle(center, center, radius, outerPaint); // 외곽선 원 그리기

            // 점의 채워지는 효과를 위해 클리핑 영역 설정
            float top = center + radius * (1 - (progress * 2));
            canvas.save();
            canvas.clipRect(center - radius, top, center + radius, center + radius);
            canvas.drawCircle(center, center, radius, innerPaint); // 내부 원 그리기
            canvas.restore(); // 클리핑 해제
        }
    }
}
