package com.example.eye_smart;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int READ_REQUEST_CODE = 18;
    private static final int LINES_PER_PAGE = 6; // 페이지당 표시할 라인 수

    private TextView textView;
    private Button buttonSelectFile;
    private Button buttonPrevPage;
    private Button buttonNextPage;

    private List<String> allLines = new ArrayList<>();
    private int currentPage = 0;
    private int totalPages = 0;
    private Uri fileUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 상단 툴바 설정
        Toolbar toolbarTop = findViewById(R.id.toolbar_top);
        setSupportActionBar(toolbarTop);

        // 하단 툴바 설정
        Toolbar toolbarBottom = findViewById(R.id.toolbar_bottom);

        // 뷰 초기화
        textView = findViewById(R.id.textView);
        buttonSelectFile = findViewById(R.id.button_select_file);
        buttonPrevPage = findViewById(R.id.button_prev_page);
        buttonNextPage = findViewById(R.id.button_next_page);

        textView.setLineSpacing(0, 4f);

        // 파일 선택 버튼 클릭 리스너 설정
        buttonSelectFile.setOnClickListener(v -> openFilePicker());

        // 이전 페이지 버튼 클릭 리스너
        buttonPrevPage.setOnClickListener(v -> loadPreviousPage());

        // 다음 페이지 버튼 클릭 리스너
        buttonNextPage.setOnClickListener(v -> loadNextPage());
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == READ_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null) {
                fileUri = data.getData();
                currentPage = 0;
                readFile();
            }
        }
    }

    private void readFile() {
        new Thread(() -> {
            try (InputStream inputStream = getContentResolver().openInputStream(fileUri);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

                allLines.clear();
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim(); // 앞뒤 공백 제거
                    if (!line.isEmpty()) {
                        allLines.add(line);
                    }
                }

                totalPages = (int) Math.ceil((double) allLines.size() / LINES_PER_PAGE);

                runOnUiThread(() -> displayPage(currentPage));

            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> textView.setText("파일 읽기 오류 발생!"));
            }
        }).start();
    }

    private void displayPage(int pageNumber) {
        int startLine = pageNumber * LINES_PER_PAGE;
        int endLine = Math.min(startLine + LINES_PER_PAGE, allLines.size());

        if (startLine >= allLines.size()) {
            textView.setText("더 이상 페이지가 없습니다.");
            return;
        }

        List<String> pageLines = allLines.subList(startLine, endLine);
        String pageContent = String.join("\n", pageLines);

        runOnUiThread(() -> textView.setText(pageContent));
    }

    private void loadNextPage() {
        if (currentPage < totalPages - 1) {
            currentPage++;
            displayPage(currentPage);
        }
    }

    private void loadPreviousPage() {
        if (currentPage > 0) {
            currentPage--;
            displayPage(currentPage);
        }
    }
}
