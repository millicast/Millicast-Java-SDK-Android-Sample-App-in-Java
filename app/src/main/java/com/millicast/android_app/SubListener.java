package com.millicast.android_app;

import com.millicast.AudioTrack;
import com.millicast.StatsTree;
import com.millicast.Subscriber;
import com.millicast.VideoTrack;

import java.util.Optional;

import static com.millicast.android_app.MillicastManager.SubscriberState.CONNECTED;
import static com.millicast.android_app.MillicastManager.SubscriberState.SUBSCRIBING;
import static com.millicast.android_app.Utils.logD;
import static com.millicast.android_app.Utils.makeSnackbar;

public class SubListener implements Subscriber.Listener {
    public static final String TAG = "SubListener";

    private SubscribeFragment subscribeFragment;
    private MillicastManager mcManager;

    public SubListener() {
        mcManager = MillicastManager.getSingleInstance();
    }

    @Override
    public void onSubscribed() {
        mcManager.setSubState(SUBSCRIBING);
        setUI();
        logD(TAG, "SUBSCRIBED");
    }

    @Override
    public void onTrack(VideoTrack videoTrack) {
        logD(TAG, "onTrack");
        makeSnackbar("Subscribed", subscribeFragment);
        setRenderSubVideo(videoTrack);
        setUI();
    }

    @Override
    public void onTrack(AudioTrack audioTrack) {
        logD(TAG, "on audio track");
        mcManager.setSubAudioTrack(audioTrack);
        mcManager.setSubAudioEnabled(true);
        setUI();
    }

    @Override
    public void onConnected() {
        mcManager.setSubState(CONNECTED);
        logD(TAG, "onConnected");
        makeSnackbar("Connected", subscribeFragment);
        mcManager.startSubscribe();
    }

    @Override
    public void onConnectionError(String reason) {
        mcManager.setSubState(MillicastManager.SubscriberState.DISCONNECTED);
        setUI();
        makeSnackbar(reason, subscribeFragment);
    }

    @Override
    public void onStatsReport(StatsTree statsTree) {
        Visitor v = new Visitor();
        statsTree.visit(v);
        String log = "[StatsReport][Sub]" + v.toString();
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
        if (subscribeFragment == null) {
            return;
        }
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
