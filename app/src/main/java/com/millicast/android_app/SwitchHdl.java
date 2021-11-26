package com.millicast.android_app;

import com.millicast.VideoSource;

import static com.millicast.android_app.Utils.logD;

class SwitchHdl implements VideoSource.SwitchCameraHandler {
    public static final String TAG = "SwitchHdl";

    @Override
    public void onCameraSwitchDone(boolean b) {
        logD(TAG, "[Switch][VideoSource][Hdl] OnCameraSwitchDone : " + b);
    }

    @Override
    public void onCameraSwitchError(String s) {
        logD(TAG, "[Switch][VideoSource][Hdl] OnCameraSwitchError : " + s);
    }
}
