package com.example.eye_smart;

import static com.example.eye_smart.gaze_utils.OptimizeUtils.showToast;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.eye_smart.gaze_utils.GazePoint;
import com.example.eye_smart.gaze_utils.GazeTrackerManager;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import camp.visual.eyedid.gazetracker.GazeTracker;
import camp.visual.eyedid.gazetracker.callback.TrackingCallback;
import camp.visual.eyedid.gazetracker.constant.GazeTrackerOptions;
import camp.visual.eyedid.gazetracker.metrics.state.TrackingState;

public class BookSelectionActivity extends AppCompatActivity {

    private GazeTracker gazeTracker;
    private GazePoint gazePoint;
    private final android.os.Handler progressHandler = new android.os.Handler();
    private Runnable progressRunnable;
    private View lastGazedButton = null;
    private ProgressBar currentProgressBar = null;

    private final Map<ImageButton, ProgressBar> buttonProgressMap = new HashMap<>();
    private final Map<ImageButton, String> buttonUrlMap = new HashMap<>();
    private final Map<ImageButton, Boolean> buttonEnabledMap = new HashMap<>();

    private final Map<ImageButton, ProgressBar> bookmarkProgressMap = new HashMap<>();
    private final Map<ImageButton, String> bookmarkUrlMap = new HashMap<>();
    private final Map<ImageButton, String> buttonTagMap = new HashMap<>();

