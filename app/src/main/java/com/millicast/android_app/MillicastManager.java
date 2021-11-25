package com.millicast.android_app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.util.Log;

import com.millicast.AudioPlayback;
import com.millicast.AudioSource;
import com.millicast.AudioTrack;
import com.millicast.Client;
import com.millicast.LogLevel;
import com.millicast.Logger;
import com.millicast.Media;
import com.millicast.Publisher;
import com.millicast.Subscriber;
import com.millicast.VideoCapabilities;
import com.millicast.VideoRenderer;
import com.millicast.VideoSource;
import com.millicast.VideoTrack;

import java.util.ArrayList;
import java.util.Optional;

import static com.millicast.android_app.Constants.ACCOUNT_ID;
import static com.millicast.android_app.Constants.ACTION_MAIN_CAMERA_CLOSE;
import static com.millicast.android_app.Constants.ACTION_MAIN_CAMERA_OPEN;
import static com.millicast.android_app.Constants.PUBLISH_TOKEN;
import static com.millicast.android_app.Constants.PUBLISH_URL;
import static com.millicast.android_app.Constants.STREAM_NAME_PUB;
import static com.millicast.android_app.Constants.STREAM_NAME_SUB;
import static com.millicast.android_app.Constants.SUBSCRIBE_TOKEN;
import static com.millicast.android_app.Constants.SUBSCRIBE_URL;
import static com.millicast.android_app.MillicastManager.Source.CURRENT;
import static com.millicast.android_app.Utils.logD;

public class MillicastManager {
    public static final String TAG = "MCM";
    public static String keyAccountId = "ACCOUNT_ID";
    public static String keyStreamNamePub = "STREAM_NAME_PUB";
    public static String keyStreamNameSub = "STREAM_NAME_SUB";
    public static String keyPublishingToken = "PUBLISH_TOKEN_PUB";
    public static String keySubscribeToken = "PUBLISH_TOKEN_SUB";
    public static String keyPublishApiUrl = "PUBLISH_URL";
    public static String keySubscribeApiUrl = "SUBSCRIBE_URL";
    public static String keyRicohTheta = "RICOH_THETA";

    private static MillicastManager SINGLE_INSTANCE;

    private Context context;
    private Activity mainActivity;

    // Whether the camera is locked by our App.
    private boolean isCameraLocked = false;
    // Set to true if camera should be restarted after switching to another App.
    private boolean toRelockCamera = false;

    enum CaptureState {
        NOT_CAPTURED,
        TRY_CAPTURE,
        IS_CAPTURED
    }

