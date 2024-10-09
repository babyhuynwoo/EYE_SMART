package com.example.eye_smart;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.style.ReplacementSpan;

public class CustomSpan extends ReplacementSpan {

    private final Paint paint = new Paint();
    private final int paddingY; // y 축 여백을 위한 변수
    private final float textSize; // TextView의 textSize를 저장할 변수

    public CustomSpan(int borderColor, int paddingY, float textSize) { // textSize 추가
        this.paddingY = paddingY; // 전달받은 y축 여백 값 저장
        this.textSize = textSize; // 전달받은 textSize 값 저장
        paint.setColor(borderColor);
        paint.setStyle(Paint.Style.STROKE); // 내부는 채우지 않고 테두리만 그리도록 설정
        paint.setStrokeWidth(5); // 테두리 두께 설정
    }

    @Override
    public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
        // draw() 메서드에서 사용할 paint를 통해 텍스트 폭 계산
        return Math.round(paint.measureText(text, start, end));
    }

    @Override
    public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint) {
        // textSize를 기반으로 margin을 설정
        int margin = Math.round(textSize / 2);  // textSize를 기준으로 margin 계산
        int adjustedTop = top - paddingY + margin; // 위쪽 여백 조정
        int adjustedBottom = bottom - paddingY + margin; // 아래쪽 여백 조정

        // 텍스트의 폭을 paint 파라미터를 사용하여 정확하게 계산
        float textWidth = paint.measureText(text, start, end);

        // 사각형의 x 축은 그대로 두고 y 축만 조절된 범위로 변경
        Rect rect = new Rect((int) x, adjustedTop, (int) (x + textWidth), adjustedBottom);
        canvas.drawRect(rect, this.paint); // 사각형 테두리 그리기

        // 텍스트 그리기 (y 좌표는 그대로 사용하여 텍스트의 위치를 유지)
        canvas.drawText(text, start, end, x, y + margin, paint);
    }
}
