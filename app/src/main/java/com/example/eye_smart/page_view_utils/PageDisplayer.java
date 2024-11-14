package com.example.eye_smart.page_view_utils;

import android.text.TextPaint;
import android.util.Log;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import com.example.eye_smart.file_utils.FileLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import android.graphics.Color;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.StaticLayout;

public class PageDisplayer {

    private final TextView textView;
    private int textViewWidth;
    private int maxLinesPerPage;
    private boolean isLayoutReady = false;
    private final List<Runnable> pendingTasks = new ArrayList<>();

    public PageDisplayer(TextView textView, float lineSpacingMultiplier, float lineSpacingExtra) {
        this.textView = textView;

        // 줄 간격 적용
        textView.setLineSpacing(lineSpacingExtra, lineSpacingMultiplier);

        // 글자간 간격 적용
        textView.setLetterSpacing(0.1f);

        // TextView의 레이아웃이 완료되면 호출되는 리스너 등록
        textView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // 리스너 제거
                textView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                // 텍스트뷰의 폭과 최대 라인 수 계산
                textViewWidth = textView.getWidth() - textView.getPaddingLeft() - textView.getPaddingRight();
                maxLinesPerPage = calculateMaxLinesPerPage(textView);

                isLayoutReady = true; // 레이아웃이 준비되었음을 표시

                // 대기 중인 작업 실행
                for (Runnable task : pendingTasks) {
                    task.run();
                }
                pendingTasks.clear(); // 작업 리스트 초기화
            }
        });
    }

    // 페이지당 최대 라인 수 계산 메서드
    private int calculateMaxLinesPerPage(TextView textView) {
        int textViewHeight = textView.getHeight();
        int availableHeight = textViewHeight - textView.getPaddingTop() - textView.getPaddingBottom();

        if (availableHeight <= 0) return 10;

        float lineHeight = textView.getLineHeight();
        if (lineHeight > 0) {
            return Math.max(1, (int) (availableHeight / lineHeight));
        }

        return 10;
    }

    // 화면에 표시할 줄 수 계산 메서드
    private int getDisplayLineCountForLine(String line, TextPaint textPaint, int width, TextView textView) {
        StaticLayout staticLayout = StaticLayout.Builder.obtain(line, 0, line.length(), textPaint, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(textView.getLineSpacingExtra(), textView.getLineSpacingMultiplier())
                .setIncludePad(textView.getIncludeFontPadding())
                .build();

        return staticLayout.getLineCount();
    }

    // 텍스트 스타일링 및 페이지 표시
    public void displayPage(FileLoader fileLoader, int pageNumber, Consumer<Integer> updateCurrentPage) {
        Runnable task = () -> {
            new Thread(() -> {
                try {
                    if (fileLoader == null) {
                        textView.post(() -> textView.setText("파일을 선택하지 않았습니다."));
                        return;
                    }

                    BufferedReader reader = fileLoader.getBufferedReader();
                    int currentPage = 0;
                    String line;
                    StringBuilder pageContent = new StringBuilder();
                    TextPaint textPaint = textView.getPaint();

                    int totalDisplayLines = 0;

                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty()) continue;

                        int lineDisplayLines = getDisplayLineCountForLine(line, textPaint, textViewWidth, textView);

                        if (totalDisplayLines + lineDisplayLines > maxLinesPerPage) {
                            if (currentPage == pageNumber) break;
                            currentPage++;
                            totalDisplayLines = 0;
                            pageContent.setLength(0);
                        }

                        pageContent.append(line).append("\n");
                        totalDisplayLines += lineDisplayLines;
                    }
                    reader.close();

                    if (currentPage < pageNumber) {
                        textView.post(() -> textView.setText("더 이상 페이지가 없습니다."));
                        return;
                    }

                    int finalCurrentPage = currentPage;
                    updateCurrentPage.accept(finalCurrentPage);

                    textView.post(() -> styleAndDisplayText(pageContent.toString()));

                } catch (IOException e) {
                    textView.post(() -> textView.setText("파일 읽기 오류 발생!"));
                    Log.e("PageDisplayer", "파일 읽기 오류 발생: " + e.getMessage());
                }
            }).start();
        };

        if (isLayoutReady) {
            task.run();
        } else {
            pendingTasks.add(task);
        }
    }

    // 텍스트 스타일링 및 표시 메서드
    private void styleAndDisplayText(String finalContent) {
        SpannableString spannableString = new SpannableString(finalContent);
        textView.setText(finalContent);

        textView.post(() -> {
            Layout layout = textView.getLayout();
            if (layout != null) {
                int lineCount = layout.getLineCount();
                float textSize = textView.getTextSize();

                for (int i = 0; i < lineCount; i++) {
                    int lineStartOffset = layout.getLineStart(i);
                    int lineEndOffset = layout.getLineEnd(i);

                    String lineText = finalContent.substring(lineStartOffset, lineEndOffset);
                    String[] words = lineText.split(" ");

                    int wordStart = lineStartOffset;
                    for (String word : words) {
                        int wordEnd = wordStart + word.length();

                        spannableString.setSpan(
                                new CustomSpan(Color.RED, 10, textSize, -50),
                                wordStart,
                                wordEnd,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        );
                        wordStart = wordEnd + 1;
                    }
                }

                textView.setText(spannableString);
            }
        });
    }
}
