package com.example.eye_smart;

import static com.example.eye_smart.gaze_utils.OptimizeUtils.showToast;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ProgressBar;

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

public class BookSelectionActivity extends AppCompatActivity {

    private GazeTracker gazeTracker;
    private GazePoint gazePoint;
    private final Handler progressHandler = new Handler();
    private Runnable progressRunnable;
    private ImageButton lastGazedButton = null;
    private ProgressBar currentProgressBar = null;

    private final Map<ImageButton, ProgressBar> buttonProgressMap = new HashMap<>();
    private final Map<ImageButton, String> buttonUrlMap = new HashMap<>();
    private final Map<ImageButton, Boolean> buttonEnabledMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookshelf);

        // GazePointView 연결
        gazePoint = findViewById(R.id.gazePointView);

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
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.main_color_purple)); // 툴바와 동일한 색상 설정
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

        buttonEnabledMap.put(book1, true);
        buttonEnabledMap.put(book2, true);
        buttonEnabledMap.put(book3, true);
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

            runOnUiThread(() -> {
                gazePoint.updateGazePoint(gazeX, gazeY);
                checkGazeOnButtons(gazeX, gazeY);
            });
        }
    };

    private void checkGazeOnButtons(float gazeX, float gazeY) {
        for (ImageButton button : buttonProgressMap.keySet()) {
            checkGazeOnButton(button, gazeX, gazeY);
        }
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
                startProgressBar(button);
            }
        } else {
            if (lastGazedButton == button) {
                pauseProgressBar();
                lastGazedButton = null;
            }
        }
    }

    private void startProgressBar(final ImageButton button) {
        if (currentProgressBar == null) return;

        currentProgressBar.setVisibility(View.VISIBLE);

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
        buttonEnabledMap.replaceAll((b, v) -> false);

        Log.d("BookSelectionActivity", "Selected file URL: " + fileUrl);

        currentProgressBar.setProgress(0);
        currentProgressBar.setVisibility(View.GONE);

        Intent intent = new Intent(BookSelectionActivity.this, MainActivity.class);
        intent.putExtra("fileUrl", fileUrl);
        startActivity(intent);
    }

    private void pauseProgressBar() {
        progressHandler.removeCallbacks(progressRunnable);
        if (currentProgressBar != null) {
            currentProgressBar.setVisibility(View.VISIBLE);
        }
    }
}