    // 북마크 정보를 저장하기 위한 변수
    private String bookmarkedBook;
    private int bookmarkedPage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookshelf);

        // GazePointView 연결
        gazePoint = findViewById(R.id.gazePointView);

        // 북마크 정보 로드
        loadBookmarkInfo();

        // 버튼과 ProgressBar 초기화
        initializeButtonsAndProgressBars();

        // GazeTracker 가져오기
        gazeTracker = GazeTrackerManager.getInstance().getGazeTracker();
        if (gazeTracker != null) {
            setupGazeTracking();
        } else {
            initTracker();
        }

        // 상태바 색상 변경
        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.main_color_purple));
    }

    private void loadBookmarkInfo() {
        SharedPreferences prefs = getSharedPreferences("Bookmarks", MODE_PRIVATE);
        bookmarkedBook = prefs.getString("bookmark_book", null);
        bookmarkedPage = prefs.getInt("bookmark_page", -1);
    }

    private void initializeButtonsAndProgressBars() {
        ImageButton book1 = findViewById(R.id.book1);
        ImageButton book2 = findViewById(R.id.book2);
        ImageButton book3 = findViewById(R.id.book3);

        ProgressBar progressBarBook1 = findViewById(R.id.progressBarBook1);
        ProgressBar progressBarBook2 = findViewById(R.id.progressBarBook2);
        ProgressBar progressBarBook3 = findViewById(R.id.progressBarBook3);

        buttonProgressMap.put(book1, progressBarBook1);
        buttonProgressMap.put(book2, progressBarBook2);
        buttonProgressMap.put(book3, progressBarBook3);

        File sampleFile1 = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "sample_1.txt");
        File sampleFile2 = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "sample_2.txt");
        File sampleFile3 = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "sample_3.txt");

        buttonUrlMap.put(book1, Uri.fromFile(sampleFile1).toString());
        buttonUrlMap.put(book2, Uri.fromFile(sampleFile2).toString());
        buttonUrlMap.put(book3, Uri.fromFile(sampleFile3).toString());

        buttonTagMap.put(book1, "Book1");
        buttonTagMap.put(book2, "Book2");
        buttonTagMap.put(book3, "Book3");

        ImageButton book1Text = findViewById(R.id.bookmark1);
        ImageButton book2Text = findViewById(R.id.bookmark2);
        ImageButton book3Text = findViewById(R.id.bookmark3);

        bookmarkProgressMap.put(book1Text, progressBarBook1);
        bookmarkProgressMap.put(book2Text, progressBarBook2);
        bookmarkProgressMap.put(book3Text, progressBarBook3);

        bookmarkUrlMap.put(book1Text, Uri.fromFile(sampleFile1).toString());
        bookmarkUrlMap.put(book2Text, Uri.fromFile(sampleFile2).toString());
        bookmarkUrlMap.put(book3Text, Uri.fromFile(sampleFile3).toString());

        book1Text.setTag("Book1");
        book2Text.setTag("Book2");
        book3Text.setTag("Book3");
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
        if (gazeInfo != null && gazeInfo.trackingState == TrackingState.SUCCESS) {
            float gazeX = gazeInfo.x;
            float gazeY = gazeInfo.y;

            runOnUiThread(() -> {
                gazePoint.updateGazePoint(gazeX, gazeY);
                checkGazeOnButtonsAndText(gazeX, gazeY);
            });
        }
    };

    private void checkGazeOnButtonsAndText(float gazeX, float gazeY) {
        // 버튼 응시 체크
        for (ImageButton button : buttonProgressMap.keySet()) {
            checkGazeOnButton(button, gazeX, gazeY);
        }
        // 텍스트 응시 체크
        for (ImageButton imageButton : bookmarkProgressMap.keySet()) {
            checkGazeOnText(imageButton, gazeX, gazeY);
        }
    }

    private void checkGazeOnText(ImageButton imageButton, float gazeX, float gazeY) {
        int[] location = new int[2];
        imageButton.getLocationOnScreen(location);
        int textX = location[0];
        int textY = location[1];

        if (gazeX >= textX && gazeX <= textX + imageButton.getWidth() &&
                gazeY >= textY && gazeY <= textY + imageButton.getHeight()) {

            if (lastGazedButton == null) {
                lastGazedButton = imageButton;
                currentProgressBar = bookmarkProgressMap.get(imageButton);
                startProgressBarForText(imageButton);
            }
        } else {
            if (lastGazedButton == imageButton) {
                pauseProgressBar();
                lastGazedButton = null;
            }
        }
    }

    private void startProgressBarForText(final ImageButton imageButton) {
        if (currentProgressBar == null) return;

        currentProgressBar.setVisibility(View.VISIBLE);
        currentProgressBar.setProgress(0);

        progressRunnable = new Runnable() {
            @Override
            public void run() {
                int progress = currentProgressBar.getProgress();
                if (progress < 100) {
                    currentProgressBar.setProgress(progress + 2);
                    progressHandler.postDelayed(this, 10);
                } else {
                    handleTextSelection(imageButton);
                }
            }
        };
        progressHandler.post(progressRunnable);
    }

    private void handleTextSelection(ImageButton imageButton) {
        String selectedBook = (String) imageButton.getTag();
        String fileUrl = bookmarkUrlMap.get(imageButton);

        currentProgressBar.setProgress(0);
        currentProgressBar.setVisibility(View.GONE);

        // Intent로 URL과 선택한 책 정보를 전송
        Intent intent = new Intent(BookSelectionActivity.this, MainActivity.class);
        intent.putExtra("fileUrl", fileUrl);
        intent.putExtra("selectedBook", selectedBook);

        // 북마크된 책과 선택한 책이 일치하는지 확인
        if (bookmarkedBook != null && bookmarkedBook.equals(selectedBook)) {
            // 북마크된 페이지로 이동
            intent.putExtra("selectedNumber", bookmarkedPage);
            showToast(this, "북마크 페이지로 이동합니다.", true);
        } else {
            // 기본 페이지로 이동
            intent.putExtra("selectedNumber", 0);
            showToast(this, "책의 첫 페이지로 이동합니다.", true);
        }

        startActivity(intent);
        finish();
    }

    private void checkGazeOnButton(ImageButton button, float gazeX, float gazeY) {
        int[] location = new int[2];
        button.getLocationOnScreen(location);
        int buttonX = location[0];
        int buttonY = location[1];

        if (gazeX >= buttonX && gazeX <= buttonX + button.getWidth() &&
                gazeY >= buttonY && gazeY <= buttonY + button.getHeight()) {

            if (Boolean.FALSE.equals(buttonEnabledMap.get(button))) return;

            if (lastGazedButton != button) {
                lastGazedButton = button;
                currentProgressBar = buttonProgressMap.get(button);
                startProgressBarForButton(button);
            }
        } else {
            if (lastGazedButton == button) {
                pauseProgressBar();
                lastGazedButton = null;
            }
        }
    }

    private void startProgressBarForButton(final ImageButton button) {
        if (currentProgressBar == null) return;

        currentProgressBar.setVisibility(View.VISIBLE);
        currentProgressBar.setProgress(0);

        progressRunnable = new Runnable() {
            @Override
            public void run() {
                int progress = currentProgressBar.getProgress();
                if (progress < 100) {
                    currentProgressBar.setProgress(progress + 2);
                    progressHandler.postDelayed(this, 10);
                } else {
                    handleButtonSelection(button);
                }
            }
        };
        progressHandler.post(progressRunnable);
    }

    private void handleButtonSelection(ImageButton button) {
        String fileUrl = buttonUrlMap.get(button);
        String selectedBook = buttonTagMap.get(button);

        currentProgressBar.setProgress(0);
        currentProgressBar.setVisibility(View.GONE);

        // Intent로 URL과 선택한 책 정보를 전송
        Intent intent = new Intent(BookSelectionActivity.this, MainActivity.class);
        intent.putExtra("fileUrl", fileUrl);
        intent.putExtra("selectedBook", selectedBook);

        // 책의 첫 페이지로 이동
        intent.putExtra("selectedNumber", 0);
        showToast(this, "책의 첫 페이지로 이동합니다.", true);

        startActivity(intent);
        finish();
    }

    private void pauseProgressBar() {
        progressHandler.removeCallbacks(progressRunnable);
        if (currentProgressBar != null) {
            currentProgressBar.setVisibility(View.GONE);
            currentProgressBar.setProgress(0);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gazeTracker != null) {
            GazeTrackerManager.getInstance().startTracking();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (gazeTracker != null) {
            GazeTrackerManager.getInstance().stopTracking();
        }
    }
}