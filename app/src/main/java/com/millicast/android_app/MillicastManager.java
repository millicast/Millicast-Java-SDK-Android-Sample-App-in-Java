package com.millicast.android_app;

import static android.os.Process.THREAD_PRIORITY_URGENT_AUDIO;
import static android.os.Process.THREAD_PRIORITY_VIDEO;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import com.millicast.Core;
import com.millicast.android_app.compat.CompatBitrateSettings;
import com.millicast.android_app.compat.CompatPublisherCredentials;
import com.millicast.android_app.compat.CompatPublisherOptions;
import com.millicast.android_app.compat.CompatSubscriberCreds;
import com.millicast.android_app.compat.CompatSubscriberOptions;
import com.millicast.android_app.compat.PublisherCompat;
import com.millicast.android_app.compat.SubscriberCompat;
import com.millicast.devices.source.video.CameraVideoSource;
import com.millicast.devices.track.Track;
import com.millicast.publishers.BitrateSettings;
import com.millicast.publishers.state.RecordingState;
import com.millicast.subscribers.ProjectionData;
import com.millicast.utils.LogLevel;
import com.millicast.utils.Logger;
import com.millicast.devices.playback.AudioPlayback;
import com.millicast.devices.source.audio.AudioSource;
import com.millicast.Media;
import com.millicast.VideoRenderer;
import com.millicast.android_app.MCTypes.Source;
import com.millicast.devices.source.video.VideoCapabilities;
import com.millicast.devices.source.video.VideoSource;
import com.millicast.devices.track.AudioTrack;
import com.millicast.devices.track.VideoTrack;
import com.millicast.subscribers.state.LayerData;
import com.voxeet.promise.Promise;

import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.RendererCommon.ScalingType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.millicast.android_app.Constants.ACCOUNT_ID;
import static com.millicast.android_app.Constants.ACTION_MAIN_CAMERA_CLOSE;
import static com.millicast.android_app.Constants.ACTION_MAIN_CAMERA_OPEN;
import static com.millicast.android_app.Constants.SOURCE_ID_PUB;
import static com.millicast.android_app.Constants.SOURCE_ID_PUB_ENABLED;
import static com.millicast.android_app.Constants.TOKEN_PUB;
import static com.millicast.android_app.Constants.URL_PUB;
import static com.millicast.android_app.Constants.STREAM_NAME_PUB;
import static com.millicast.android_app.Constants.STREAM_NAME_SUB;
import static com.millicast.android_app.Constants.TOKEN_SUB;
import static com.millicast.android_app.Constants.URL_SUB;
import static com.millicast.android_app.MCStates.*;
import static com.millicast.android_app.MCTypes.Source.CURRENT;
import static com.millicast.android_app.Utils.getArrayStr;
import static com.millicast.android_app.Utils.logD;
import static com.millicast.devices.type.Type.NDI;
import static com.millicast.devices.type.Type.DEVICE;


import static org.webrtc.RendererCommon.ScalingType.SCALE_ASPECT_FIT;

import kotlin.Unit;

/**
 * The {@link MillicastManager} helps to manage the Millicast SDK and provides
 * a simple set of public APIs for common operations so that UI layer can achieve goals
 * such as publishing / subscribing without knowledge of SDK operations.
 * It takes care of:
 * - Important operations using the SDK, such as managing audio/video sources and renderers,
 * Publisher/Subscriber credentials, options and preferred codecs.
 * - Managing Millicast related states to ensure operations are valid before executing them.
 */
public class MillicastManager {
    public static final String TAG = "MCM";

    private static MillicastManager SINGLE_INSTANCE;
    private Context context;
    private Activity mainActivity;

    // Handler for running processes in the main thread.
    private Handler handlerMain;
    // Handler for running audio processes in a background thread.
    private Handler handlerAudio;
    private HandlerThread threadAudio;
    // Handler for running video processes in a background thread.
    private Handler handlerVideo;
    private HandlerThread threadVideo;

    // States: Millicast
    private CaptureState capState = CaptureState.NOT_CAPTURED;
    private PublisherState pubState = PublisherState.DISCONNECTED;
    private SubscriberState subState = SubscriberState.DISCONNECTED;
    // Set the AudioOnly state to true if not capturing video.
    private boolean audioOnly = false;

    // States: Audio/Video mute.
    private boolean audioEnabledPub = false;
    private boolean videoEnabledPub = false;
    private boolean audioEnabledSub = false;
    private boolean videoEnabledSub = false;
    private boolean ndiOutputVideo = false;
    private boolean ndiOutputAudio = false;
    private boolean recordingEnabledPub = false;

    // States: Ricoh Theta only states.
    private boolean isRicohTheta = false;
    /**
     * Whether the Ricoh Theta camera is locked by our App.
     */
    private boolean isCameraLocked = false;
    /**
     * Set to true if camera should be restarted after switching to another App.
     */
    private boolean toRelockCamera = false;

    /**
     * Millicast platform and credential values.
     * Default values are assign from {@link Constants},
     * and updated with values in device memory, if these exist.
     * These can also be modified from the UI at the Millicast Settings page.
     */
    private String accountId = ACCOUNT_ID;
    private String streamNamePub = STREAM_NAME_PUB;
    private String streamNameSub = STREAM_NAME_SUB;
    private String tokenPub = TOKEN_PUB;
    private String tokenSub = TOKEN_SUB;
    private String sourceIdPub = SOURCE_ID_PUB;
    private boolean sourceIdPubEnabled = SOURCE_ID_PUB_ENABLED;
    private String urlPub = URL_PUB;
    private String urlSub = URL_SUB;

    public static String keyAccountId = "ACCOUNT_ID";
    public static String keyStreamNamePub = "STREAM_NAME_PUB";
    public static String keyStreamNameSub = "STREAM_NAME_SUB";
    public static String keyTokenPub = "TOKEN_PUB";
    public static String keyTokenSub = "TOKEN_SUB";
    public static String keyUrlPub = "URL_PUB";
    public static String keyUrlSub = "URL_SUB";
    public static String keySourceIdPub = "SOURCE_ID_PUB";
    public static String keySourceIdPubEnabled = "SOURCE_ID_PUB_ENABLED";
    public static String keyRicohTheta = "RICOH_THETA";
    public static String keyConVisible = "CON_VISIBLE";

    private ArrayList<AudioSource> audioSourceList;
    private String audioSourceIndexKey = "AUDIO_SOURCE_INDEX";
    private int audioSourceIndexDefault = 0;
    private int audioSourceIndex;
    private AudioSource audioSource;

    private ArrayList<VideoSource> videoSourceList;
    private String videoSourceIndexKey = "VIDEO_SOURCE_INDEX";
    private int videoSourceIndexDefault = 0;
    private int videoSourceIndex;
    private CameraVideoSource videoSource;
    private CameraVideoSource videoSourceSwitched;

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
    private String videoCodec;

    /**
     * The list of {@link AudioPlayback} devices available for us to play subscribed audio.
     * The desired device must be selected and {@link AudioPlayback#initPlayback() initiated}
     * before the {@link AudioTrack} is subscribed.
     */
    private ArrayList<AudioPlayback> audioPlaybackList;
    private String audioPlaybackIndexKey = "AUDIO_PLAYBACK_INDEX";
    private int audioPlaybackIndexDefault = 0;
    private int audioPlaybackIndex;
    private AudioPlayback audioPlayback;

    // SDK Media objects
    private Media media;
    private AudioTrack audioTrackPub;
    private AudioTrack audioTrackSub;
    private VideoTrack videoTrackPub;
    private VideoTrack videoTrackSub;

    // Display
    private VideoRenderer rendererPub;
    private VideoRenderer rendererSub;
    // Whether Publisher's local video view is mirrored.
    private boolean mirroredPub = false;
    private ScalingType scalingPub = SCALE_ASPECT_FIT;
    private ScalingType scalingSub = SCALE_ASPECT_FIT;

    // Publish/Subscribe
    private PublisherCompat publisher;
    private SubscriberCompat subscriber;


    // Options objects for Publish/Subscribe
    private CompatPublisherOptions optionPub;
    private CompatSubscriberOptions optionSub;

    // View objects
    private SwitchHdl switchHdl;
    private VideoSourceEvtHdl videoSourceEvtHdl;
    private PubListener listenerPub;
    private SubListener listenerSub;
    private PublishFragment fragmentPub;
    private SubscribeFragment fragmentSub;
    private SettingsMediaFragment fragmentSetMedia;

    // Multisource objects
    // MID (Media Id) for received audio/video tracks.
    private String midAudio;
    private String midVideo;
    /**
     * Map of SourceId : {@link SourceInfo} of received and currently active sources.
     */
    private HashMap<String, SourceInfo> sourceMap;
    // SourceId being subscribed to for Audio.
    private String sourceIdAudioSub = null;
    // SourceId being subscribed to for Video.
    private String sourceIdVideoSub = null;

    private MillicastManager() {
    }

    /**
     * Initialize the {@link MillicastManager} instance before using it for the first time.
     *
     * @param context The {@link android.content.Context ApplicationContext}.
     */
    public void init(Context context) {
        this.context = context;

        // Initialize handlers.
        handlerMain = new Handler(Looper.getMainLooper());
        threadAudio = new HandlerThread("MC-Background-Audio", THREAD_PRIORITY_URGENT_AUDIO);
        threadAudio.start();
        handlerAudio = new Handler(threadAudio.getLooper());
        threadVideo = new HandlerThread("MC-Background-Video", THREAD_PRIORITY_VIDEO);
        threadVideo.start();
        handlerVideo = new Handler(threadVideo.getLooper());

        Core.INSTANCE.initialize();
        // Set Logger
        Logger.INSTANCE.setLoggerListener((String msg, LogLevel level) -> {
            String logTag = "[SDK][Log][L:" + level + "] ";
            LogLevel minLevel = LogLevel.MC_LOG;
            if (level.ordinal() > minLevel.ordinal()) {
                return;
            }
            logD(TAG, logTag + msg);
        });
        // Prepare Media
        getMedia();

        // Get the media sources in the background to avoid blocking the main thread.
        // Subsequently, get media indices from stored values if present, else from default values.
        // Views will be reloaded if possible after media sources are obtained.
        refreshMediaSourceLists();

        // Set other media values using indices.
        setAudioPlaybackIndex(
                Utils.getSaved(audioPlaybackIndexKey, audioPlaybackIndexDefault, context));
        setCodecIndex(Utils.getSaved(audioCodecIndexKey, audioCodecIndexDefault, context), true);
        setCodecIndex(Utils.getSaved(videoCodecIndexKey, videoCodecIndexDefault, context), false);

        // Create Publisher and Subscriber Options
        optionPub = new CompatPublisherOptions();
        optionPub.setStereo(true);
        optionSub = new CompatSubscriberOptions();
        sourceMap = new HashMap<>();

        // Set credentials from stored values if present, else from Constants file values.
        setAccountId(Utils.getSaved(keyAccountId, ACCOUNT_ID, context), false);
        setStreamNamePub(Utils.getSaved(keyStreamNamePub, STREAM_NAME_PUB, context), false);
        setStreamNameSub(Utils.getSaved(keyStreamNameSub, STREAM_NAME_SUB, context), false);
        setSourceIdPubEnabled(Utils.getSaved(keySourceIdPubEnabled, SOURCE_ID_PUB_ENABLED, context), false);
        setTokenPub(Utils.getSaved(keyTokenPub, TOKEN_PUB, context), false);
        setTokenSub(Utils.getSaved(keyTokenSub, TOKEN_SUB, context), false);
        setSourceIdPub(Utils.getSaved(keySourceIdPub, SOURCE_ID_PUB, context), false);
        setUrlPub(Utils.getSaved(keyUrlPub, URL_PUB, context), false);
        setUrlSub(Utils.getSaved(keyUrlSub, URL_SUB, context), false);
        setRicohTheta(Utils.getSaved(keyRicohTheta, false, context), false);

        logD(TAG, "[McMan][Init] OK.");
    }

    //**********************************************************************************************
    // APIs
    //**********************************************************************************************

    //**********************************************************************************************
    // Millicast platform
    //**********************************************************************************************

    /**
     * Method to get the MillicastManager Singleton instance.
     * Must call {@link #init(Context)} before using the instance for the first time.
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

    public String getAccountId(Source source) {
        return Utils.getProperty(source, accountId, ACCOUNT_ID, keyAccountId, context);
    }

    /**
     * Set the {@link #accountId Millicast accountId} to be used when next connecting to subscribe.
     * Value will only be set if not currently connected to subscribe.
     * Specify whether to save the value into device memory as well.
     *
     * @param newValue
     * @param save
     * @return True if value successfully set, false otherwise.
     */
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

    /**
     * Set the {@link #streamNamePub publish stream name} to be used when next publishing.
     * Value will only be set if not currently publishing.
     * Specify whether to save the value into device memory as well.
     *
     * @param newValue
     * @param save
     * @return True if value successfully set, false otherwise.
     */
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

    /**
     * Set the {@link #streamNameSub subscribe stream name} to be used when next subscribing.
     * Value will only be set if not currently subscribing.
     * Specify whether to save the value into device memory as well.
     *
     * @param newValue
     * @param save
     * @return True if value successfully set, false otherwise.
     */
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

    /**
     * Get an Optional of the {@link #sourceIdPub publisher sourceId} of the given
     * {@link Source information source (file, memory, or applied)}.
     * If the sourceId is empty or if {@link #sourceIdPubEnabled} is false,
     * then an empty Optional will be returned.
     *
     * @param source
     * @return
     */
    public Optional getOptSourceIdPub(Source source) {
        String srdId = getSourceIdPub(source);
        if (srdId.isEmpty() || !isSourceIdPubEnabled(source)) {
            return Optional.empty();
        }
        return Optional.of(srdId);
    }

    /**
     * Get {@link #sourceIdPub publisher sourceId} from the desired
     * {@link Source information source (file, memory, or applied)}.
     *
     * @param source
     * @return
     */
    public String getSourceIdPub(Source source) {
        return Utils.getProperty(source, sourceIdPub, SOURCE_ID_PUB, keySourceIdPub, context);
    }

