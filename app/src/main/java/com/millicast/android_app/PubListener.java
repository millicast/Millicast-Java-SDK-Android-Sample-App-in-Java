package com.millicast.android_app;

import com.millicast.Publisher;

import org.webrtc.RTCStatsReport;

import java.util.Optional;

import static com.millicast.android_app.MillicastManager.PublisherState.CONNECTED;
import static com.millicast.android_app.MillicastManager.PublisherState.DISCONNECTED;
import static com.millicast.android_app.MillicastManager.PublisherState.PUBLISHING;
import static com.millicast.android_app.Utils.logD;
import static com.millicast.android_app.Utils.makeSnackbar;

public class PubListener implements Publisher.Listener {
    public static final String TAG = "PubListener";

    private PublishFragment publishFragment;
    private MillicastManager mcManager;
    private String logTag = "[Pub][Ltn]";

    public PubListener() {
        mcManager = MillicastManager.getSingleInstance();
    }

    @Override
    public void onPublishing() {
        mcManager.setPubState(PUBLISHING);
        setUI();
        String logTag = this.logTag + "[On] ";
        makeSnackbar(logTag, "Publishing", publishFragment);
    }

    @Override
    public void onPublishingError(String s) {
        String logTag = this.logTag + "[Error] ";
        makeSnackbar(logTag, "Publish Error:" + s, publishFragment);
    }

    @Override
    public void onConnected() {
        String logTag = this.logTag + "[Con][On] ";
        mcManager.setPubState(CONNECTED);
        mcManager.enablePubStats(1000);
        setUI();
        makeSnackbar(logTag, "Connected", publishFragment);
        mcManager.startPublish();
    }

    @Override
    public void onConnectionError(String reason) {
        String logTag = this.logTag + "[Con][Error] ";
        mcManager.setPubState(DISCONNECTED);
        setUI();
        makeSnackbar(logTag, "Connection FAILED! " + reason, publishFragment);
    }

    @Override
    public void onSignalingError(String s) {
        String logTag = this.logTag + "[Sig][Error] ";
        makeSnackbar(logTag, "Signaling Error:" + s, publishFragment);
    }

    @Override
    public void onStatsReport(RTCStatsReport statsReport) {
        String logTag = this.logTag + "[Stat] ";
        String log = logTag + statsReport.toString();
        logD(TAG, log);
    }

    @Override
    public void onViewerCount(int count) {
        String logTag = this.logTag + "[Viewer] ";
        logD(TAG, logTag + "Count: " + count + ".");
    }

    @Override
    public void onActive() {
        String logTag = this.logTag + "[Viewer][Active] ";
        logD(TAG, logTag + "A viewer has subscribed to our stream.");
    }

    @Override
    public void onInactive() {
        String logTag = this.logTag + "[Viewer][Active][In] ";
        logD(TAG, logTag + "No viewers are currently subscribed to our stream.");
    }

    public void setPublishFragment(PublishFragment publishFragment) {
        this.publishFragment = publishFragment;
    }

    /**
     * Set UI states if containing view is available.
     */
    private void setUI() {
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
