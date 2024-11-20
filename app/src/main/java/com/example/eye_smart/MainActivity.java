package com.example.eye_smart;

import static com.example.eye_smart.gaze_utils.OptimizeUtils.showToast;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.Layout;
import android.util.Log;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.example.eye_smart.dict_utils.JsonParser;
import com.example.eye_smart.dict_utils.ServerCommunicator;
import com.example.eye_smart.file_utils.FileLoader;
import com.example.eye_smart.gaze_utils.GazePoint;
import com.example.eye_smart.gaze_utils.GazeTrackerManager;
import com.example.eye_smart.page_view_utils.PageDisplayer;

import camp.visual.eyedid.gazetracker.GazeTracker;
import camp.visual.eyedid.gazetracker.callback.TrackingCallback;
import camp.visual.eyedid.gazetracker.constant.GazeTrackerOptions;
import camp.visual.eyedid.gazetracker.metrics.state.TrackingState;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 1;
    private int currentPage = 0;

    private GazeTracker gazeTracker;
    private GazePoint gazePoint;
    private TextView textView;
    private TextView textView3;
    private FileLoader fileLoader;
    private PageDisplayer pageDisplayer;
    private ServerCommunicator serverCommunicator;

    private float gazeX, gazeY;

    private String fileUrl;
    private String currentGazedWord = "";

    private long gazeMissingStartTime = 0;

    private ImageButton bookMarkButton;
    private boolean isBookmarked = false;

    private static final String PREFS_NAME = "Bookmarks";
    private static final String BOOKMARK_BOOK_KEY = "bookmark_book";
    private static final String BOOKMARK_PAGE_KEY = "bookmark_page";

    private String currentBook;

    private class GazeTarget {
        private final Rect rect;
        private long gazeStartTime = 0;
        private final long threshold;
        private final Runnable action;

        public GazeTarget(Rect rect, long threshold, Runnable action) {
            this.rect = rect;
            this.threshold = threshold;
            this.action = action;
        }

        public void checkGaze(float gazeX, float gazeY) {
            if (rect.contains((int) gazeX, (int) gazeY)) {
                if (gazeStartTime == 0) {
                    gazeStartTime = System.currentTimeMillis();
                }
                long gazeDuration = System.currentTimeMillis() - gazeStartTime;
                if (gazeDuration >= threshold) {
                    runOnUiThread(action);
                    reset();
                }
            } else {
                reset();
            }
        }

        private void reset() {
            gazeStartTime = 0;
        }
    }

    private GazeTarget prevButtonGazeTarget;
    private GazeTarget nextButtonGazeTarget;
    private GazeTarget backButtonGazeTarget;
    private GazeTarget bookmarkButtonGazeTarget;

    private long gazeStartTimeWord = 0;
    private boolean wordAlreadySent = false;

    private ActivityResultLauncher<Intent> manageStoragePermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI 초기화
        initUI();

        // Intent에서 데이터 읽기
        Intent intent = getIntent();
        fileUrl = intent.getStringExtra("fileUrl");
        currentPage = intent.getIntExtra("selectedNumber", 0);
        currentBook = intent.getStringExtra("selectedBook");

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

        // 북마크 버튼 설정
        setupBookmarkButton();

        requestStoragePermission();

        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.main_color_purple));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 권한이 있는 경우에만 파일 로드
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
            float rawGazeX = gazeInfo.x;
            float rawGazeY = gazeInfo.y;

            // 화면 크기 가져오기
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            int screenHeight = getResources().getDisplayMetrics().heightPixels;

            // Gaze 좌표 클램핑
            gazeX = Math.max(0, Math.min(rawGazeX, screenWidth));
            gazeY = Math.max(0, Math.min(rawGazeY, screenHeight));

            if (gazeInfo.trackingState == TrackingState.GAZE_MISSING) {
                if (gazeMissingStartTime == 0) {
                    gazeMissingStartTime = System.currentTimeMillis();
                } else {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - gazeMissingStartTime >= 2000) {
                        runOnUiThread(this::handleGazeMissing);
                        gazeMissingStartTime = 0;
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

    private void setupBookmarkButton() {
        bookMarkButton.setOnClickListener(v -> toggleBookmark());
    }

    private void toggleBookmark() {
        if (isBookmarked) {
            bookMarkButton.setImageResource(R.drawable.bookmark);
            removeBookmark();
        } else {
            bookMarkButton.setImageResource(R.drawable.bookmark_trans);
            saveBookmark();
        }
        isBookmarked = !isBookmarked;
    }

    private void saveBookmark() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(BOOKMARK_BOOK_KEY, currentBook);
        editor.putInt(BOOKMARK_PAGE_KEY, currentPage);
        editor.apply();

        Toast.makeText(this, "북마크가 저장되었습니다.", Toast.LENGTH_SHORT).show();
    }

    private void removeBookmark() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(BOOKMARK_BOOK_KEY);
        editor.remove(BOOKMARK_PAGE_KEY);
        editor.apply();

        Toast.makeText(this, "북마크가 삭제되었습니다.", Toast.LENGTH_SHORT).show();
    }

    private void checkGaze(float gazeX, float gazeY) {
        textView.post(() -> {
            Layout layout = textView.getLayout();
            if (layout == null) return;

            long currentTime = System.currentTimeMillis();

            Button buttonPrevPage = findViewById(R.id.button_prev_page);
            Button buttonNextPage = findViewById(R.id.button_next_page);
            Button backBookSelection = findViewById(R.id.backBookSelection);

            Rect prevButtonRect = new Rect();
            buttonPrevPage.getGlobalVisibleRect(prevButtonRect);

            Rect nextButtonRect = new Rect();
            buttonNextPage.getGlobalVisibleRect(nextButtonRect);

            Rect backButtonRect = new Rect();
            backBookSelection.getGlobalVisibleRect(backButtonRect);

            Rect bookmarkButtonRect = new Rect();
            bookMarkButton.getGlobalVisibleRect(bookmarkButtonRect);

            if (prevButtonGazeTarget == null) {
                prevButtonGazeTarget = new GazeTarget(prevButtonRect, 500, this::loadPreviousPage);
            }
            if (nextButtonGazeTarget == null) {
                nextButtonGazeTarget = new GazeTarget(nextButtonRect, 500, this::loadNextPage);
            }
            if (backButtonGazeTarget == null) {
                backButtonGazeTarget = new GazeTarget(backButtonRect, 500, () -> {
                    Intent intent = new Intent(MainActivity.this, BookSelectionActivity.class);
                    startActivity(intent);
                    finish();
                });
            }
            if (bookmarkButtonGazeTarget == null) {
                bookmarkButtonGazeTarget = new GazeTarget(bookmarkButtonRect, 500, bookMarkButton::performClick);
            }

            prevButtonGazeTarget.checkGaze(gazeX, gazeY);
            nextButtonGazeTarget.checkGaze(gazeX, gazeY);
            backButtonGazeTarget.checkGaze(gazeX, gazeY);
            bookmarkButtonGazeTarget.checkGaze(gazeX, gazeY);

            checkWordGaze(gazeX, gazeY, layout, currentTime);
        });
    }

    private void checkWordGaze(float gazeX, float gazeY, Layout layout, long currentTime) {
        int[] textViewLocation = new int[2];
        textView.getLocationOnScreen(textViewLocation);
        int textViewTop = textViewLocation[1];
        int textViewLeft = textViewLocation[0];

        float adjustedGazeX = gazeX - textViewLeft;
        float adjustedGazeY = gazeY - textViewTop;

        boolean gazeOnWord = false;

        for (int lineIndex = 0; lineIndex < layout.getLineCount(); lineIndex++) {
            int lineStartOffset = layout.getLineStart(lineIndex);
            int lineEndOffset = layout.getLineEnd(lineIndex);

            int safeStart = Math.max(0, Math.min(lineStartOffset, textView.getText().length()));
            int safeEnd = Math.max(0, Math.min(lineEndOffset, textView.getText().length()));

            if (safeStart >= safeEnd) continue;

            String lineText = textView.getText().subSequence(safeStart, safeEnd).toString();
            String[] words = lineText.split(" ");

            int wordStart = safeStart;
            for (String word : words) {
                int wordEnd = wordStart + word.length();

                if (word.trim().isEmpty()) {
                    wordStart = wordEnd + 1;
                    continue;
                }

                float textSize = textView.getTextSize();

                int[] wordCoordinates = getWordCoordinates(layout, wordStart, wordEnd, textSize, 10, -50);
                Rect wordRect = new Rect(wordCoordinates[0], wordCoordinates[1], wordCoordinates[2], wordCoordinates[3]);

                if (wordRect.contains((int) adjustedGazeX, (int) adjustedGazeY)) {
                    gazeOnWord = true;
                    if (!word.equals(currentGazedWord)) {
                        currentGazedWord = word;
                        gazeStartTimeWord = currentTime;
                        wordAlreadySent = false;
                    } else {
                        long gazeDurationWord = currentTime - gazeStartTimeWord;
                        if (gazeDurationWord >= 1000 && !wordAlreadySent) {
                            sendTextToServer(word);
                            showToast(this, word, true);
                            wordAlreadySent = true;
                        }
                    }
                    break;
                }
                wordStart = wordEnd + 1;
            }
            if (gazeOnWord) break;
        }

        if (!gazeOnWord) {
            currentGazedWord = "";
            gazeStartTimeWord = 0;
            wordAlreadySent = false;
        }
    }

    private void handleGazeMissing() {
        showToast(this, "시선이 감지되지 않습니다.", true);
        Intent intent = new Intent(MainActivity.this, CalibrationActivity.class);
        startActivity(intent);
        finish();
    }

    private int[] getWordCoordinates(Layout layout, int start, int end, float textSize, int paddingY, int offsetY) {
        int lineIndex = layout.getLineForOffset(start); // 해당 단어가 위치한 줄의 인덱스를 얻음
        int lineTop = layout.getLineTop(lineIndex); // 해당 줄의 상단 좌표
        int lineBottom = layout.getLineBottom(lineIndex); // 해당 줄의 하단 좌표

        float wordStartX = layout.getPrimaryHorizontal(start); // 단어 시작 x 좌표
        float wordEndX = layout.getPrimaryHorizontal(end); // 단어 끝 x 좌표

        int margin = Math.round(textSize / 2);
        int adjustedTop = lineTop - paddingY + margin + offsetY;
        int adjustedBottom = lineBottom - paddingY + margin + offsetY;

        // 상하좌우 여백 적용 (좌우 여백 추가)
        int horizontalPadding = Math.round(textSize * 0.1f);

        return new int[]{
                (int) wordStartX - horizontalPadding, // 왼쪽 x 좌표
                adjustedTop,                          // 위쪽 y 좌표
                (int) wordEndX + horizontalPadding,   // 오른쪽 x 좌표
                adjustedBottom                        // 아래쪽 y 좌표
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
        if (!hasStoragePermission()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                manageStoragePermissionLauncher.launch(intent);
            } else {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
            }
        } else {
            loadFileFromIntent();
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
        if (fileUrl == null || fileUrl.isEmpty()) {
            textView.setText("파일 URL이 전달되지 않았습니다.");
            Log.e("MainActivity", "파일 URL이 null 또는 빈 문자열입니다.");
            return;
        }

        Uri fileUri = Uri.parse(fileUrl);
        loadFileAndDisplay(fileUri);
    }

    private void initUI() {
        Toolbar toolbarTop = findViewById(R.id.toolbar);
        setSupportActionBar(toolbarTop);

        textView = findViewById(R.id.textView);
        textView3 = findViewById(R.id.textView3);
        Button buttonPrevPage = findViewById(R.id.button_prev_page);
        Button buttonNextPage = findViewById(R.id.button_next_page);
        Button backBookSelection = findViewById(R.id.backBookSelection);
        bookMarkButton = findViewById(R.id.bookMarkButton);

        buttonPrevPage.setOnClickListener(v -> loadPreviousPage());
        buttonNextPage.setOnClickListener(v -> loadNextPage());
        backBookSelection.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, BookSelectionActivity.class);
            startActivity(intent);
            finish();
        });

        manageStoragePermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (hasStoragePermission()) {
                        loadFileFromIntent();
                    } else {
                        Toast.makeText(this, "저장소 관리 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }
        );
    }

    private void loadFileAndDisplay(Uri uri) {
        if (uri != null) {
            try {
                fileLoader = new FileLoader(this, uri);
                displayPage(currentPage);
            } catch (Exception e) {
                textView.setText("파일 로드 실패: " + e.getMessage());
                Log.e("MainActivity", "파일 로드 오류: ", e);
            }
        } else {
            textView.setText("파일을 로드할 수 없습니다.");
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,@NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadFileFromIntent();
            } else {
                Toast.makeText(this, "저장소 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}
