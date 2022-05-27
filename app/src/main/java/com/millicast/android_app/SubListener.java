package com.millicast.android_app;

import com.millicast.AudioTrack;
import com.millicast.LayerData;
import com.millicast.Subscriber;
import com.millicast.VideoTrack;

import org.webrtc.RTCStatsReport;

import java.util.ArrayList;
import java.util.Optional;

import static com.millicast.android_app.MillicastManager.SubscriberState.CONNECTED;
import static com.millicast.android_app.MillicastManager.SubscriberState.SUBSCRIBING;
import static com.millicast.android_app.Utils.logD;
import static com.millicast.android_app.Utils.makeSnackbar;

public class SubListener implements Subscriber.Listener {
    public static final String TAG = "SubListener";

    private final MillicastManager mcManager;
    private String logTag = "[Sub][Ltn]";

    public SubListener() {
        mcManager = MillicastManager.getSingleInstance();
    }

    @Override
    public void onSubscribed() {
        mcManager.setSubState(SUBSCRIBING);
        setUI();
        String logTag = this.logTag + "[On] ";
        makeSnackbar(logTag, "Subscribing", mcManager.getSubFragment());
    }

    @Override
    public void onSubscribedError(String s) {
        String logTag = this.logTag + "[Error] ";
        makeSnackbar(logTag, "Subscribe Error:" + s, mcManager.getSubFragment());
    }

    @Override
    public void onConnected() {
        String logTag = this.logTag + "[Con][On] ";
        mcManager.setSubState(CONNECTED);
        mcManager.enableSubStats(1000);
        makeSnackbar(logTag, "Connected", mcManager.getSubFragment());
        mcManager.startSubscribe();
    }

    @Override
    public void onConnectionError(String reason) {
        String logTag = this.logTag + "[Con][Error] ";
        mcManager.setSubState(MillicastManager.SubscriberState.DISCONNECTED);
        setUI();
        makeSnackbar(logTag, "Connection FAILED! " + reason, mcManager.getSubFragment());
    }

    @Override
    public void onStopped() {
        String logTag = "[Sub][Stop] ";
        logD(TAG, logTag + "OK.");
    }

    @Override
    public void onSignalingError(String s) {
        String logTag = this.logTag + "[Sig][Error] ";
        makeSnackbar(logTag, "Signaling Error:" + s, mcManager.getSubFragment());
    }

    @Override
    public void onStatsReport(RTCStatsReport statsReport) {
        String logTag = this.logTag + "[Stat] ";
        String log = logTag + statsReport.toString();
        logD(TAG, log);
    }

    @Override
    public void onTrack(VideoTrack videoTrack, Optional<String> mid) {
        String logTag = this.logTag + "[Track][Video] ";
        String trackId = videoTrack.getName();
        mcManager.setMidVideo(mid.get());
        logD(TAG, logTag + "Name: " + trackId + ", TransceiverId: " + mid + " has been negotiated.");

        mcManager.setRenderSubVideo(videoTrack);
        setUI();
        makeSnackbar(logTag, "Video received", mcManager.getSubFragment());
    }

    @Override
    public void onTrack(AudioTrack audioTrack, Optional<String> mid) {
        String logTag = this.logTag + "[Track][Audio] ";
        String trackId = audioTrack.getName();
        mcManager.setMidAudio(mid.get());
        logD(TAG, logTag + "Name: " + trackId + ", TransceiverId: " + mid + " has been negotiated.");
        mcManager.setSubAudioTrack(audioTrack);
        mcManager.setSubAudioEnabled(true);
        setUI();
        makeSnackbar(logTag, "Audio received", mcManager.getSubFragment());
    }

