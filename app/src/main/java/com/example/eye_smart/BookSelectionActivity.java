package com.example.eye_smart;

import static com.example.eye_smart.gaze_utils.OptimizeUtils.showToast;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eye_smart.gaze_utils.GazePoint;
import com.example.eye_smart.gaze_utils.GazeTrackerManager;

import java.io.File;

import camp.visual.eyedid.gazetracker.GazeTracker;
import camp.visual.eyedid.gazetracker.callback.TrackingCallback;
import camp.visual.eyedid.gazetracker.constant.GazeTrackerOptions;

public class BookSelectionActivity extends AppCompatActivity {

    private GazeTracker gazeTracker;
    private GazePoint gazePoint;
    private final Handler progressHandler = new Handler();
    private Runnable progressRunnable;
    private ImageButton lastGazedButton = null; // 마지막으로 gaze한 버튼
    private ProgressBar currentProgressBar = null; // 현재 진행 중인 ProgressBar

    private ProgressBar progressBarBook1;
    private ProgressBar progressBarBook2;
    private ProgressBar progressBarBook3;

    private ImageButton book1;
    private ImageButton book2;
    private ImageButton book3;

    private String urlBook1;
    private String urlBook2;
    private String urlBook3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookshelf);

        // GazePointView 연결
        gazePoint = findViewById(R.id.gazePointView);

        // 버튼 초기화
        book1 = findViewById(R.id.book1);
        book2 = findViewById(R.id.book2);
        book3 = findViewById(R.id.book3);

        // ProgressBar 초기화
        progressBarBook1 = findViewById(R.id.progressBarBook1);
        progressBarBook2 = findViewById(R.id.progressBarBook2);
        progressBarBook3 = findViewById(R.id.progressBarBook3);

        // 파일 경로 설정
        File sampleFile1 = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "sample_1.txt");
        File sampleFile2 = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "sample_2.txt");
        File sampleFile3 = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "sample_3.txt");

        urlBook1 = Uri.fromFile(sampleFile1).toString();
        urlBook2 = Uri.fromFile(sampleFile2).toString();
        urlBook3 = Uri.fromFile(sampleFile3).toString();

        // GazeTracker 가져오기
        gazeTracker = GazeTrackerManager.getInstance().getGazeTracker();

        if (gazeTracker != null) {
            setupGazeTracking();
        } else {
            initTracker();
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
            float gazeX = gazeInfo.x;
            float gazeY = gazeInfo.y;

            // GazePointView에 시선 위치 업데이트
            runOnUiThread(() -> {
                gazePoint.updateGazePoint(gazeX, gazeY);
                checkGazeOnButtons(gazeX, gazeY); // 버튼 체크
            });
        }
    };

    private void checkGazeOnButtons(float gazeX, float gazeY) {
        // 각 버튼에 대해 gaze 체크
        checkGazeOnButton(book1, gazeX, gazeY, progressBarBook1);
        checkGazeOnButton(book2, gazeX, gazeY, progressBarBook2);
        checkGazeOnButton(book3, gazeX, gazeY, progressBarBook3);
    }

    private void checkGazeOnButton(ImageButton button, float gazeX, float gazeY, ProgressBar progressBar) {
        int[] location = new int[2];
        button.getLocationOnScreen(location);
        int buttonX = location[0];
        int buttonY = location[1];

        // 버튼 영역 확인
        if (gazeX >= buttonX && gazeX <= buttonX + button.getWidth() &&
                gazeY >= buttonY && gazeY <= buttonY + button.getHeight()) {

            if (lastGazedButton != button) { // 시선이 다른 버튼으로 이동한 경우
                lastGazedButton = button;
                currentProgressBar = progressBar;
                startProgressBar(button); // 새로운 버튼에 대한 ProgressBar 시작
            }
        } else {
            if (lastGazedButton == button) { // 버튼에서 gaze가 벗어난 경우
                pauseProgressBar(); // ProgressBar 멈추기 (초기화하지 않음)
                lastGazedButton = null;
            }
        }
    }

    private void startProgressBar(final ImageButton button) {
        if (currentProgressBar == null) return;

        currentProgressBar.setVisibility(View.VISIBLE); // ProgressBar 보이기

        progressRunnable = new Runnable() {
            @Override
            public void run() {
                int progress = currentProgressBar.getProgress();
                if (progress < 100) {
                    currentProgressBar.setProgress(progress + 2); // 진행 상태 증가
                    progressHandler.postDelayed(this, 10);
                } else {
                    String fileUrl = null;
                    if (button == book1) {
                        fileUrl = urlBook1;
                    } else if (button == book2) {
                        fileUrl = urlBook2;
                    } else if (button == book3) {
                        fileUrl = urlBook3;
                    }

                    // 파일 URL을 로그로 출력
                    Log.d("BookSelectionActivity", "Selected file URL: " + fileUrl);
                    currentProgressBar.setProgress(0);

                    // 시선 추적과 핸들러 중지 및 버튼 비활성화
                    stopGazeTrackingAndDisableButtons();

                    // 인텐트 시작
                    Intent intent = new Intent(BookSelectionActivity.this, MainActivity.class);
                    intent.putExtra("fileUrl", fileUrl); // 파일 URL을 인텐트로 전달
                    startActivity(intent);
                }
            }
        };
        progressHandler.post(progressRunnable); // ProgressBar 시작
    }

    private void stopGazeTrackingAndDisableButtons() {
        // 시선 추적 중지
        if (gazeTracker != null) {
            gazeTracker.stopTracking();
        }
        // 핸들러 콜백 제거하여 진행 중지
        progressHandler.removeCallbacks(progressRunnable);

        // ProgressBar 숨김
        if (currentProgressBar != null) {
            currentProgressBar.setVisibility(View.GONE);
        }

        // 버튼 비활성화
        book1.setEnabled(false);
        book2.setEnabled(false);
        book3.setEnabled(false);
    }

    private void pauseProgressBar() {
        progressHandler.removeCallbacks(progressRunnable); // 진행 상태 중지
        if (currentProgressBar != null) {
            currentProgressBar.setVisibility(View.VISIBLE); // ProgressBar를 보이도록 설정 (현재 상태 유지)
        }
    }
}