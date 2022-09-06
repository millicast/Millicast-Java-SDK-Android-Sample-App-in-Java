package com.millicast.android_app;

import com.millicast.Publisher;

import org.webrtc.RTCStatsReport;

import static com.millicast.android_app.MCStates.PublisherState.CONNECTED;
import static com.millicast.android_app.MCStates.PublisherState.DISCONNECTED;
import static com.millicast.android_app.MCStates.PublisherState.PUBLISHING;
import static com.millicast.android_app.Utils.logD;
import static com.millicast.android_app.Utils.makeSnackbar;

/**
 * Implementation of Publisher's Listener.
 * This handles events sent to the Publisher being listened to.
 */
public class PubListener implements Publisher.Listener {
    public static final String TAG = "PubListener";

    private MillicastManager mcMan;
    private String logTag = "[Pub][Ltn]";

    public PubListener() {
        mcMan = MillicastManager.getSingleInstance();
    }

    @Override
    public void onPublishing() {
        mcMan.setPubState(PUBLISHING);
        setUI();
        String logTag = this.logTag + "[On] ";
        makeSnackbar(logTag, "OK. Publish started.", mcMan.getFragmentPub());
    }

    @Override
    public void onPublishingError(String s) {
        String logTag = this.logTag + "[Error] ";
        makeSnackbar(logTag, "Publish Error:" + s, mcMan.getFragmentPub());
    }

    @Override
    public void onConnected() {
        String logTag = this.logTag + "[Con][On] ";
        mcMan.setPubState(CONNECTED);
        setUI();
        makeSnackbar(logTag, "Connected", mcMan.getFragmentPub());
        mcMan.startPub();
    }

    @Override
    public void onConnectionError(String reason) {
        String logTag = this.logTag + "[Con][Error] ";
        mcMan.setPubState(DISCONNECTED);
        setUI();
        makeSnackbar(logTag, "Connection FAILED! " + reason, mcMan.getFragmentPub());
    }

    @Override
    public void onSignalingError(String s) {
        String logTag = this.logTag + "[Sig][Error] ";
        makeSnackbar(logTag, "Signaling Error:" + s, mcMan.getFragmentPub());
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

    /**
     * Set UI states if containing view is available.
     */
    private void setUI() {
        mcMan.getMainActivity().runOnUiThread(() -> {
            PublishFragment publishFragment = mcMan.getFragmentPub();
            if (publishFragment != null) {
                publishFragment.setUI();
            }
        });
    }

}
