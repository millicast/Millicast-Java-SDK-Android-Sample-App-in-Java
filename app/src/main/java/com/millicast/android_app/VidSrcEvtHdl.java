package com.millicast.android_app;

import android.app.Activity;
import android.os.Handler;
import android.util.Log;

import com.millicast.VideoSource;

import static com.millicast.android_app.MillicastManager.Source.CURRENT;

class VidSrcEvtHdl implements VideoSource.EventsHandler {
    public static final String TAG = "VidSrcEvtHdl";

    private PublishFragment publishFragment;
    private MillicastManager mcManager;

    public VidSrcEvtHdl() {
        mcManager = MillicastManager.getSingleInstance();
    }

    @Override
    public void onCameraError(String s) {
        Log.d(TAG, "OnCameraError : " + s);
    }

    @Override
    public void onCameraDisconnected() {
        Log.d(TAG, "OnCameraDisconnected : ");
    }

    @Override
    public void onCameraFreezed(String s) {
        Log.d(TAG, "OnCameraFreezed : " + s);
    }

    @Override
    public void onCameraOpening(String s) {
        Log.d(TAG, "OnCameraOpening : " + s);
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
                    if(MillicastManager.CaptureState.IS_CAPTURED != mcManager.getCapState()) {
                        mcManager.setCapState(MillicastManager.CaptureState.IS_CAPTURED);
                    }
                    setButtons();
                    Log.d("onCameraOpening", "Set camera params success at " + delaySec[0] + " s.");
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
            if(MillicastManager.CaptureState.IS_CAPTURED != mcManager.getCapState()) {
                mcManager.setCapState(MillicastManager.CaptureState.IS_CAPTURED);
            }
            setButtons();
        }
    }

    @Override
    public void onCameraOpened() {
        Log.d(TAG, "OnCameraOpened");
    }

    @Override
    public void onFirstFrameAvailable() {
        Log.d(TAG, "OnFirstFrameAvailable");
    }

    @Override
    public void onCameraClosed() {
        Log.d(TAG, "OnCameraClosed");
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
