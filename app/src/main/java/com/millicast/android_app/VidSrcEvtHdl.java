package com.millicast.android_app;

import android.os.Handler;

import com.millicast.VideoSource;

import static com.millicast.android_app.MillicastManager.Source.CURRENT;
import static com.millicast.android_app.Utils.logD;
import static com.millicast.android_app.Utils.makeSnackbar;

class VidSrcEvtHdl implements VideoSource.EventsHandler {
    public static final String TAG = "VidSrcEvtHdl";

    private PublishFragment publishFragment;
    private MillicastManager mcManager;
    private String logTag = "[Video][EvtHdl] ";

    public VidSrcEvtHdl() {
        mcManager = MillicastManager.getSingleInstance();
    }

    @Override
    public void onCameraError(String s) {
        logD(TAG, "OnCameraError : " + s);
    }

    @Override
    public void onCameraDisconnected() {
        logD(TAG, "OnCameraDisconnected : ");
    }

    @Override
    public void onCameraFreezed(String s) {
        logD(TAG, "OnCameraFreezed : " + s);
    }

    @Override
    public void onCameraOpening(String s) {
        makeSnackbar(logTag + "Camera opening... " + s, publishFragment);
        final boolean[] paramPreviewSet = new boolean[1];
        Handler handler = new Handler();
        final double[] delaySec = {0};

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                paramPreviewSet[0] = mcManager.setCameraParams("RicMoviePreview3840");
                if (!paramPreviewSet[0]) {
                    delaySec[0] = delaySec[0] + 0.5;
                    handler.postDelayed(this, 500);
                } else {
                    mcManager = MillicastManager.getSingleInstance();
                    // If this is a camera switch, current state would already be IS_CAPTURED.
                    // Do not change states in this case.
                    if (MillicastManager.CaptureState.IS_CAPTURED != mcManager.getCapState()) {
                        mcManager.setCapState(MillicastManager.CaptureState.IS_CAPTURED);
                    }
                    setButtons();
                    makeSnackbar(logTag + "RT Camera opening... Set camera params success at " +
                            delaySec[0] + " s.", publishFragment);
                }
            }
        };

        if (mcManager.isRicohTheta(CURRENT)) {
            // NOTE: If the setCameraParams were called directly
            // (instead of with a postDelayed of 0 ms), the call usually fails with error:
            // org.webrtc.Camera1Session.getParameters()' on a null object reference
            handler.postDelayed(runnable, 0);
        } else {
            // If this is a camera switch, current state would already be IS_CAPTURED.
            // Do not change states in this case.
            if (MillicastManager.CaptureState.IS_CAPTURED != mcManager.getCapState()) {
                mcManager.setCapState(MillicastManager.CaptureState.IS_CAPTURED);
            }
            setButtons();
        }
    }

    @Override
    public void onCameraOpened() {
        makeSnackbar(logTag + "Camera opened", publishFragment);
    }

    @Override
    public void onFirstFrameAvailable() {
        makeSnackbar(logTag + "First Frame available", publishFragment);
    }

    @Override
    public void onCameraClosed() {
        makeSnackbar(logTag + "Camera closed", publishFragment);
    }

    public void setPublishFragment(PublishFragment publishFragment) {
        this.publishFragment = publishFragment;
    }

    /**
     * Set button states if containing view is available.
     */
    private void setButtons() {
        if (publishFragment == null) {
            return;
        }
        mcManager.getMainActivity().runOnUiThread(() -> {
            if (publishFragment != null) {
                publishFragment.setUI();
            }
        });
    }
}
