package com.example.eye_smart.gaze_utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;

public class GazePoint extends View {
    private final Paint paint;
    private float offsetX, offsetY;

    public GazePoint(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
        paint.setColor(Color.RED); // 빨간색 점
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
    }

    // 오프셋을 설정하는 메서드
    public void setOffset(float x, float y) {
        offsetX = x;
        offsetY = y;
    }

    // GazePointManager를 통해 좌표를 업데이트하는 메서드
    public void updateGazePoint(float x, float y) {
        float adjustedX = x - offsetX;
        float adjustedY = y - offsetY;

        GazePointManager.getInstance().setGazePoint(adjustedX, adjustedY);
        invalidate(); // View를 다시 그리도록 요청
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        // GazePointManager에서 좌표 가져오기
        float gazeX = GazePointManager.getInstance().getGazeX();
        float gazeY = GazePointManager.getInstance().getGazeY();

        // 유효한 좌표가 존재할 때만 원을 그림
        if (gazeX >= 0 && gazeY >= 0) {
            canvas.drawCircle(gazeX, gazeY, 20, paint); // 점의 크기를 20으로 설정
        }
    }
}
