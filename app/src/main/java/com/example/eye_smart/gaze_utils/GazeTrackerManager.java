package com.example.eye_smart.gaze_utils;

import camp.visual.eyedid.gazetracker.GazeTracker;

public class GazeTrackerManager {
    private static GazeTrackerManager instance;
    private GazeTracker gazeTracker;

    private GazeTrackerManager() { }

    public static synchronized GazeTrackerManager getInstance() {
        if (instance == null) {
            instance = new GazeTrackerManager();
        }
        return instance;
    }

    public synchronized void setGazeTracker(GazeTracker gazeTracker) {
        this.gazeTracker = gazeTracker;
    }

    public synchronized GazeTracker getGazeTracker() {
        return gazeTracker;
    }

    public synchronized void startTracking() {
        if (gazeTracker != null && !gazeTracker.isTracking()) {
            gazeTracker.startTracking();
        }
    }

    public synchronized void stopTracking() {
        if (gazeTracker != null && gazeTracker.isTracking()) {
            gazeTracker.stopTracking();
        }
    }
}

