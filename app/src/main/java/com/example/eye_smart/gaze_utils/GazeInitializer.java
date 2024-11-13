package com.example.eye_smart.gaze_utils;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.eye_smart.camera_utils.CheckCamera;

import camp.visual.eyedid.gazetracker.GazeTracker;
import camp.visual.eyedid.gazetracker.callback.CalibrationCallback;
import camp.visual.eyedid.gazetracker.callback.InitializationCallback;
import camp.visual.eyedid.gazetracker.callback.StatusCallback;
import camp.visual.eyedid.gazetracker.callback.TrackingCallback;
import camp.visual.eyedid.gazetracker.constant.GazeTrackerOptions;

public class GazeInitializer implements CheckCamera.PermissionCallback {
    private static final String TAG = "GazeInitializer";
    private final Activity activity;
    private final String licenseKey;
    private final CheckCamera checkCamera;
    private GazeTracker gazeTracker;
    private InitializationCallback initializationCallback;
    private final ErrorCallback errorCallback;

    public interface ErrorCallback {
        void onError(String message);
    }

    // ErrorCallback을 받는 생성자
    public GazeInitializer(Activity activity, String licenseKey, ErrorCallback errorCallback) {
        this.activity = activity;
        this.licenseKey = licenseKey;
        this.errorCallback = errorCallback;
        this.checkCamera = new CheckCamera(activity, this);
        Log.d(TAG, "GazeInitializer 생성자 호출");
    }

    // InitializationCallback 설정 메서드 추가
    public void setInitializationCallback(InitializationCallback initializationCallback) {
        this.initializationCallback = initializationCallback;
    }

    // 초기화 시작 메서드 추가
    public void initialize() {
        checkCamera.checkCameraPermission(); // 권한 요청 시작
    }

    // 초기화 실패 시 에러를 처리하는 메서드
    private void handleInitializationError(String errorMessage) {
        Log.e(TAG, "초기화 오류: " + errorMessage);
        if (errorCallback != null) {
            errorCallback.onError(errorMessage);
        } else {
            Toast.makeText(activity, errorMessage, Toast.LENGTH_SHORT).show();
        }
        if (initializationCallback != null) {
            initializationCallback.onInitialized(null, null); // 초기화 실패 알림
        }
    }

    // 카메라 권한이 허용된 경우 호출되는 콜백 메서드
    @Override
    public void onPermissionGranted() {
        Log.d(TAG, "카메라 권한 허용됨");
        if (gazeTracker == null) {
            Log.d(TAG, "GazeTracker가 생성되지 않아 새로 생성.");
            GazeTrackerOptions options = new GazeTrackerOptions.Builder()
                    .setUseBlink(true)
                    .build();
            GazeTracker.initGazeTracker(activity, licenseKey, (tracker, error) -> {
                if (tracker != null) {
                    this.gazeTracker = tracker;
                    Log.d(TAG, "GazeTracker 초기화 성공");
                    if (initializationCallback != null) {
                        initializationCallback.onInitialized(tracker, error); // 초기화 성공 시 콜백 호출
                    }
                } else {
                    handleInitializationError("Gaze Tracker 초기화 실패: " + error.name());
                }
            }, options);
        } else {
            Log.d(TAG, "GazeTracker 생성 확인 초기화 완료로 간주");
            if (initializationCallback != null) {
                initializationCallback.onInitialized(gazeTracker, null);
            }
        }
    }

    // 카메라 권한이 거부된 경우 호출되는 콜백 메서드
    @Override
    public void onPermissionDenied() {
        Log.e(TAG, "카메라 권한 거부됨");
        handleInitializationError("카메라 권한이 필요합니다.");
    }

    // TrackingCallback 설정 메서드
    public void setTrackingCallback(TrackingCallback trackingCallback) {
        if (gazeTracker != null) {
            Log.d(TAG, "TrackingCallback 설정 완료");
            gazeTracker.setTrackingCallback(trackingCallback);
        } else {
            Log.e(TAG, "TrackingCallback 설정 실패");
        }
    }

    // CalibrationCallback 설정 메서드
    public void setCalibrationCallback(CalibrationCallback calibrationCallback) {
        if (gazeTracker != null) {
            Log.d(TAG, "CalibrationCallback 설정 완료");
            gazeTracker.setCalibrationCallback(calibrationCallback);
        } else {
            Log.e(TAG, "CalibrationCallback 설정 실패");
        }
    }

    // StatusCallback 설정 메서드
    public void setStatusCallback(StatusCallback statusCallback) {
        if (gazeTracker != null) {
            Log.d(TAG, "StatusCallback 설정 완료");
            gazeTracker.setStatusCallback(statusCallback);
        } else {
            Log.e(TAG, "StatusCallback 설정 실패");
        }
    }

    // Activity에서 onRequestPermissionsResult를 호출할 때 사용
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult 호출");
        checkCamera.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}