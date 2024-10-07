package com.example.eye_smart;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;


import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

public class MainActivity extends AppCompatActivity {

    private static final int LINES_PER_PAGE_DEFAULT = 10; // 기본 페이지 당 표시할 라인 수

    private TextView textView;
    private int currentPage = 0;
    private Uri fileUri;
    private FilePicker filePicker;

    private int maxLinesPerPage = LINES_PER_PAGE_DEFAULT; // 페이지 당 최대 화면 라인 수

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 레이아웃이 모두 그려진 후에 hideSystemUI() 호출
        getWindow().getDecorView().post(this::hideSystemUI);

        // 상단 툴바 설정
        Toolbar toolbarTop = findViewById(R.id.toolbar_top);
        setSupportActionBar(toolbarTop);

        // 뷰 초기화
        textView = findViewById(R.id.textView);
        Button buttonSelectFile = findViewById(R.id.button_select_file);
        Button buttonPrevPage = findViewById(R.id.button_prev_page);
        Button buttonNextPage = findViewById(R.id.button_next_page);

        // 줄 간격 설정
        float lineSpacing = 3;
        textView.setLineSpacing(0, lineSpacing);

        // TextView의 높이를 계산하여 LINES_PER_PAGE를 설정
        calculateLinesPerPage();

        // 파일 선택기 객체 생성 및 런처 설정
        ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            fileUri = data.getData();
                            currentPage = 0;
                            displayPage(currentPage);
                        }
                    }
                }
        );
        filePicker = new FilePicker(this, filePickerLauncher);

        // 파일 선택 버튼 클릭 리스너 설정
        buttonSelectFile.setOnClickListener(v -> filePicker.pickTextFile());

        // 이전 페이지 버튼 클릭 리스너
        buttonPrevPage.setOnClickListener(v -> loadPreviousPage());

        // 다음 페이지 버튼 클릭 리스너
        buttonNextPage.setOnClickListener(v -> loadNextPage());
    }

    // 전체 화면 모드 설정 메서드
    private void hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // API 30 이상 (Android 11 이상)
            getWindow().setDecorFitsSystemWindows(false);
            final WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else { // API 30 미만 (Android 10 이하)
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            );
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI(); // 다시 화면이 포커스를 얻었을 때 시스템 UI 숨김 처리
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // 화면 회전 시 페이지당 라인 수 재계산 및 페이지 내용 업데이트
        calculateLinesPerPage();
    }

    private void calculateLinesPerPage() {
        textView.post(() -> {
            // 실제 텍스트 뷰 높이와 가용 높이 계산
            int textViewHeight = textView.getHeight();
            int availableHeight = textViewHeight - textView.getPaddingTop() - textView.getPaddingBottom();

            if (availableHeight <= 0) {
                maxLinesPerPage = LINES_PER_PAGE_DEFAULT;
                return;
            }

            // 줄 높이 계산
            float lineHeight = textView.getLineHeight();

            if (lineHeight > 0) {
                // 가용 높이와 라인 높이를 이용하여 페이지당 표시할 수 있는 최대 라인 수 계산
                maxLinesPerPage = Math.max(1, (int) Math.floor(availableHeight / lineHeight)); // 최소 1줄은 출력되도록 설정
            } else {
                maxLinesPerPage = LINES_PER_PAGE_DEFAULT;
            }

            // 페이지 계산 후 표시
            displayPage(currentPage);
        });
    }

    private void displayPage(int pageNumber) {
        new Thread(() -> {
            try {
                if (fileUri == null) {
                    runOnUiThread(() -> textView.setText("파일을 선택하지 않았습니다."));
                    return;
                }

                InputStream inputStream = getContentResolver().openInputStream(fileUri);
                if (inputStream == null) {
                    runOnUiThread(() -> textView.setText("파일을 열 수 없습니다."));
                    return;
                }

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                    int currentPage = 0;
                    String line;
                    List<String> pageLines = new ArrayList<>();
                    StringBuilder pageContent = new StringBuilder();
                    TextPaint textPaint = textView.getPaint();
                    int width = textView.getWidth() - textView.getPaddingLeft() - textView.getPaddingRight();

                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty()) {
                            continue;
                        }

                        // 라인을 페이지에 추가
                        pageLines.add(line);
                        pageContent.append(line).append("\n");

                        // 현재까지의 텍스트로 생성되는 화면 라인 수 계산
                        int displayLines = getDisplayLineCount(pageContent.toString(), textPaint, width);

                        if (displayLines > maxLinesPerPage) {
                            if (pageLines.size() == 1) {
                                // 한 라인 자체가 페이지를 초과하는 경우, 해당 라인을 포함시킴
                                if (currentPage == pageNumber) {
                                    // 현재 페이지이므로 그대로 표시
                                    break;
                                } else {
                                    // 다음 페이지로 이동
                                    currentPage++;
                                    pageLines.clear();
                                    pageContent.setLength(0);
                                }
                            } else {
                                // 라인을 제거하지 않고 바로 다음 페이지로 이동
                                if (currentPage == pageNumber) {
                                    // 현재 페이지이면 표시
                                    break;
                                } else {
                                    // 다음 페이지로 이동
                                    currentPage++;
                                    pageLines.clear();
                                    pageContent.setLength(0);
                                }
                            }
                        }
                    }

                    // 원하는 페이지에 도달했는지 확인
                    if (currentPage < pageNumber) {
                        runOnUiThread(() -> textView.setText("더 이상 페이지가 없습니다."));
                        return;
                    }

                    String finalContent = pageContent.toString();

                    runOnUiThread(() -> {
                        // 텍스트를 SpannableString으로 변환하여 스타일 적용
                        SpannableString spannableString = new SpannableString(finalContent);

                        textView.setText(finalContent);

                        // 각 라인의 좌표를 가져오기 위해 레이아웃이 준비된 후 실행
                        textView.post(() -> {
                            Layout layout = textView.getLayout();
                            if (layout != null) {
                                int lineCount = layout.getLineCount();
                                for (int i = 0; i < lineCount; i++) {
                                    int lineStartOffset = layout.getLineStart(i);
                                    int lineEndOffset = layout.getLineEnd(i);

                                    // 각 라인의 (x, y) 좌표 가져오기
                                    float x = layout.getLineLeft(i) + textView.getPaddingLeft();
                                    float y = layout.getLineBaseline(i) + textView.getPaddingTop();

                                    // 라인 텍스트 가져오기
                                    String lineText = finalContent.substring(lineStartOffset, lineEndOffset);
                                    String[] words = lineText.split(" ");

                                    int wordStart = lineStartOffset;
                                    for (String word : words) {
                                        int wordEnd = wordStart + word.length();

                                        // 각 단어에 CustomSpan 적용하여 테두리만 표시
                                        spannableString.setSpan(
                                                new CustomSpan(Color.BLACK), // 테두리 색상 지정
                                                wordStart,
                                                wordEnd,
                                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                                        );

                                        // 다음 단어의 시작 위치로 이동
                                        wordStart = wordEnd + 1; // 단어 간 공백 고려

                                        // 좌표와 함께 로그 출력
                                        Log.d("LineInfo", "단어: " + word + " (x: " + x + ", y: " + y + ")");
                                    }
                                }

                                // 스타일 적용된 SpannableString을 TextView에 설정
                                textView.setText(spannableString);
                            }
                        });
                    });

                } catch (IOException e) {
                    e.printStackTrace();
                    runOnUiThread(() -> textView.setText("파일 읽기 오류 발생!"));
                }

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> textView.setText("파일 열기 오류 발생!"));
            }
        }).start();
    }

    private int getDisplayLineCount(String text, TextPaint textPaint, int width) {
        StaticLayout staticLayout;
        staticLayout = StaticLayout.Builder.obtain(text, 0, text.length(), textPaint, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(textView.getLineSpacingExtra(), textView.getLineSpacingMultiplier())
                .setIncludePad(textView.getIncludeFontPadding())
                .build();

        return staticLayout.getLineCount();
    }

    private void loadNextPage() {
        currentPage++;
        displayPage(currentPage);
    }

    private void loadPreviousPage() {
        if (currentPage > 0) {
            currentPage--;
            displayPage(currentPage);
        } else {
            runOnUiThread(() -> textView.setText("이전 페이지가 없습니다."));
        }
    }
}

