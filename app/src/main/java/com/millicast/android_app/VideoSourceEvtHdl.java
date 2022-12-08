package com.millicast.android_app;

import android.os.Handler;

import com.millicast.VideoSource;

import static com.millicast.android_app.MCStates.*;
import static com.millicast.android_app.MCTypes.Source.CURRENT;
import static com.millicast.android_app.Utils.logD;
import static com.millicast.android_app.Utils.makeSnackbar;

/**
 * Implementation of VideoSource's event listener.
 * This handles camera events that allows us to know the outcome and details of camera operations.
 */
class VideoSourceEvtHdl implements VideoSource.EventsHandler {
    public static final String TAG = "VidSrcEvtHdl";

    private MillicastManager mcMan;
    private String logTag = "[Video][Source][Evt][Hdl][Cam] ";

    public VideoSourceEvtHdl() {
        mcMan = MillicastManager.getSingleInstance();
    }

    @Override
    public void onCameraError(String s) {
        logD(TAG, logTag + "Error: " + s);
    }

    @Override
    public void onCameraDisconnected() {
        logD(TAG, logTag + "Disconnected.");
    }

    @Override
    public void onCameraFreezed(String s) {
        logD(TAG, "Freezed: " + s);
    }

    @Override
    public void onCameraOpening(String s) {
        makeSnackbar(logTag, "Camera opening... " + s, mcMan.getFragmentPub());
        final boolean[] paramPreviewSet = new boolean[1];
        Handler handler = new Handler();
        final double[] delaySec = {0};

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                paramPreviewSet[0] = mcMan.setCameraParams("RicMoviePreview3840");
                if (!paramPreviewSet[0]) {
                    delaySec[0] = delaySec[0] + 0.5;
                    handler.postDelayed(this, 500);
                } else {
                    mcMan = MillicastManager.getSingleInstance();
                    // If this is a camera switch, current state would already be IS_CAPTURED.
                    // Do not change states in this case.
                    if (CaptureState.IS_CAPTURED != mcMan.getCapState()) {
                        mcMan.setCapState(CaptureState.IS_CAPTURED);
                    }
                    setButtons();
                    makeSnackbar(logTag, "RT Camera opening... Set camera params success at " +
                            delaySec[0] + " s.", mcMan.getFragmentPub());
                }
            }
        };

        if (mcMan.isRicohTheta(CURRENT)) {
            // NOTE: If the setCameraParams were called directly
            // (instead of with a postDelayed of 0 ms), the call usually fails with error:
            // org.webrtc.Camera1Session.getParameters()' on a null object reference
            handler.postDelayed(runnable, 0);
        } else {
            // If this is a camera switch, current state would already be IS_CAPTURED.
            // Do not change states in this case.
            if (CaptureState.IS_CAPTURED != mcMan.getCapState()) {
                mcMan.setCapState(CaptureState.IS_CAPTURED);
            }
            setButtons();
        }
    }

    @Override
    public void onCameraOpened() {
        makeSnackbar(logTag, "Camera opened", mcMan.getFragmentPub());
    }

    @Override
    public void onFirstFrameAvailable() {
        makeSnackbar(logTag, "First Frame available", mcMan.getFragmentPub());
    }

    @Override
    public void onCameraClosed() {
        makeSnackbar(logTag, "Camera closed", mcMan.getFragmentPub());
    }

    /**
     * Set button states if containing view is available.
     */
    private void setButtons() {
        mcMan.getMainActivity().runOnUiThread(() -> {
            PublishFragment publishFragment = mcMan.getFragmentPub();
            if (publishFragment != null) {
                publishFragment.setUI();
            }
        });
    }
}
