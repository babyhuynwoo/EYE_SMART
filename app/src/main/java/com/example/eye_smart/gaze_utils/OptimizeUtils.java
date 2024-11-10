package com.example.eye_smart.gaze_utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

public class OptimizeUtils {

    // Toast 메시지를 표시하는 static 메서드
    public static void showToast(Context context, String msg, boolean isShort) {
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(context, msg, isShort ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG).show()
        );
    }
}
