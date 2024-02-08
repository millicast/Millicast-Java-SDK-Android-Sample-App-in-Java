package com.millicast.android_app;

import com.millicast.Publisher;
import com.millicast.clients.stats.AudioSource;
import com.millicast.clients.stats.Codecs;
import com.millicast.clients.stats.InboundRtpStream;
import com.millicast.clients.stats.OutboundRtpStream;
import com.millicast.clients.stats.RemoteInboundRtpStream;
import com.millicast.clients.stats.RemoteOutboundRtpStream;
import com.millicast.clients.stats.RtsReport;
import com.millicast.clients.stats.VideoSource;
import com.millicast.publishers.listener.PublisherListener;

import org.webrtc.RTCStatsReport;

import static com.millicast.android_app.MCStates.PublisherState.CONNECTED;
import static com.millicast.android_app.MCStates.PublisherState.DISCONNECTED;
import static com.millicast.android_app.MCStates.PublisherState.PUBLISHING;
import static com.millicast.android_app.Utils.logD;
import static com.millicast.android_app.Utils.makeSnackbar;

import androidx.annotation.NonNull;

import java.util.ArrayList;

/**
 * Implementation of Publisher's Listener.
 * This handles events sent to the Publisher being listened to.
 */
public class PubListener implements PublisherListener {
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
        logD(TAG, logTag + "Publish stopped and we have disconnected.");
        mcMan.setPubState(MCStates.PublisherState.DISCONNECTED);
        mcMan.getFragmentPub().setUI();
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
    public void onStatsReport(@NonNull RtsReport rtsReport) {
        String logTag = logTagClass + "[Stat]";
        StringBuilder log =  new StringBuilder();
        rtsReport.stats().forEach(stat -> {
            switch (stat.statsType()){
                case CODEC -> {
                    log.append("[CODEC] " + ((Codecs)stat).getMimeType());
                }
                case INBOUND_RTP -> {
                    log.append("[INBOUND_RTP] " + "FPS :" + ((InboundRtpStream)stat).getFramesPerSecond());
                }
                case OUTBOUND_RTP -> {
                    log.append("[OUTBOUND_RTP] " + "FPS :"+ ((OutboundRtpStream)stat).getFramesPerSecond());
                }
                case VIDEO_SOURCE -> {
                    log.append("[VIDEO_SOURCE] " + "FPS :"+ ((VideoSource)stat).getFramesPerSecond());
                }
            }

        });
        logD(TAG, log.toString(), logTag);
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

    @Override
    public void onTransformableFrame(int i, int i1, @NonNull ArrayList<Byte> arrayList) {

    }

    public void onRecordingStarted(){
        logD(TAG, "[Rec][Start]", "Started recording");
        mcMan.setRecordingEnabled(true);
        mcMan.getFragmentPub().setUI();
    }

    public void onRecordingStopped(){
        logD(TAG, "[Rec][Stop]", "Stopped recording");
        mcMan.setRecordingEnabled(false);
        mcMan.getFragmentPub().setUI();

    }

    public void onFailedToStartRecording(){
        logD(TAG, "[Rec][Start]"+"Failed to start recording");
    }

    public void onFailedToStopRecording(){
        logD(TAG, "[Rec][Stop]", "Failed to stop recording");

    }
}