    /**
     * Set and apply the given value as {@link #sourceIdPub publisher sourceId}.
     * Specify whether to save the value into device memory as well.
     *
     * @param newValue
     * @param save
     * @return True if value successfully set, false otherwise.
     */
    public boolean setSourceIdPub(String newValue, boolean save) {
        String logTag = "[Source][Id][Pub][Set] ";
        if (pubState != PublisherState.DISCONNECTED) {
            logD(TAG, logTag + "Failed! Cannot set when pubState is " + pubState + ".");
            return false;
        }
        if (save) {
            Utils.saveValue(keySourceIdPub, sourceIdPub, newValue, logTag, context);
        }
        sourceIdPub = newValue;
        return true;
    }

    /**
     * Check if the {@link #sourceIdPub publisher sourceId} will be used when publishing.
     *
     * @param source
     * @return
     */
    public boolean isSourceIdPubEnabled(Source source) {
        return Utils.getProperty(source, sourceIdPubEnabled, SOURCE_ID_PUB_ENABLED, keySourceIdPubEnabled, context);
    }

    /**
     * Set the {@link #sourceIdPub publisher sourceId} to be used (or not) when next publishing.
     * Value will only be set if not currently publishing.
     * Specify whether to save the value into device memory as well.
     *
     * @param newValue
     * @param save
     * @return True if value successfully set, false otherwise.
     */
    public boolean setSourceIdPubEnabled(boolean newValue, boolean save) {
        String logTag = "[Src][Id][Pub][Enabled][Set] ";
        if (pubState != PublisherState.DISCONNECTED) {
            logD(TAG, logTag + "Failed! Cannot set when pubState is " + pubState + ".");
            return false;
        }
        if (save) {
            Utils.saveValue(keySourceIdPubEnabled, sourceIdPubEnabled, newValue, logTag, context);
        }
        sourceIdPubEnabled = newValue;
        return true;
    }

    public String getTokenPub(Source source) {
        return Utils.getProperty(source, tokenPub, TOKEN_PUB, keyTokenPub, context);
    }

    /**
     * Set the {@link #tokenPub publish token} to be used when next publishing.
     * Value will only be set if not currently publishing.
     * Specify whether to save the value into device memory as well.
     *
     * @param newValue
     * @param save
     * @return True if value successfully set, false otherwise.
     */
    public boolean setTokenPub(String newValue, boolean save) {
        String logTag = "[Token][Pub][Set] ";
        if (pubState != PublisherState.DISCONNECTED) {
            logD(TAG, logTag + "Failed! Cannot set when pubState is " + pubState + ".");
            return false;
        }
        if (save) {
            Utils.saveValue(keyTokenPub, tokenPub, newValue, logTag, context);
        }
        tokenPub = newValue;
        return true;
    }

    public String getTokenSub(Source source) {
        return Utils.getProperty(source, tokenSub, TOKEN_SUB, keyTokenSub, context);
    }

    /**
     * Set the {@link #tokenSub subscribe token} to be used when next subscribing.
     * Value will only be set if not currently subscribing.
     * Specify whether to save the value into device memory as well.
     *
     * @param newValue
     * @param save
     * @return True if value successfully set, false otherwise.
     */
    public boolean setTokenSub(String newValue, boolean save) {
        String logTag = "[Token][Sub][Set] ";
        if (subState != SubscriberState.DISCONNECTED) {
            logD(TAG, logTag + "Failed! Cannot set when subState is " + subState + ".");
            return false;
        }
        if (save) {
            Utils.saveValue(keyTokenSub, tokenSub, newValue, logTag, context);
        }
        tokenSub = newValue;
        return true;
    }

    public String getUrlPub(Source source) {
        return Utils.getProperty(source, urlPub, URL_PUB, keyUrlPub, context);
    }

    /**
     * Set the {@link #urlPub publish API URL} to be used when next publishing.
     * Value will only be set if not currently publishing.
     * Specify whether to save the value into device memory as well.
     *
     * @param newValue
     * @param save
     * @return True if value successfully set, false otherwise.
     */
    public boolean setUrlPub(String newValue, boolean save) {
        String logTag = "[Url][Pub][Set] ";
        if (pubState != PublisherState.DISCONNECTED) {
            logD(TAG, logTag + "Failed! Cannot set when pubState is " + pubState + ".");
            return false;
        }
        if (save) {
            Utils.saveValue(keyUrlPub, urlPub, newValue, logTag, context);
        }
        urlPub = newValue;
        return true;
    }

    public String getUrlSub(Source source) {
        return Utils.getProperty(source, urlSub, URL_SUB, keyUrlSub, context);
    }

    /**
     * Set the {@link #urlSub subscribe API URL} to be used when next subscribing.
     * Value will only be set if not currently subscribing.
     * Specify whether to save the value into device memory as well.
     *
     * @param newValue
     * @param save
     * @return True if value successfully set, false otherwise.
     */
    public boolean setUrlSub(String newValue, boolean save) {
        String logTag = "[Url][Sub][Set] ";
        if (subState != SubscriberState.DISCONNECTED) {
            logD(TAG, logTag + "Failed! Cannot set when subState is " + subState + ".");
            return false;
        }
        if (save) {
            Utils.saveValue(keyUrlSub, urlSub, newValue, logTag, context);
        }
        urlSub = newValue;
        return true;
    }

    public boolean isRicohTheta(Source source) {
        return Utils.getProperty(source, isRicohTheta, false, keyRicohTheta, context);
    }

    /**
     * Set the {@link #isRicohTheta if current device is Ricoh Theta value} to be used when next capturing.
     * Value will only be set if not currently capturing.
     * Specify whether to save the value into device memory as well.
     *
     * @param newValue
     * @param save
     * @return True if value successfully set, false otherwise.
     */
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

    //**********************************************************************************************
    // Query/Select videoSource, capability.
    //**********************************************************************************************

    /**
     * Refresh the currently available lists of audio and video sources if currently not capturing and no other refresh is on going.
     * Media indices will be read from stored values if present, else from default values.
     * Affected views (i.e., Publish and Media Settings) will be reloaded if possible after media sources are obtained.
     * This can be useful when the lists changed, for e.g. when an NDI source is added or removed.
     * If an action is required after the source list is refreshed, provide it as a Runnable.
     *
     * @return True if able to proceed to refresh media sources, false otherwise.
     */
    public synchronized boolean refreshMediaSourceLists() {
        String logTag = "[Source][List][Refresh] ";
        if (CaptureState.NOT_CAPTURED != capState) {
            logD(TAG, logTag + "Failed! CapState is " + capState + ".");
            return false;
        }
        setCapState(CaptureState.REFRESH_SOURCE);

        Runnable callbackReloadView = new Runnable() {
            @Override
            public void run() {
                loadViewPub();
                loadViewSetMedia();
            }
        };
        refreshAudioSourceList(callbackReloadView);
        refreshVideoSourceList(callbackReloadView);
        logD(TAG, logTag + "OK. Refreshing...");
        return true;
    }

    /**
     * Get the current list of AudioSources available.
     * If none is available, return an empty ArrayList.
     *
     * @return
     */
    public ArrayList<AudioSource> getAudioSourceList() {
        String logTag = "[Source][Audio][List] ";
        if (audioSourceList == null) {
            ArrayList<AudioSource> emptyList = new ArrayList<>();
            logD(TAG, logTag + "No audioSource is available! Using empty list.");
            return emptyList;
        }
        logD(TAG, logTag + "Using existing audioSources.");
        return audioSourceList;
    }

