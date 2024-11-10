package com.example.eye_smart.page_view_utils;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.style.ReplacementSpan;

public class CustomSpan extends ReplacementSpan {

    private final Paint paint = new Paint();
    private final int paddingY; // y 축 여백을 위한 변수
    private final float textSize; // TextView의 textSize를 저장할 변수
    private final int offsetY; // 박스를 위로 이동시키기 위한 오프셋

    public CustomSpan(int borderColor, int paddingY, float textSize, int offsetY) { // offsetY 추가
        this.paddingY = paddingY;
        this.textSize = textSize;
        this.offsetY = offsetY; // 전달받은 오프셋 값 저장
        paint.setColor(borderColor);
        paint.setStyle(Paint.Style.STROKE); // 내부는 채우지 않고 테두리만 그리도록 설정
        paint.setStrokeWidth(5); // 테두리 두께 설정
    }

    @Override
    public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
        return Math.round(paint.measureText(text, start, end));
    }

    @Override
    public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint) {
        // textSize를 기반으로 margin을 설정
        int margin = Math.round(textSize / 2);
        int adjustedTop = top - paddingY + margin + offsetY; // offsetY로 위로 이동
        int adjustedBottom = bottom - paddingY + margin + offsetY; // offsetY로 위로 이동

        // 텍스트의 폭을 paint 파라미터를 사용하여 정확하게 계산
        float textWidth = paint.measureText(text, start, end);

        // 사각형 그리기
        Rect rect = new Rect((int) x, adjustedTop, (int) (x + textWidth), adjustedBottom);
        canvas.drawRect(rect, this.paint);

        // 텍스트 그리기
        canvas.drawText(text, start, end, x, y + margin, paint);
    }
}