    @Override
    public void onActive(String streamId, String[] tracks, Optional<String> sourceId) {
        String logTag = this.logTag + "[Active][Source][Id]";
        String source = "";
        if (sourceId.isPresent()) {
            source = sourceId.get();
            logTag += ":" + source + " ";
        } else {
            /**
             * If a source was published without a sourceId,
             * it would be taken to be a default/main,
             * which keeps backwards compatibility with Apps written before multisource
             * (https://docs.millicast.com/docs/multisource-streams) was available.
             */
            source = "";
            logTag += ":" + source + " ";
            logD(TAG, logTag + "This source has no sourceId and so represents the default/main source.");
        }

        ArrayList<String> audioTrackIdList = new ArrayList<>();
        ArrayList<String> videoTrackIdList = new ArrayList<>();
        for (String track : tracks) {
            String[] split = track.split("/");
            if (split[0].equals("audio")) {
                audioTrackIdList.add(split[1]);
            } else if (split[0].equals("video")) {
                videoTrackIdList.add(split[1]);
            } else {
                logD(TAG, logTag + "Error! Not adding this track with kind: " + split[0] +
                        ", trackId: " + split[1] + ".");
            }
        }

        // Add to sourceList:
        SourceInfo sourceInfo = new SourceInfo(
                source, audioTrackIdList.toArray(new String[audioTrackIdList.size()]),
                videoTrackIdList.toArray(new String[videoTrackIdList.size()])
        );

        logD(TAG, logTag + "Adding active source...");
        mcManager.addSource(source, sourceInfo);
        if (sourceInfo.hasAudio()) {
            mcManager.loadViewSource(true, true);
        }
        if (sourceInfo.hasVideo()) {
            mcManager.loadViewSource(false, true);
        }
    }

    @Override
    public void onInactive(String streamId, Optional<String> sourceId) {
        String logTag = this.logTag + "[Active][In][Source][Id]";
        String source = "";
        if (sourceId.isPresent()) {
            source = sourceId.get();
            logTag += ":" + source + " ";
        } else {
            source = "";
            logTag += ":" + source + " ";
            logD(TAG, logTag + "This source has no sourceId and so represents the default/main source.");
        }
        logD(TAG, logTag + "Removing inactive source (streamId: " + streamId + ")...");
        SourceInfo sourceInfo = mcManager.removeSource(source);
        if (sourceInfo == null) {
            logD(TAG, logTag + "Failed! Unable to find the Source of this sourceId!");
            return;
        }
        if (sourceInfo.hasAudio()) {
            mcManager.loadViewSource(true, true);
        }
        if (sourceInfo.hasVideo()) {
            mcManager.loadViewSource(false, true);
        }
    }

    /**
     * Called when simulcast/svc layers are available
     *
     * @param mid            The mid associated to the track
     * @param activeLayers   Active simulcast/SVC layers
     * @param inactiveLayers inactive simulcast/SVC layers
     */
    @Override
    public void onLayers(String mid, LayerData[] activeLayers, LayerData[] inactiveLayers) {
        String logTag = this.logTag + "[Layer] ";
        String log = "mid:" + mid + " Active(" + activeLayers.length + "):[" +
                SourceInfo.getLayerListStr(activeLayers) + "]," +
                " Inactive(" + inactiveLayers.length + "):[" +
                SourceInfo.getLayerListStr(inactiveLayers) + "].";

        logD(TAG, logTag + log);
        mcManager.setLayerActiveList(activeLayers);
    }

    /**
     * Called when a source id is being multiplexed into the audio track based on the voice activity level.
     *
     * @param mid      The media id.
     * @param sourceId The source id.
     */
    @Override
    public void onVad(String mid, Optional<String> sourceId) {
        String logTag = this.logTag + "[Vad][Source][Id]";
        String source = "";
        if (sourceId.isPresent()) {
            source = sourceId.get();
            logTag += ":" + source + " ";
        } else {
            /**
             * A default/main source as described at
             * {@link onActive(String streamId, String[] tracks, Optional<String> sourceId)}.
             */
            source = "";
            logTag += ":" + source + " ";
            logD(TAG, logTag + "This source has no sourceId and so represents the default/main source.");
        }
        logD(TAG, logTag + " audio multiplexed onto mid:" + mid + ".");
    }

    @Override
    public void onViewerCount(int count) {
        String logTag = this.logTag + "[Viewer] ";
        logD(TAG, logTag + "Count: " + count + ".");
    }

    /**
     * Set UI states if containing view is available.
     */
    private void setUI() {
        mcManager.getMainActivity().runOnUiThread(() -> {
            SubscribeFragment subscribeFragment = mcManager.getSubFragment();
            if (subscribeFragment != null) {
                subscribeFragment.setUI();
            }
        });
    }
}
