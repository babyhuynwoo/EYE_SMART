package com.example.eye_smart;

import static com.example.eye_smart.gaze_utils.OptimizeUtils.showToast;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eye_smart.gaze_utils.GazePoint;
import com.example.eye_smart.gaze_utils.GazeTrackerManager;

import camp.visual.eyedid.gazetracker.GazeTracker;
import camp.visual.eyedid.gazetracker.callback.TrackingCallback;
import camp.visual.eyedid.gazetracker.constant.GazeTrackerOptions;

public class BookSelectionActivity extends AppCompatActivity {

    private GazeTracker gazeTracker;
    private GazePoint gazePoint;
    private Handler progressHandler = new Handler();
    private Runnable progressRunnable;
    private boolean isGazeOnButton = false; // 버튼 위에 gaze 여부
    private ImageButton lastGazedButton = null; // 마지막으로 gaze한 버튼
    private ProgressBar currentProgressBar = null; // 현재 진행 중인 ProgressBar

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookshelf);

        // GazePointView 연결
        gazePoint = findViewById(R.id.gazePointView);

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
        ImageButton book1 = findViewById(R.id.book1);
        ImageButton book2 = findViewById(R.id.book2);
        ImageButton book3 = findViewById(R.id.book3);
        ProgressBar progressBarBook1 = findViewById(R.id.progressBarBook1);
        ProgressBar progressBarBook2 = findViewById(R.id.progressBarBook2);
        ProgressBar progressBarBook3 = findViewById(R.id.progressBarBook3);

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
                stopProgressBar(); // ProgressBar 멈추기
                lastGazedButton = null;
            }
        }
    }

    private void startProgressBar(final ImageButton button) {
        currentProgressBar.setVisibility(View.VISIBLE); // ProgressBar 보이기
        currentProgressBar.setProgress(0); // 진행 상태 초기화

        progressRunnable = new Runnable() {
            @Override
            public void run() {
                int progress = currentProgressBar.getProgress();
                if (progress < 100) {
                    currentProgressBar.setProgress(progress + 1); // 진행 상태 증가
                    progressHandler.postDelayed(this, 20); // 20ms마다 업데이트
                } else {
                    // ProgressBar가 가득 찼을 때 버튼 클릭 이벤트 발생
                    button.performClick();
                    currentProgressBar.setVisibility(View.GONE); // 완료 후 ProgressBar 숨김
                    showToast(BookSelectionActivity.this, "2초간 응시하여 버튼이 클릭되었습니다.", true);
                }
            }
        };

        progressHandler.post(progressRunnable); // ProgressBar 시작
    }

    private void stopProgressBar() {
        progressHandler.removeCallbacks(progressRunnable); // 진행 상태 중지
        if (currentProgressBar != null) {
            currentProgressBar.setVisibility(View.GONE); // ProgressBar 숨김
        }
    }
}
