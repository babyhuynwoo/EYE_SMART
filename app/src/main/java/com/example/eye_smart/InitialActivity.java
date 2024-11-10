package com.example.eye_smart;

import static com.example.eye_smart.gaze_utils.OptimizeUtils.showToast;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.eye_smart.gaze_utils.GazeInitializer;
import com.example.eye_smart.gaze_utils.GazePointView;

import camp.visual.eyedid.gazetracker.GazeTracker;
import camp.visual.eyedid.gazetracker.callback.CalibrationCallback;
import camp.visual.eyedid.gazetracker.callback.InitializationCallback;
import camp.visual.eyedid.gazetracker.callback.TrackingCallback;
import camp.visual.eyedid.gazetracker.constant.AccuracyCriteria;
import camp.visual.eyedid.gazetracker.constant.CalibrationModeType;

public class InitialActivity extends AppCompatActivity {
    private GazeInitializer gazeInitializer;
    private GazeTracker gazeTracker;
    private GazePointView gazePointView;
    private boolean isCalibrationStarted = false;

    private final CalibrationModeType calibrationType = CalibrationModeType.DEFAULT;
    private final AccuracyCriteria accuracyCriteria = AccuracyCriteria.HIGH;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_initial);

        gazePointView = findViewById(R.id.gazePointView);
        initGazeTracker();
    }

    private void initGazeTracker() {
        final String EYEDID_SDK_LICENSE = BuildConfig.EYEDID_API_KEY;
        gazeInitializer = new GazeInitializer(this, EYEDID_SDK_LICENSE, this::showErrorToast);

        // InitializationCallback 설정
        gazeInitializer.setInitializationCallback(initializationCallback);

        // 초기화 시작
        gazeInitializer.initialize();
    }

    private void startGazeTracking() {
        if (gazeTracker != null && !gazeTracker.isTracking()) {
            gazeTracker.startTracking();
        }
    }

    private void setupGazeTrackerCallbacks() {
        gazeInitializer.setTrackingCallback(trackingCallback);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        gazeInitializer.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void showErrorToast(String message) {
        runOnUiThread(() -> showToast(this, message, true));
    }

    private final TrackingCallback trackingCallback = (timestamp, gazeInfo, faceInfo, blinkInfo, userStatusInfo) -> {
        if (gazeInfo != null && gazeTracker.isTracking()) {
            float gazeX = (float) gazeInfo.x;
            float gazeY = (float) gazeInfo.y;

            // Log.d("GazeTracker", "Gaze Position: X=" + gazeX + ", Y=" + gazeY);

            runOnUiThread(() -> gazePointView.updateGazePoint(gazeX, gazeY));
        }

        if (blinkInfo != null && blinkInfo.isBlink && !isCalibrationStarted) {
            // 눈 깜빡임이 감지되었고, CalibrationActivity가 시작되지 않은 경우에만 수행
            isCalibrationStarted = true; // 플래그 설정
            Intent intent = new Intent(InitialActivity.this, CalibrationActivity.class);
            startActivity(intent);
            // finish(); // 이전 Activity 종료
            Log.d("GazeTracker", "Blink Detected.");
        }
    };

    // 캘리브레이션 콜백 설정
    private final CalibrationCallback calibrationCallback = new CalibrationCallback() {
        @Override
        public void onCalibrationProgress(float progress) {
            // 캘리브레이션 진행 상황 로그 출력
            Log.d("Calibration", "Progress: " + progress);
        }

        @Override
        public void onCalibrationNextPoint(float x, float y) {
            Log.d("Calibration", "Next Point: x=" + x + ", y=" + y);
        }

        @Override
        public void onCalibrationFinished(double[] calibrationData) {
            Log.d("Calibration", "Calibration Finished");
            showToast(InitialActivity.this, "Calibration Finished", true);
        }
    };

    // 초기화 완료 후 캘리브레이션 시작
    private final InitializationCallback initializationCallback = (gazeTracker, error) -> {
        runOnUiThread(() -> {
            if (gazeTracker == null) {
                String errorMessage = error != null ? "Gaze Tracker 초기화 실패: " + error.name() : "권한 거부";
                showToast(this, errorMessage, true);
            } else {
                this.gazeTracker = gazeTracker;
                setupGazeTrackerCallbacks();
                startGazeTracking();

                // 캘리브레이션 시작
                gazeTracker.startCalibration(calibrationType, accuracyCriteria);
            }
        });
    };
}