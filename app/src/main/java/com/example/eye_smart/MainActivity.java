package com.example.eye_smart;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.Manifest;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.eye_smart.file_utils.FileLoader;
import com.example.eye_smart.file_utils.FilePicker;
import com.example.eye_smart.page_view_utils.PageDisplayer;
import com.example.eye_smart.dict_utils.ServerCommunicator;
import com.example.eye_smart.dict_utils.JsonParser;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final int MANAGE_STORAGE_PERMISSION_REQUEST_CODE = 2;
    private int currentPage = 0;

    private TextView textView;
    private FileLoader fileLoader;
    private PageDisplayer pageDisplayer;
    private ServerCommunicator serverCommunicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI 초기화
        initUI();

        // PageDisplayer와 ServerCommunicator 초기화
        pageDisplayer = new PageDisplayer(textView, 3f, 2f);
        serverCommunicator = new ServerCommunicator();

        // 권한 요청 및 파일 로드
        checkAndRequestPermissions();
    }

    // 자동 권한 요청 메서드
    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11 이상인 경우
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, MANAGE_STORAGE_PERMISSION_REQUEST_CODE);
            } else {
                loadFileFromIntent();  // 권한이 이미 있는 경우 파일 로드
            }
        } else {
            // Android 10 이하인 경우
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE);
            } else {
                loadFileFromIntent();  // 권한이 이미 있는 경우 파일 로드
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadFileFromIntent(); // 권한이 허용되면 파일 로드
            } else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    textView.setText("파일 접근 권한이 필요합니다.");
                } else {
                    textView.setText("파일 접근 권한을 설정에서 허용해야 합니다.");
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MANAGE_STORAGE_PERMISSION_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    loadFileFromIntent();  // 권한이 허용된 경우 파일 로드
                } else {
                    textView.setText("파일 접근 권한을 설정에서 허용해야 합니다.");
                }
            }
        }
    }

    // 파일을 Intent로부터 로드하는 메서드
    private void loadFileFromIntent() {
        String fileUrl = getIntent().getStringExtra("fileUrl");
        if (fileUrl != null) {
            Uri fileUri = Uri.parse(fileUrl);
            loadFileAndDisplay(fileUri);
        }
    }

    private void initUI() {
        // 상단 툴바 설정
        Toolbar toolbarTop = findViewById(R.id.toolbar_top);
        setSupportActionBar(toolbarTop);

        // 뷰 초기화
        textView = findViewById(R.id.textView);
        Button buttonSelectFile = findViewById(R.id.button_select_file);
        Button buttonPrevPage = findViewById(R.id.button_prev_page);
        Button buttonNextPage = findViewById(R.id.button_next_page);

        // 파일 선택 버튼 클릭 리스너 설정
        buttonSelectFile.setOnClickListener(v -> new FilePicker(this, this::onFileSelected).pickTextFile());

        // 이전/다음 페이지 버튼 클릭 리스너 설정
        buttonPrevPage.setOnClickListener(v -> loadPreviousPage());
        buttonNextPage.setOnClickListener(v -> loadNextPage());
    }

    private void loadFileAndDisplay(Uri uri) {
        if (uri != null) {
            fileLoader = new FileLoader(this, uri);
            currentPage = 0;
            displayPage(currentPage);  // 파일의 첫 번째 페이지를 바로 표시
        } else {
            textView.setText("파일을 로드할 수 없습니다.");
        }
    }

    private void onFileSelected(Uri uri) {
        if (uri != null) {
            loadFileAndDisplay(uri);
        }
    }

    private void displayPage(int pageNumber) {
        if (pageDisplayer != null && fileLoader != null) {
            pageDisplayer.displayPage(fileLoader, pageNumber, current -> currentPage = current);
        } else {
            textView.setText("PageDisplayer 또는 FileLoader가 초기화되지 않았습니다.");
        }
    }

    private void loadNextPage() {
        displayPage(++currentPage);
    }

    private void loadPreviousPage() {
        if (currentPage > 0) displayPage(--currentPage);
        else textView.setText("이전 페이지가 없습니다.");
    }

    // 서버로 텍스트 전송 메서드
    private void sendTextToServer(String text) {
        serverCommunicator.sendText(text, new ServerCommunicator.ResponseCallback() {
            @Override
            public void onResponse(String responseData) {
                JsonParser jsonParser = new JsonParser();
                String displayText = jsonParser.parseResponse(responseData);
                runOnUiThread(() -> textView.setText(displayText));
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> textView.setText(errorMessage));
            }
        });
    }
}

