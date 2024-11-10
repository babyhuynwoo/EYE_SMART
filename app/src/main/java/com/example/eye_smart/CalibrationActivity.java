package com.example.eye_smart;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import camp.visual.eyedid.gazetracker.GazeTracker;
import camp.visual.eyedid.gazetracker.callback.CalibrationCallback;
import camp.visual.eyedid.gazetracker.constant.GazeTrackerOptions;

import android.widget.TextView;

import com.example.eye_smart.gaze_utils.CalibrationView;

public class CalibrationActivity extends AppCompatActivity {
    private GazeTracker gazeTracker;
    private CalibrationView calibrationView;
    private TextView countdownTextView;
    private final HandlerThread backgroundThread = new HandlerThread("calibrationBackground");
    private final Handler handler = new Handler();

    private final float[][] calibrationPositions = new float[9][2];
    private int currentPositionIndex = 0;

    private final CalibrationCallback calibrationCallback = new CalibrationCallback() {
        @Override
        public void onCalibrationProgress(float progress) {
            Log.d("CalibrationActivity", "Calibration progress: " + progress);
        }

        @Override
        public void onCalibrationNextPoint(float x, float y) {
            runOnUiThread(() -> calibrationView.setPointPosition(x, y));
        }

        @Override
        public void onCalibrationFinished(double[] calibrationData) {
            runOnUiThread(() -> {
                calibrationView.setVisibility(View.INVISIBLE);
                showToast("Calibration completed successfully!");
            });
        }
    };

    private int countdownTime = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibration);

        initViews();
        initPositions();
        initTracker();

        backgroundThread.start();
        startCountdown();
    }

    private void initViews() {
        calibrationView = findViewById(R.id.view_calibration);
        countdownTextView = findViewById(R.id.countdown_text_view);
    }

    private void initPositions() {
        calibrationView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                float width = calibrationView.getWidth();
                float height = calibrationView.getHeight();

                calibrationPositions[0] = new float[]{width / 2, height / 2};
                calibrationPositions[1] = new float[]{width * 0.1f, height * 0.1f};
                calibrationPositions[2] = new float[]{width / 2, height * 0.1f};
                calibrationPositions[3] = new float[]{width * 0.9f, height * 0.1f};
                calibrationPositions[4] = new float[]{width * 0.9f, height / 2};
                calibrationPositions[5] = new float[]{width * 0.9f, height * 0.9f};
                calibrationPositions[6] = new float[]{width / 2, height * 0.9f};
                calibrationPositions[7] = new float[]{width * 0.1f, height * 0.9f};
                calibrationPositions[8] = new float[]{width * 0.1f, height / 2};

                calibrationView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
    }

    private void startCountdown() {
        countdownTextView.setVisibility(View.VISIBLE);
        countdownTextView.setText("카운트다운 이후에 나오는 점을 응시하세요");

        new Handler().postDelayed(() -> {
            countdownTime = 5;
            startCountdownTimer();
        }, 2000);
    }

    private void startCountdownTimer() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (countdownTime > 0) {
                    countdownTextView.setVisibility(View.VISIBLE);
                    countdownTextView.setText(String.valueOf(countdownTime));
                    countdownTime--;
                    handler.postDelayed(this, 1000);
                } else {
                    countdownTextView.setVisibility(View.GONE);
                    calibrationView.setVisibility(View.VISIBLE);
                    startPositionSequence();
                }
            }
        });
    }

    private void startPositionSequence() {
        if (currentPositionIndex < calibrationPositions.length) {
            float[] position = calibrationPositions[currentPositionIndex];
            runOnUiThread(() -> {
                calibrationView.setPointPosition(position[0], position[1]);
                calibrationView.startPointAnimation();
            });
            currentPositionIndex++;

            handler.postDelayed(this::startPositionSequence, 2000);
        } else {
            endCalibration();
        }
    }

    private void endCalibration() {
        currentPositionIndex = 0;
        calibrationView.setVisibility(View.INVISIBLE);

        Intent intent = new Intent(CalibrationActivity.this, MainActivity.class);
        startActivity(intent);

        showToast("Calibration이 완료되었습니다!");
    }

    private void initTracker() {
        if (gazeTracker == null) {
            GazeTrackerOptions options = new GazeTrackerOptions.Builder().build();
            GazeTracker.initGazeTracker(this, BuildConfig.EYEDID_API_KEY, (gazeTracker, error) -> {
                if (gazeTracker != null) {
                    this.gazeTracker = gazeTracker;
                    this.gazeTracker.setCalibrationCallback(calibrationCallback);
                } else {
                    showToast("Tracker initialization failed: " + error.name());
                }
            }, options);
        }
    }

    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(CalibrationActivity.this, message, Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);  // 모든 핸들러 작업 취소
        backgroundThread.quitSafely();
    }
}
