package com.millicast.android_app;

import android.util.Log;

import com.google.android.material.snackbar.Snackbar;
import com.millicast.Publisher;
import com.millicast.StatsTree;

import java.util.Optional;

import static com.millicast.android_app.MillicastManager.PublisherState.CONNECTED;
import static com.millicast.android_app.MillicastManager.PublisherState.DISCONNECTED;
import static com.millicast.android_app.MillicastManager.PublisherState.PUBLISHING;

public class PubListener implements Publisher.Listener {
    public static final String TAG = "PubListener";

    private PublishFragment publishFragment;
    private MillicastManager mcManager;

    public PubListener() {
        mcManager = MillicastManager.getSingleInstance();
    }

    @Override
    public void onPublishing() {
        mcManager.setPubState(PUBLISHING);
        setUI();
        makeSnackbar("Publishing");
    }

    @Override
    public void onConnected() {
        mcManager.setPubState(CONNECTED);
        setUI();
        makeSnackbar("Connected");
        mcManager.startPublish();
    }

    @Override
    public void onConnectionError(String reason) {
        makeSnackbar(reason);
        mcManager.setPubState(DISCONNECTED);
        setUI();
    }

    @Override
    public void onStatsReport(StatsTree statsTree) {
        Visitor v = new Visitor();
        statsTree.visit(v);
        String log = "[StatsReport][Pub]" + v.toString();
        Log.d("STATS", log);
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

    private void makeSnackbar(String msg) {
        if (publishFragment == null) {
            return;
        }
        mcManager.getMainActivity().runOnUiThread(() -> {
            if (publishFragment != null) {
                Snackbar.make(publishFragment.getView(), msg, Snackbar.LENGTH_SHORT)
                        .setAction("Action", null).show();
            }
        });
    }
}
