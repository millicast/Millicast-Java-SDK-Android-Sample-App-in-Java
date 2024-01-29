package com.millicast.android_app;

import com.millicast.devices.source.video.VideoSource;
import com.millicast.devices.type.SwitchCameraHandler;

import static com.millicast.android_app.Utils.logD;

/**
 * Implementation of VideoSource's camera switch listener.
 * This handles camera switch events that allows us to know the outcome and details of camera switching.
 */
class SwitchHdl implements SwitchCameraHandler {
    public static final String TAG = "SwitchHdl";
    private String logTag = "[Video][Source][Cam][Switch][Hdl] ";

    @Override
    public void onCameraSwitchDone(boolean b) {
        logD(TAG, logTag + "Done: " + b);
    }

    @Override
    public void onCameraSwitchError(String s) {
        logD(TAG, logTag + "Error: " + s);
    }
}
