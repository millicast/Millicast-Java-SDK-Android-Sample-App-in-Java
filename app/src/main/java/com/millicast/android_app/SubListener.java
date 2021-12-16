package com.millicast.android_app;

import com.millicast.AudioTrack;
import com.millicast.Subscriber;
import com.millicast.VideoTrack;

import org.webrtc.RTCStatsReport;

import java.util.Optional;

import static com.millicast.android_app.MillicastManager.SubscriberState.CONNECTED;
import static com.millicast.android_app.MillicastManager.SubscriberState.SUBSCRIBING;
import static com.millicast.android_app.Utils.logD;
import static com.millicast.android_app.Utils.makeSnackbar;

public class SubListener implements Subscriber.Listener {
    public static final String TAG = "SubListener";

    private SubscribeFragment subscribeFragment;
    private final MillicastManager mcManager;
    private final String logTag = "[Sub][Ltn] ";

    public SubListener() {
        mcManager = MillicastManager.getSingleInstance();
    }

    @Override
    public void onSubscribed() {
        mcManager.setSubState(SUBSCRIBING);
        setUI();
        makeSnackbar(logTag, "Subscribed", subscribeFragment);
    }

    @Override
    public void onTrack(VideoTrack videoTrack) {
        setRenderSubVideo(videoTrack);
        setUI();
        makeSnackbar(logTag, "Video received", subscribeFragment);
    }

    @Override
    public void onTrack(AudioTrack audioTrack) {
        mcManager.setSubAudioTrack(audioTrack);
        mcManager.setSubAudioEnabled(true);
        setUI();
        makeSnackbar(logTag, "Audio received", subscribeFragment);
    }

    @Override
    public void onConnected() {
        mcManager.setSubState(CONNECTED);
        mcManager.enableSubStats(1000);
        mcManager.startSubscribe();
        makeSnackbar(logTag, "Connected", subscribeFragment);
    }

    @Override
    public void onConnectionError(String reason) {
        mcManager.setSubState(MillicastManager.SubscriberState.DISCONNECTED);
        setUI();
        makeSnackbar(logTag, "Connection FAILED! " + reason, subscribeFragment);
    }

    @Override
    public void onStatsReport(RTCStatsReport statsReport) {
        String log = "[Sub][Stats][Report] " + statsReport.toString();
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

    public void setSubscribeFragment(SubscribeFragment subscribeFragment) {
        this.subscribeFragment = subscribeFragment;
    }

    private void setRenderSubVideo(VideoTrack videoTrack) {
        mcManager.getMainActivity().runOnUiThread(() -> {
            mcManager.setRenderSubVideoTrack(videoTrack);
        });
    }

    /**
     * Set UI states if containing view is available.
     */
    private void setUI() {
        if (subscribeFragment == null) {
            return;
        }
        mcManager.getMainActivity().runOnUiThread(() -> {
            if (subscribeFragment != null) {
                subscribeFragment.setUI();
            }
        });
    }

}
