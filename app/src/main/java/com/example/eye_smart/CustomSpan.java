package com.example.eye_smart;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.style.ReplacementSpan;

public class CustomSpan extends ReplacementSpan {

    private final Paint paint = new Paint();

    public CustomSpan(int borderColor) {
        paint.setColor(borderColor);
        paint.setStyle(Paint.Style.STROKE); // 내부는 채우지 않고 테두리만 그리도록 설정
        paint.setStrokeWidth(5); // 테두리 두께 설정
    }

    @Override
    public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
        return Math.round(paint.measureText(text, start, end)); // 텍스트의 폭 계산
    }

    @Override
    public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint) {
        // 텍스트의 범위를 네모 상자로 그림 (테두리만)
        Rect rect = new Rect((int) x, top, (int) (x + paint.measureText(text, start, end)), bottom);
        canvas.drawRect(rect, this.paint); // 사각형 테두리 그리기

        // 텍스트 그리기
        canvas.drawText(text, start, end, x, y, paint);
    }
}