    enum PublisherState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        PUBLISHING
    }

    enum SubscriberState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        SUBSCRIBING
    }

    /**
     * The source of a value to read from or to write to.
     */
    public enum Source {
        /**
         * The current value in MillicastManager.
         */
        CURRENT,
        /**
         * The Constants.java file.
         */
        FILE,
        /**
         * Where the device saves value.
         */
        SAVED;
    }

    private CaptureState capState = CaptureState.NOT_CAPTURED;
    private PublisherState pubState = PublisherState.DISCONNECTED;
    private SubscriberState subState = SubscriberState.DISCONNECTED;

    private boolean pubAudioEnabled = false;
    private boolean pubVideoEnabled = false;
    private boolean subAudioEnabled = false;
    private boolean subVideoEnabled = false;
    private boolean ndiVideo = false;
    private boolean ndiAudio = false;

    private String accountId = ACCOUNT_ID;
    private String streamNamePub = STREAM_NAME_PUB;
    private String streamNameSub = STREAM_NAME_SUB;
    private String publishingToken = PUBLISH_TOKEN;
    private String subscribeToken = SUBSCRIBE_TOKEN;
    private String publishApiUrl = PUBLISH_URL;
    private String subscribeApiUrl = SUBSCRIBE_URL;
    private boolean isRicohTheta = false;

    private Media media;
    private String audioSourceIndexKey = "AUDIO_SOURCE_INDEX";
    private int audioSourceIndexDefault = 0;
    private int audioSourceIndex;
    private ArrayList<AudioSource> audioSourceList;
    private AudioSource audioSource;
    private ArrayList<AudioPlayback> audioPlaybackList;
    private String audioPlaybackIndexKey = "AUDIO_PLAYBACK_INDEX";
    private int audioPlaybackIndexDefault = 0;
    private int audioPlaybackIndex;
    private AudioPlayback audioPlayback;

    private ArrayList<VideoSource> videoSourceList;
    private String videoSourceIndexKey = "VIDEO_SOURCE_INDEX";
    private int videoSourceIndexDefault = 0;
    private int videoSourceIndex;
    private VideoSource videoSource;
    private VideoSource videoSourceSwitched;

    private ArrayList<VideoCapabilities> capabilityList;
    private String capabilityIndexKey = "CAPABILITY_INDEX";
    private int capabilityIndexDefault = 0;
    private int capabilityIndex;
    private VideoCapabilities capability;

    private ArrayList<String> audioCodecList;
    private String audioCodecIndexKey = "AUDIO_CODEC_INDEX";
    private int audioCodecIndexDefault = 0;
    private int audioCodecIndex;
    private String audioCodec;
    private ArrayList<String> videoCodecList;
    private String videoCodecIndexKey = "VIDEO_CODEC_INDEX";
    private int videoCodecIndexDefault = 0;
    private int videoCodecIndex;
    private String videoCodecDefault = "VP8";
    private String videoCodecRTV = "H264";
    private String videoCodec;

    private AudioTrack pubAudioTrack;
    private AudioTrack subAudioTrack;
    private VideoTrack pubVideoTrack;
    private VideoTrack subVideoTrack;
    private VideoRenderer pubRenderer;
    private VideoRenderer subRenderer;

    private Publisher publisher;
    private Subscriber subscriber;

    // Options objects
    private Publisher.Option pubOptions;
    private Client.Option subOptions;

    // View objects
    private SwitchHdl switchHdl;
    private VidSrcEvtHdl vidSrcEvtHdl;
    private PubListener pubListener;
    private SubListener subListener;

    private MillicastManager() {
    }

    //**********************************************************************************************
    // APIs
    //**********************************************************************************************

    /**
     Millicast platform
     */

    /**
     * Method to get and initialize MillicastManager Singleton instance.
     *
     * @return
     */
    public static MillicastManager getSingleInstance() {
        synchronized (MillicastManager.class) {
            if (SINGLE_INSTANCE == null) {
                SINGLE_INSTANCE = new MillicastManager();
            }
        }
        return SINGLE_INSTANCE;
    }

    public void init(Context context) {
        this.context = context;
        Client.initMillicastSdk(this.context);
        // Set Logger
        Logger.setLoggerListener((String msg, LogLevel level) -> {
            String logTag = "[SDK][Log][L:" + level + "] ";
            logD(TAG, logTag + msg);
        });
        // Prepare Media
        getMedia();
        // Get media indices from stored values if present, else from default values.
        setVideoSourceIndex(
                Utils.getSaved(videoSourceIndexKey, videoSourceIndexDefault, context), false);
        setCapabilityIndex(Utils.getSaved(capabilityIndexKey, capabilityIndexDefault, context));
        setAudioSourceIndex(
                Utils.getSaved(audioSourceIndexKey, audioSourceIndexDefault, context));
        setAudioPlaybackIndex(
                Utils.getSaved(audioPlaybackIndexKey, audioPlaybackIndexDefault, context));
        setCodecIndex(Utils.getSaved(audioCodecIndexKey, audioCodecIndexDefault, context), true);
        setCodecIndex(Utils.getSaved(videoCodecIndexKey, videoCodecIndexDefault, context), false);

        // Set credentials from stored values if present, else from file values.
        setAccountId(Utils.getSaved(keyAccountId, ACCOUNT_ID, context), false);
        setStreamNamePub(Utils.getSaved(keyStreamNamePub, STREAM_NAME_PUB, context), false);
        setStreamNameSub(Utils.getSaved(keyStreamNameSub, STREAM_NAME_SUB, context), false);
        setPublishingToken(Utils.getSaved(keyPublishingToken, PUBLISH_TOKEN, context), false);
        setSubscribeToken(Utils.getSaved(keySubscribeToken, SUBSCRIBE_TOKEN, context), false);
        setPublishApiUrl(Utils.getSaved(keyPublishApiUrl, PUBLISH_URL, context), false);
        setSubscribeApiUrl(Utils.getSaved(keySubscribeApiUrl, SUBSCRIBE_URL, context), false);
        setRicohTheta(Utils.getSaved(keyRicohTheta, false, context), false);

        logD(TAG, "[init] OK.");

        pubOptions = new Publisher.Option();
        subOptions = new Client.Option();
    }

    public String getAccountId(Source source) {
        return Utils.getProperty(source, accountId, ACCOUNT_ID, keyAccountId, context);
    }

    public boolean setAccountId(String newValue, boolean save) {
        String logTag = "[Account][Index][Set] ";
        if (subState != SubscriberState.DISCONNECTED) {
            logD(TAG, logTag + "Failed! Cannot set when subState is " + subState + ".");
            return false;
        }
        if (save) {
            Utils.saveValue(keyAccountId, accountId, newValue, logTag, context);
        }
        accountId = newValue;
        return true;
    }

    public String getStreamNamePub(Source source) {
        return Utils.getProperty(source, streamNamePub, STREAM_NAME_PUB, keyStreamNamePub, context);
    }

    public boolean setStreamNamePub(String newValue, boolean save) {
        String logTag = "[StreamNamePub][Set] ";
        if (pubState != PublisherState.DISCONNECTED) {
            logD(TAG, logTag + "Failed! Cannot set when pubState is " + pubState + ".");
            return false;
        }
        if (save) {
            Utils.saveValue(keyStreamNamePub, streamNamePub, newValue, logTag, context);
        }
        streamNamePub = newValue;
        return true;
    }

    public String getStreamNameSub(Source source) {
        return Utils.getProperty(source, streamNameSub, STREAM_NAME_SUB, keyStreamNameSub, context);
    }

    public boolean setStreamNameSub(String newValue, boolean save) {
        String logTag = "[StreamNameSub][Set] ";
        if (subState != SubscriberState.DISCONNECTED) {
            logD(TAG, logTag + "Failed! Cannot set when subState is " + subState + ".");
            return false;
        }
        if (save) {
            Utils.saveValue(keyStreamNameSub, streamNameSub, newValue, logTag, context);
        }
        streamNameSub = newValue;
        return true;
    }

    public String getPublishingToken(Source source) {
        return Utils.getProperty(source, publishingToken, PUBLISH_TOKEN, keyPublishingToken, context);
    }

    public boolean setPublishingToken(String newValue, boolean save) {
        String logTag = "[PublishingToken][Set] ";
        if (pubState != PublisherState.DISCONNECTED) {
            logD(TAG, logTag + "Failed! Cannot set when pubState is " + pubState + ".");
            return false;
        }
        if (save) {
            Utils.saveValue(keyPublishingToken, publishingToken, newValue, logTag, context);
        }
        publishingToken = newValue;
        return true;
    }

    public String getSubscribeToken(Source source) {
        return Utils.getProperty(source, subscribeToken, SUBSCRIBE_TOKEN, keySubscribeToken, context);
    }

    public boolean setSubscribeToken(String newValue, boolean save) {
        String logTag = "[SubscribeToken][Set] ";
        if (subState != SubscriberState.DISCONNECTED) {
            logD(TAG, logTag + "Failed! Cannot set when subState is " + subState + ".");
            return false;
        }
        if (save) {
            Utils.saveValue(keySubscribeToken, subscribeToken, newValue, logTag, context);
        }
        subscribeToken = newValue;
        return true;
    }

    public String getPublishApiUrl(Source source) {
        return Utils.getProperty(source, publishApiUrl, PUBLISH_URL, keyPublishApiUrl, context);
    }

    public boolean setPublishApiUrl(String newValue, boolean save) {
        String logTag = "[PublishApiUrl][Set] ";
        if (pubState != PublisherState.DISCONNECTED) {
            logD(TAG, logTag + "Failed! Cannot set when pubState is " + pubState + ".");
            return false;
        }
        if (save) {
            Utils.saveValue(keyPublishApiUrl, publishApiUrl, newValue, logTag, context);
        }
        publishApiUrl = newValue;
        return true;
    }

    public String getSubscribeApiUrl(Source source) {
        return Utils.getProperty(source, subscribeApiUrl, SUBSCRIBE_URL, keySubscribeApiUrl, context);
    }

    public boolean setSubscribeApiUrl(String newValue, boolean save) {
        String logTag = "[SubscribeApiUrl][Set] ";
        if (subState != SubscriberState.DISCONNECTED) {
            logD(TAG, logTag + "Failed! Cannot set when subState is " + subState + ".");
            return false;
        }
        if (save) {
            Utils.saveValue(keySubscribeApiUrl, subscribeApiUrl, newValue, logTag, context);
        }
        subscribeApiUrl = newValue;
        return true;
    }

    public boolean isRicohTheta(Source source) {
        return Utils.getProperty(source, isRicohTheta, false, keyRicohTheta, context);
    }

    public boolean setRicohTheta(boolean newValue, boolean save) {
        String logTag = "[RicohTheta][Set] ";
        if (capState != CaptureState.NOT_CAPTURED) {
            logD(TAG, logTag + "Failed! Cannot set when capState is " + capState + ".");
            return false;
        }
        if (save) {
            Utils.saveValue(keyRicohTheta, isRicohTheta, newValue, logTag, context);
        }
        isRicohTheta = newValue;
        return true;
    }

    public CaptureState getCapState() {
        Log.d(TAG, "getCapState: " + capState);
        return capState;
    }

    public void setCapState(CaptureState capState) {
        this.capState = capState;
        Log.d(TAG, "setCapState: " + capState);
    }

    public PublisherState getPubState() {
        Log.d(TAG, "getPubState: " + pubState);
        return pubState;
    }

    public void setPubState(PublisherState pubState) {
        this.pubState = pubState;
        Log.d(TAG, "setPubState: " + pubState);
    }

    public SubscriberState getSubState() {
        Log.d(TAG, "getSubState: " + subState);
        return subState;
    }

    public void setSubState(SubscriberState subState) {
        this.subState = subState;
        Log.d(TAG, "setSubState: " + subState);
    }

    /**
     Connect
     */

    /**
     * Connect to Millicast for publishing.
     * Publishing credentials required.
     */
    public void pubConnect() {
        setPubState(MillicastManager.PublisherState.CONNECTING);
        connectPublisher();
        logD(TAG, "[Connect][Pub] Trying...");
    }

    /**
     * Connect to Millicast for subscribing.
     * Subscribing credentials required.
     */
    public void subConnect() {
        String logTag = "[Connect][Sub] ";
        if (getSubscriber() == null) {
            logD(TAG, logTag + "Failed as Subscriber is not available!");
            return;
        }

        if (subscriber.isConnected()) {
            logD(TAG, logTag + "Not doing as we're already connected!");
            return;
        }

        setSubState(MillicastManager.SubscriberState.CONNECTING);
        Subscriber.Credential creds = this.subscriber.getCredentials();
        creds.accountId = getAccountId(CURRENT);
        creds.streamName = getStreamNameSub(CURRENT);
        creds.apiUrl = getSubscribeApiUrl(CURRENT);

        audioPlaybackStart();

        this.subscriber.setCredentials(creds);
        this.subscriber.setOptions(subOptions);
        this.subscriber.connect();
        Log.d(TAG, "Starting media subscribe...");
        Log.d(TAG, "Started display video.");

        logD(TAG, logTag + "Trying...");
    }

    /**
     * Capture
     */

    public int getAudioCodecIndex() {
        return audioCodecIndex;
    }

    public int getVideoCodecIndex() {
        return videoCodecIndex;
    }


    /**
     * Get the current list of VideoCapabilities available.
     *
     * @return
     */
    public ArrayList<VideoCapabilities> getCapabilityList() {
        return capabilityList;
    }

    /**
     * Start capturing both audio and video (based on selected videoSource).
     */
    public void startAudioVideoCapture() {
        logD(TAG, "[Capture][Audio][Video][Start] Starting Capture...");
        startCaptureVideo();
        startCaptureAudio();
    }

    /**
     * Stop capturing both audio and video.
     */
    public void stopAudioVideoCapture() {
        logD(TAG, "[Capture][Audio][Video][Stop] Stopping Capture...");
        stopCaptureVideo();
        stopCaptureAudio();
    }

    public AudioTrack getPubAudioTrack() {
        return pubAudioTrack;
    }

    public VideoTrack getPubVideoTrack() {
        return pubVideoTrack;
    }

    public AudioTrack getSubAudioTrack() {
        return subAudioTrack;
    }

    public void setSubAudioTrack(AudioTrack subAudioTrack) {
        this.subAudioTrack = subAudioTrack;
    }

    public VideoTrack getSubVideoTrack() {
        return subVideoTrack;
    }

    /**
     * Render
     */

    public VideoRenderer getPubRenderer() {
        // If it's not available, create it with application context.
        if (pubRenderer == null) {
            pubRenderer = new VideoRenderer(context);
            logD(TAG, "[getPubRenderer] Created renderer with application context.");
        } else {
            logD(TAG, "[getPubRenderer] Using existing renderer.");
        }
        return pubRenderer;
    }

    public VideoRenderer getSubRenderer() {
        // If it's not available, create it with application context.
        if (subRenderer == null) {
            subRenderer = new VideoRenderer(context);
            logD(TAG, "[getSubRenderer] Created renderer with application context.");
        } else {
            logD(TAG, "[getSubRenderer] Using existing renderer.");
        }

        return subRenderer;
    }

    /**
     * Set the videoTrack in MillicastManager and render it.
     * Must be called on UI thread.
     *
     * @param subVideoTrack
     */
    public void setRenderSubVideoTrack(VideoTrack subVideoTrack) {
        this.subVideoTrack = subVideoTrack;
        if (subVideoTrack == null) {
            logD(TAG, "[setRenderSubVideoTrack] videoTrack is null, so not rendering it...");
            return;
        }

        if (isNdiVideoRequested()) {
            subVideoTrack.enableNdiOutput("android-video");
            logD(TAG, "[setRenderSubVideoTrack] Enabled NDI output.");
        }

        setSubVideoEnabled(true);
        logD(TAG, "[setRenderSubVideoTrack] Set videoTrack, trying to render it...");

        renderSubVideo();
    }

    /**
     * Mute / unmute audio / video.
     */

    public boolean isPubAudioEnabled() {
        return pubAudioEnabled;
    }

    public void setPubAudioEnabled(boolean pubAudioEnabled) {
        this.pubAudioEnabled = pubAudioEnabled;
    }

    public boolean isPubVideoEnabled() {
        return pubVideoEnabled;
    }

    public void setPubVideoEnabled(boolean pubVideoEnabled) {
        this.pubVideoEnabled = pubVideoEnabled;
    }

    public boolean isSubAudioEnabled() {
        return subAudioEnabled;
    }

    public void setSubAudioEnabled(boolean subAudioEnabled) {
        this.subAudioEnabled = subAudioEnabled;
    }

    public boolean isSubVideoEnabled() {
        return subVideoEnabled;
    }

    public void setSubVideoEnabled(boolean subVideoEnabled) {
        this.subVideoEnabled = subVideoEnabled;
    }


    /**
     * Select/Switch videoSource, capability and codec.
     */


    /**
     * Get or generate (if null) the current list of AudioSources available.
     *
     * @return
     */
    public ArrayList<AudioSource> getAudioSourceList() {
        String logTag = "[Source][Audio][Get] ";
        if (audioSourceList != null) {
            logD(TAG, logTag + "Using existing audioSources.");
        } else {
            logD(TAG, logTag + "Getting new audioSources.");
            // Get new audioSources.
            audioSourceList = getMedia().getAudioSources();
            if (audioSourceList == null) {
                logD(TAG, logTag + "No audioSource is available!");
                return null;
            }
        }

        // Print out list of audioSources.
        logD(TAG, logTag + "Checking for audioSources...");
        int size = audioSourceList.size();
        if (size < 1) {
            logD(TAG, logTag + "No audioSource is available!");
            return null;
        } else {
            String log = logTag;
            for (int index = 0; index < size; ++index) {
                AudioSource as = audioSourceList.get(index);
                log += "[" + index + "]:" + getAudioSourceStr(as, true) + " ";
            }
            logD(TAG, log + ".");
        }

        return audioSourceList;
    }

    public int getAudioSourceIndex() {
        String log = "[Source][Audio][Index] " + audioSourceIndex + ".";
        logD(TAG, log);
        return audioSourceIndex;
    }

    /**
     * Set the selected audioSource index to the specified value and save to device memory,
     * unless currently capturing, in which case no change will be made.
     * If set, a new audioSource will be set using this value.
     *
     * @param newValue The new value to be set.
     * @return true if new index set, false otherwise.
     */
    public boolean setAudioSourceIndex(int newValue) {

        String logTag = "[Source][Audio][Index][Set] ";

        // If currently capturing, do not set new audioSourceIndex.
        if (isAudioCaptured()) {
            logD(TAG, logTag + "NOT setting to " + newValue + " as currently capturing.");
            logD(TAG, logTag + "\nCaptured:" +
                    getAudioSourceStr(audioSource, true) +
                    " Cap:" + audioSource.isCapturing() + ".");
            return false;
        }

        Utils.saveValue(audioSourceIndexKey, audioSourceIndex, newValue, logTag, context);
        audioSourceIndex = newValue;

        // Set new audioSource.
        setAudioSource();
        logD(TAG, logTag + "OK.");
        return true;
    }

    /**
     * Get or generate (if null) the current list of VideoSources available.
     *
     * @return
     */
    public ArrayList<VideoSource> getVideoSourceList() {
        String logTag = "[Source][[Video][Get] ";
        if (videoSourceList != null) {
            logD(TAG, logTag + "Using existing videoSources.");
        } else {
            logD(TAG, logTag + "Getting new videoSources.");
            // Get new videoSources.
            videoSourceList = getMedia().getVideoSources();
            if (videoSourceList == null) {
                logD(TAG, logTag + "No videoSource is available!");
                return null;
            }
        }

        // Print out list of videoSources.
        logD(TAG, logTag + "Checking for videoSources...");
        int size = videoSourceList.size();
        if (size < 1) {
            logD(TAG, logTag + "No videoSource is available!");
            return null;
        } else {
            String log = logTag;
            for (int index = 0; index < size; ++index) {
                VideoSource vs = videoSourceList.get(index);
                log += "[" + index + "]:" + getVideoSourceStr(vs, true) + " ";
            }
            logD(TAG, log + ".");
        }

        return videoSourceList;
    }

    public int getVideoSourceIndex() {
        String log = "[Source][Video][Index] " + videoSourceIndex + ".";
        logD(TAG, log);
        return videoSourceIndex;
    }

    /**
     * Set the selected videoSource index to the specified value and save to device memory.
     * A new videoSource or videoSourceSwitched will be set using this value.
     * New capabilityList and capability will also be set using the new videoSource.
     * This on its own will not start capturing on a new videoSource,
     * if none is currently capturing.
     *
     * @param newValue    The new value to be set.
     * @param setCapIndex If true, will setCapabilityIndex with current value to update capability.
     *                    If false, only new capabilityList will be set.
     */
    public void setVideoSourceIndex(int newValue, boolean setCapIndex) {

        String logTag = "[Source][Video][Index][Set] ";
        Utils.saveValue(videoSourceIndexKey, videoSourceIndex, newValue, logTag, context);

        videoSourceIndex = newValue;
        // Set new videoSource or videoSourceSwitched.
        setVideoSource();

        // Set new capabilityList as it might have changed.
        logD(TAG, logTag + "Setting new capabilityList again.");
        setCapabilityList();
        if (setCapIndex) {
            logD(TAG, logTag + "Checking if capabilityIndex" +
                    " needs to be reset by setting capability again with current value...");
            // Set capability again as videoSource has changed.
            setCapabilityIndex(capabilityIndex);
        } else {
            logD(TAG, logTag + "Not setting capabilityIndex again.");
        }
        logD(TAG, logTag + "OK.");

    }

    /**
     * Stop capturing on current videoSource and capture using the next available videoSource on device.
     * If not currently capturing, this will set the videoSource to be used when capturing starts.
     * If at end of range of videoSources, cycle to start of the other end.
     *
     * @param ascending If true, "next" videoSource is defined in the direction of increasing index,
     *                  otherwise it is in the opposite direction.
     */
    public void switchVideoSource(boolean ascending) {

        Integer newValue = videoSourceIndexNext(ascending);
        if (newValue == null) {
            logD(TAG, "[VideoSource][Switch] FAILED! Unable to get next camera!");
            return;
        }

        // Set new videoSource
        logD(TAG, "[VideoSource][Switch] Setting videoSource index to:"
                + newValue + " and updating Capability for new VideoSource.");
        setVideoSourceIndex(newValue, true);

        if (!isVideoCaptured()) {
            // If not currently capturing, we are unable to switch videoSource,
            // as that requires a currently capturing videoSource.
            logD(TAG, "[VideoSource][Switch] Not switching videoSource as not currently capturing.");
            return;
        } else {
            logD(TAG, "[VideoSource][Switch] OK.");
        }
    }

    public int getCapabilityIndex() {
        String log = "[Capability][Index] " + capabilityIndex + ".";
        logD(TAG, log);
        return capabilityIndex;
    }

    /**
     * Set the selected capability index to the specified value and save to device memory.
     * A new capability will be set using this value.
     * This capability will be set into the videoSource, if available.
     *
     * @param newValue The new value to be set.
     */
    public void setCapabilityIndex(int newValue) {
        // Set new value into SharePreferences.
        String logTag = "[Capability][Index][Set] ";
        Utils.saveValue(capabilityIndexKey, capabilityIndex, newValue, logTag, context);
        capabilityIndex = newValue;

        // Set new capability
        setCapability();
        logD(TAG, logTag + "OK.");
    }

    /**
     * Stop capturing with current capability and capture using the next available capability.
     * If not currently capturing, this will set the capability to be used when capturing starts.
     * If at end of range of capabilities, cycle to start of the other end.
     *
     * @param ascending If true, "next" capability is defined in the direction of increasing index,
     *                  otherwise it is in the opposite direction.
     */
    public void switchCapability(boolean ascending) {

        Integer newValue = capabilityIndexNext(ascending);
        if (newValue == null) {
            logD(TAG, "[Capability][Switch] FAILED! Unable to get next capability!");
            return;
        }

        logD(TAG, "[Capability][Switch] Setting capability index to:"
                + newValue + ".");
        setCapabilityIndex(newValue);

        logD(TAG, "[Capability][Switch] OK. VideoSource: " +
                getVideoSourceName() +
                " Capability: " + getCapabilityStr(capability) + ".");
    }

    /**
     * Get or generate (if null) the current list of Video Codec supported.
     *
     * @param isAudio
     * @return
     */
    public ArrayList<String> getCodecList(boolean isAudio) {
        String logTag = "[Codec][List] ";
        String log;
        ArrayList<String> codecList;
        if (isAudio) {
            logTag = "[Audio]" + logTag;
            if (audioCodecList == null) {
                audioCodecList = getMedia().getSupportedAudioCodecs();
                log = logTag + "Getting new ones.";
            } else {
                log = logTag + "Using existing.";
            }
            codecList = audioCodecList;
        } else {
            logTag = "[Video]" + logTag;
            if (videoCodecList == null) {
                videoCodecList = getMedia().getSupportedVideoCodecs();
                log = logTag + "Getting new ones.";
            } else {
                log = logTag + "Using existing.";
            }
            codecList = videoCodecList;
        }
        log += " Codecs are: " + codecList;
        logD(TAG, log);

        return codecList;
    }

    /**
     * Set the selected codec index to the specified value and save to device memory.
     * A new videoCodec will be set using this value, unless the Publisher is publishing.
     * This videoCodec will be set into the Publisher, if it is available and not publishing.
     *
     * @param newValue The new value to be set.
     * @param isAudio
     * @return true if new index set, false otherwise.
     */
    public boolean setCodecIndex(int newValue, boolean isAudio) {
        String logTag = "[Codec][Index][Set] ";
        // Set new value into SharePreferences.
        int oldValue = videoCodecIndex;
        String key = videoCodecIndexKey;
        if (isAudio) {
            logTag = "[Audio]" + logTag;
        } else {
            logTag = "[Video]" + logTag;
        }
        if (isPublishing()) {
            logD(TAG, logTag + "Failed! Unable to set new codec while publishing!");
            return false;
        }

        if (isAudio) {
            oldValue = audioCodecIndex;
            key = audioCodecIndexKey;
            audioCodecIndex = newValue;
        } else {
            videoCodecIndex = newValue;
        }

        Utils.setSaved(key, newValue, context);
        logD(TAG, logTag + "Now: " + newValue +
                " Was: " + oldValue);

        // Set new codec
        setCodecs();
        return true;
    }

    /**
     * Set the codec for publishing / subscribing to the next available codec.
     * This can only be done when not publishing / subscribing.
     * If at end of range of codec, cycle to start of the other end.
     *
     * @param ascending If true, "next" codec is defined in the direction of increasing index,
     *                  otherwise it is in the opposite direction.
     * @param isAudio
     */
    public void switchCodec(boolean ascending, boolean isAudio) {
        String logTag = "[Codec][Switch] ";
        if (isAudio) {
            logTag = "[Audio]" + logTag;
        } else {
            logTag = "[Video]" + logTag;
        }

        Integer newValue = codecIndexNext(ascending, isAudio);
        if (newValue == null) {
            logD(TAG, logTag + "FAILED! Unable to get next codec!");
            return;
        }

        logD(TAG, logTag + "Setting codec index to:"
                + newValue + ".");
        setCodecIndex(newValue, isAudio);

        logD(TAG, logTag + "OK.");
    }


    /**
     * Publish
     */

    /**
     * Sets the Publish View into the Publisher.Listener and VideoSource.EventsHandler.
     * If either of these do not exist, they will be created.
     *
     * @param view
     */
    public void setPubView(PublishFragment view) {
        if (pubListener == null) {
            logD(TAG, "[setPubView] PubListener does not exist...");
            pubListener = new PubListener();
            logD(TAG, "[setPubView] Created a new PubListener.");
        }
        pubListener.setPublishFragment(view);
        if (vidSrcEvtHdl == null) {
            logD(TAG, "[setPubView] VideoSource.EventsHandler does not exist...");
            vidSrcEvtHdl = new VidSrcEvtHdl();
            logD(TAG, "[setPubView] Created a new VideoSource.EventsHandler.");
        }
        vidSrcEvtHdl.setPublishFragment(view);
        logD(TAG, "[setPubView] Set Publish View into the Publisher.Listener and VideoSource.EventsHandler.");
    }

    /**
     * Publish audio and video tracks that are already captured.
     * Must first be connected to Millicast.
     */
    public void startPublish() {
        String logTag = "[Publish][Start] ";
        if (publisher == null) {
            logD(TAG, logTag + "Not publishing as Publisher is not available!");
            return;
        }
        if (!publisher.isConnected()) {
            if (pubState == PublisherState.CONNECTED) {
                logD(TAG, logTag + "Client.isConnected FALSE!!! " +
                        "Continuing as pubState is " + pubState + ".");
            } else {
                logD(TAG, logTag + "Not publishing as we are not connected!" +
                        " pubState is " + pubState + ".");
                return;
            }
        }
        if (publisher.isPublishing()) {
            logD(TAG, logTag + "Not publishing as we are already publishing!");
            return;
        }

        if (!isAudioVideoCaptured()) {
            logD(TAG, logTag + "Not publishing as audio & video are both not captured!.");
            return;
        }

        getPublisher().addTrack(getPubVideoTrack());
        publisher.addTrack(getPubAudioTrack());

        setCodecs();
        logD(TAG, logTag + "Set " + audioCodec + "/" + videoCodec +
                " as preferred audio/video codec.");
        publisher.publish();
        logD(TAG, logTag + "Trying...");
    }

    /**
     * Stop publishing and disconnect from Millicast.
     * Does not affect capturing.
     */
    public void stopPublish() {
        String logTag = "[Publish][Stop] ";
        if (!isPublishing()) {
            logD(TAG, logTag + "Failed as we are not publishing!");
            return;
        }

        try {
            publisher.unpublish();
        } catch (Exception e) {
            logD(TAG, logTag + "Failed! Error: " + e.getLocalizedMessage() + ".");
        }
        logD(TAG, logTag + "Stopped Publishing.");
        setPubState(MillicastManager.PublisherState.CONNECTED);

        publisher.disconnect();
        enablePubStats(0);
        setPubState(MillicastManager.PublisherState.DISCONNECTED);
        logD(TAG, logTag + "Disconnected from Millicast.");

        // Replace with new Publisher in case a newly captured video
        // is to be published.
        publisher = null;
        Log.d(TAG, logTag + "Publisher released and recreating...");
        getPublisher();
        logD(TAG, logTag + "Publisher recreated.");
        logD(TAG, logTag + "Setting new audio/videoCodec...");
        setCodecIndex(audioCodecIndex, true);
        setCodecIndex(videoCodecIndex, false);
        logD(TAG, logTag + "OK.");
    }

    /**
     * Override WebRTC bandwidth estimate (BWE) with the given value.
     *
     * @param value Set BWE to this value in bytes.
     */
    public void overrideBWE(int value) {
        pubOptions.bwe = Optional.of(value);
        logD(TAG, "[overrideBWE] Overridden Publisher BWE to " + value + " bytes.");
    }

    /**
     * Subscribe
     */

    /**
     * Sets the Subscribe View into the Subscriber.Listener.
     * If the Listener does not exist, it will be created.
     *
     * @param view
     */
    public void setSubView(SubscribeFragment view) {
        if (subListener == null) {
            logD(TAG, "[setSubView] SubListener does not exist...");
            subListener = new SubListener();
            logD(TAG, "[setSubView] Created a new SubListener.");
        }
        subListener.setSubscribeFragment(view);
        logD(TAG, "[setSubView] Set Subscribe View into the Subscriber.Listener.");
    }

    /**
     * Subscribe to specified stream on Millicast.
     * Must first be connected to Millicast.
     */
    public void startSubscribe() {
        if (subscriber == null) {
            logD(TAG, "[startSubscribe] Not subscribing as Subscriber is not available!");
            return;
        }
        if (!subscriber.isConnected()) {
            if (subState == SubscriberState.CONNECTED) {
                logD(TAG, "[startSubscribe] Client.isConnected FALSE!!! " +
                        "Continuing as subState is " + subState + ".");
            } else {
                logD(TAG, "[startSubscribe] Not subscribing as we are not connected!" +
                        " subState is " + subState + ".");
                return;
            }
        }
        if (subscriber.isSubscribed()) {
            logD(TAG, "[startSubscribe] Not subscribing as we are already subscribing!");
            return;
        }

        subscriber.subscribe();
        logD(TAG, "[startSubscribe] Trying...");
    }

    /**
     * Stop subscribing and disconnect Subscriber from Millicast .
     */
    public void stopSubscribe() {
        if (subscriber == null || !subscriber.isSubscribed()) {
            logD(TAG, "[stopSubscribe] Failed as we are not subscribing!");
            return;
        }

        // Stop subscribing
        stopSubscribeAudioVideo();

        // Disconnect Subscriber
        subscriber.disconnect();
        enableSubStats(0);
        setSubState(SubscriberState.DISCONNECTED);
        logD(TAG, "Disconnected from Millicast.");
    }

    public ArrayList<AudioPlayback> getAudioPlaybackList() {
        if (audioPlaybackList == null) {
            audioPlaybackList = getMedia().getAudioPlayback();
        }
        String log = "[getAudioPlaybackList] AudioPlaybackList is: " + audioPlaybackList;
        Log.d(TAG, log);
        return audioPlaybackList;
    }

    public int getAudioPlaybackIndex() {
        String log = "[Playback][Audio][Index] " + audioPlaybackIndex + ".";
        logD(TAG, log);
        return audioPlaybackIndex;
    }

    public boolean setAudioPlaybackIndex(int newValue) {
        String logTag = "[Playback][Audio][Index][Set] ";

        // If currently subscribing, do not set new audioSourceIndex.
        if (isSubscribing()) {
            logD(TAG, logTag + "NOT setting to " + newValue + " as currently subscribing.");
            logD(TAG, logTag + "AudioPlayback:" +
                    getAudioSourceStr(audioPlayback, true));
            return false;
        }

        Utils.saveValue(audioPlaybackIndexKey, audioPlaybackIndex, newValue, logTag, context);
        audioPlaybackIndex = newValue;
        setAudioPlayback();
        return true;
    }

    /**
     * Utilities
     */

    public Context getContext() {
        return context;
    }

    /**
     * Get the name of the currently selected videoSource.
     *
     * @return
     */
    public String getVideoSourceName() {
        String name;
        String log = "[VideoSource][Name] Using ";
        // Get videoSource name of selected index.
        name = getVideoSourceStr(getVideoSource(true), true);
        log += "Selected VS: " + name;
        logD(TAG, log);
        return name;
    }

    /**
     * Get the name of the currently selected Capability.
     *
     * @return
     */
    public String getCapabilityName() {
        String name = "[" + capabilityIndex + "] ";
        String log = "[Capability][Name] Using ";
        // Get capability name of selected index.
        name += getCapabilityStr(capability);
        log += "Selected Cap: ";
        logD(TAG, log + name);
        return name;
    }

    /**
     * Get the name of the currently selected video Codec.
     *
     * @param isAudio
     * @return
     */
    public String getCodecName(boolean isAudio) {
        int index = videoCodecIndex;
        String codec = videoCodec;
        if (isAudio) {
            index = audioCodecIndex;
            codec = audioCodec;
        }
        String name = "[" + index + "] ";
        String log = "[Codec][Name] Using ";
        // Get codec name of selected index.
        name += codec;
        log += "Selected Codec: ";
        logD(TAG, log + name);
        return name;
    }

    /**
     * Enable or disable Publisher's WebRTC stats.
     * Do NOT call before Publisher is connected to Millicast.
     *
     * @param enable The interval in ms between stats reports.
     *               Set to 0 to disable stats.
     */
    public void enablePubStats(int enable) {
        if (publisher != null) {
            String logTag = "[Pub][Stats][Enable] ";
            if (enable > 0) {
                publisher.getStats(enable);
                logD(TAG, logTag + "YES. Interval: " + enable + "ms.");
            } else {
                publisher.getStats(0);
                logD(TAG, logTag + "NO.");
            }
        }
    }

    /**
     * Enable or disable Subscriber's WebRTC stats.
     * Do NOT call before Subscriber is connected to Millicast.
     *
     * @param enable The interval in ms between stats reports.
     *               Set to 0 to disable stats.
     */
    public void enableSubStats(int enable) {
        if (subscriber != null) {
            String logTag = "[Sub][Stats][Enable] ";
            if (enable > 0) {
                subscriber.getStats(enable);
                logD(TAG, logTag + "YES. Interval: " + enable + "ms.");
            } else {
                subscriber.getStats(0);
                logD(TAG, logTag + "NO.");
            }
        }
    }

    public boolean isNdiVideoRequested() {
        return ndiVideo;
    }

    public void setNdiVideo(boolean ndiVideo) {
        this.ndiVideo = ndiVideo;
    }

    public boolean isNdiAudioRequested() {
        return ndiAudio;
    }

    public void setNdiAudio(boolean ndiAudio) {
        this.ndiAudio = ndiAudio;
    }

    /**
     * For Ricoh Theta cameras only.
     * Set the camera status to locked (so that SA can use it),
     * or unlocked (so that other Apps can use it).
     *
     * @param isLocked
     */
    public void setCameraLock(boolean isLocked) {
        if (!isRicohTheta(CURRENT)) {
            return;
        }

        if (isLocked) {
            if (!isCameraLocked) {
                context.sendBroadcast(new Intent(ACTION_MAIN_CAMERA_CLOSE));
                isCameraLocked = true;
            }
        } else {
            if (isCameraLocked) {
                context.sendBroadcast(new Intent(ACTION_MAIN_CAMERA_OPEN));
                isCameraLocked = false;
            }
        }
    }

    /**
     * Relock the camera if it was unlocked when changing App.
     *
     * @return
     */
    public void restoreCameraLock() {
        if (toRelockCamera) {
            setCameraLock(true);
            toRelockCamera = false;
        }
    }

    /**
     * Record if camera should be restored when SA resumes.
     * Call only when SA stops.
     *
     * @return
     */
    public void flagCameraRestore() {
        if (isCameraLocked) {
            toRelockCamera = true;
        }
    }

    private SwitchHdl getSwitchHdl() {
        if (switchHdl == null) {
            switchHdl = new SwitchHdl();
        }
        return switchHdl;
    }

    public boolean setCameraParams(String shootMode) {
        boolean result = true;
        try {
            VideoSource videoSource = MillicastManager.getSingleInstance().getVideoSource(false);
            Camera.Parameters parameters = videoSource.getParameters();

            Log.d(TAG, "setCameraParams: setting " + shootMode + "... ");
            parameters.set("RIC_PROC_STITCHING", "RicStaticStitching");
            parameters.set("RIC_SHOOTING_MODE", shootMode);

            String current = parameters.flatten();
            videoSource.setParameters(parameters);
            logD(TAG, "setCameraParams: " + shootMode + " set.\n" +
                    "Current Params: " + current);
        } catch (NullPointerException e) {
            Log.d(TAG, "setCameraParams: Failed to set " + shootMode + ".\n" +
                    "Error: " + e.getLocalizedMessage());
            result = false;
        } catch (ClassCastException e) {
            Log.d(TAG, "setCameraParams: Failed to set " + shootMode + ".\n" +
                    "Error: " + e.getLocalizedMessage());
            result = false;
        } catch (IllegalStateException e) {
            Log.d(TAG, "setCameraParams: Failed to set " + shootMode + ".\n" +
                    "Error: " + e.getLocalizedMessage());
            result = false;
        }

        return result;
    }

    public void releaseViews() {
        String logTag = TAG + "[releaseViews]";
        if (vidSrcEvtHdl != null) {
            vidSrcEvtHdl.setPublishFragment(null);
        }
        Log.d(logTag, "VideoSource EventHandler removed.");
        if (pubListener != null) {
            pubListener.setPublishFragment(null);
        }
        Log.d(logTag, "Publisher Listener removed.");
        if (subListener != null) {
            subListener.setSubscribeFragment(null);
        }
        Log.d(logTag, "Subscriber Listener removed.");
    }

    public void release() {
        String logTag = TAG + "[release]";
        media = null;
        Log.d(logTag, "Media removed.");

        if (publisher != null) {
            publisher.release();
            publisher = null;
            Log.d(logTag, "Publisher released.");
            pubVideoTrack = null;
            pubAudioTrack = null;
            pubAudioEnabled = false;
            pubVideoEnabled = false;
            Log.d(logTag, "Publisher Video and Audio tracks released.");
        }
        if (subscriber != null) {
            subscriber.release();
            subscriber = null;
            Log.d(logTag, "Subscriber released.");
            subVideoTrack = null;
            subAudioTrack = null;
            subAudioEnabled = false;
            subVideoEnabled = false;
            Log.d(logTag, "Subscriber Video and Audio tracks released.");
        }

        if (pubRenderer != null) {
            pubRenderer.release();
            pubRenderer = null;
        }
        Log.d(logTag, "Publisher renderer removed.");
        if (subRenderer != null) {
            subRenderer.release();
            subRenderer = null;
        }
        Log.d(logTag, "Subscriber renderer removed.");

        videoSourceList = null;
        Log.d(logTag, "VideoSources removed.");

        Log.d(logTag, "All released.");
    }

    public Activity getMainActivity() {
        return mainActivity;
    }

    public void setMainActivity(Activity mainActivity) {
        this.mainActivity = mainActivity;
    }

    //**********************************************************************************************
    // Internal methods
    //**********************************************************************************************

    /**
     Millicast platform
     */


    /**
     Connect
     */

    /**
     * Connect to Millicast for publishing. Publishing credentials required.
     */
    private void connectPublisher() {
        if (getPublisher() == null) {
            logD(TAG, "[connectPublisher] Failed as Publisher is not available!");
            return;
        }

        if (publisher.isConnected()) {
            logD(TAG, "[connectPublisher] Not doing as we're already connected!");
            return;
        }

        Publisher.Credential creds = publisher.getCredentials();
        creds.apiUrl = getPublishApiUrl(CURRENT);
        creds.streamName = getStreamNamePub(CURRENT);
        creds.token = getPublishingToken(CURRENT);

        publisher.setCredentials(creds);
        publisher.setOptions(pubOptions);

        boolean success;
        String error = "Failed to connect to Millicast!";
        try {
            success = publisher.connect();
        } catch (IllegalStateException e) {
            success = false;
            error += " " + e.getLocalizedMessage();
        }
        if (success) {
            logD(TAG, "[connectPublisher] Connecting to Millicast...");
        } else {
            setPubState(MillicastManager.PublisherState.DISCONNECTED);
            logD(TAG, error);
        }
    }


    /**
     * Capture
     */

    private Media getMedia() {
        if (media == null) {
            media = Media.getInstance(context);
        }
        return media;
    }

    /**
     * Set the audioSource at the audioSourceIndex of the current audioSourceList
     * as the current audioSource, unless currently capturing.
     */
    private void setAudioSource() {
        String logTag = "[Source][Audio][Set] ";

        // Create new audioSource based on index.
        AudioSource audioSourceNew;

        getAudioSourceList();
        if (audioSourceList == null) {
            logD(TAG, logTag + "Failed as no valid audioSource was available!");
            return;
        }

        int size = audioSourceList.size();
        // If the selected index is larger than size, set it to maximum size.
        // This might happen if the list of audioSources changed.
        if (audioSourceIndex >= size) {
            logD(TAG, logTag + "Resetting audioSourceIndex as " +
                    "it is greater than number of audioSources available" + size + "!");
            setAudioSourceIndex(size - 1);
            return;
        }

        audioSourceNew = audioSourceList.get(audioSourceIndex);

        String log;
        if (audioSourceNew != null) {
            log = getAudioSourceStr(audioSourceNew, true);
        } else {
            log = "None";
        }
        logD(TAG, logTag + "New at index:" +
                audioSourceIndex + " is: " + log + ".");

        // Set as new audioSource.
        audioSource = audioSourceNew;
        logD(TAG, logTag + "New at index:" +
                audioSourceIndex + " is: " + log + ".");

    }

    /**
     * Using the selected audioSource, capture audio into a pubAudioTrack.
     */
    private void startCaptureAudio() {
        String logTag = "[Capture][Audio][Start] ";
        if (isAudioCaptured()) {
            logD(TAG, logTag + "AudioSource is already capturing!");
            return;
        }
        if (audioSource == null) {
            logD(TAG, logTag + "Failed as unable to get valid audioSource!");
            return;
        }
        pubAudioTrack = (AudioTrack) audioSource.startCapture();
        setPubAudioEnabled(true);
        logD(TAG, logTag + "OK");
    }

    /**
     * Stop capturing audio, if audio is being captured.
     */
    private void stopCaptureAudio() {
        String logTag = "[Audio][Capture][Stop] ";
        if (!isAudioCaptured()) {
            logD(TAG, logTag + "Not stopping as audio is not captured!");
            return;
        }

        removeAudioSource();
        logD(TAG, logTag + "Audio captured stopped.");
        setPubAudioEnabled(false);
        pubAudioTrack = null;
    }

    /**
     * Set audioSource to null.
     * If audioSource is currently capturing, stop capture first.
     * New audioSource will be created again
     * based on audioSourceIndex.
     */
    private void removeAudioSource() {
        String logTag = "[Source][Audio][Remove] ";

        // Remove audioSource
        if (isAudioCaptured()) {
            audioSource.stopCapture();
            logD(TAG, logTag + "Audio capture stopped.");
        }
        audioSource = null;
        logD(TAG, logTag + "Removed audioSource.");
        logD(TAG, logTag + "Setting new audioSource.");
        setAudioSourceIndex(audioSourceIndex);
        logD(TAG, logTag + "OK.");
        return;
    }

    /**
     * Either return the current videoSource, or the videoSourceSwitched,
     * based on value of getSwitched.
     * If videoSourceSwitched is not available, return videoSource.
     *
     * @param getSwitched If true, return videoSourceSwitched instead.
     */
    private VideoSource getVideoSource(boolean getSwitched) {

        String logTag = "[Source][Video][Get] ";
        if (getSwitched) {
            if (videoSourceSwitched == null) {
                logD(TAG, logTag + "Switched does not exist, will return videoSource.");
            } else {
                logD(TAG, logTag + "Returning videoSourceSwitched.");
                return videoSourceSwitched;
            }
        }

        // Return videoSource.
        if (videoSource == null) {
            logD(TAG, logTag + "None.");
        } else {
            logD(TAG, logTag + getVideoSourceStr(videoSource, true) + ".");
        }
        return videoSource;
    }

    /**
     * Set the videoSource at the videoSourceIndex of the current videoSourceList
     * as the active videoSource.
     * The setting of active videoSource is defined as below:
     * If currently capturing: videoSourceSwitched.
     * Else: videoSource.
     */
    private void setVideoSource() {
        String logTag = "[Source][Video][Set] ";

        // Create new videoSource based on index.
        VideoSource videoSourceNew;

        getVideoSourceList();
        if (videoSourceList == null) {
            logD(TAG, logTag + "Failed as no valid videoSource was available!");
            return;
        }

        int size = videoSourceList.size();
        // If the selected index is larger than size, set it to maximum size.
        // This might happen if the list of videoSources changed.
        if (videoSourceIndex >= size) {
            logD(TAG, logTag + "Resetting videoSourceIndex as " +
                    "it is greater than number of videoSources available" + size + "!");
            setVideoSourceIndex(size - 1, true);
            return;
        }

        videoSourceNew = videoSourceList.get(videoSourceIndex);

        String log;
        if (videoSourceNew != null) {
            log = getVideoSourceStr(videoSourceNew, true);
        } else {
            log = "None";
        }

        // If currently capturing, do not set new videoSource, but videoSourceSwitched instead.
        if (isVideoCaptured()) {
            logD(TAG, logTag + "Setting videoSourceSwitched as currently capturing.");
            // Set our videoSourceSwitched to the new one.
            videoSourceSwitched = videoSourceNew;
            videoSource.switchCamera(getSwitchHdl(), videoSourceSwitched.getName());
            logD(TAG, logTag + "\nCaptured:" +
                    getVideoSourceStr(videoSource, true) +
                    " Cap:" + videoSource.isCapturing() +
                    "\nSwitched:" + getVideoSourceStr(videoSourceSwitched, true) +
                    " Cap:" + videoSourceSwitched.isCapturing() + ".");
            return;
        }

        // Set as new videoSource.
        videoSource = videoSourceNew;
        logD(TAG, logTag + "New at index:" +
                videoSourceIndex + " is: " + log + ".");
    }

    /**
     * Set all forms of videoSource to null.
     * If videoSource is currently capturing, stop capture first.
     * New videoSource and capability will be created again
     * based on videoSourceIndex and capabilityIndex.
     */
    private void removeVideoSource() {
        String logTag = "[Source][Video][Remove] ";

        // Remove all videoSource
        if (isVideoCaptured()) {
            videoSource.stopCapture();
            logD(TAG, logTag + "Video capture stopped.");
        }
        videoSource = null;
        videoSourceSwitched = null;
        logD(TAG, logTag + "Removed all forms of videoSource.");
        logD(TAG, logTag + "Setting new videoSource.");
        setVideoSourceIndex(videoSourceIndex, true);
        logD(TAG, logTag + "OK.");
        return;
    }

    /**
     * Set list of Capabilities supported by the active videoSource.
     * The active videoSource is selected as the first non-null value (or null if none is available)
     * in the following list:
     * videoSourceSwitched, videoSource.
     *
     * @return
     */
    private void setCapabilityList() {
        String logTag = "[Capability][List][Set] ";
        String log = logTag;

        VideoSource vs = null;
        if (videoSourceSwitched != null) {
            vs = videoSourceSwitched;
            log += "From videoSourceSwitched.";
        } else if (videoSource != null) {
            vs = videoSource;
            log += "From videoSource.";
        }
        capabilityList = vs.getCapabilities();
        logD(TAG, log);

        int size = 0;
        if (capabilityList != null) {
            size = capabilityList.size();
        }

        if (capabilityList == null || size < 1) {
            logD(TAG, logTag + "No capability is supported by selected videoSource (" +
                    getVideoSourceStr(getVideoSource(true), true) + ")!");
            return;
        }

        logD(TAG, logTag + "Checking for capabilities...");
        log = logTag + "VS(" + getVideoSourceStr(vs, true) + ") ";
        for (int index = 0; index < size; ++index) {
            VideoCapabilities cap = capabilityList.get(index);
            log += "[" + index + "]:" + getCapabilityStr(cap) + " ";
        }
        logD(TAG, log + ".");
    }

    /**
     * Set the current capability at the capabilityIndex of the current capabilityList,
     * and in the videoSource if available.
     */
    private void setCapability() {
        String logTag = "[Capability][Set] ";
        if (capabilityList == null) {
            logD(TAG, "[Capability] Failed as no valid capability was available!");
            return;
        }
        int size = capabilityList.size();

        // If the selected index is larger than size, set it to maximum size.
        // This can happen when the videoSource has changed.
        if (capabilityIndex >= size) {
            logD(TAG, logTag + "Resetting capabilityIndex to " + (size - 1) + "as it is greater than " +
                    "number of capabilities available (" + size + ")!");
            setCapabilityIndex(size - 1);
            return;
        }
        if (capabilityIndex < 0) {
            logD(TAG, logTag + "Resetting capabilityIndex to 0 as it was negative");
            capabilityIndex = 0;
            return;
        }

        capability = capabilityList.get(capabilityIndex);

        String log;
        if (capability != null) {
            log = getCapabilityStr(capability);
        } else {
            log = "None";
        }
        logD(TAG, logTag + "New at index:" + capabilityIndex +
                " is: " + log + ".");

        log = logTag + "New set on ";
        if (videoSource != null) {
            if (videoSource.isCapturing()) {
                videoSource.changeCaptureFormat(capability);
                log += "capturing";
            } else {
                videoSource.setCapability(capability);
                log += "to be captured";
            }
            log += " videoSource (" + getVideoSourceStr(videoSource, true) + ").";
        }
        logD(TAG, log);
    }

    /**
     * Using the selected videoSource and capability, capture video into a pubVideoTrack.
     */
    private void startCaptureVideo() {
        String logTag = "[Video][Capture][Start] ";
        if (isVideoCaptured()) {
            String log = logTag + "VideoSource is already capturing!";
            if (capState == CaptureState.NOT_CAPTURED) {
                logD(TAG, log + " Continuing as capState is " + capState + ".");
            } else {
                logD(TAG, log + " NOT continuing as capState is " + capState + ".");
                return;
            }
        }

        setCapState(MillicastManager.CaptureState.TRY_CAPTURE);

        if (videoSource == null) {
            setCapState(CaptureState.NOT_CAPTURED);
            logD(TAG, logTag + "Failed as unable to get valid videoSource!");
            return;
        }

        if (capability == null) {
            setCapState(CaptureState.NOT_CAPTURED);
            logD(TAG, logTag + "Failed as unable to get valid Capability for videoSource!");
            return;
        }
        logD(TAG, logTag + "Set " + getVideoSourceStr(videoSource, false) +
                " with Cap: " + getCapabilityStr(capability) + ".");

        videoSource.setEventsHandler(vidSrcEvtHdl);
        VideoTrack videoTrack = (VideoTrack) videoSource.startCapture();

        if (videoSource.getType() == com.millicast.Source.Type.NDI) {
            this.setCapState(MillicastManager.CaptureState.IS_CAPTURED);
        }

        setRenderPubVideoTrack(videoTrack);
    }

    /**
     * Stop capturing video, if video is being captured.
     */
    private void stopCaptureVideo() {
        String logTag = "[Video][Capture][Stop] ";
        if (!isVideoCaptured()) {
            logD(TAG, logTag + "Not stopping as video is not captured!");
            return;
        }

        removeVideoSource();
        logD(TAG, logTag + "Video captured stopped.");
        if (pubRenderer != null) {
            pubRenderer.release();
            pubRenderer = null;
        }
        Log.d(TAG, logTag + "Publisher renderer removed.");
        setPubVideoEnabled(false);
        pubVideoTrack = null;
        setCapState(MillicastManager.CaptureState.NOT_CAPTURED);
    }

    /**
     * Check if either audio or video is captured.
     */
    private boolean isAudioVideoCaptured() {
        if (!isAudioCaptured() && !isVideoCaptured()) {
            logD(TAG, "[isAudioVideoCaptured] No!");
            return false;
        }
        logD(TAG, "[isAudioVideoCaptured] Yes.");
        return true;
    }

    /**
     * Check if audio is captured.
     */
    private boolean isAudioCaptured() {
        if (audioSource == null || !audioSource.isCapturing()) {
            logD(TAG, "[isAudioCaptured] No!");
            return false;
        }
        logD(TAG, "[isAudioCaptured] Yes.");
        return true;
    }

    /**
     * Check if video is captured.
     */
    private boolean isVideoCaptured() {
        if (videoSource == null || !videoSource.isCapturing()) {
            logD(TAG, "[isVideoCaptured] No!");
            return false;
        }
        logD(TAG, "[isVideoCaptured] Yes.");
        return true;
    }

    /**
     * Render
     */

    private void setRenderPubVideoTrack(VideoTrack pubVideoTrack) {
        this.pubVideoTrack = pubVideoTrack;
        if (pubVideoTrack == null) {
            logD(TAG, "[setRenderPubVideoTrack] videoTrack is null, so not rendering it...");
            return;
        }
        setPubVideoEnabled(true);
        logD(TAG, "[setRenderPubVideoTrack] Set videoTrack, trying to render it...");
        renderPubVideo();
    }

    private void renderPubVideo() {
        if (pubVideoTrack == null) {
            logD(TAG, "[renderPubVideo] Unable to render as videoTrack does not exist.");
            return;
        }
        // Set our pub renderer in our pub video track.
        pubRenderer = getPubRenderer();
        pubVideoTrack.setRenderer(pubRenderer);
        logD(TAG, "[renderPubVideo] Set renderer in video track.");
    }

    private void renderSubVideo() {
        if (subVideoTrack == null) {
            logD(TAG, "[renderSubVideo] Unable to render as videoTrack does not exist.");
            return;
        }
        // Set our sub renderer in our sub video track.
        subRenderer = getSubRenderer();
        subVideoTrack.setRenderer(subRenderer);
        logD(TAG, "[renderSubVideo] Set renderer in video track.");
    }


    /**
     * Mute / unmute audio / video.
     */


    /**
     * Select/Switch videoSource, capability and codec.
     */

    /**
     * Gets the index of the next available camera.
     * If at end of camera range, cycle to start of the other end.
     * Returns null if none available.
     *
     * @param ascending If true, cycle in the direction of increasing index,
     *                  otherwise cycle in opposite direction.
     * @return
     */
    private Integer videoSourceIndexNext(boolean ascending) {
        String logTag = "[videoSourceIndexNext] ";
        if (getVideoSourceList() == null) {
            logD(TAG, logTag + "Failed as VideoSources not created!");
            return null;
        }
        int size = videoSourceList.size();
        if (size < 1) {
            logD(TAG, logTag + "Failed as the device does not have a camera!");
            return null;
        }
        Integer next;
        int now = videoSourceIndex;
        if (ascending) {
            if (now >= (size - 1)) {
                next = 0;
                logD(TAG, logTag + next + " (Cycling back to start)");
            } else {
                next = now + 1;
                logD(TAG, logTag + next + " Incrementing index.");
            }
        } else {
            if (now <= 0) {
                next = size - 1;
                logD(TAG, logTag + next + " (Cycling back to end)");
            } else {
                next = now - 1;
                logD(TAG, logTag + next + " Decrementing index.");
            }
        }
        return next;
    }

    /**
     * Gets the index of the next available capability.
     * If at end of capability range, cycle to start of the other end.
     * Returns null if none available.
     *
     * @param ascending If true, cycle in the direction of increasing index,
     *                  otherwise cycle in opposite direction.
     * @return
     */
    private Integer capabilityIndexNext(boolean ascending) {
        String logTag = "[Capability][Index][Next] ";
        int size = 0;

        if (capabilityList != null) {
            size = capabilityList.size();
        }
        if (capabilityList == null || size < 1) {
            logD(TAG, logTag + "Failed as the device does not have a capability!");
            return null;
        }

        Integer next;
        int now = capabilityIndex;
        if (ascending) {
            if (now >= (size - 1)) {
                next = 0;
                logD(TAG, logTag + next + " (Cycling back to start)");
            } else {
                next = now + 1;
                logD(TAG, logTag + next + " Incrementing index.");
            }
        } else {
            if (now <= 0) {
                next = size - 1;
                logD(TAG, logTag + next + " (Cycling back to end)");
            } else {
                next = now - 1;
                logD(TAG, logTag + next + " Decrementing index.");
            }
        }
        return next;
    }

    /**
     * Gets the index of the next available codec.
     * If at end of codec range, cycle to start of the other end.
     * Returns null if none available.
     *
     * @param ascending If true, cycle in the direction of increasing index,
     *                  otherwise cycle in opposite direction.
     * @param isAudio   If true, this is for audio codecs, otherwise for video codecs.
     * @return
     */
    private Integer codecIndexNext(boolean ascending, boolean isAudio) {
        String logTag = "[Codec][Index][Next] ";
        int now;

        if (isAudio) {
            logTag = "[Audio]" + logTag;
            now = audioCodecIndex;
        } else {
            logTag = "[Video]" + logTag;
            now = videoCodecIndex;
        }

        int size;
        ArrayList<String> codecList = getCodecList(isAudio);
        size = codecList.size();
        if (codecList == null || size < 1) {
            logD(TAG, logTag + "Failed as there is no codec!");
            return null;
        }

        Integer next;
        if (ascending) {
            if (now >= (size - 1)) {
                next = 0;
                logD(TAG, logTag + next + " (Cycling back to start)");
            } else {
                next = now + 1;
                logD(TAG, logTag + next + " Incrementing index.");
            }
        } else {
            if (now <= 0) {
                next = size - 1;
                logD(TAG, logTag + next + " (Cycling back to end)");
            } else {
                next = now - 1;
                logD(TAG, logTag + next + " Decrementing index.");
            }
        }
        logD(TAG, logTag + "Next: " + next +
                " Now: " + now);
        return next;
    }

    /**
     Publish
     */

    /**
     * Get the Publisher.
     * If none exist, create one if a Publish View had been set.
     *
     * @return
     */
    private Publisher getPublisher() {
        if (publisher != null) {
            logD(TAG, "[getPublisher] Returning existing Publisher.");
            return publisher;
        }

        logD(TAG, "[getPublisher] Trying to create one...");
        if (pubListener == null) {
            logD(TAG, "[getPublisher] Failed to create Publisher as Listener is not available!");
            return null;
        }
        publisher = Publisher.createPublisher(pubListener);

        logD(TAG, "[getPublisher] Created and returning a new Publisher.");
        return publisher;
    }

    /**
     * Check if we are currently publishing.
     */
    private boolean isPublishing() {
        String logTag = "[Publish][Is] ";
        if (publisher == null || !publisher.isPublishing()) {
            logD(TAG, logTag + "No!");
            return false;
        }
        logD(TAG, logTag + "Yes.");
        return true;
    }

    /**
     * Set the current audio/videoCodec at the audio/videoCodecIndex of the current
     * audio/videoCodecList,
     * and in the publisher as preferred codecs if available and not currently publishing.
     * The current videoCodec will NOT be set if the Publisher is publishing.
     */
    private void setCodecs() {
        String logTag = "[Codec][Set] ";
        String ac = "None";
        String vc = "None";

        getCodecList(true);
        getCodecList(false);

        logD(TAG, logTag + "Selecting a new one based on selected index.");

        if (audioCodecList == null) {
            logD(TAG, logTag + "Failed to set audio codec as none was available!");
        } else {
            int size = audioCodecList.size();

            // If the selected index is larger than size, set it to maximum size.
            if (audioCodecIndex >= size) {
                logD(TAG, logTag + "Resetting audioCodecIndex as it is greater than number of codecs available" + size + "!");
                setCodecIndex(size - 1, true);
            }
            ac = audioCodecList.get(audioCodecIndex);
        }

        if (videoCodecList == null) {
            logD(TAG, logTag + "Failed to set video codec as none was available!");
        } else {
            int size = videoCodecList.size();

            // If the selected index is larger than size, set it to maximum size.
            if (videoCodecIndex >= size) {
                logD(TAG, logTag + "Resetting videoCodecIndex as it is greater than number of codecs available" + size + "!");
                setCodecIndex(size - 1, false);
            }
            vc = videoCodecList.get(videoCodecIndex);
        }


        logD(TAG, logTag + "Selected at index:" + audioCodecIndex + "/" + videoCodecIndex +
                " is: " + ac + "/" + vc + ".");

        String log = logTag + "OK. New ";
        if (publisher != null) {
            if (!publisher.isPublishing()) {
                audioCodec = ac;
                videoCodec = vc;
                pubOptions.audioCodec = Optional.of(ac);
                pubOptions.videoCodec = Optional.of(vc);
                log += "set on Publisher: ";

            } else {
                log += "NOT set, as publishing is ongoing: ";
            }
        } else {
            log += "NOT set, as publisher does not exists: ";
            audioCodec = ac;
            videoCodec = vc;
        }
        log += "Audio:" + audioCodec + " Video:" + videoCodec;
        logD(TAG, log);
    }

    /**
     * Subscribe
     */

    /**
     * Get the Subscriber.
     * If none exist, create one if a Subscribe View had been set.
     *
     * @return
     */
    private Subscriber getSubscriber() {
        if (subscriber == null) {
            logD(TAG, "[getSubscriber] Returning existing Subscriber.");
        }

        logD(TAG, "[getSubscriber] Trying to create one...");
        if (subListener == null) {
            logD(TAG, "[getSubscriber] Failed to create Subscriber as Listener is not available!");
            return null;
        }
        subscriber = Subscriber.createSubscriber(subListener);
        logD(TAG, "[getSubscriber] Created and returning a new Subscriber.");
        return subscriber;
    }

    /**
     * Check if we are currently subscribing.
     */
    private boolean isSubscribing() {
        String logTag = "[Subscribe][Is] ";
        if (subscriber == null || !subscriber.isSubscribed()) {
            logD(TAG, logTag + "No!");
            return false;
        }
        logD(TAG, logTag + "Yes.");
        return true;
    }

    /**
     * Set subscribe audio and video states to not subscribing.
     */
    private void stopSubscribeAudioVideo() {
        subscriber.unsubscribe();
        logD(TAG, "[stopSubscribeAudioVideo] Stopped Subscribing.");

        if (subRenderer != null) {
            subRenderer.release();
            subRenderer = null;
        }
        Log.d(TAG, "Subscriber renderer removed.");
        setSubAudioEnabled(false);
        subAudioTrack = null;
        setSubVideoEnabled(false);
        subVideoTrack = null;
        setSubState(SubscriberState.CONNECTED);
    }

    private void setAudioPlayback() {
        String logTag = "[Playback][Audio][Set] ";

        // Create new audioPlayback based on index.
        AudioPlayback audioPlaybackNew;

        getAudioPlaybackList();
        if (audioPlaybackList == null) {
            logD(TAG, logTag + "Failed as no valid audioPlayback was available!");
            return;
        }

        int size = audioPlaybackList.size();
        // If the selected index is larger than size, set it to maximum size.
        // This might happen if the list of audioPlaybacks changed.
        if (audioPlaybackIndex >= size) {
            logD(TAG, logTag + "Resetting audioPlaybackIndex as " +
                    "it is greater than number of audioPlaybacks available" + size + "!");
            setAudioPlaybackIndex(size - 1);
            return;
        }

        audioPlaybackNew = audioPlaybackList.get(audioPlaybackIndex);

        String log;
        if (audioPlaybackNew != null) {
            log = getAudioSourceStr(audioPlaybackNew, true);
        } else {
            log = "None";
        }

        // Set as new audioPlayback
        audioPlayback = audioPlaybackNew;
        logD(TAG, logTag + "New at index:" +
                audioPlaybackIndex + " is: " + log + ".");
    }

    /**
     * Start the playback of selected audioPlayback if available.
     */
    private void audioPlaybackStart() {
        String logTag = "[Playback][Audio][Start] ";
        if (audioPlayback == null) {
            logD(TAG, logTag + "Creating new audioPlayback...");
            audioPlayback = getAudioPlaybackList().get(audioPlaybackIndex);
            if (audioPlayback == null) {
                logD(TAG, logTag + "Failed! Unable to create audioPlayback.");
            }
        } else {
            logD(TAG, logTag + "Using existing audioPlayback...");
        }
        logD(TAG, logTag + "AudioPlayback is: " + audioPlayback);
        audioPlayback.initPlayback();
        logD(TAG, logTag + "OK. Playback initiated.");
    }


    /**
     * Utilities
     */

    /**
     * Get a String that describes a MCVideoCapabilities.
     */
    private String getCapabilityStr(VideoCapabilities cap) {
        String name;
        if (cap == null) {
            name = "Cap:NULL!";
        } else {
            // Note: FPS given in frames per 1000 seconds (FPKS).
            name = cap.width + "x" + cap.height + " fps:" + cap.fps / 1000;
        }
        return name;
    }

    /**
     * Get a String that describes a MCVideoSource.
     */
    private String getAudioSourceStr(com.millicast.Source audioSource, boolean longForm) {
        String name = "Audio:";
        if (audioSource == null) {
            name += "NULL!";
            return name;
        }

        name = "Audio:" + audioSource.getName();
        if (longForm) {
            name += " (" + audioSource.getType() + ") " + "id:" + audioSource.getId();
        }
        return name;
    }

    /**
     * Get a String that describes a MCVideoSource.
     */
    private String getVideoSourceStr(VideoSource vs, boolean longForm) {
        String name = "Cam:";
        if (vs == null) {
            name += "NULL!";
            return name;
        }

        name = "Cam:" + vs.getName();
        if (longForm) {
            name += " (" + vs.getType() + ") " + "id:" + vs.getId();
        }
        return name;
    }

}
