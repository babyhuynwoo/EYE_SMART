package com.example.eye_smart;

import android.graphics.Color;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;
import android.widget.TextView;

public class TextDisplayUtil {

    // TextView의 가용 높이에 기반한 페이지 당 최대 라인 수 계산 메서드
    public static int calculateMaxLinesPerPage(TextView textView) {
        int textViewHeight = textView.getHeight();
        int availableHeight = textViewHeight - textView.getPaddingTop() - textView.getPaddingBottom();

        Log.d("TextDisplayUtil", "TextView 높이: " + textViewHeight + ", 가용 높이: " + availableHeight);

        if (availableHeight <= 0) return 10;  // 기본 값

        // TextView의 실제 라인 높이를 가져옵니다.
        float lineHeight = textView.getLineHeight();

        Log.d("TextDisplayUtil", "TextView의 getLineHeight(): " + lineHeight);

        if (lineHeight > 0) {
            int maxLines = Math.max(1, (int) (availableHeight / lineHeight));
            Log.d("TextDisplayUtil", "계산된 최대 라인 수: " + maxLines);
            return maxLines;
        }

        return 10; // 기본 값 반환
    }

    // 각 라인의 실제 화면 라인 수를 계산하는 메서드
    public static int getDisplayLineCountForLine(String line, TextPaint textPaint, int width, TextView textView) {
        // 백그라운드 스레드에서 안전하게 실행하기 위해 StaticLayout 사용
        StaticLayout staticLayout = StaticLayout.Builder.obtain(line, 0, line.length(), textPaint, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(textView.getLineSpacingExtra(), textView.getLineSpacingMultiplier())
                .setIncludePad(textView.getIncludeFontPadding())
                .build();

        int displayLineCount = staticLayout.getLineCount(); // 해당 라인의 화면 라인 수 계산

        // 각 라인의 화면 라인 수 로그 출력
        Log.d("TextDisplayUtil", "텍스트: \"" + line + "\"의 화면 라인 수: " + displayLineCount);
        return displayLineCount;
    }

    // 텍스트 스타일링 및 표시 메서드
    public static void styleAndDisplayText(TextView textView, String finalContent) {
        SpannableString spannableString = new SpannableString(finalContent);
        textView.setText(finalContent);

        textView.post(() -> {
            Layout layout = textView.getLayout();
            if (layout != null) {
                int lineCount = layout.getLineCount();

                // TextView의 textSize 값을 가져옵니다.
                float textSize = textView.getTextSize(); // TextSize를 가져옴 (픽셀 단위)

                for (int i = 0; i < lineCount; i++) {
                    int lineStartOffset = layout.getLineStart(i);
                    int lineEndOffset = layout.getLineEnd(i);

                    String lineText = finalContent.substring(lineStartOffset, lineEndOffset);
                    String[] words = lineText.split(" ");

                    int wordStart = lineStartOffset;
                    for (String word : words) {
                        int wordEnd = wordStart + word.length();

                        // CustomSpan 생성 시 textSize 값을 전달
                        spannableString.setSpan(
                                new CustomSpan(Color.BLACK, 50, textSize), // textSize 추가 전달
                                wordStart,
                                wordEnd,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        );
                        wordStart = wordEnd + 1; // 다음 단어로 이동
                    }
                }

                textView.setText(spannableString);
            }
        });
    }
}
