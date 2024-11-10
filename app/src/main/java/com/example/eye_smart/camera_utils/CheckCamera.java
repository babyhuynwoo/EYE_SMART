package com.example.eye_smart.camera_utils;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class CheckCamera {

    private static final int CAMERA_PERMISSION_CODE = 100;
    private final Activity activity;
    private final PermissionCallback callback;

    public CheckCamera(Activity activity, PermissionCallback callback) {
        this.activity = activity;
        this.callback = callback;
    }

    // 카메라 권한을 확인하고 요청하는 메서드
    public void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            // 권한이 허용되지 않았다면 권한 요청
            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_CODE
            );
        } else {
            // 권한이 이미 허용된 경우 콜백 호출
            callback.onPermissionGranted();
        }
    }

    // 권한 요청 결과를 처리하는 메서드
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 권한이 허용된 경우 콜백 호출
                callback.onPermissionGranted();
            } else {
                // 권한이 거부된 경우 처리
                Toast.makeText(activity, "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
                callback.onPermissionDenied();
            }
        }
    }

    // 권한 요청 콜백 인터페이스
    public interface PermissionCallback {
        void onPermissionGranted();
        void onPermissionDenied();
    }
}
