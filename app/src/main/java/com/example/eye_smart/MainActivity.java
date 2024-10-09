package com.example.eye_smart;

import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextPaint;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import android.util.Log;

public class MainActivity extends AppCompatActivity {

    private static final int LINES_PER_PAGE_DEFAULT = 10; // 기본 페이지 당 표시할 라인 수

    private TextView textView;
    private int textViewWidth;
    private int currentPage = 0;
    private Uri fileUri;
    private FilePicker filePicker;
    private FileLoader fileLoader;

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
        float lineSpacing = 3f;
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
                            fileLoader = new FileLoader(this, fileUri); // FileLoader 초기화
                            displayPage(currentPage);
                        }
                    }
                }
        );
        filePicker = new FilePicker(filePickerLauncher);

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

        // TextView의 너비를 미리 계산하여 저장
        textView.post(() -> {
            textViewWidth = textView.getWidth() - textView.getPaddingLeft() - textView.getPaddingRight();
            TextDisplayUtil.calculateMaxLinesPerPage(textView);
        });
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // 화면 회전 시 페이지당 라인 수 재계산 및 페이지 내용 업데이트
        calculateLinesPerPage();
    }

    private void calculateLinesPerPage() {
        textView.post(() -> {
            maxLinesPerPage = TextDisplayUtil.calculateMaxLinesPerPage(textView);
            Log.d("MainActivity", "계산된 maxLinesPerPage: " + maxLinesPerPage);
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

                // 파일 읽기 시도
                BufferedReader reader = fileLoader.getBufferedReader();

                int currentPage = 0;
                String line;
                StringBuilder pageContent = new StringBuilder();
                TextPaint textPaint = textView.getPaint();
                int width = textViewWidth; // 미리 계산된 너비 사용

                int totalDisplayLines = 0; // 현재 페이지의 총 화면 라인 수

                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) {
                        continue;
                    }

                    // 개별 라인의 화면 라인 수 계산
                    int lineDisplayLines = TextDisplayUtil.getDisplayLineCountForLine(line, textPaint, width, textView);

                    if (totalDisplayLines + lineDisplayLines > maxLinesPerPage) {
                        // 페이지 크기를 초과한 경우
                        if (currentPage == pageNumber) {
                            // 현재 페이지이면 페이지 내용을 표시
                            break;
                        } else {
                            // 다음 페이지로 이동
                            currentPage++;
                            totalDisplayLines = 0; // 새로운 페이지의 라인 수 초기화
                            pageContent.setLength(0); // 페이지 내용 초기화
                        }
                    }

                    // 페이지 내용에 라인 추가
                    pageContent.append(line).append("\n");
                    totalDisplayLines += lineDisplayLines; // 현재 페이지의 총 화면 라인 수 업데이트
                }

                reader.close();

                // 원하는 페이지에 도달했는지 확인
                if (currentPage < pageNumber) {
                    runOnUiThread(() -> textView.setText("더 이상 페이지가 없습니다."));
                    return;
                }

                String finalContent = pageContent.toString();

                runOnUiThread(() -> {
                    TextDisplayUtil.styleAndDisplayText(textView, finalContent);
                });

            } catch (IOException e) {
                // 파일 읽기 오류에 대한 예외 처리 및 로그 출력
                Log.e("MainActivity", "파일 읽기 오류 발생: " + e.getMessage());
                e.printStackTrace();
                runOnUiThread(() -> textView.setText("파일 읽기 오류 발생!"));
            } catch (Exception e) {
                // 기타 예외 처리 및 로그 출력
                Log.e("MainActivity", "파일 열기 오류 발생: " + e.getMessage());
                e.printStackTrace();
                runOnUiThread(() -> textView.setText("파일 열기 오류 발생!"));
            }
        }).start();
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
