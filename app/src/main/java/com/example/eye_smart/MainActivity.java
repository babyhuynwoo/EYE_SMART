package com.example.eye_smart;

import static com.example.eye_smart.gaze_utils.OptimizeUtils.showToast;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.Manifest;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.Layout;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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
import camp.visual.eyedid.gazetracker.metrics.state.TrackingState;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final int MANAGE_STORAGE_PERMISSION_REQUEST_CODE = 2;
    private int currentPage = 0;

    private GazeTracker gazeTracker;
    private GazePoint gazePoint;
    private TextView textView;
    private TextView textView3;
    private FileLoader fileLoader;
    private PageDisplayer pageDisplayer;
    private ServerCommunicator serverCommunicator;

    private float gazeX, gazeY;
    private long gazeStartTime;
    private long gazeDuration; // 누적 응시 시간

    private String fileUrl;
    private String currentGazedWord = "";

    private long gazeMissingStartTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI 초기화
        initUI();

        // GazePointView 연결
        gazePoint = findViewById(R.id.gazePointView);
        gazeTracker = GazeTrackerManager.getInstance().getGazeTracker();

        if (gazeTracker != null) {
            setupGazeTracking();
        } else {
            initTracker();
        }

        pageDisplayer = new PageDisplayer(textView, 3f, 2f);
        serverCommunicator = new ServerCommunicator();

        fileUrl = getIntent().getStringExtra("fileUrl");
        requestStoragePermission();

        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.main_color_purple));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (hasStoragePermission()) {
            loadFileFromIntent();
        }
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
            gazeX = gazeInfo.x;
            gazeY = gazeInfo.y;

            // Gaze 상태 확인
            if (gazeInfo.trackingState == TrackingState.GAZE_MISSING) {
                if (gazeMissingStartTime == 0) {
                    gazeMissingStartTime = System.currentTimeMillis();
                } else {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - gazeMissingStartTime >= 2000) {
                        runOnUiThread(this::handleGazeMissing);
                        gazeMissingStartTime = 0; // 초기화
                    }
                }
            } else {
                gazeMissingStartTime = 0;
            }

            runOnUiThread(() -> {
                gazePoint.updateGazePoint(gazeX, gazeY);
                checkGaze(gazeX, gazeY);
            });
        }
    };

    private void checkGaze(float gazeX, float gazeY) {

        textView.post(() -> { // UI 스레드에서 실행
            Layout layout = textView.getLayout();
            if (layout == null) return;

            // TextView의 화면 상 위치 가져오기
            int[] textViewLocation = new int[2];
            textView.getLocationOnScreen(textViewLocation);
            int textViewTop = textViewLocation[1];
            int textViewLeft = textViewLocation[0]; // 왼쪽 위치 추가

            // gazeY 조정: 화면 전체 좌표에서 TextView 내부 좌표로 변환
            float adjustedGazeX = gazeX - textViewLeft; // X 좌표 보정
            float adjustedGazeY = gazeY - textViewTop;  // Y 좌표 보정

            // 버튼 영역 확인
            Button buttonPrevPage = findViewById(R.id.button_prev_page);
            Button buttonNextPage = findViewById(R.id.button_next_page);
            Button backBookSelection = findViewById(R.id.backBookSelection);

            Rect prevButtonRect = new Rect();
            buttonPrevPage.getGlobalVisibleRect(prevButtonRect);

            Rect nextButtonRect = new Rect();
            buttonNextPage.getGlobalVisibleRect(nextButtonRect);

            Rect BookSelection = new Rect();
            backBookSelection.getGlobalVisibleRect(BookSelection);

            long currentTime = System.currentTimeMillis();

            // 단어별로 좌표를 확인
            for (int lineIndex = 0; lineIndex < layout.getLineCount(); lineIndex++) {
                int lineStartOffset = layout.getLineStart(lineIndex);
                int lineEndOffset = layout.getLineEnd(lineIndex);

                String lineText = textView.getText().subSequence(lineStartOffset, lineEndOffset).toString();
                String[] words = lineText.split(" ");

                int wordStart = lineStartOffset;
                for (String word : words) {
                    int wordEnd = wordStart + word.length();

                    if (word.trim().isEmpty()) {
                        wordStart = wordEnd + 1;
                        continue;
                    }

                    int[] wordCoordinates = getWordCoordinates(layout, wordStart, wordEnd);
                    Rect wordRect = new Rect(wordCoordinates[0], wordCoordinates[1], wordCoordinates[2], wordCoordinates[3]);

                    // gazeX와 조정된 adjustedGazeY를 사용하여 시선이 단어 영역 안에 있는지 확인
                    if (wordRect.contains((int) adjustedGazeX, (int) adjustedGazeY)) {
                        if (!word.equals(currentGazedWord)) {
                            currentGazedWord = word;
                            gazeDuration = 0; // 초기화
                            gazeStartTime = currentTime;

                        } else {
                            gazeDuration += currentTime - gazeStartTime;
                            gazeStartTime = currentTime;

                            if (gazeDuration >= 3000) { // 1초 이상 응시
                                sendTextToServer(word);
                                gazeDuration = 0; // 초기화
                            }
                        }
                        return; // 단어가 감지되면 메서드를 종료
                    }
                    wordStart = wordEnd + 1;
                }
            }

            // 이전 페이지 버튼 시선 확인
            if (prevButtonRect.contains((int) gazeX, (int) gazeY)) {
                gazeDuration += currentTime - gazeStartTime; // 시간 누적
                gazeStartTime = currentTime; // 현재 시간으로 갱신

                if (gazeDuration >= 1000) { // 1초 이상 응시
                    Toast.makeText(this, "이전버튼", Toast.LENGTH_SHORT).show();
                    loadPreviousPage(); // 페이지 전환
                    gazeDuration = 0; // 초기화
                }
            } else {
                gazeDuration = 0; // 버튼에서 벗어나면 초기화
            }

            // 다음 페이지 버튼 시선 확인
            if (nextButtonRect.contains((int) gazeX, (int) gazeY)) {
                gazeDuration += currentTime - gazeStartTime; // 시간 누적
                gazeStartTime = currentTime; // 현재 시간으로 갱신

                if (gazeDuration >= 1000) { // 1초 이상 응시
                    Toast.makeText(this, "다음버튼", Toast.LENGTH_SHORT).show();
                    loadNextPage(); // 페이지 전환
                    gazeDuration = 0; // 초기화
                }
            } else {
                gazeDuration = 0; // 버튼에서 벗어나면 초기화
            }

            if (BookSelection.contains((int) gazeX, (int) gazeY)) {
                gazeDuration += currentTime - gazeStartTime; // 시간 누적
                gazeStartTime = currentTime; // 현재 시간으로 갱신

                if (gazeDuration >= 1000) { // 1초 이상 응시
                    Intent intent = new Intent(MainActivity.this, BookSelectionActivity.class);
                    startActivity(intent);
                    finish();
                    gazeDuration = 0; // 초기화
                }
            } else {
                gazeDuration = 0; // 버튼에서 벗어나면 초기화
            }

            currentGazedWord = ""; // 단어 응시 초기화
        });
    }

    private void handleGazeMissing() {
        showToast(this, "시선이 감지되지 않습니다.", true);
        Intent intent = new Intent(MainActivity.this, CalibrationActivity.class);
        startActivity(intent);
        finish();
    }

    private int[] getWordCoordinates(Layout layout, int start, int end) {
        int lineIndex = layout.getLineForOffset(start);
        int lineTop = layout.getLineTop(lineIndex);
        int lineBottom = layout.getLineBottom(lineIndex);

        int margin = Math.round(textView.getTextSize() / 2);
        int paddingY = Math.round(textView.getTextSize() * 0.2f);
        int offsetY = -Math.round(textView.getTextSize() * 0.1f);

        int adjustedTop = lineTop - paddingY + margin + offsetY;
        int adjustedBottom = lineBottom - paddingY + margin + offsetY;

        float wordStartX = layout.getPrimaryHorizontal(start);
        float wordEndX = layout.getPrimaryHorizontal(end);

        return new int[]{
                (int) wordStartX,  // 왼쪽 좌표
                adjustedTop,       // 위쪽 좌표
                (int) wordEndX,    // 오른쪽 좌표
                adjustedBottom     // 아래쪽 좌표
        };
    }

    private void sendTextToServer(String word) {
        serverCommunicator.sendWord(word, new ServerCommunicator.ResponseCallback() {
            @Override
            public void onResponse(String responseData) {
                JsonParser jsonParser = new JsonParser();
                String meaning = jsonParser.parseResponse(responseData);
                runOnUiThread(() -> textView3.setText(meaning));
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> showToast(MainActivity.this, "서버 오류: " + errorMessage, true));
            }
        });
    }

    private void requestStoragePermission() {
        if (hasStoragePermission()) {
            loadFileFromIntent();
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, MANAGE_STORAGE_PERMISSION_REQUEST_CODE);
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE);
            }
        }
    }

    private boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void loadFileFromIntent() {
        if (fileUrl != null) {
            Uri fileUri = Uri.parse(fileUrl);
            loadFileAndDisplay(fileUri);
        } else {
            textView.setText("파일 URL이 전달되지 않았습니다.");
        }
    }

    private void initUI() {
        Toolbar toolbarTop = findViewById(R.id.toolbar);
        setSupportActionBar(toolbarTop);

        textView = findViewById(R.id.textView);
        textView3 = findViewById(R.id.textView3);
        Button buttonPrevPage = findViewById(R.id.button_prev_page);
        Button buttonNextPage = findViewById(R.id.button_next_page);
        Button backBookSelection = findViewById(R.id.backBookSelection);

        buttonPrevPage.setOnClickListener(v -> loadPreviousPage());
        buttonNextPage.setOnClickListener(v -> loadNextPage());
        backBookSelection.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, BookSelectionActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void loadFileAndDisplay(Uri uri) {
        if (uri != null) {
            try {
                fileLoader = new FileLoader(this, uri);
                currentPage = 0;
                displayPage(currentPage);
            } catch (Exception e) {
                textView.setText("파일 로드 실패: " + e.getMessage());
            }
        } else {
            textView.setText("파일을 로드할 수 없습니다.");
        }
    }

    private void displayPage(int pageNumber) {
        if (pageDisplayer != null && fileLoader != null) {
            pageDisplayer.displayPage(fileLoader, pageNumber, current -> currentPage = current);
        }  else {
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
}