    public int getAudioSourceIndex() {
        String log = "[Source][Index][Audio] " + audioSourceIndex + ".";
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

        String logTag = "[Source][Index][Set][Audio]:" + audioSourceIndex + "->" + newValue + " ";

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
     * Get the current list of VideoSources available.
     * If none is available, return an empty ArrayList.
     *
     * @return
     */
    public ArrayList<VideoSource> getVideoSourceList() {
        String logTag = "[Source][List][Video] ";
        if (videoSourceList == null) {
            ArrayList<VideoSource> emptyList = new ArrayList<>();
            logD(TAG, logTag + "No videoSource is available! Using empty list.");
            return emptyList;
        }
        logD(TAG, logTag + "Using existing videoSources.");
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
     * @return Null if {@link #videoSourceIndex} could be set, else an error message might be returned.
     */
    public String setVideoSourceIndex(int newValue, boolean setCapIndex) {

        String logTag = "[Source][Video][Index][Set]:" + videoSourceIndex + "->" + newValue + " ";

        // If capturing, do not allow changing to and from NDI.
        String error = getErrorSwitchNdi(newValue, videoSource, videoSourceList);
        if (error != null) {
            logD(TAG, logTag + error);
            return error;
        }

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
        return null;
    }

    /**
     * Get the current list of VideoCapabilities available.
     * If none is available, return an empty ArrayList.
     *
     * @return
     */
    public ArrayList<VideoCapabilities> getCapabilityList() {
        String logTag = "[Source][Capability][List] ";
        if (capabilityList == null) {
            ArrayList<VideoCapabilities> emptyList = new ArrayList<>();
            logD(TAG, logTag + "No capability is available! Using empty list.");
            return emptyList;
        }
        logD(TAG, logTag + "Using existing capability list.");
        return capabilityList;
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

    public String getMidAudio() {
        return midAudio;
    }

    public void setMidAudio(String midAudio) {
        this.midAudio = midAudio;
    }

    public String getMidVideo() {
        return midVideo;
    }

    public void setMidVideo(String midVideo) {
        this.midVideo = midVideo;
    }

    //**********************************************************************************************
    // Switch Media
    //**********************************************************************************************

    /**
     * Select the next available audioSource on device.
     * This will set the audioSource to be used when capturing starts.
     * If at end of range of audioSources, cycle to start of the other end.
     * If capturing, switching audioSource will not be allowed.
     *
     * @param ascending If true, "next" audioSource is defined in the direction of increasing index,
     *                  otherwise it is in the opposite direction.
     * @return Null if {@link #audioSource} could be set, else an error message might be returned.
     */
    public String switchAudioSource(boolean ascending) {

        String logTag = "[Source][Audio][Switch] ";
        String error;
        Integer newValue = null;

        // If videoSource is already capturing, switch to only non-NDI videoSource.
        if (isAudioCaptured()) {
            error = "Failed! Unable to switch audioSource when capturing.";
            logD(TAG, logTag + error);
            return error;
        }

        if (audioSourceList == null) {
            error = "Failed! No audio source exists.";
            logD(TAG, logTag + error);
            return error;
        }

        newValue = audioSourceIndexNext(ascending, audioSourceIndex, audioSourceList.size());
        if (newValue == null) {
            error = "FAILED! Unable to get next audioSource!";
            logD(TAG, logTag + error);
            return error;
        }

        // Set new audioSource
        logD(TAG, logTag + "Setting audioSource index to:"
                + newValue + ".");
        setAudioSourceIndex(newValue);

        logD(TAG, logTag + "OK.");
        return null;
    }

    /**
     * Stop capturing on current videoSource and capture using the next available videoSource on device.
     * If not currently capturing, this will set the videoSource to be used when capturing starts.
     * If at end of range of videoSources, cycle to start of the other end.
     * If capturing, only the device's cameras can be switched to and fro.
     * If the next videoSource is NDI, then it will be skipped and the next device camera switched to.
     *
     * @param ascending If true, "next" videoSource is defined in the direction of increasing index,
     *                  otherwise it is in the opposite direction.
     * @return Null if {@link #videoSource} could be set, else an error message might be returned.
     */
    public String switchVideoSource(boolean ascending) {

        String logTag = "[Source][Video][Switch] ";

        // If capturing with NDI, do not allow changing.
        // Check using current videoSourceIndex.
        String error = getErrorSwitchNdi(videoSourceIndex, videoSource, videoSourceList);
        if (error != null) {
            logD(TAG, logTag + error);
            return error;
        }

        Integer newValue = null;
        // If videoSource is already capturing, switch to only non-NDI videoSource.
        if (isVideoCaptured()) {
            newValue = videoSourceIndexNextNonNdi(ascending, videoSourceIndex, videoSourceList);
        } else {
            int size = 0;
            if (videoSourceList != null) {
                size = getVideoSourceList().size();
            }
            newValue = videoSourceIndexNext(ascending, videoSourceIndex, size);
        }
        if (newValue == null) {
            error = "FAILED! Unable to get next camera!";
            logD(TAG, logTag + error);
            return error;
        }

        // Set new videoSource
        logD(TAG, logTag + "Setting videoSource index to:"
                + newValue + " and updating Capability for new VideoSource.");
        setVideoSourceIndex(newValue, true);

        logD(TAG, logTag + " OK.");
        return null;
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
            // This will be the case for NDI source.
            capability = null;
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

    //**********************************************************************************************
    // Capture
    //**********************************************************************************************

    /**
     * Checks if we are in AudioOnly mode, i.e. video is not captured.
     *
     * @return
     */
    public boolean isAudioOnly() {
        return audioOnly;
    }

    /**
     * Sets AudioOnly mode as desired. In AudioOnly mode, video is not captured.
     *
     * @param audioOnly True to turn on AudioOnly mode, false otherwise.
     */
    public void setAudioOnly(boolean audioOnly) {
        this.audioOnly = audioOnly;
    }

    /**
     * Toggles the recording feature for the publisher
     * If publisher has already started broadcasting, the currently published stream
     * will be recorded. If there is not stream yet, a flag will be set to start recording after publishing.
     */
    public void toggleRecordingEnabled() {
        String logClass = "[Pub][Rec] ";
        if (publisher == null) {
            logD(TAG, logClass + "Currently not publishing. Setting flag to " + !recordingEnabledPub);
            recordingEnabledPub = !recordingEnabledPub;
            fragmentPub.setUI();
            return;
        }
        publisher.isPublishing().then(publishing -> {
            if (publishing) {
                if (publisher.isRecording()) {
                    logD(TAG, logClass + "Currently recording the published stream. Stopping...");
                    publisher.stopRecording().then(success -> {
                        logD(TAG, logClass + "Stopped");
                    }).error(e -> {
                        logD(TAG, logClass + "Failed! NOT stopped");
                        logD(TAG, logClass + e.getLocalizedMessage());
                    });
                } else {
                    logD(TAG, logClass + "Currently not recording the published stream. Starting...");
                    publisher.startRecording().then(success -> {
                        logD(TAG, logClass + "Started");
                    }).error(e -> {
                        logD(TAG, logClass + "Failed! NOT started");
                        logD(TAG, logClass + e.getLocalizedMessage());

                    });
                }
            } else {
                logD(TAG, logClass + "Currently not publishing. Setting flag to " + !recordingEnabledPub);
                recordingEnabledPub = !recordingEnabledPub;
                fragmentPub.setUI();
            }
        }).error(e -> {
            logD(TAG, "[Rec][Error] " + e.getLocalizedMessage());
        });
    }

    public void setRecordingEnabled(boolean enabled) {
        this.recordingEnabledPub = enabled;
    }

    /**
     * Start capturing both audio and video (based on selected videoSource) if currently not capturing and no media sources refresh is on going.
     *
     * @return True if able to proceed to start audio video capture, false otherwise.
     */
    public synchronized boolean startAudioVideoCapture() {
        String logTag = "[Capture][Audio][Video][Start] ";

        if (CaptureState.NOT_CAPTURED != capState) {
            logD(TAG, logTag + "Failed! CapState is " + capState + ".");
            return false;
        }

        logD(TAG, logTag + "Starting Capture...");
        setCapState(CaptureState.TRY_CAPTURE);

        startCaptureVideo();
        startCaptureAudio();
        logD(TAG, logTag + "OK.");
        return true;
    }

    /**
     * Stop capturing both audio and video.
     */
    public void stopAudioVideoCapture() {
        logD(TAG, "[Capture][Audio][Video][Stop] Stopping Capture...");
        stopCaptureVideo();
        stopCaptureAudio();
    }

    public AudioTrack getAudioTrackPub() {
        return audioTrackPub;
    }

    public VideoTrack getVideoTrackPub() {
        return videoTrackPub;
    }

    public AudioTrack getAudioTrackSub() {
        return audioTrackSub;
    }

    /**
     * Set the received videoTrack into MillicastManager.
     */
    public void setAudioTrackSub(AudioTrack audioTrackSub) {
        this.audioTrackSub = audioTrackSub;
        enableNdiOutput(isNdiOutputEnabled(true), true, null);
    }

    public VideoTrack getVideoTrackSub() {
        return videoTrackSub;
    }

    //**********************************************************************************************
    // Mute / unmute audio / video.
    //**********************************************************************************************

    public boolean isAudioEnabledPub() {
        return audioEnabledPub;
    }

    public void setAudioEnabledPub(boolean audioEnabledPub) {
        this.audioEnabledPub = audioEnabledPub;
    }

    public boolean isVideoEnabledPub() {
        return videoEnabledPub;
    }

    public void setVideoEnabledPub(boolean videoEnabledPub) {
        this.videoEnabledPub = videoEnabledPub;
    }

    public boolean isAudioEnabledSub() {
        return audioEnabledSub;
    }

    public void setAudioEnabledSub(boolean audioEnabledSub) {
        this.audioEnabledSub = audioEnabledSub;
    }

    public boolean isVideoEnabledSub() {
        return videoEnabledSub;
    }

    public void setVideoEnabledSub(boolean videoEnabledSub) {
        this.videoEnabledSub = videoEnabledSub;
    }

    //**********************************************************************************************
    // Render - Audio
    //**********************************************************************************************

    public ArrayList<AudioPlayback> getAudioPlaybackList() {
        if (audioPlaybackList == null) {
            audioPlaybackList = new ArrayList<>(getMedia().getAudioPlayback());
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

    /**
     * If not currently subscribed, this will set the selected {@link #audioPlaybackIndex}
     * to the specified value and save to device memory.
     * A new {@link #audioPlayback} will be set using this value.
     * If currently subscribed, no changes to the current {@link #audioPlayback} will be made
     * as changes can only be made when there is no subscription on going.
     *
     * @param newValue
     * @return
     */
    public boolean setAudioPlaybackIndex(int newValue) {
        String logTag = "[Playback][Audio][Index][Set] ";

        // If currently subscribing, do not set new audioSourceIndex.
        if (isSubscribing()) {
            logD(TAG, logTag + "NOT setting to " + newValue + " as currently subscribing.");
            logD(TAG, logTag + "AudioPlayback:" + "");
            getAudioPlaybackStr(audioPlayback, true);
            return false;
        }

        Utils.saveValue(audioPlaybackIndexKey, audioPlaybackIndex, newValue, logTag, context);
        audioPlaybackIndex = newValue;
        setAudioPlayback();
        return true;
    }

    //**********************************************************************************************
    // Render - Video
    //**********************************************************************************************

    /**
     * Gets the {@link VideoRenderer} for the Publisher.
     * Creates one if none currently exists.
     * By default this renderer will be
     * {@link org.webrtc.SurfaceViewRenderer#setScalingType(ScalingType) scaled} to
     * {@link ScalingType#SCALE_ASPECT_FIT SCALE_ASPECT_FIT}.
     * The scaling effect is local only and is not transmitted to the Subscriber(s).
     *
     * @return {@link VideoRenderer}
     */
    public VideoRenderer getRendererPub() {
        String logTag = "[Video][Render][er][Pub] ";
        // If it's not available, create it with application context.
        if (rendererPub == null) {
            rendererPub = new VideoRenderer(context, null);
            rendererPub.setScalingType(scalingPub);

            logD(TAG, logTag + "Created renderer with application context.");
        } else {
            logD(TAG, logTag + "Using existing renderer.");
        }
        return rendererPub;
    }

    /**
     * Gets the {@link VideoRenderer} for the Subscriber.
     * Creates one if none currently exists.
     * By default this renderer will be
     * {@link org.webrtc.SurfaceViewRenderer#setScalingType(ScalingType) scaled} to
     * {@link ScalingType#SCALE_ASPECT_FIT SCALE_ASPECT_FIT}.
     * The scaling effect is local only and is not transmitted to the Publisher.
     *
     * @return {@link VideoRenderer}
     */
    public VideoRenderer getRendererSub() {
        String logTag = "[Video][Render][er][Sub] ";
        // If it's not available, create it with application context.
        if (rendererSub == null) {
            rendererSub = new VideoRenderer(context, null);
            rendererSub.setScalingType(scalingSub);
            logD(TAG, logTag + "Created renderer with application context.");
        } else {
            logD(TAG, logTag + "Using existing renderer.");
        }
        return rendererSub;
    }

    /**
     * Renders the subscribed video.
     * Executes on UI thread.
     *
     * @param videoTrack
     */
    public void renderVideoSub(VideoTrack videoTrack) {
        getMainActivity().runOnUiThread(() -> {
            setRenderVideoTrackSub(videoTrack);
        });
    }

    /**
     * Checks if the Publisher's local video view is mirrored.
     *
     * @return True if mirrored, false otherwise.
     */
    public boolean isMirroredPub() {
        String logTag = "[Mirror][Pub][?] ";
        logD(TAG, logTag + mirroredPub + ".");
        return mirroredPub;
    }

    /**
     * Sets the mirroring of the Publisher's local video view to the specified value.
     * The mirroring effect is local only and is not transmitted to the Subscriber(s).
     *
     * @param toMirror If true, view is mirrored, else not.
     */
    private void setMirror(boolean toMirror) {
        String logTag = "[Mirror][Set][Pub] ";

        if (getRendererPub() == null) {
            logD(TAG, logTag + "Failed! The videoRenderer is not available.");
            return;
        }

        if (toMirror == mirroredPub) {
            logD(TAG, logTag + "Not setting mirroring to " + toMirror + " as current mirror state is already " + mirroredPub + ".");
            return;
        }

        rendererPub.setMirror(toMirror);
        mirroredPub = toMirror;
        logD(TAG, logTag + "OK. Updated mirroredPub to " + toMirror + ".");
    }

    /**
     * Switches the mirroring of the Publisher's local video view from mirrored to not mirrored,
     * and vice-versa.
     */
    public void switchMirror() {
        String logTag = "[Mirror][Switch][Pub] ";
        logD(TAG, logTag + "Trying to set mirroring for videoRenderer to: " + !mirroredPub + ".");
        setMirror(!mirroredPub);
    }

    /**
     * Apply again the current {@link ScalingType} of the specified videoRenderer.
     * This could be useful when the videoView's aspect ratio has changed.
     *
     * @param forPub If true, will be performed for the Publisher's videoRenderer,
     *               else for the Subscriber's videoRenderer.
     * @return
     */
    public ScalingType applyScaling(boolean forPub) {
        String logTag = "[Scale][Apply]";
        VideoRenderer renderer = rendererSub;
        ScalingType scaling = scalingSub;
        if (forPub) {
            logTag += "[Pub] ";
            renderer = rendererPub;
            scaling = scalingPub;
        } else {
            logTag += "[Sub] ";
        }

        if (renderer == null) {
            logD(TAG, logTag + "Failed! The videoRenderer is not available.");
            return null;
        }

        renderer.setScalingType(scaling);
        logD(TAG, logTag + "OK: " + scaling + ".");
        return scaling;
    }

    /**
     * Set the {@link ScalingType} of the specified videoRenderer to the next type.
     * If at end of range of ScalingType, cycle to start of the other end.
     * The types of scaling in use are:
     * {@link ScalingType#SCALE_ASPECT_FIT} - video frame is scaled to fit the size of the view by
     * maintaining the aspect ratio (black borders may be displayed).
     * {@link ScalingType#SCALE_ASPECT_FILL} - video frame is scaled to fill the size of the view by
     * maintaining the aspect ratio. Some portion of the video frame may be
     * clipped.
     * {@link ScalingType#SCALE_ASPECT_BALANCED} - Compromise between FIT and FILL. Video frame will fill as much as
     * possible of the view while maintaining aspect ratio, under the constraint that at least
     * ~0.5625 of the frame content will be shown.
     *
     * @param ascending If true, "next" ScalingType is defined in the direction of increasing index,
     *                  otherwise it is in the opposite direction.
     * @param forPub    If true, will be performed for the Publisher's videoRenderer,
     *                  else for the Subscriber's videoRenderer.
     * @return
     */
    public ScalingType switchScaling(boolean ascending, boolean forPub) {
        String logTag = "[Scale][Switch]";
        VideoRenderer renderer = rendererSub;
        ScalingType scaling = scalingSub;
        if (forPub) {
            logTag += "[Pub] ";
            renderer = rendererPub;
            scaling = scalingPub;
        } else {
            logTag += "[Sub] ";
        }

        if (renderer == null) {
            logD(TAG, logTag + "Failed! The videoRenderer is not available.");
            return null;
        }

        int now = scaling.ordinal();
        int size = ScalingType.values().length;
        int next = Utils.indexNext(size, now, ascending, logTag);
        ScalingType nextScaling = ScalingType.values()[next];
        logD(TAG, logTag + "Next for videoRenderer: " + nextScaling + ".");

        renderer.setScalingType(nextScaling);
        if (forPub) {
            scalingPub = nextScaling;
        } else {
            scalingSub = nextScaling;
        }
        logD(TAG, logTag + "OK.");
        return nextScaling;
    }

    //**********************************************************************************************
    // Render - Audio / Video
    //**********************************************************************************************

    /**
     * <p>
     * Warning: NDI output for {@link AudioTrack} is not functional, see warning for
     * {@link #enableNdiOutput} for more details.
     * </p>
     * Checks if subscribed media (audio or video as specified) has
     * enabled (or requested if not yet capturing) NDI output.
     *
     * @param forAudio If true, this is for audio NDI output, otherwise for video NDI output.
     * @return
     */
    public boolean isNdiOutputEnabled(boolean forAudio) {
        String logTag = "[Sub][NDI]";
        Track track = audioTrackSub;
        boolean ndiEnabled = ndiOutputAudio;

        if (forAudio) {
            logTag += "[Audio][?] ";
        } else {
            logTag += "[Video][?] ";
            track = videoTrackSub;
            ndiEnabled = ndiOutputVideo;
        }

        if (track == null) {
            logD(TAG, logTag + "Flag: " + ndiEnabled + ", Track: Does not exist.");
            return ndiEnabled;
        }
        return ndiEnabled;
    }

    /**
     * <p>
     * Warning: As of SDK 1.1.1 and until documented otherwise in the SDK,
     * {@link VideoTrack#enableNdiOutput enabling} and {@link VideoTrack#disableNdiOutput() disabling}
     * NDI output for {@link AudioTrack} is still not functional, i.e. will not have any impact on
     * NDI output when called. To enable audio NDI output for subscribed stream, please use
     * "ndi output" from the {@link #audioPlaybackList list of Audio Playback Devices}.
     * Please see {@link #setAudioPlaybackIndex(int)} and {@link #setAudioPlayback()}.
     * </p>
     * Enable/disable subscribed media (audio or video as specified) to be available as NDI output.
     * If media track is not currently available, this will set a flag to enable/disable
     * NDI output for the media when it is available.
     *
     * @param enable     If true, will enable NDI output, else disable.
     * @param forAudio   If true, this is for audio NDI output, otherwise for video NDI output.
     * @param sourceName If NDI output enabled, this will be the NDI source name.
     */
    public void enableNdiOutput(boolean enable, boolean forAudio, String sourceName) {
        /*String logTag = "[Sub][NDI]";
        String log = "";
        Track track = audioTrackSub;

        if (forAudio) {
            logTag += "[Audio] ";
        } else {
            logTag += "[Video] ";
            track = videoTrackSub;
        }

        if (track == null) {
            if (forAudio) {
                ndiOutputAudio = enable;
            } else {
                ndiOutputVideo = enable;
            }
            logD(TAG, logTag + "Only set flag to " + enable + " Media track does not exist.");
            return;
        }

        // Set a default source name if none was given.
        if (sourceName == null || sourceName.isEmpty()) {
            if (forAudio) {
                sourceName = context.getResources().getString(R.string.ndi_name_audio);
            } else {
                sourceName = context.getResources().getString(R.string.ndi_name_video);
            }
        }
        boolean success = false;
        if (enable) {
            try {
                track.enableNdiOutput(sourceName);
                log = "Enabled. Source name: " + sourceName + ".";
                success = true;
            } catch (IllegalStateException e) {
                log = "Failed. Error: " + e.getLocalizedMessage();
            }
        } else {
            try {
                track.disableNdiOutput();
                log = "Disabled.";
                success = true;
            } catch (IllegalStateException e) {
                log = "Failed. Error: " + e.getLocalizedMessage();
            }
        }
        // Set flags if successful.
        if (success) {
            if (forAudio) {
                ndiOutputAudio = enable;
            } else {
                ndiOutputVideo = enable;
            }
        }
        logD(TAG, logTag + log);
        */
    }

    //**********************************************************************************************
    // Publish - Options
    //**********************************************************************************************

    /**
     * Set the specified {@link BitrateSettings} to use for publishing.
     * Setting this will only affect the next publish,
     * and not the current publish if one is ongoing.
     * Values set are only guidelines and will be affected by other factors such as the bandwidth available.
     *
     * @param bitrate
     * @param type
     */
    public void setBitrate(int bitrate, MCTypes.Bitrate type) {
        String logTag = "[Bitrate][Set]";
        if (bitrate < 0) {
            logD(TAG, logTag + " Failed! Bitrate value (" + bitrate + ") must be positive.");
        }
        if (getPublisher() == null) {
            logD(TAG, logTag + " Failed! Publisher not available.");
            return;
        }

        CompatBitrateSettings settings = optionPub.getBitrateSettings();
        switch (type) {
            case MIN:
                logTag += "[Min] ";
                settings.setMinBitrateKbps(bitrate);
                break;
            case MAX:
                logTag += "[Max] ";
                settings.setMaxBitrateKbps(bitrate);
                break;
        }
        logD(TAG, logTag + bitrate + "kbps.");
    }

    /**
     * Get or generate (if null) the current list of Video Codec supported.
     *
     * @param forAudio
     * @return
     */
    public ArrayList<String> getCodecList(boolean forAudio) {
        String logTag = "[Codec][List] ";
        String log;
        ArrayList<String> codecList;
        if (forAudio) {
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

    public int getAudioCodecIndex() {
        return audioCodecIndex;
    }

    public int getVideoCodecIndex() {
        return videoCodecIndex;
    }

    /**
     * Set the selected codec index to the specified value and save to device memory.
     * A new videoCodec will be set using this value, unless the Publisher is publishing.
     * This videoCodec will be set into the Publisher, if it is available and not publishing.
     *
     * @param newValue The new value to be set.
     * @param forAudio
     * @return true if new index set, false otherwise.
     */
    public boolean setCodecIndex(int newValue, boolean forAudio) {
        String logTag = "[Codec][Index][Set] ";
        // Set new value into SharePreferences.
        AtomicInteger oldValue = new AtomicInteger(videoCodecIndex);
        AtomicReference<String> key = new AtomicReference<>(videoCodecIndexKey);
        if (forAudio) {
            logTag = "[Audio]" + logTag;
        } else {
            logTag = "[Video]" + logTag;
        }
        String finalLogTag = logTag;
        if (pubState == PublisherState.PUBLISHING) {
            logD(TAG, finalLogTag + "Failed! Unable to set new codec while publishing!");
            return false;
        }
        if (forAudio) {
            oldValue.set(audioCodecIndex);
            key.set(audioCodecIndexKey);
            audioCodecIndex = newValue;
        } else {
            videoCodecIndex = newValue;
        }
        Utils.setSaved(key.get(), newValue, context);
        logD(TAG, finalLogTag + "Now: " + newValue +
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
     * @param forAudio
     */
    public void switchCodec(boolean ascending, boolean forAudio) {
        String logTag = "[Codec][Switch] ";
        if (forAudio) {
            logTag = "[Audio]" + logTag;
        } else {
            logTag = "[Video]" + logTag;
        }

        Integer newValue = codecIndexNext(ascending, forAudio);
        if (newValue == null) {
            logD(TAG, logTag + "FAILED! Unable to get next codec!");
            return;
        }

        logD(TAG, logTag + "Setting codec index to:"
                + newValue + ".");
        setCodecIndex(newValue, forAudio);

        logD(TAG, logTag + "OK.");
    }

    //**********************************************************************************************
    // Connect
    //**********************************************************************************************

    /**
     * Connect to Millicast for publishing.
     * Publishing credentials required.
     * Credentials are specified in Constants file, but can also be modified on Settings UI.
     */
    public void connectPub() {
        String logTag = "[Pub][Con] ";

        // Create Publisher if not present
        if (getPublisher() == null) {
            logD(TAG, logTag + "Failed! Publisher not available.");
            return;
        }

        publisher.isConnected()
                .then(connected -> {
                    if (connected) {
                        logD(TAG, logTag + "Not doing as we're already connected!");
                    } else {
                        setPubState(PublisherState.CONNECTING);
                        logD(TAG, logTag + "Trying...");
                        // Connect Publisher.
                        connectPubMc().then(success -> {
                            if (success) {
                                logD(TAG, logTag + "OK.");
                            } else {
                                setPubState(PublisherState.DISCONNECTED);
                                logD(TAG, logTag + "Failed! Connection requirements not fulfilled. Check inputs (e.g. credentials) and any Millicast error message.");
                            }
                        }).error(e -> {
                            logD(TAG, logTag + e.getLocalizedMessage());
                        });
                    }
                }).error(e -> {
                    logD(TAG, logTag + e.getLocalizedMessage());
                });


    }

    /**
     * Connect to Millicast for subscribing.
     * Subscribing credentials required.
     * Credentials are specified in Constants file, but can also be modified on Settings UI.
     */
    public void connectSub() {
        String logTag = "[Sub][Con] ";

        // Create Subscriber if not present
        if (getSubscriber() == null) {
            logD(TAG, logTag + "Failed! Subscriber is not available.");
            return;
        }

        subscriber.isConnected().then(connected -> {
            if (connected) {
                logD(TAG, logTag + "Not doing as we're already connected!");
                return;
            }

            setSubState(SubscriberState.CONNECTING);

            // Connect Subscriber.
            logD(TAG, logTag + "Trying...");
            connectSubMc();


        }).error(e -> {
            logD(TAG, logTag + e.getLocalizedMessage());
        });
    }


    //**********************************************************************************************
    // Publish
    //**********************************************************************************************

    /**
     * Publish audio and video tracks that are already captured.
     * Must first be connected to Millicast.
     */

    public void startPub() {
        String logTag = "[Pub][Start] ";
        if (publisher == null) {
            logD(TAG, logTag + "Failed! Publisher is not available!");
            return;
        }

        publisher.isConnected()
                .then(connected -> {
                    if (!connected) {
                        logD(TAG, logTag + "Not publishing as we are not connected!");
                        return;
                    }
                    publisher.isPublishing()
                            .then(publishing -> {
                                if (publishing) {
                                    logD(TAG, logTag + "Not publishing as we are already publishing!");
                                    return;
                                }
                                if (!isAudioVideoCaptured()) {
                                    logD(TAG, logTag + "Failed! Both audio & video are not captured.");
                                    return;
                                }
                                logD(TAG, logTag + "Trying...");
                                startPubMc();
                            })
                            .error(e -> {
                                logD(TAG, logTag + e.getLocalizedMessage());
                            });

                }).error(e -> {
                    logD(TAG, logTag + e.getLocalizedMessage());
                });
    }

    /**
     * Stop publishing and disconnect from Millicast.
     * Does not affect capturing.
     */
    public void stopPub() {
        String logTag = "[Pub][Stop] ";
        publisher.isPublishing().then(publishing -> {
            if (!publishing) {
                logD(TAG, logTag + "Not doing as we're not publishing!");
                return;
            }
            // Stop publishing
            logD(TAG, logTag + "Trying to stop publish...");
            stopPubMc();
        }).error(e -> {
            logD(TAG, logTag + e.getLocalizedMessage());
        });
    }


    public PublishFragment getFragmentPub() {
        return fragmentPub;
    }

    /**
     * Keep a reference to the Publish View in the {@link MillicastManager}.
     *
     * @param view
     */
    public void setViewPub(PublishFragment view) {
        String logTag = "[Pub][View][set] ";
        fragmentPub = view;
        logD(TAG, logTag + "OK.");
    }

    /**
     * Keep a reference to the SettingsMediaFragment View in the {@link MillicastManager}.
     *
     * @param view
     */
    public void setViewSetMedia(SettingsMediaFragment view) {
        String logTag = "[SetMedia][View][set] ";
        fragmentSetMedia = view;
        logD(TAG, logTag + "OK.");
    }
    //**********************************************************************************************
    // Subscribe
    //**********************************************************************************************

    /**
     * Subscribe to stream from Millicast.
     * Must first be connected to Millicast.
     */
    public void startSub() {
        String logTag = "[Sub][Start] ";
        if (subscriber == null) {
            logD(TAG, logTag + "Failed! Subscriber is not available!");
            return;
        }

        subscriber.isConnected().then(connected -> {
            if (!connected) {
                logD(TAG, logTag + "Failed! Subscriber not connected!" + " subState is " + subState + ".");
                return;
            }
            if (isSubscribing()) {
                logD(TAG, logTag + "Not subscribing as we are already subscribing!");
                return;
            }

            // Subscribe to Millicast
            logD(TAG, logTag + "Trying...");
            startSubMc().then(success -> {
                logD(TAG, logTag + "OK. Starting subscribe to Millicast.");
                // Enable Subscriber stats
                logD(TAG, logTag + "Enabling stats...");
                enableStatsSub(true);
                logD(TAG, logTag + "OK.");
            }).error(e -> {
                logD(TAG, logTag + "Failed! Check current states and any Millicast error message.");
                logD(TAG, logTag + "Failed!" + e.getLocalizedMessage());
            });
        }).error(e -> {
            logD(TAG, logTag + "Failed!" + e.getLocalizedMessage());
        });
    }

    /**
     * Stop subscribing and disconnect from Millicast.
     * Does not affect capturing.
     */
    public void stopSub() {
        String logTag = "[Sub][Stop] ";
        if (!isSubscribing()) {
            logD(TAG, logTag + "Not doing as we're not subscribing!");
            return;
        }

        // Stop subscribing
        logD(TAG, logTag + "Trying to stop subscribe...");
        stopSubMc();

        enableStatsSub(false);
        logD(TAG, logTag + "Stats stopped.");

        // Remove Subscriber.
        subscriber = null;
        logD(TAG, logTag + "Subscriber removed.");

        sourceMap = null;
        sourceIdAudioSub = null;
        sourceIdVideoSub = null;
        logD(TAG, logTag + "Subscribe sourceMap and sourceIds removed.");

        // Remove subscribed media
        removeSubscribeMedia();
        logD(TAG, logTag + "Subscribe media removed.");

        logD(TAG, logTag + "OK.");
    }

    public SubscribeFragment getFragmentSub() {
        return fragmentSub;
    }

    /**
     * Keep a reference to the Subscribe View in the {@link MillicastManager}.
     *
     * @param view
     */
    public void setViewSub(SubscribeFragment view) {
        String logTag = "[Sub][View][set] ";
        fragmentSub = view;
        logD(TAG, logTag + "OK.");
    }

    //**********************************************************************************************
    // Subscribe - Sources and Layers
    //**********************************************************************************************

    /**
     * Get the current list of active sources.
     * If there are no active sources, list returned will have size 0.
     *
     * @return
     */
    public ArrayList<String> getSourceList() {
        String logTag = "[Source][Id][List] ";
        ArrayList<String> list;
        if (sourceMap == null || sourceMap.size() == 0) {
            list = new ArrayList<>();
            logD(TAG, logTag + "Empty list as there are no active sources.");
            return list;
        }

        list = new ArrayList<>(sourceMap.keySet());
        logD(TAG, logTag + getArrayStr(list, ", ", null));
        return list;
    }

    public String getSourceIdAudioSub() {
        return sourceIdAudioSub;
    }

    public String getSourceIdVideoSub() {
        return sourceIdVideoSub;
    }

    /**
     * Get the {@link SourceInfo} of the currently projected audio or video source.
     *
     * @param isAudio
     * @return
     */
    public SourceInfo getSourceProjected(boolean isAudio) {
        String logTag = "[Source][Id]";
        String sourceId = this.sourceIdAudioSub;
        if (!isAudio) {
            logTag += "[Video] ";
            sourceId = this.sourceIdVideoSub;
        } else {
            logTag += "[Audio] ";
        }

        if (sourceId == null) {
            logD(TAG, logTag + "None. There is no projected source.");
            return null;
        }
        // Get the SourceInfo of the project source.
        SourceInfo source = sourceMap.get(sourceId);
        if (source == null) {
            logD(TAG, logTag + "None. The projected source cannot be found!");
            return null;
        }
        logD(TAG, logTag + source + ".");
        return source;
    }

    /**
     * Request a received source of a given sourceId to be projected onto our audio or video track.
     * To project the default/main source (the latest one published without a sourceId),
     * provide the sourceId as an empty String ("").
     *
     * @param sourceId
     * @param isAudio
     * @return
     */
    public boolean projectSource(String sourceId, boolean isAudio) {
        String logTag = "[Source][Id][Project]:" + sourceId + " ";
        Promise<Boolean> rval;
        if (isAudio) {
            logTag += "A ";
        } else {
            logTag += "V ";
        }
        String log;

        if (sourceId == "") {
            log = "Note: This is the default/main source with no sourceId";
            logD(TAG, logTag + log);
        }

        // Get MediaInfo of the source we want.
        if (sourceMap == null) {
            log = "Failed! sourceMap is not available.";
            logD(TAG, logTag + log);
            return false;
        }
        SourceInfo sourceInfo = sourceMap.get(sourceId);
        if (sourceInfo == null) {
            log = "Failed! sourceId is not available! " +
                    "sourceMap: " + sourceMap + ".";
            logD(TAG, logTag + log);
            return false;
        }

        log = "Source: " + sourceInfo + ".";
        logD(TAG, logTag + log);

        // Get the mid of our track on which to receive the source.
        String mid = getMidAudio();
        if (!isAudio) {
            mid = getMidVideo();
        }

        // Generate the ProjectionData required from the MediaInfo.
        ArrayList<ProjectionData> projectionData =
                sourceInfo.getProjectionData(mid, isAudio);
        if (projectionData == null) {
            log = "Failed! ProjectData is not available for sourceInfo! " +
                    "sourceMap: " + sourceMap + ".";
            logD(TAG, logTag + log);
            return false;
        }

        // Send the request to Millicast and return the result of the request.
        log = "Trying to project source onto mid:" + mid + " ...";
        logD(TAG, logTag + log);
        String finalLogTag = logTag;
        subscriber.project(sourceId, projectionData).then(success -> {
            String log2 = "[Sub][Project]";
            // If successful, track the new sourceId.
            log2 += "OK. Projected new ";
            if (isAudio) {
                log2 += "Audio source.\n";
                sourceIdAudioSub = sourceId;
                logD(TAG, finalLogTag + log2);
            } else {
                log2 += "Video source.\n";
                sourceIdVideoSub = sourceId;
                // Set view to display the Layers of the projected video source.
                log2 += "Setting new Layers of the project source in the view...";
                logD(TAG, finalLogTag + log2);
                // Reset the Layers UI in the view.
                loadViewSubLayer();
            }
        }).error(e -> {
            logD(TAG, finalLogTag + "Failed! ");
            logD(TAG, finalLogTag + e.getLocalizedMessage());
        });
        return true;
    }

    /**
     * Adds a new source in the form of a {@link SourceInfo} to our list of active source list.
     *
     * @param sourceId
     * @param sourceInfo
     */
    public void addSource(String sourceId, SourceInfo sourceInfo) {
        String logTag = "[Source][Id][Add]:" + sourceId + " ";
        String log;
        if (sourceId == null || sourceInfo == null) {
            log = "Failed! sourceMap: " + sourceMap + ", sourceInfo: " + sourceInfo + ".";
            logD(TAG, logTag + log);
            return;
        }
        if (sourceMap == null) {
            sourceMap = new HashMap<>();
            log = "Created new sourceMap: " + sourceMap + ", sourceInfo: " + sourceInfo + ".";
            logD(TAG, logTag + log);
        }

        sourceMap.put(sourceId, sourceInfo);
        log = "OK. Added source (" + sourceInfo + ") to our list of active sources: " + sourceMap + ".";
        logD(TAG, logTag + log);
    }

    /**
     * Remove a source from our {@link #sourceMap}.
     * If this is a currently projected source:
     * - Clear the affected {@link #sourceIdAudioSub} and {@link #sourceIdVideoSub}.
     * - Project one of the remaining source in {@link #sourceMap} (if any).
     *
     * @param sourceId
     * @return The {@link SourceInfo} removed or null if the source could not be found.
     */
    public SourceInfo removeSource(String sourceId) {
        String logTag = "[Source][Id][Remove]:" + sourceId + " ";
        String log;
        if (sourceId == null || sourceMap == null) {
            log = "Failed! sourceMap: " + sourceMap + ".";
            logD(TAG, logTag + log);
            return null;
        }
        SourceInfo sourceInfo = sourceMap.remove(sourceId);
        if (sourceInfo == null) {
            log = "Failed! sourceId is not available! sourceMap: " + sourceMap + ".";
            logD(TAG, logTag + log);
            return null;
        }

        String newSid;
        if (sourceId.equals(sourceIdAudioSub)) {
            sourceIdAudioSub = null;
            log = "Removed current sourceIdAudio.";
            if (sourceMap.size() == 0) {
                log += " No remaining audio source to project.";
                logD(TAG, logTag + log);
            } else {
                newSid = (String) (sourceMap.keySet().toArray())[0];
                log += " Trying to project another audio source: " + newSid + "...";
                logD(TAG, logTag + log);
                projectSource(newSid, true);
            }
        }

        if (sourceId.equals(sourceIdVideoSub)) {
            sourceIdVideoSub = null;
            log = "Removed current sourceIdVideo.";
            if (sourceMap.size() == 0) {
                log += " No remaining video source to project. Reloading layer view...";
                logD(TAG, logTag + log);
                // Reload layer view.
                loadViewSubLayer();
            } else {
                newSid = (String) (sourceMap.keySet().toArray())[0];
                log += " Trying to project another video source: " + newSid + "...";
                logD(TAG, logTag + log);
                projectSource(newSid, false);
            }
        }

        log = "OK. Source (" + sourceInfo + ") removed from our list of active sources: " + sourceMap + ".";
        logD(TAG, logTag + log);
        return sourceInfo;
    }

    /**
     * Load the Source UI in the view when the list of active sources change.
     *
     * @param isAudio Set to null if both audio and video sources are to be loaded.
     * @param changed True if the source List has changed, false otherwise.
     */
    public void loadViewSource(Boolean isAudio, boolean changed) {
        String logTag = "[View][Source][Load] ";
        if (fragmentSub == null) {
            logD(TAG, logTag + "Failed! The SubscribeFragment does not exist.");
            return;
        }

        ArrayList<String> spinnerList = getSourceList();
        Utils.GetSelectedIndex lambdaAudio = new Utils.GetSelectedIndex() {
            public int getSelectedIndex(ArrayList list) {
                String selection = MillicastManager.this.getSourceIdAudioSub();
                if (list == null || selection == null) {
                    return -1;
                }
                return list.indexOf(selection);
            }
        };
        Utils.GetSelectedIndex lambdaVideo = new Utils.GetSelectedIndex() {
            public int getSelectedIndex(ArrayList list) {
                String selection = MillicastManager.this.getSourceIdVideoSub();
                if (list == null || selection == null) {
                    return -1;
                }
                return list.indexOf(selection);
            }
        };

        getMainActivity().runOnUiThread(() -> {
            if (fragmentSub == null) {
                return;
            }
            if (isAudio == null) {
                fragmentSub.loadSourceSpinner(spinnerList, lambdaAudio, true, changed);
                fragmentSub.loadSourceSpinner(spinnerList, lambdaVideo, false, changed);
                return;
            }
            if (isAudio) {
                fragmentSub.loadSourceSpinner(spinnerList, lambdaAudio, isAudio, changed);
            } else {
                fragmentSub.loadSourceSpinner(spinnerList, lambdaVideo, isAudio, changed);

            }
        });
        logD(TAG, logTag + "OK.");
    }

    /**
     * Set the layerActiveList into the {@link SourceInfo} of the current projected video source.
     * If a video source not currently projected, this would fail.
     *
     * @param layerActiveList
     * @return True if a new layerActiveList was set, and false otherwise.
     */
    public boolean setLayerActiveList(LayerData[] layerActiveList) {
        String logTag = "[Layer][Active][List][Set] ";
        SourceInfo source = getSourceProjected(false);
        if (source == null) {
            logD(TAG, logTag + "Failed! No video source exists.");
            return false;
        }
        if (source.setLayerActiveList(layerActiveList)) {
            logD(TAG, logTag + "OK.");
            // Reset the Layers UI in the view.
            loadViewSubLayer();
            return true;
        } else {
            logD(TAG, logTag + "OK. This list already exists.");
            return false;
        }
    }

    /**
     * Get the layerId (from {@link SourceInfo#getLayerId}) list of the active layers of the
     * currently projected video source.
     * Return a list of size 0 if there is no projected video source.
     * Otherwise, return a list that at least has an empty String ("") as the layer automatically selected by Millicast.
     *
     * @return
     */
    public ArrayList<String> getLayerActiveIdList() {
        String logTag = "[Layer][Active][Id][List] ";

        // Get the SourceInfo of the project source.
        SourceInfo source = getSourceProjected(false);
        ArrayList<String> list;
        if (source == null) {
            logD(TAG, logTag + "None. The projected video source cannot be found!");
            list = new ArrayList<>();
        } else {
            list = source.getLayerActiveIdList();
        }
        return list;
    }

    /**
     * Get the {@link SourceInfo#layerActiveId} of the selected Layer of the currently projected video source.
     * An empty String ("") indicates that the layer is automatically selected by Millicast.
     *
     * @return The layerId, or null if there is no projected video source.
     */
    public String getLayerActiveId() {
        String logTag = "[Layer][Active][Id][List] ";

        // Get the SourceInfo of the projected source.
        SourceInfo source = getSourceProjected(false);
        if (source == null) {
            logD(TAG, logTag + "None. The projected video source cannot be found!");
            return null;
        }
        String id = source.getLayerActiveId();
        logD(TAG, logTag + id + ".");
        return id;
    }

    /**
     * Get the Optional {@link LayerData} of the selected Layer of the currently projected video source.
     * An empty Optional indicates that the layer is automatically selected by Millicast.
     *
     * @return The Optional or null if there is no active source.
     */
    public Optional<LayerData> getLayerData() {
        String logTag = "[Layer][Active] ";

        // Get the SourceInfo of the project source.
        SourceInfo source = getSourceProjected(false);
        if (source == null) {
            logD(TAG, logTag + "None. The projected video source cannot be found!");
            return null;
        }
        Optional<LayerData> layerDataOpt = source.getLayerData();
        logD(TAG, logTag + "OK.");
        return layerDataOpt;
    }

    /**
     * Select a layer of the current video source based on its {@link SourceInfo#layerActiveId}.
     * Will not perform selection if the layer has already been selected.
     *
     * @param layerId
     * @return True if the Layer of the layerId was (or already) selected, false otherwise.
     */
    public Promise<Boolean> selectLayer(String layerId) {
        return new Promise<>((resolve, reject) -> {
            String logTag = "[Layer][Select]:" + layerId + " ";
            String log;

            if (layerId == null) {
                logD(TAG, logTag + "Failed! LayerId invalid.");
                reject.call(new Throwable("LayerId invalid."));

            }

            SourceInfo source = getSourceProjected(false);
            if (source == null) {
                logD(TAG, logTag + "Failed! No video source set.");
                reject.call(new Throwable("No video source set."));
            }

            // Check if already selected.
            String currentLayerId = getLayerActiveId();
            if (layerId.equals(currentLayerId)) {
                log = "OK. Already selected, not selecting again.";
                logD(TAG, logTag + log);
                reject.call(new Throwable("Already selected, not selecting again."));
            }

            // Get the layerData that we should select.
            LayerData layerData = source.getLayerData(layerId).get();
            if (layerData == null) {
                logD(TAG, logTag + "Failed! Unable to get Layer.");
                reject.call(new Throwable("Unable to get Layer."));
            }

            // Perform layer selection and return the result.
            subscriber.select(layerData)
                    .then(success -> {
                        logD(TAG, logTag + "OK.");
                        if (source.setLayerActiveId(layerId)) {
                            logD(TAG, logTag + "Set new layer active layerId.");
                            resolve.call(true);
                        } else {
                            logD(TAG, logTag + "Error! Failed to set new layer active layerId!");
                            reject.call(new Throwable("Failed to set new layer active layerId!"));
                        }
                    }).error(e -> {
                        logD(TAG, logTag + "Failed! Could not select layer!");
                        logD(TAG, logTag + e.getLocalizedMessage());
                        reject.call(e);
                    });
        });

    }

    /**
     * Load the UI on the main thread in the Publish view if the view exists.
     */
    public void loadViewPub() {
        handlerMain.post(() -> {
            String logTag = "[View][Load][Pub] ";
            if (fragmentPub == null) {
                logD(TAG, logTag + "Failed! The PublishFragment does not exist.");
                return;
            }
            fragmentPub.setUI();
            logD(TAG, logTag + "OK.");
        });
    }

    /**
     * Load the UI on the main thread in the SettingsMediaFragment view if the view exists.
     */
    public void loadViewSetMedia() {
        handlerMain.post(() -> {
            String logTag = "[View][Load][SetMedia] ";
            if (fragmentSetMedia == null) {
                logD(TAG, logTag + "Failed! The SettingsMediaFragment does not exist.");
                return;
            }
            fragmentSetMedia.setUI();
            logD(TAG, logTag + "OK.");
        });
    }

    /**
     * Load the Layer UI in the Subscribe view when the list of active remote sources change.
     */
    public void loadViewSubLayer() {
        String logTag = "[View][Load][Sub][Layer] ";
        if (fragmentSub == null) {
            logD(TAG, logTag + "Failed! The SubscribeFragment does not exist.");
            return;
        }
        ArrayList<String> layerList = getLayerActiveIdList();
        Utils.GetSelectedIndex lambda = new Utils.GetSelectedIndex() {
            public int getSelectedIndex(ArrayList list) {
                String selection = MillicastManager.this.getLayerActiveId();
                if (list == null || selection == null) {
                    return -1;
                }
                return list.indexOf(selection);
            }
        };
        getMainActivity().runOnUiThread(() -> {
            if (fragmentSub != null) {
                fragmentSub.loadLayerSpinner(layerList, lambda);
            }
        });
        logD(TAG, logTag + "OK.");
    }

    /**
     * Add an empty Audio or Video Track, that can be used to project a Source
     * received from Millicast.
     *
     * @param isAudio
     * @return
     */

    //TODO clean up
//    public boolean addMediaTrack(boolean isAudio) {
//        TrackType kind = TrackType.Audio;
//        if (!isAudio) {
//            kind = TrackType.Video;
//        }
//        return this.subscriber.addRemoteTrack(kind);
//    }
//
//    /**
//     * Get the Mid of a Subscribed track by the given trackId.
//     * The trackId can be obtained by calling {@link Track#getName()}.
//     *
//     * @param trackId
//     * @return
//     */
//    public String getTrackMidSub(String trackId) {
//        return this.subscriber.getMid(trackId);
//    }


    //**********************************************************************************************
    // Utilities
    //**********************************************************************************************
    public Context getContext() {
        return context;
    }

    /**
     * Get the name of the currently selected videoSource.
     *
     * @return
     */
    public String getAudioSourceName() {
        String name = "[" + audioSourceIndex + "] ";
        String log = "[Source][Audio][Name] Using ";
        // Get audioSource name of selected index.
        name += getAudioSourceStr(getAudioSource(), true);
        log += "Selected AS: " + name;
        logD(TAG, log);
        return name;
    }

    /**
     * Get the name of the currently selected videoSource.
     *
     * @return
     */
    public String getVideoSourceName() {
        String name = "[" + videoSourceIndex + "] ";
        String log = "[Source][Video][Name] Using ";
        // Get videoSource name of selected index.
        name += getVideoSourceStr(getVideoSource(true), true);
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
     * Get the name of the currently selected ScalingType.
     *
     * @param forPub If true, will be performed for the Publisher's videoRenderer,
     *               else for the Subscriber's videoRenderer.
     * @return
     */
    public String getScalingName(boolean forPub) {
        String name = "";
        String logTag = "[Scale][Name]";

        ScalingType scaling = scalingSub;
        if (forPub) {
            logTag += "[Pub] ";
            scaling = scalingPub;
        } else {
            logTag += "[Sub] ";
        }

        switch (scaling) {
            case SCALE_ASPECT_FIT:
                name = "FIT";
                break;
            case SCALE_ASPECT_FILL:
                name = "FILL";
                break;
            case SCALE_ASPECT_BALANCED:
                name = "BAL";
                break;
        }

        logD(TAG, logTag + name + ".");
        return name;
    }

    /**
     * Get the name of the currently selected video Codec.
     *
     * @param forAudio
     * @return
     */
    public String getCodecName(boolean forAudio) {
        int index = videoCodecIndex;
        String codec = videoCodec;
        if (forAudio) {
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
     * Stats are only collected after Publisher is connected to Millicast.
     *
     * @param enable
     */
    public void enableStatsPub(boolean enable) {
        if (publisher != null) {
            String logTag = "[Pub][Stats][Enable] ";
            publisher.getStats(enable);
            logD(TAG, logTag + enable);

        }
    }

    /**
     * Enable or disable Subscriber's WebRTC stats.
     * Stats are only collected after Subscriber is connected to Millicast.
     *
     * @param enable
     */
    public void enableStatsSub(boolean enable) {
        if (subscriber != null) {
            String logTag = "[Sub][Stats][Enable] ";
            subscriber.getStats(enable);
            logD(TAG, logTag + enable);

        }
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
            CameraVideoSource videoSource = MillicastManager.getSingleInstance().getVideoSource(false);
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
        String logTag = "[Release][Views] ";
        videoSourceEvtHdl = null;
        logD(TAG, logTag + "VideoSource EventHandler removed.");
        fragmentPub = null;
        logD(TAG, logTag + "Publisher view removed.");
        fragmentSub = null;
        logD(TAG, logTag + "Subscriber view removed.");
        fragmentSetMedia = null;
        logD(TAG, logTag + "Media Settings view removed.");
    }

    public void release() {
        String logTag = TAG + "[Release]";
        media = null;
        Log.d(logTag, "Media removed.");

        if (publisher != null) {
            listenerPub = null;
            publisher.release();
            publisher = null;
            Log.d(logTag, "Publisher released.");
            videoTrackPub = null;
            audioTrackPub = null;
            audioEnabledPub = false;
            videoEnabledPub = false;
            Log.d(logTag, "Publisher Video and Audio tracks released.");
        }
        if (subscriber != null) {
            listenerSub = null;
            subscriber.release();
            subscriber = null;
            Log.d(logTag, "Subscriber released.");
            videoTrackSub = null;
            audioTrackSub = null;
            audioEnabledSub = false;
            videoEnabledSub = false;
            sourceMap = null;
            Log.d(logTag, "Subscriber Video and Audio tracks released.");
        }

        if (rendererPub != null) {
            rendererPub.release();
            rendererPub = null;
        }
        Log.d(logTag, "Publisher renderer removed.");
        if (rendererSub != null) {
            rendererSub.release();
            rendererSub = null;
        }
        Log.d(logTag, "Subscriber renderer removed.");

        videoSourceList = null;
        Log.d(logTag, "VideoSources removed.");

        threadAudio.quitSafely();
        threadVideo.quitSafely();
        Log.d(logTag, "Threads Loopers quitted.");

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

    //**********************************************************************************************
    // Millicast platform
    //**********************************************************************************************

    //**********************************************************************************************
    // Query/Select videoSource, capability.
    //**********************************************************************************************

    private Media getMedia() {
        if (media == null) {
            media = Media.INSTANCE;
        }
        return media;
    }

    /**
     * Refresh the current list of AudioSources available in a background thread.
     * If none is available, return an empty ArrayList.
     * Audio source index will be read from stored value if present, else from default value.
     * If an action is required after the source list is refreshed, provide it as a Runnable.
     *
     * @param callback Runnable for action required after source list is refreshed.
     *                 Null if none is required.
     * @return
     */
    private void refreshAudioSourceList(Runnable callback) {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                CaptureState.refreshAudio = true;
                String logTag = "[Source][List][Refresh][Audio] ";

                logD(TAG, logTag + "Getting new audioSources.");
                // Get new audioSources.
                audioSourceList = (ArrayList<AudioSource>) getMedia().getAudioSources();

                // Print out list of audioSources.
                logD(TAG, logTag + "Checking for audioSources...");
                if (audioSourceList == null || audioSourceList.size() < 1) {
                    logD(TAG, logTag + "No audioSource is available!");
                } else {
                    // Print out new sourceList.
                    String log = logTag;
                    for (int index = 0; index < audioSourceList.size(); ++index) {
                        AudioSource as = audioSourceList.get(index);
                        log += "[" + index + "]:" + getAudioSourceStr(as, true) + " ";
                    }
                    logD(TAG, log + ".");

                    // Set currently selected source.
                    logD(TAG, "Setting currently selected source.");
                    setAudioSourceIndex(
                            Utils.getSaved(audioSourceIndexKey, audioSourceIndexDefault, context));
                }

                // Run callback, if any.
                if (callback == null) {
                    return;
                }

                logD(TAG, "Running provided callback...");
                callback.run();

                // Reset CapState if possible.
                CaptureState.refreshAudio = false;
                if (CaptureState.refreshVideo) {
                    logD(TAG, logTag + "Unable to reset CapState as video sources are still being refreshed.");
                } else {
                    setCapState(CaptureState.NOT_CAPTURED);
                    logD(TAG, logTag + "CapState reset to NOT_CAPTURED.");
                }
                logD(TAG, "OK.");
            }
        };
        handlerAudio.post(task);
    }

    /**
     * Return the current audioSource.
     */
    private AudioSource getAudioSource() {

        String logTag = "[Source][Audio][Get] ";
        // Return audioSource.
        if (audioSource == null) {
            logD(TAG, logTag + "None.");
        } else {
            logD(TAG, logTag + getAudioSourceStr(audioSource, true) + ".");
        }
        return audioSource;
    }

    /**
     * Set the audioSource at the audioSourceIndex of the current audioSourceList
     * as the current audioSource.
     */
    private void setAudioSource() {
        String logTag = "[Source][Audio][Set] ";

        // Create new audioSource based on index.
        AudioSource audioSourceNew;

        if (audioSourceList == null) {
            logD(TAG, logTag + "Failed! No audio source exists.");
            return;
        }

        int size = audioSourceList.size();
        if (size < 1) {
            logD(TAG, logTag + "Failed as list size was " + size + "!");
            return;
        }

        // If the selected index is larger than size, set it to maximum size.
        // This might happen if the list of audioSources changed.
        if (audioSourceIndex >= size) {
            logD(TAG, logTag + "Resetting index to " + (size - 1) + "as it is greater than " +
                    "size of list (" + size + ")!");
            setAudioSourceIndex(size - 1);
            return;
        }
        if (audioSourceIndex < 0) {
            logD(TAG, logTag + "Resetting index to 0 as it was negative!");
            setAudioSourceIndex(0);
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
     * Refresh the current list of AudioSources available in a background thread.
     * If none is available, return an empty ArrayList.
     * Video source index will be read from stored value if present, else from default value.
     * If an action is required after the source list is refreshed, provide it as a Runnable.
     *
     * @param callback Runnable for action required after source list is refreshed.
     *                 Null if none is required.
     * @return
     */
    private void refreshVideoSourceList(Runnable callback) {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                CaptureState.refreshVideo = true;
                String logTag = "[Source][List][Refresh][Video] ";

                logD(TAG, logTag + "Getting new videoSources.");
                // Get new videoSources.
                videoSourceList = new ArrayList<>(getMedia().getVideoSources());

                // Print out list of videoSources.
                logD(TAG, logTag + "Checking for videoSources...");
                if (videoSourceList == null || videoSourceList.size() < 1) {
                    logD(TAG, logTag + "No videoSource is available!");
                } else {
                    String log = logTag;
                    for (int index = 0; index < videoSourceList.size(); ++index) {
                        VideoSource vs = videoSourceList.get(index);
                        log += "[" + index + "]:" + getVideoSourceStr(vs, true) + " ";
                    }
                    logD(TAG, log + ".");

                    // Set currently selected source.
                    logD(TAG, logTag + "Setting currently selected source and capability.");
                    setVideoSourceIndex(
                            Utils.getSaved(videoSourceIndexKey, videoSourceIndexDefault, context), false);
                    setCapabilityIndex(Utils.getSaved(capabilityIndexKey, capabilityIndexDefault, context));
                }

                // Run callback, if any.
                if (callback == null) {
                    return;
                }

                logD(TAG, "Running provided callback...");
                callback.run();

                // Reset CapState if possible.
                CaptureState.refreshVideo = false;
                if (CaptureState.refreshAudio) {
                    logD(TAG, logTag + "Unable to reset CapState as Audio sources are still being refreshed.");
                } else {
                    setCapState(CaptureState.NOT_CAPTURED);
                    logD(TAG, logTag + "CapState reset to NOT_CAPTURED.");
                }
                logD(TAG, "OK.");
            }
        };
        handlerVideo.post(task);
    }

    /**
     * Either return the current videoSource, or the videoSourceSwitched,
     * based on value of getSwitched.
     * If videoSourceSwitched is not available, return videoSource.
     *
     * @param getSwitched If true, return videoSourceSwitched instead.
     */
    private CameraVideoSource getVideoSource(boolean getSwitched) {

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
        CameraVideoSource videoSourceNew;

        if (videoSourceList == null) {
            logD(TAG, logTag + "Failed as no valid videoSource was available!");
            return;
        }

        int size = videoSourceList.size();
        if (size < 1) {
            logD(TAG, logTag + "Failed as list size was " + size + "!");
            return;
        }

        // If the selected index is larger than size, set it to maximum size.
        // This might happen if the list of videoSources changed.
        if (videoSourceIndex >= size) {
            logD(TAG, logTag + "Resetting index to " + (size - 1) + "as it is greater than " +
                    "size of list (" + size + ")!");
            setVideoSourceIndex(size - 1, true);
            return;
        }
        if (videoSourceIndex < 0) {
            logD(TAG, logTag + "Resetting index to 0 as it was negative!");
            setVideoSourceIndex(0, true);
            return;
        }

        videoSourceNew = (CameraVideoSource) videoSourceList.get(videoSourceIndex);

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
            mirrorFrontCamera();
            return;
        }

        // Set as new videoSource.
        videoSource = videoSourceNew;
        logD(TAG, logTag + "New at index:" +
                videoSourceIndex + " is: " + log + ".");
    }

    /**
     * Get the video source event handler .
     * If none exist, create and return a new one.
     */
    private VideoSourceEvtHdl getVideoSourceEvtHdl() {
        String logTag = "[Video][Source][Evt][Hdl] ";
        if (videoSourceEvtHdl == null) {
            logD(TAG, logTag + "videoSourceEvtHdl does not exist...");
            videoSourceEvtHdl = new VideoSourceEvtHdl();
            logD(TAG, logTag + "Created a new videoSourceEvtHdl.");
        } else {
            logD(TAG, logTag + "Returning existing one.");
        }
        return videoSourceEvtHdl;
    }

    /**
     * Set list of Capabilities supported by the active videoSource.
     * The active videoSource is selected as the first non-null value (or null if none is available)
     * in the following list:
     * videoSourceSwitched, videoSource.
     */
    private void setCapabilityList() {
        String logTag = "[Capability][List][Set] ";
        String log = logTag;

        CameraVideoSource vs = null;
        if (videoSourceSwitched != null) {
            vs = videoSourceSwitched;
            log += "From videoSourceSwitched.";
        } else if (videoSource != null) {
            vs = videoSource;
            log += "From videoSource.";
        } else {
            logD(TAG, logTag + "Failed! No videoSource is available.");
            return;
        }

        capabilityList = (ArrayList<VideoCapabilities>) vs.getCapabilities();
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
            capability = null;
            logD(TAG, logTag + "Failed as no list was available!");
            return;
        }
        int size = capabilityList.size();
        if (size < 1) {
            capability = null;
            logD(TAG, logTag + "Failed as list size was " + size + "!");
            return;
        }

        // If the selected index is larger than size, set it to maximum size.
        // This can happen when the videoSource has changed.
        if (capabilityIndex >= size) {
            logD(TAG, logTag + "Resetting index to " + (size - 1) + "as it is greater than " +
                    "size of list (" + size + ")!");
            setCapabilityIndex(size - 1);
            return;
        }
        if (capabilityIndex < 0) {
            logD(TAG, logTag + "Resetting index to 0 as it was negative!");
            setCapabilityIndex(0);
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

//**********************************************************************************************
    // Switch Media
    //**********************************************************************************************

    /**
     * Gets the index of the next available audioSource.
     * If at end of audioSource range, cycle to start of the other end.
     * Returns null if none available.
     *
     * @param ascending If true, cycle in the direction of increasing index,
     *                  otherwise cycle in opposite direction.
     * @param curIndex  The current audioSourceIndex.
     * @param size
     * @return
     */
    private Integer audioSourceIndexNext(boolean ascending, int curIndex, int size) {
        String logTag = "[Source][Index][Next][Audio] ";
        if (size < 1) {
            logD(TAG, logTag + "Failed as the device does not have a audioSource!");
            return null;
        }
        int now = curIndex;
        return Utils.indexNext(size, now, ascending, logTag);
    }

    /**
     * Gets the index of the next available camera.
     * If at end of camera range, cycle to start of the other end.
     * Returns null if none available.
     *
     * @param ascending If true, cycle in the direction of increasing index,
     *                  otherwise cycle in opposite direction.
     * @param curIndex  The current videoSourceIndex.
     * @param size
     * @return
     */
    private Integer videoSourceIndexNext(boolean ascending, int curIndex, int size) {
        String logTag = "[Source][Index][Next][Video] ";
        if (size < 1) {
            logD(TAG, logTag + "Failed as the device does not have a camera!");
            return null;
        }
        int now = curIndex;
        return Utils.indexNext(size, now, ascending, logTag);
    }

    /**
     * Gets the index of the next available non-NDI camera.
     * If at end of camera range, cycle to start of the other end.
     * Returns null if none available.
     *
     * @param ascending       If true, cycle in the direction of increasing index,
     *                        otherwise cycle in opposite direction.
     * @param curIndex        The current videoSourceIndex.
     * @param videoSourceList
     * @return
     */
    private Integer videoSourceIndexNextNonNdi(boolean ascending, int curIndex, ArrayList<
            VideoSource> videoSourceList) {
        String logTag = "[Source][Index][Next][Video][Non][Ndi] ";
        if (videoSourceList == null) {
            logD(TAG, logTag + "Failed! VideoSources not created!");
            return null;
        }

        int size = videoSourceList.size();

        if (size < 1) {
            logD(TAG, logTag + "Failed! Device does not have a camera!");
            return null;
        }
        int now = curIndex;
        int next = videoSourceIndexNext(ascending, now, size);
        // Keep searching for the next non-NDI until
        while (videoSourceList.get(next).getType() == DEVICE) {
            if (next != now) {
                return next;
            }
            next = videoSourceIndexNext(ascending, next, size);
        }
        logD(TAG, logTag + next + " Failed! 1 complete cycle done.");
        return null;
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

        int now = capabilityIndex;
        return Utils.indexNext(size, now, ascending, logTag);
    }

    /**
     * Check if it is possible to switch directly to a new videoSource.
     * If current videoSource is capturing, do not allow changing to and from NDI.
     * Only device cameras can be switch directly to each other without stopping capture.
     *
     * @param newIndex        The index of the new videoSource to switch to.
     * @param videoSource     The current videoSource.
     * @param videoSourceList The current videoSourceList.
     * @return null if it is possible to switch directly to the new videoSource,
     * else return the error message.
     */
    private String getErrorSwitchNdi(int newIndex, VideoSource
            videoSource, ArrayList<VideoSource> videoSourceList) {
        String logTag = "[Error][Ndi] ";
        String log = null;
        if (videoSourceList == null) {
            log = "No video source exists!";
            return log;
        }
        if (isVideoCaptured()) {
            String error1 = "Failed! Unable to switch capture ";
            String error2 = " NDI directly. Please stop (if) publishing, stop capturing, " +
                    "then start capturing again with ";
            if (videoSource.getType() == NDI) {
                log = error1 + "from" + error2 + "non-NDI videoSource.";
            } else if (videoSourceList.get(newIndex).getType() == NDI) {
                log = error1 + "to" + error2 + "NDI videoSource.";
            }
        }

        if (log == null) {
            logD(TAG, logTag + "Can switch.");
        } else {
            logD(TAG, logTag + "Can NOT switch.");
        }
        return log;
    }

    //**********************************************************************************************
    // Capture
    //**********************************************************************************************

    /**
     * Using the selected audioSource, capture audio into a pubAudioTrack.
     */
    private void startCaptureAudio() {
        String logTag = "[Audio][Capture][Start] ";
        if (isAudioCaptured()) {
            logD(TAG, logTag + "Source is already capturing!");
            return;
        }
        if (audioSource == null) {
            logD(TAG, logTag + "Failed! Source does not exists.");
            return;
        }

        // Capture AudioTrack for publishing.
        AudioTrack track = (AudioTrack) audioSource.startCapture();
        renderAudioPub(track);

        // Set capState if video is not captured.
        if (videoSource == null || audioOnly) {
            setCapState(CaptureState.IS_CAPTURED);
            logD(TAG, logTag + "Set CapState to " + capState + " as video is not captured.");
        }
        logD(TAG, logTag + "OK");
    }

    /**
     * Stop capturing audio, if audio is being captured.
     */
    private void stopCaptureAudio() {
        String logTag = "[Audio][Capture][Stop] ";
        if (!isAudioCaptured()) {
            logD(TAG, logTag + "Not stopping as not captured!");
            return;
        }

        removeAudioSource();
        logD(TAG, logTag + "Audio captured stopped.");
        setAudioEnabledPub(false);

        // Remove Track.
        if (audioTrackPub != null) {
            audioTrackPub = null;
            logD(TAG, logTag + "Track removed.");
        }

        // Set capState if video is not captured.
        if (videoSource == null || audioOnly) {
            setCapState(CaptureState.NOT_CAPTURED);
            logD(TAG, logTag + "Set CapState to " + capState + " as video is not captured.");
        }
        logD(TAG, logTag + "OK");
    }

    /**
     * Set audioSource to null.
     * If audioSource is currently capturing, stop capture first.
     * New audioSource will be created again
     * based on audioSourceIndex.
     */
    private void removeAudioSource() {
        String logTag = "[Audio][Source][Remove] ";

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
     * Using the selected videoSource and capability, capture video into a video track for publishing.
     */
    private void startCaptureVideo() {
        String logTag = "[Video][Capture][Start] ";

        if (isAudioOnly()) {
            logD(TAG, logTag + " NOT continuing as we are in AudioOnly mode.");
            return;
        }

        if (isVideoCaptured()) {
            String log = logTag + "Source is already capturing!";
            logD(TAG, log + " NOT continuing as capState is " + capState + ".");
            return;
        }

        if (videoSource == null) {
            setCapState(CaptureState.NOT_CAPTURED);
            logD(TAG, logTag + "Failed as unable to get valid videoSource!");
            return;
        }

        // Only NDI videoSource is allowed to have null capability.
        if (capability == null && videoSource.getType() != NDI) {
            setCapState(CaptureState.NOT_CAPTURED);
            logD(TAG, logTag + "Failed as unable to get valid Capability for videoSource!");
            return;
        }
        logD(TAG, logTag + "Set " + getVideoSourceStr(videoSource, false) +
                " with Cap: " + getCapabilityStr(capability) + ".");

        videoSource.setEventsHandler(getVideoSourceEvtHdl());
        VideoTrack videoTrack = videoSource.startCapture();

        if (videoSource.getType() == NDI) {
            setCapState(CaptureState.IS_CAPTURED);
            logD(TAG, logTag + "Set CapState to " + capState + " as video is NDI.");
        } else {
            mirrorFrontCamera();
        }

        setRenderVideoTrackPub(videoTrack);
        logD(TAG, logTag + "OK");
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
        if (rendererPub != null) {
            rendererPub.release();
            rendererPub = null;
        }
        mirroredPub = false;
        Log.d(TAG, logTag + "Publisher renderer removed.");
        setVideoEnabledPub(false);
        videoTrackPub = null;
        setCapState(CaptureState.NOT_CAPTURED);
        logD(TAG, logTag + "Set CapState to " + capState + ".");
        logD(TAG, logTag + "OK");
    }

    /**
     * Set all forms of videoSource to null.
     * If videoSource is currently capturing, stop capture first.
     * New videoSource and capability will be created again
     * based on videoSourceIndex and capabilityIndex.
     */
    private void removeVideoSource() {
        String logTag = "[Video][Source][Remove] ";

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
     * Check if either audio or video is captured.
     */
    private boolean isAudioVideoCaptured() {
        String logTag = "[Capture][Audio][Video][?] ";
        if (!isAudioCaptured() && !isVideoCaptured()) {
            logD(TAG, logTag + "No!");
            return false;
        }
        logD(TAG, logTag + "Yes.");
        return true;
    }

    /**
     * Check if audio is captured.
     */
    private boolean isAudioCaptured() {
        String logTag = "[Capture][Audio][?] ";
        if (audioSource == null || !audioSource.isCapturing()) {
            logD(TAG, logTag + "No!");
            return false;
        }
        logD(TAG, logTag + "Yes.");
        return true;
    }

    /**
     * Check if video is captured.
     */
    private boolean isVideoCaptured() {
        boolean result = false;
        String logTag = "[Capture][Video][?] ";
        String log = logTag;
        if (videoSource == null) {
            log += "F. videoSource does not exist.";
            logD(TAG, log);
            return result;
        }

        if (videoSource.isCapturing()) {
            result = true;
            log += "T.";
        } else {
            log += "F.";
        }

        if (NDI == videoSource.getType()) {
            log += " NDI";
        } else {
            log += " non-NDI";
        }
        log += " videoSource.isCapturing: " + videoSource.isCapturing() +
                ". capState is " + capState + ".";
        logD(TAG, log);
        return result;
    }

    //**********************************************************************************************
    // Mute / unmute audio / video.
    //**********************************************************************************************

    //**********************************************************************************************
    // Render - Audio
    //**********************************************************************************************

    /**
     * Sets the {@link #audioPlayback} at the {@link #audioPlaybackIndex}
     * of the {@link #audioPlaybackList}.
     * To set the {@link #audioPlaybackIndex}, use {@link #setAudioPlaybackIndex(int)}.
     */
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
        if (size < 1) {
            logD(TAG, logTag + "Failed as list size was " + size + "!");
            return;
        }

        // If the selected index is larger than size, set it to maximum size.
        // This might happen if the list of audioPlaybacks changed.
        if (audioPlaybackIndex >= size) {
            logD(TAG, logTag + "Resetting index to " + (size - 1) + "as it is greater than " +
                    "size of list (" + size + ")!");
            setAudioPlaybackIndex(size - 1);
            return;
        }
        if (audioPlaybackIndex < 0) {
            logD(TAG, logTag + "Resetting index to 0 as it was negative!");
            setAudioPlaybackIndex(0);
            return;
        }

        audioPlaybackNew = audioPlaybackList.get(audioPlaybackIndex);

        String log;
        if (audioPlaybackNew != null) {
            log = getAudioPlaybackStr(audioPlaybackNew, true);
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
     * Process the local audio.
     */
    private void renderAudioPub(AudioTrack track) {
        String logTag = "[Pub][Render][Audio] ";
        if (track == null) {
            logD(TAG, logTag + "Failed! audioTrack does not exist.");
            return;
        }
        audioTrackPub = track;
        setAudioEnabledPub(true);
        logD(TAG, logTag + "OK");
    }
    //**********************************************************************************************
    // Render - Video
    //**********************************************************************************************

    private void setRenderVideoTrackPub(VideoTrack pubVideoTrack) {
        this.videoTrackPub = pubVideoTrack;
        if (pubVideoTrack == null) {
            logD(TAG, "[setRenderPubVideoTrack] videoTrack is null, so not rendering it...");
            return;
        }

        setVideoEnabledPub(true);
        logD(TAG, "[setRenderPubVideoTrack] Set videoTrack, trying to render it...");
        renderVideoPub();
    }

    private void renderVideoPub() {
        if (videoTrackPub == null) {
            logD(TAG, "[renderPubVideo] Unable to render as videoTrack does not exist.");
            return;
        }
        // Set our pub renderer in our pub video track.
        rendererPub = getRendererPub();
        videoTrackPub.setRenderer(rendererPub);
        logD(TAG, "[renderPubVideo] Set renderer in video track.");
    }

    /**
     * Set the videoTrack in MillicastManager and render it.
     * Must be called on UI thread.
     *
     * @param videoTrack
     */
    private void setRenderVideoTrackSub(VideoTrack videoTrack) {
        this.videoTrackSub = videoTrack;
        if (videoTrack == null) {
            logD(TAG, "[setRenderSubVideoTrack] videoTrack is null, so not rendering it...");
            return;
        }
        enableNdiOutput(isNdiOutputEnabled(false), false, null);

        setVideoEnabledSub(true);
        logD(TAG, "[setRenderSubVideoTrack] Set videoTrack, trying to render it...");

        renderVideoSub();
    }

    private void renderVideoSub() {
        if (videoTrackSub == null) {
            logD(TAG, "[renderSubVideo] Unable to render as videoTrack does not exist.");
            return;
        }
        // Set our sub renderer in our sub video track.
        rendererSub = getRendererSub();
        videoTrackSub.setRenderer(rendererSub);
        logD(TAG, "[renderSubVideo] Set renderer in video track.");
    }

    /**
     * Stop rendering, release and remove subscribe audio and video objects and reset their states to default values.
     */
    private void removeSubscribeMedia() {
        String logTag = "[Sub][Audio][Video][X] ";

        // Remove audio.
        // Disable NDI outputs, if any.
        enableNdiOutput(false, true, null);
        setAudioEnabledSub(false);
        audioTrackSub = null;
        logD(TAG, logTag + "Audio removed.");

        // Remove video
        // Disable NDI outputs, if any.
        enableNdiOutput(false, false, null);
        setVideoEnabledSub(false);
        if (rendererSub != null) {
            rendererSub.release();
            rendererSub = null;
            logD(TAG, logTag + "Renderer removed.");
        } else {
            logD(TAG, logTag + "Not removing renderer as it did not exist.");
        }
        videoTrackSub = null;
        logD(TAG, logTag + "Video removed.");
        logD(TAG, logTag + "OK.");
    }

    /**
     * By default, if the active video source is a front facing camera, the local Publisher video view will be mirrored,
     * to give the Publisher a natural feel when looking at the local Publisher video view.
     * If it is a non-front facing camera, the local video view will be set to not mirrored.
     */
    private void mirrorFrontCamera() {
        String logTag = "[Video][Front][Mirror] ";
        if (videoSource.getType() == NDI) {
            logD(TAG, logTag + "Not mirroring NDI view.");
            setMirror(false);
            return;
        }
        CameraEnumerator cameraEnumerator;
        String name = getVideoSource(true).getName();
        if (Media.INSTANCE.isCamera2Supported()) {
            cameraEnumerator = new Camera2Enumerator(context);
        } else {
            cameraEnumerator = new Camera1Enumerator();
        }
        if (cameraEnumerator.isFrontFacing(name)) {
            logD(TAG, logTag + "Mirroring front camera view...");
            setMirror(true);
        } else {
            logD(TAG, logTag + "Not mirroring non-front camera view...");
            setMirror(false);
        }
    }

    //**********************************************************************************************
    // Publish - Codecs
    //**********************************************************************************************

    /**
     * Set the current audio/videoCodec at the audio/videoCodecIndex of the current
     * audio/videoCodecList,
     * and in the {@link com.millicast.publishers.Option} as preferred codecs if available and NOT currently publishing.
     */
    private void setCodecs() {
        String logTag = "[Codec][Set] ";
        final String none = "None";
        String ac;
        String vc;

        getCodecList(true);
        getCodecList(false);

        logD(TAG, logTag + "Selecting a new one based on selected index.");

        if (audioCodecList == null || audioCodecList.size() < 1) {
            ac = none;
            logD(TAG, logTag + "Failed to set audio codec as none was available!");
        } else {
            int size = audioCodecList.size();

            // If the selected index is larger than size, set it to maximum size.
            if (audioCodecIndex >= size) {
                logD(TAG, logTag + "Resetting audioCodecIndex to " + (size - 1) + "as it is greater than " +
                        "size of list (" + size + ")!");
                setCodecIndex(size - 1, true);
            }
            if (audioCodecIndex < 0) {
                logD(TAG, logTag + "Resetting audioCodecIndex to 0 as it was negative!");
                setCodecIndex(0, true);
                return;
            }
            ac = audioCodecList.get(audioCodecIndex);
        }

        if (videoCodecList == null || videoCodecList.size() < 1) {
            vc = none;
            logD(TAG, logTag + "Failed to set video codec as none was available!");
        } else {
            int size = videoCodecList.size();

            // If the selected index is larger than size, set it to maximum size.
            if (videoCodecIndex >= size) {
                logD(TAG, logTag + "Resetting videoCodecIndex to " + (size - 1) + "as it is greater than " +
                        "size of list (" + size + ")!");
                setCodecIndex(size - 1, false);
            }
            if (videoCodecIndex < 0) {
                logD(TAG, logTag + "Resetting videoCodecIndex to 0 as it was negative!");
                setCodecIndex(0, false);
                return;
            }
            vc = videoCodecList.get(videoCodecIndex);
        }


        logD(TAG, logTag + "Selected at index:" + audioCodecIndex + "/" + videoCodecIndex +
                " is: " + ac + "/" + vc + ".");

        StringBuilder sb = new StringBuilder();
        sb.append(logTag + "OK. ");
        if (publisher != null) {
            publisher.isPublishing().then(publishing -> {
                if (!publishing) {
                    if (!none.equals(ac)) {
                        audioCodec = ac;
                        optionPub.setAudioCodec(ac);
                        sb.append("Set preferred Audio:" + audioCodec + " on Publisher. ");
                    } else {
                        sb.append("Audio NOT set on Publisher.");
                    }
                    if (!none.equals(vc)) {
                        videoCodec = vc;
                        optionPub.setVideoCodec(vc);
                        sb.append("Set preferred Video:" + videoCodec + " on Publisher.");
                    } else {
                        sb.append("Video NOT set on Publisher.");
                    }

                } else {
                    sb.append("NOT set, as publishing is ongoing: ");
                }
                logD(TAG, sb.toString());
            }).error(e -> {
                logD(TAG, logTag + e.getLocalizedMessage());
            });
        } else {
            sb.append("NOT set, as publisher does not exists: ");
            audioCodec = ac;
            videoCodec = vc;
            logD(TAG, sb.toString());
        }

    }

    /**
     * Gets the index of the next available codec.
     * If at end of codec range, cycle to start of the other end.
     * Returns null if none available.
     *
     * @param ascending If true, cycle in the direction of increasing index,
     *                  otherwise cycle in opposite direction.
     * @param forAudio  If true, this is for audio codecs, otherwise for video codecs.
     * @return
     */
    private Integer codecIndexNext(boolean ascending, boolean forAudio) {
        String logTag = "[Codec][Index][Next] ";
        int now;

        if (forAudio) {
            logTag = "[Audio]" + logTag;
            now = audioCodecIndex;
        } else {
            logTag = "[Video]" + logTag;
            now = videoCodecIndex;
        }

        int size;
        ArrayList<String> codecList = getCodecList(forAudio);
        size = codecList.size();
        if (codecList == null || size < 1) {
            logD(TAG, logTag + "Failed as there is no codec!");
            return null;
        }

        return Utils.indexNext(size, now, ascending, logTag);
    }
    //**********************************************************************************************
    // Connect
    //**********************************************************************************************

    /**
     * Millicast methods to connect to Millicast for publishing.
     * Publishing credentials required.
     * If connecting requirements are met, will return true and trigger SDK to start connecting to Millicast. Otherwise, will return false.
     * Actual connection success will be reported by {@link PubListener#onConnected()}.
     */
    private Promise<Boolean> connectPubMc() {
        return new Promise<>((resolve, reject) -> {
            String logTag = "[Pub][Con][Mc] ";

            // Set Credentials
            CompatPublisherCredentials creds = publisher.getCredentials();
            creds.setApiUrl(getUrlPub(CURRENT));
            creds.setStreamName(getStreamNamePub(CURRENT));
            creds.setToken(getTokenPub(CURRENT));
            publisher.setCredentials(creds);
            logD(TAG, logTag + "Set Credentials.");

            // Connect Publisher to Millicast.
            getPublisher().connect().then(success -> {
                logD(TAG, logTag + "OK. Connecting to Millicast.");
                resolve.call(true);
            }).error(e -> {
                logD(TAG, logTag + "Failed. Not Connecting to Millicast.");
                logD(TAG, "Failed! Error: " + e.getLocalizedMessage());
                reject.call(e);
            });
        });

    }

    /**
     * Millicast methods to connect to Millicast for subscribing.
     * Subscribing credentials required.
     * If connecting requirements are met, will return true and trigger SDK to start connecting to Millicast. Otherwise, will return false.
     * Actual connection success will be reported by {@link com.millicast.subscribers.SubscriberListener#onConnected()}.
     */
    private void connectSubMc() {
        String logTag = "[Sub][Con][Mc] ";

        // Set Credentials
        CompatSubscriberCreds creds = subscriber.getCredentials();
        creds.setAccountId(getAccountId(CURRENT));
        creds.setStreamName(getStreamNameSub(CURRENT));
        creds.setApiUrl(getUrlSub(CURRENT));
        // If a Subscribe Token is required, add it.
        // If it is not required:
        // - There is no need to add it.
        // - Adding a valid Subscribe Token is harmless.
        String tokenSub = getTokenSub(CURRENT);
        if (tokenSub != null && !tokenSub.isEmpty()) {
            creds.setToken(tokenSub);
            logD(TAG, logTag + "Added Subscribe Token.");
        }
        subscriber.setCredentials(creds);
        logD(TAG, logTag + "Set Credentials.");

        // Connect Subscriber to Millicast.
        StringBuilder error = new StringBuilder();
        error.append(logTag + "Failed!");
        try {
            getSubscriber().connect().then(success -> {
                logD(TAG, logTag + "OK. Connecting to Millicast.");
                logD(TAG, logTag + "Initializing audio playback device...");
                audioPlaybackStart();
            }).error(e -> {
                setSubState(SubscriberState.DISCONNECTED);
                logD(TAG, logTag + "Failed! Connection requirements not fulfilled. Check inputs (e.g. credentials) and any Millicast error message.");
                error.append(" Error: " + e.getLocalizedMessage());
                logD(TAG, logTag + error.toString());

            });
        } catch (Exception e) {
            logD(TAG, error.toString() + e.getLocalizedMessage());

        }
    }

//**********************************************************************************************
    // Publish
    //**********************************************************************************************

    /**
     * Get the Publisher's listener.
     * If none exist, create and return a new one.
     */
    private PubListener getListenerPub() {
        String logTag = "[Pub][Ltn] ";
        if (listenerPub == null) {
            logD(TAG, logTag + "PubListener does not exist...");
            listenerPub = new PubListener();
            logD(TAG, logTag + "Created a new PubListener.");
        } else {
            logD(TAG, logTag + "Returning existing one.");
        }
        return listenerPub;
    }

    /**
     * Get the Publisher.
     * If none exist, create and return a new one.
     *
     * @return
     */
    private PublisherCompat getPublisher() {
        if (publisher != null) {
            logD(TAG, "[getPublisher] Returning existing Publisher.");
            return publisher;
        }

        logD(TAG, "[getPublisher] Trying to create one...");
        publisher = new PublisherCompat(getListenerPub());

        logD(TAG, "[getPublisher] Created and returning a new Publisher.");
        return publisher;
    }

    /**
     * Millicast methods to start publishing.
     * Audio and video tracks that are already captured will be added to Publisher.
     * {@link com.millicast.publishers.Option} (including preferred codecs) will be set into Publisher.
     * If publishing requirements are met, will return true and trigger SDK to start publish. Otherwise, will return false.
     * Actual publishing success will be reported by {@link PubListener}.
     */
    private void startPubMc() {
        String logTag = "[Pub][Start][Mc] ";

        if (audioTrackPub != null) {
            publisher.addTrack(audioTrackPub).then(success -> {
                logD(TAG, logTag + "Audio track added.");
            }).error(e -> {
                logD(TAG, logTag + "Failed! Audio NOT track added.");
            });

        } else {
            logD(TAG, logTag + "Audio track NOT added as it does not exist.");
        }

        if (videoTrackPub != null) {
            publisher.addTrack(videoTrackPub).then(success -> {
                logD(TAG, logTag + "Video track added.");
            }).error(e -> {
                logD(TAG, logTag + "Failed! Video NOT track added.");
            });
        } else {
            logD(TAG, logTag + "Video track NOT added as it does not exist.");
        }

        // Set Publisher Options
        optionPub.setSourceId(getOptSourceIdPub(CURRENT).toString());
        optionPub.setRecordStream(this.recordingEnabledPub);
        logD(TAG, logTag + "SourceId (" + sourceIdPub + ") set in Option.");

        setCodecs();
        logD(TAG, logTag + "Preferred codecs set in Option.");

        setBitrate(0, MCTypes.Bitrate.MIN);
        setBitrate(2500, MCTypes.Bitrate.MAX);
        logD(TAG, logTag + "Preferred bitrates set in Option.");

        publisher.setOptions(optionPub);
        logD(TAG, logTag + "Options set in Publisher.");

        // Publish to Millicast

        publisher.publish()
                .then(success -> {
                    logD(TAG, logTag + "OK. Starting publish to Millicast.");
                    // Enable Publisher Stats
                    enableStatsPub(true);
                    logD(TAG, logTag + "OK.");
                })
                .error(e -> {
                    logD(TAG, logTag + e.getLocalizedMessage());
                });
    }

    /**
     * Millicast methods to stop publishing.
     */
    private void stopPubMc() {
        String logTag = "[Pub][Stop][Mc] ";

        // Stop publishing to Millicast.
        publisher.unpublish()
                .then(success -> {
                    logD(TAG, logTag + "OK. Stopped publishing to Millicast.");
                    enableStatsPub(false);
                    logD(TAG, logTag + "Stats stopped");
                    setPubState(PublisherState.DISCONNECTED);
                    fragmentPub.setUI();
                    // Remove Publisher.
                    publisher = null;
                    logD(TAG, logTag + "Publisher removed.");
                    logD(TAG, logTag + "OK.");
                })
                .error(e -> {
                    logD(TAG, logTag + "Failed! Stop publishing requirements not fulfilled. Check current states and any Millicast error message.");
                    logD(TAG, logTag + e.getLocalizedMessage());
                });
    }

    /**
     * Check if we are currently publishing.
     */
//    private boolean isPublishing() {
//        String logTag = "[Pub][?] ";
//        if (publisher == null || getPubState() != PublisherState.PUBLISHING) {
//            logD(TAG, logTag + "No!");
//            return false;
//        }
//        logD(TAG, logTag + "Yes.");
//        return true;
//    }

    //**********************************************************************************************
    // Subscribe
    //**********************************************************************************************

    /**
     * Get the Subscriber's listener.
     * If none exist, create and return a new one.
     */
    private SubListener getListenerSub() {
        String logTag = "[Sub][Ltn] ";
        if (listenerSub == null) {
            logD(TAG, logTag + "SubListener does not exist...");
            listenerSub = new SubListener();
            logD(TAG, logTag + "Created a new SubListener.");
        } else {
            logD(TAG, logTag + "Returning existing one.");
        }
        return listenerSub;
    }

    /**
     * Get the Subscriber.
     * If none exist, create and return a new one.
     *
     * @return
     */
    private SubscriberCompat getSubscriber() {
        if (subscriber != null) {
            logD(TAG, "[getSubscriber] Returning existing Subscriber.");
            return subscriber;
        }

        logD(TAG, "[getSubscriber] Trying to create one...");
        subscriber = new SubscriberCompat(getListenerSub());
        logD(TAG, "[getSubscriber] Created and returning a new Subscriber.");
        return subscriber;
    }

    /**
     * Millicast methods to start subscribing.
     * {@link com.millicast.subscribers.Option} will be set into Subscriber.
     * If subscribing requirements are met, will return true and trigger SDK to start subscribe. Otherwise, will return false.
     * Actual subscribing success will be reported by {@link SubListener#onConnected()}.
     */
    private Promise<Unit> startSubMc() {
        String logTag = "[Pub][Start][Mc] ";

        // Set Subscriber Options
        subscriber.setOptions(optionSub);
        logD(TAG, logTag + "Options set.");

        // Subscribe to Millicast
        logD(TAG, logTag + "Starting subscribe...");
        return subscriber.subscribe();
    }

    /**
     * Millicast methods to stop subscribing.
     */
    private void stopSubMc() {
        String logTag = "[Sub][Stop][Mc] ";

        // Stop subscribing to Millicast.
        subscriber.unsubscribe().then(success -> {
            logD(TAG, logTag + "OK. Stopped subscribing to Millicast.");
        }).error(e -> {
            logD(TAG, logTag + "Failed!");
            logD(TAG, logTag + e.getLocalizedMessage());
        });
    }

    /**
     * Check if we are currently subscribing.
     */
    private boolean isSubscribing() {
        String logTag = "[Sub][?] ";
        if (subscriber == null || !subscriber.isSubscribed()) {
            logD(TAG, logTag + "No!");
            return false;
        }
        logD(TAG, logTag + "Yes.");
        return true;
    }

    //**********************************************************************************************
    // Utilities
    //**********************************************************************************************

    /**
     * Get a String that describes a MCVideoSource.
     */
    private String getAudioSourceStr(AudioSource audioSource, boolean longForm) {
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

    private String getAudioPlaybackStr(AudioPlayback audioPlayback, boolean longForm) {
        String name = "Audio:";
        if (audioPlayback == null) {
            name += "NULL!";
            return name;
        }

        name = "Audio:" + audioPlayback.getName();
        if (longForm) {
            name += " (" + audioPlayback.getType() + ") " + "id:" + audioPlayback.getId();
        }
        return name;
    }

    /**
     * Get a String that describes a MCVideoSource.
     */
    private String getVideoSourceStr(VideoSource vs, boolean longForm) {
        String name = "Cam:";
        if (vs == null) {
            name += "N.A.";
            return name;
        }

        name = "Cam:" + vs.getName();
        if (longForm) {
            name += " (" + vs.getType() + ") " + "id:" + vs.getId();
        }
        return name;
    }

    /**
     * Get a String that describes a MCVideoCapabilities.
     */
    private String getCapabilityStr(VideoCapabilities cap) {
        String name;
        if (cap == null) {
            name = "Cap: N.A.";
        } else {
            // Note: FPS given in frames per 1000 seconds (FPKS).
            name = cap.getWidth() + "x" + cap.getHeight() + " fps:" + cap.getFps() / 1000;
        }
        return name;
    }

    public boolean isRecordingEnabledPub() {
        return recordingEnabledPub;
    }

}
