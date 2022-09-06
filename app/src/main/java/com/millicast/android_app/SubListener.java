package com.millicast.android_app;

import com.millicast.AudioTrack;
import com.millicast.LayerData;
import com.millicast.Subscriber;
import com.millicast.VideoTrack;

import org.webrtc.RTCStatsReport;

import java.util.ArrayList;
import java.util.Optional;

import static com.millicast.android_app.MCStates.SubscriberState.CONNECTED;
import static com.millicast.android_app.MCStates.SubscriberState.SUBSCRIBING;
import static com.millicast.android_app.Utils.logD;
import static com.millicast.android_app.Utils.makeSnackbar;

/**
 * Implementation of Subscriber's Listener.
 * This handles events sent to the Subscriber being listened to.
 */
public class SubListener implements Subscriber.Listener {
    public static final String TAG = "SubListener";

    private final MillicastManager mcMan;
    private String logTag = "[Sub][Ltn]";

    public SubListener() {
        mcMan = MillicastManager.getSingleInstance();
    }

    @Override
    public void onSubscribed() {
        mcMan.setSubState(SUBSCRIBING);
        setUI();
        String logTag = this.logTag + "[On] ";
        makeSnackbar(logTag, "Subscribing", mcMan.getFragmentSub());
    }

    @Override
    public void onSubscribedError(String s) {
        String logTag = this.logTag + "[Error] ";
        makeSnackbar(logTag, "Subscribe Error:" + s, mcMan.getFragmentSub());
    }

    @Override
    public void onConnected() {
        String logTag = this.logTag + "[Con][On] ";
        mcMan.setSubState(CONNECTED);
        makeSnackbar(logTag, "Connected", mcMan.getFragmentSub());
        mcMan.startSub();
    }

    @Override
    public void onConnectionError(String reason) {
        String logTag = this.logTag + "[Con][Error] ";
        mcMan.setSubState(MCStates.SubscriberState.DISCONNECTED);
        setUI();
        makeSnackbar(logTag, "Connection FAILED! " + reason, mcMan.getFragmentSub());
    }

    @Override
    public void onStopped() {
        String logTag = "[Sub][Ltn][Stop] ";
        logD(TAG, logTag + "OK.");
    }

    @Override
    public void onSignalingError(String s) {
        String logTag = this.logTag + "[Sig][Error] ";
        makeSnackbar(logTag, "Signaling Error:" + s, mcMan.getFragmentSub());
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
        mcMan.setMidVideo(mid.get());
        logD(TAG, logTag + "Name: " + trackId + ", TransceiverId: " + mid + " has been negotiated.");

        mcMan.subRenderVideo(videoTrack);
        setUI();
        makeSnackbar(logTag, "Video received", mcMan.getFragmentSub());
    }

    @Override
    public void onTrack(AudioTrack audioTrack, Optional<String> mid) {
        String logTag = this.logTag + "[Track][Audio] ";
        String trackId = audioTrack.getName();
        mcMan.setMidAudio(mid.get());
        logD(TAG, logTag + "Name: " + trackId + ", TransceiverId: " + mid + " has been negotiated.");
        mcMan.setAudioTrackSub(audioTrack);
        mcMan.setAudioEnabledSub(true);
        setUI();
        makeSnackbar(logTag, "Audio received", mcMan.getFragmentSub());
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
        mcMan.addSource(source, sourceInfo);
        if (sourceInfo.hasAudio()) {
            mcMan.loadViewSource(true, true);
        }
        if (sourceInfo.hasVideo()) {
            mcMan.loadViewSource(false, true);
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
        SourceInfo sourceInfo = mcMan.removeSource(source);
        if (sourceInfo == null) {
            logD(TAG, logTag + "Failed! Unable to find the Source of this sourceId!");
            return;
        }
        if (sourceInfo.hasAudio()) {
            mcMan.loadViewSource(true, true);
        }
        if (sourceInfo.hasVideo()) {
            mcMan.loadViewSource(false, true);
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
        mcMan.setLayerActiveList(activeLayers);
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
        mcMan.getMainActivity().runOnUiThread(() -> {
            SubscribeFragment subscribeFragment = mcMan.getFragmentSub();
            if (subscribeFragment != null) {
                subscribeFragment.setUI();
            }
        });
    }
}
