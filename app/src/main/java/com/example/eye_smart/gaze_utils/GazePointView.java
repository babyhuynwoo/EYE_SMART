package com.example.eye_smart.gaze_utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;

public class GazePointView extends View {
    private final Paint paint;
    private float gazeX = -1;
    private float gazeY = -1;

    public GazePointView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
        paint.setColor(Color.RED); // 빨간색 점
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
    }

    public void updateGazePoint(float x, float y) {
        gazeX = x;
        gazeY = y;
        invalidate(); // View를 다시 그리도록 요청
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        // Log.d("GazePointView", "onDraw 호출됨. gazeX=" + gazeX + ", gazeY=" + gazeY);
        if (gazeX >= 0 && gazeY >= 0) {
            canvas.drawCircle(gazeX, gazeY, 20, paint); // 점의 크기를 20으로 늘려서 더 잘 보이게 함
        }
    }
}

