package com.example.eye_smart;

import android.Manifest;

import android.os.Handler;
import android.os.HandlerThread;
import android.view.View;
import android.widget.Button;

import camp.visual.eyedid.gazetracker.GazeTracker;
import camp.visual.eyedid.gazetracker.callback.TrackingCallback;
import camp.visual.eyedid.gazetracker.constant.CalibrationModeType;
import camp.visual.eyedid.gazetracker.metrics.state.TrackingState;
import camp.visual.eyedid.gazetracker.util.ViewLayoutChecker;

import com.example.eye_smart.view.CalibrationViewer;
import com.example.eye_smart.view.PointView;

public class GazeTrackingActivity {
    private GazeTracker gazeTracker;
    private final String EYEDID_SDK_LICENSE = BuildConfig.EYEDID_API_KEY;
    private final CalibrationModeType calibrationType = CalibrationModeType.DEFAULT;
    private final String[] PERMISSIONS = new String[]{
            Manifest.permission.CAMERA
    };

    private final int REQ_PERMISSION = 1000;

    private View layoutProgress;
    private PointView viewPoint;
    private boolean skipProgress = false;
    private Button btnStartTracking, btnStopTracking, btnStartCalibration;
    private CalibrationViewer viewCalibration;
    private final ViewLayoutChecker viewLayoutChecker = new ViewLayoutChecker();
    private Handler backgroundHandler;
    private final HandlerThread backgroundThread = new HandlerThread("background");

    private final TrackingCallback trackingCallback = (timestamp, gazeInfo, faceInfo, blinkInfo, userStatusInfo) -> {
        if (gazeInfo.trackingState == TrackingState.SUCCESS) {
            viewPoint.setPosition(gazeInfo.x, gazeInfo.y);
        }
    };

    private boolean isFirstPoint = false;

}
