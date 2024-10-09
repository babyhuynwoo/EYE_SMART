package com.example.eye_smart;

import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.widget.TextView;

public class TextDisplayUtil {

    // TextView의 가용 높이에 기반한 페이지 당 최대 라인 수 계산 메서드
    public static int calculateMaxLinesPerPage(TextView textView) {
        int textViewHeight = textView.getHeight();
        int availableHeight = textViewHeight - textView.getPaddingTop() - textView.getPaddingBottom();

        if (availableHeight <= 0) return 10;  // 기본 값

        float lineHeight = textView.getLineHeight();
        if (lineHeight > 0) {
            return Math.max(1, (int) Math.floor(availableHeight / lineHeight));
        }
        return 10;
    }

    // 텍스트의 실제 화면 라인 수 계산
    public static int getDisplayLineCount(String text, TextPaint textPaint, int width, TextView textView) {
        StaticLayout staticLayout = StaticLayout.Builder.obtain(text, 0, text.length(), textPaint, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(textView.getLineSpacingExtra(), textView.getLineSpacingMultiplier())
                .setIncludePad(textView.getIncludeFontPadding())
                .build();

        return staticLayout.getLineCount();
    }
}
