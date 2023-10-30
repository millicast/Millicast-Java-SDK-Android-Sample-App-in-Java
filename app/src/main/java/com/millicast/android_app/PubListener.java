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
    private String logTagClass = "[Pub][Ltn]";

    public PubListener() {
        mcMan = MillicastManager.getSingleInstance();
    }

    @Override
    public void onPublishing() {
        mcMan.setPubState(PUBLISHING);
        setUI();
        String logTag = logTagClass + "[On] ";
        makeSnackbar(logTag, "OK. Publish started.", mcMan.getFragmentPub());
    }

    @Override
    public void onPublishingError(String s) {
        String logTag = logTagClass + "[Error] ";
        makeSnackbar(logTag, "Publish Error:" + s, mcMan.getFragmentPub());
    }

    @Override
    public void onConnected() {
        String logTag = logTagClass + "[Con][On] ";
        mcMan.setPubState(CONNECTED);
        setUI();
        makeSnackbar(logTag, "Connected", mcMan.getFragmentPub());
        mcMan.startPub();
    }

    @Override
    public void onDisconnected() {
        String logTag = logTagClass + "[Con][On][X] ";
        makeSnackbar(logTag, "Disconnected", mcMan.getFragmentPub());
    }

    @Override
    public void onConnectionError(int status, String reason) {
        String logTag = logTagClass + "[Con][Error] ";
        mcMan.setPubState(DISCONNECTED);
        setUI();
        makeSnackbar(logTag, "Connection FAILED! " + reason, mcMan.getFragmentPub());
    }

    @Override
    public void onSignalingError(String s) {
        String logTag = logTagClass + "[Sig][Error] ";
        makeSnackbar(logTag, "Signaling Error:" + s, mcMan.getFragmentPub());
    }

    @Override
    public void onStatsReport(RTCStatsReport statsReport) {
        String logTag = logTagClass + "[Stat] ";
        String log = statsReport.toString();
        logD(TAG, log, logTag);
    }

    @Override
    public void onViewerCount(int count) {
        String logTag = logTagClass + "[Viewer] ";
        logD(TAG, logTag + "Count: " + count + ".");
    }

    @Override
    public void onActive() {
        String logTag = logTagClass + "[Viewer][Active] ";
        logD(TAG, logTag + "A viewer has subscribed to our stream.");
    }

    @Override
    public void onInactive() {
        String logTag = logTagClass + "[Viewer][Active][In] ";
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
