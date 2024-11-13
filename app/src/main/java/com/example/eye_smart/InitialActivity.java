package com.example.eye_smart;

import static com.example.eye_smart.gaze_utils.OptimizeUtils.showToast;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.eye_smart.gaze_utils.GazeInitializer;
import com.example.eye_smart.gaze_utils.GazePoint;
import com.example.eye_smart.gaze_utils.GazePointManager;
import com.example.eye_smart.gaze_utils.GazeTrackerManager;

import camp.visual.eyedid.gazetracker.GazeTracker;
import camp.visual.eyedid.gazetracker.callback.InitializationCallback;
import camp.visual.eyedid.gazetracker.callback.TrackingCallback;

public class InitialActivity extends AppCompatActivity {
    private GazeInitializer gazeInitializer;
    private GazeTracker gazeTracker;
    private GazePoint gazePoint;
    private boolean isCalibrationStarted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_initial);

        gazePoint = findViewById(R.id.gazePointView);
        initGazeTracker();
    }

    private void initGazeTracker() {
        final String EYEDID_SDK_LICENSE = BuildConfig.EYEDID_API_KEY;
        gazeInitializer = new GazeInitializer(this, EYEDID_SDK_LICENSE, message -> showToast(this, message, true));

        gazeInitializer.setInitializationCallback(initializationCallback);
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

    private final TrackingCallback trackingCallback = (timestamp, gazeInfo, faceInfo, blinkInfo, userStatusInfo) -> {
        if (gazeInfo != null && gazeTracker.isTracking()) {
            GazePointManager.getInstance().setGazePoint(gazeInfo.x, gazeInfo.y);
            runOnUiThread(() -> gazePoint.invalidate());
        }

        if (blinkInfo != null && blinkInfo.isBlink && !isCalibrationStarted) {
            isCalibrationStarted = true;
            Intent intent = new Intent(InitialActivity.this, CalibrationActivity.class);
            startActivity(intent);
            Log.d("GazeTracker", "Blink Detected.");
        }
    };

    private final InitializationCallback initializationCallback = (gazeTracker, error) -> {
        runOnUiThread(() -> {
            if (gazeTracker == null) {
                String errorMessage = error != null ? "Gaze Tracker 초기화 실패: " + error.name() : "권한 거부";
                showToast(this, errorMessage, true);
            } else {
                this.gazeTracker = gazeTracker;
                GazeTrackerManager.getInstance().setGazeTracker(gazeTracker);

                setupGazeTrackerCallbacks();
                startGazeTracking();
            }
        });
    };
}
