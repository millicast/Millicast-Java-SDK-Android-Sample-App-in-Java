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
    private String logTag = "[Pub][Ltn] ";

    public PubListener() {
        mcManager = MillicastManager.getSingleInstance();
    }

    @Override
    public void onPublishing() {
        mcManager.setPubState(PUBLISHING);
        setUI();
        makeSnackbar(logTag, "Publishing", publishFragment);
    }

    @Override
    public void onConnected() {
        mcManager.setPubState(CONNECTED);
        mcManager.enablePubStats(1000);
        setUI();
        makeSnackbar(logTag, "Connected", publishFragment);
        mcManager.startPublish();
    }

    @Override
    public void onConnectionError(String reason) {
        makeSnackbar(logTag, reason, publishFragment);
        mcManager.setPubState(DISCONNECTED);
        setUI();
    }

    @Override
    public void onStatsReport(RTCStatsReport statsReport) {
        String log = "[Pub][Stats][Report] " + statsReport.toString();
        logD("STATS", log);
    }

    @Override
    public void onActive(String s, String[] strings, Optional<String> optional) {

    }

    @Override
    public void onInactive(String s, Optional<String> optional) {

    }

    @Override
    public void onStopped() {

    }

    @Override
    public void onVad(String s, Optional<String> optional) {

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
