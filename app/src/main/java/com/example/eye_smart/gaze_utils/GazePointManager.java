package com.example.eye_smart.gaze_utils;

public class GazePointManager {
    private static GazePointManager instance;
    private float gazeX = -1;
    private float gazeY = -1;

    // Singleton 패턴: private 생성자를 통해 외부에서 직접 인스턴스 생성 방지
    private GazePointManager() {}

    // 싱글톤 인스턴스를 가져오는 메서드
    public static synchronized GazePointManager getInstance() {
        if (instance == null) {
            instance = new GazePointManager();
        }
        return instance;
    }

    // Gaze 좌표를 설정하는 메서드
    public void setGazePoint(float x, float y) {
        this.gazeX = x;
        this.gazeY = y;
    }

    // Gaze X 좌표를 가져오는 메서드
    public float getGazeX() {
        return gazeX;
    }

    // Gaze Y 좌표를 가져오는 메서드
    public float getGazeY() {
        return gazeY;
    }

    // Gaze 좌표를 초기화하는 메서드 (필요에 따라 사용 가능)
    public void resetGazePoint() {
        this.gazeX = -1;
        this.gazeY = -1;
    }
}
