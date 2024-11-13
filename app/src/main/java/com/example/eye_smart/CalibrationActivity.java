package com.example.eye_smart;

import static com.example.eye_smart.gaze_utils.OptimizeUtils.showToast;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.View;
import android.widget.ProgressBar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import camp.visual.eyedid.gazetracker.GazeTracker;
import camp.visual.eyedid.gazetracker.callback.CalibrationCallback;
import camp.visual.eyedid.gazetracker.callback.InitializationCallback;
import camp.visual.eyedid.gazetracker.callback.StatusCallback;
import camp.visual.eyedid.gazetracker.callback.TrackingCallback;
import camp.visual.eyedid.gazetracker.constant.AccuracyCriteria;
import camp.visual.eyedid.gazetracker.constant.CalibrationModeType;
import camp.visual.eyedid.gazetracker.constant.StatusErrorType;
import camp.visual.eyedid.gazetracker.metrics.state.TrackingState;
import camp.visual.eyedid.gazetracker.util.ViewLayoutChecker;

import com.example.eye_smart.gaze_utils.CalibrationView;
import com.example.eye_smart.gaze_utils.GazeInitializer;
import com.example.eye_smart.gaze_utils.GazePoint;
import com.example.eye_smart.gaze_utils.GazeTrackerManager;

public class CalibrationActivity extends AppCompatActivity {

    private GazePoint gazePointView;
    private CalibrationView calibrationView;
    private ProgressBar loadingSpinner;
    private final ViewLayoutChecker layoutChecker = new ViewLayoutChecker();
    private Handler backgroundHandler;
    private final HandlerThread backgroundThread = new HandlerThread("CalibrationBackground");

    private boolean isInitialCalibrationPoint = true;

    private final TrackingCallback trackingCallback = (timestamp, gazeInfo, faceInfo, blinkInfo, userStatusInfo) -> {
        if (gazeInfo.trackingState == TrackingState.SUCCESS) {
            runOnUiThread(() -> gazePointView.updateGazePoint(gazeInfo.x, gazeInfo.y));
        }
    };

    private final CalibrationCallback calibrationCallback = new CalibrationCallback() {
        @Override
        public void onCalibrationProgress(float progress) {
            runOnUiThread(() -> calibrationView.setDotAnimationScale(progress));
        }

        @Override
        public void onCalibrationNextPoint(final float x, final float y) {
            runOnUiThread(() -> {
                calibrationView.setVisibility(View.VISIBLE);
                if (isInitialCalibrationPoint) {
                    backgroundHandler.postDelayed(() -> displayCalibrationPoint(x, y), 2500);
                } else {
                    displayCalibrationPoint(x, y);
                }
            });
        }

        @Override
        public void onCalibrationFinished(double[] calibrationData) {
            hideCalibrationView();
            showToast(CalibrationActivity.this, "Calibration Completed", true);
            Intent intent = new Intent(CalibrationActivity.this, BookSelectionActivity.class);
            startActivity(intent);
        }
    };

    private final StatusCallback statusCallback = new StatusCallback() {
        @Override
        public void onStarted() {
            // Tracker started
        }

        @Override
        public void onStopped(StatusErrorType error) {
            if (error == StatusErrorType.ERROR_CAMERA_START) {
                showToast(CalibrationActivity.this, "Camera Start Error", false);
            } else if (error == StatusErrorType.ERROR_CAMERA_INTERRUPT) {
                showToast(CalibrationActivity.this, "Camera Interrupted", false);
            }
        }
    };

    private final InitializationCallback initializationCallback = (gazeTracker, error) -> {
        runOnUiThread(() -> loadingSpinner.setVisibility(View.VISIBLE)); // 로딩 표시
        if (gazeTracker == null) {
            showToast(this, "Initialization Error: " + error.name(), true);
            runOnUiThread(() -> loadingSpinner.setVisibility(View.GONE)); // 초기화 실패 시 로딩 숨기기
        } else {
            GazeTrackerManager.getInstance().setGazeTracker(gazeTracker);
            gazeTracker.setTrackingCallback(trackingCallback);
            gazeTracker.setCalibrationCallback(calibrationCallback);
            gazeTracker.setStatusCallback(statusCallback);

            gazeTracker.startTracking();
            startCalibrationProcess();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_calibration);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeViews();
        initializeGazeTracker();

        Drawable gradientBackground = ContextCompat.getDrawable(this, R.drawable.gradient_background);
        if (gradientBackground != null) {
            calibrationView.setBackgroundDrawable(gradientBackground);
        }

        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void initializeViews() {
        calibrationView = findViewById(R.id.view_calibration);
        gazePointView = findViewById(R.id.view_point);
        loadingSpinner = findViewById(R.id.progress_bar); // 로딩 ProgressBar 추가

        gazePointView.updateGazePoint(-999, -999);
        layoutChecker.setOverlayView(gazePointView, (x, y) -> {
            gazePointView.setOffset(x, y);
            calibrationView.setOffset(x, y);
        });
    }

    private void initializeGazeTracker() {
        String EYEDID_SDK_LICENSE = BuildConfig.EYEDID_API_KEY;
        GazeInitializer gazeInitializer = new GazeInitializer(this, EYEDID_SDK_LICENSE, message -> showToast(this, message, true));
        gazeInitializer.setInitializationCallback(initializationCallback);

        runOnUiThread(() -> loadingSpinner.setVisibility(View.VISIBLE)); // 초기화 과정 중 로딩 표시
        gazeInitializer.initialize();
    }

    private void startCalibrationProcess() {
        GazeTracker gazeTracker = GazeTrackerManager.getInstance().getGazeTracker();
        if (gazeTracker == null) return;

        boolean calibrationStarted = gazeTracker.startCalibration(CalibrationModeType.DEFAULT, AccuracyCriteria.HIGH);
        if (calibrationStarted) {
            isInitialCalibrationPoint = true;
            runOnUiThread(() -> {
                calibrationView.setDotPosition(-9999, -9999);
                calibrationView.setMessageVisibility(true);
                gazePointView.setVisibility(View.INVISIBLE);

                loadingSpinner.setVisibility(View.GONE); // 캘리브레이션 시작 시 로딩 숨기기
            });
        } else {
            showToast(this, "Calibration Start Failed", false);
            runOnUiThread(() -> loadingSpinner.setVisibility(View.GONE));
        }
    }

    private void hideCalibrationView() {
        toggleViewVisibility(calibrationView, false);
        toggleViewVisibility(gazePointView, true);
    }

    private void displayCalibrationPoint(final float x, final float y) {
        calibrationView.setDotAnimationScale(0);
        calibrationView.setMessageVisibility(false);
        calibrationView.moveToNextDotColor();
        calibrationView.setDotPosition(x, y);
        long delay = isInitialCalibrationPoint ? 0 : 1200;

        backgroundHandler.postDelayed(() -> {
            GazeTracker gazeTracker = GazeTrackerManager.getInstance().getGazeTracker();
            if (gazeTracker != null) gazeTracker.startCollectSamples();
        }, delay);

        isInitialCalibrationPoint = false;
    }

    private void toggleViewVisibility(View view, boolean isVisible) {
        runOnUiThread(() -> view.setVisibility(isVisible ? View.VISIBLE : View.GONE));
    }
}
