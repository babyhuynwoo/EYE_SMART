package com.example.eye_smart;

import static com.example.eye_smart.gaze_utils.OptimizeUtils.showToast;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.Manifest;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.eye_smart.file_utils.FileLoader;
import com.example.eye_smart.gaze_utils.GazePoint;
import com.example.eye_smart.gaze_utils.GazeTrackerManager;
import com.example.eye_smart.page_view_utils.PageDisplayer;
import com.example.eye_smart.dict_utils.ServerCommunicator;
import com.example.eye_smart.dict_utils.JsonParser;

import camp.visual.eyedid.gazetracker.GazeTracker;
import camp.visual.eyedid.gazetracker.callback.TrackingCallback;
import camp.visual.eyedid.gazetracker.constant.GazeTrackerOptions;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final int MANAGE_STORAGE_PERMISSION_REQUEST_CODE = 2;
    private int currentPage = 0;

    private GazeTracker gazeTracker;
    private GazePoint gazePoint;
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

        // GazePointView 연결
        gazePoint = findViewById(R.id.gazePointView);
        // GazeTracker 가져오기
        gazeTracker = GazeTrackerManager.getInstance().getGazeTracker();

        if (gazeTracker != null) {
            setupGazeTracking();
        } else {
            initTracker();
        }

        // PageDisplayer와 ServerCommunicator 초기화
        pageDisplayer = new PageDisplayer(textView, 3f, 2f);
        serverCommunicator = new ServerCommunicator();

        // 권한 요청 및 파일 로드
        requestStoragePermission();
    }

    private void initTracker() {
        GazeTrackerOptions options = new GazeTrackerOptions.Builder().build();
        GazeTracker.initGazeTracker(this, BuildConfig.EYEDID_API_KEY, (gazeTracker, error) -> {
            if (gazeTracker != null) {
                this.gazeTracker = gazeTracker;
                GazeTrackerManager.getInstance().setGazeTracker(gazeTracker);
                setupGazeTracking();
            } else {
                showToast(this, "GazeTracker 초기화 실패: " + error.name(), true);
                finish();
            }
        }, options);
    }

    private void setupGazeTracking() {
        gazeTracker.setTrackingCallback(trackingCallback);
        GazeTrackerManager.getInstance().startTracking();
    }

    private final TrackingCallback trackingCallback = (timestamp, gazeInfo, faceInfo, blinkInfo, userStatusInfo) -> {
        if (gazeInfo != null) {
            float gazeX = gazeInfo.x;
            float gazeY = gazeInfo.y;

            // GazePointView에 시선 위치 업데이트
            runOnUiThread(() -> {
                gazePoint.updateGazePoint(gazeX, gazeY);
                // 버튼이나 다른 UI 요소에 대한 gaze 체크를 추가할 수 있습니다.
            });
        }
    };

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, MANAGE_STORAGE_PERMISSION_REQUEST_CODE);
            } else {
                loadFileFromIntent();  // 권한이 이미 있는 경우 파일 로드
            }
        } else {
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
            handlePermissionResult(grantResults);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MANAGE_STORAGE_PERMISSION_REQUEST_CODE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                loadFileFromIntent();  // 권한이 허용된 경우 파일 로드
            } else {
                displayPermissionError();
            }
        }
    }

    private void handlePermissionResult(int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadFileFromIntent();
        } else {
            displayPermissionError();
        }
    }

    private void displayPermissionError() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            textView.setText("파일 접근 권한이 필요합니다.");
        } else {
            textView.setText("파일 접근 권한을 설정에서 허용해야 합니다.");
        }
    }

    private void loadFileFromIntent() {
        String fileUrl = getIntent().getStringExtra("fileUrl");
        if (fileUrl != null) {
            Log.d("MainActivity", "fileUrl received: " + fileUrl);
            Uri fileUri = Uri.parse(fileUrl);
            loadFileAndDisplay(fileUri);
        } else {
            Log.d("MainActivity", "fileUrl is null, unable to load file.");
            textView.setText("파일 URL이 전달되지 않았습니다.");
        }
    }

    private void initUI() {
        Toolbar toolbarTop = findViewById(R.id.toolbar_top);
        setSupportActionBar(toolbarTop);

        textView = findViewById(R.id.textView);
        Button buttonPrevPage = findViewById(R.id.button_prev_page);
        Button buttonNextPage = findViewById(R.id.button_next_page);

        buttonPrevPage.setOnClickListener(v -> loadPreviousPage());
        buttonNextPage.setOnClickListener(v -> loadNextPage());
    }

    private void loadFileAndDisplay(Uri uri) {
        if (uri != null) {
            fileLoader = new FileLoader(this, uri);
            currentPage = 0;
            displayPage(currentPage);  // 첫 페이지 표시
            Log.d("MainActivity", "File loaded and displaying page 0.");
        } else {
            textView.setText("파일을 로드할 수 없습니다.");
            Log.d("MainActivity", "URI is null, cannot load file.");
        }
    }

    private void displayPage(int pageNumber) {
        if (pageDisplayer != null && fileLoader != null) {
            pageDisplayer.displayPage(fileLoader, pageNumber, current -> {
                currentPage = current;
                Log.d("PageDisplay", "Displayed page number: " + currentPage);
            });
        } else {
            textView.setText("PageDisplayer 또는 FileLoader가 초기화되지 않았습니다.");
            Log.d("PageDisplay", "PageDisplayer 또는 FileLoader가 초기화되지 않았습니다.");
        }
    }

    private void loadNextPage() {
        displayPage(++currentPage);
    }

    private void loadPreviousPage() {
        if (currentPage > 0) displayPage(--currentPage);
        else textView.setText("이전 페이지가 없습니다.");
    }

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