package com.millicast.android_app;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.millicast.AudioTrack;
import com.millicast.VideoRenderer;
import com.millicast.VideoTrack;

import org.webrtc.RendererCommon;

import static com.millicast.android_app.MillicastManager.Source.CURRENT;
import static com.millicast.android_app.MillicastManager.keyConVisible;
import static com.millicast.android_app.Utils.logD;
import static com.millicast.android_app.Utils.makeSnackbar;

public class PublishFragment extends Fragment {
    public static final String TAG = "PublishFragment";

    private final MillicastManager mcManager;
    private LinearLayout linearLayoutVideo;
    private LinearLayout linearLayoutCon;
    private TextView textView;
    private Switch switchDirection;
    private Button buttonRefresh;
    private Button buttonAudioSrc;
    private Button buttonVideoSrc;
    private Button buttonResolution;
    private Button buttonCapture;
    private Button buttonAudio;
    private Button buttonVideo;
    private Button buttonMirror;
    private Button buttonScale;
    private Button buttonAudioCodec;
    private Button buttonVideoCodec;
    private Button buttonPublish;

    private boolean ascending = true;
    private boolean conVisible = true;

    public PublishFragment() {
        mcManager = MillicastManager.getSingleInstance();

        // Set this view into listeners/handlers
        mcManager.setPubView(this);

        // Set buttons again in case states changed while vidSrcEvtHdl had no access to this view.
        setUI();
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_publish, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState == null) {
            // Will only lock if it's the first time.
            mcManager.setCameraLock(true);
            if (mcManager.isRicohTheta(CURRENT)) {
                mcManager.overrideBWE(40000000);
            }
        } else {
            conVisible = savedInstanceState.getBoolean(keyConVisible);
        }

        linearLayoutVideo = view.findViewById(R.id.linear_layout_video_pub);
        linearLayoutCon = view.findViewById(R.id.linear_layout_con_pub);
        textView = view.findViewById(R.id.textViewPub);

        switchDirection = view.findViewById(R.id.switchDirectionPub);

        buttonRefresh = view.findViewById(R.id.button_refresh_pub);
        buttonAudioSrc = view.findViewById(R.id.button_audio_src);
        buttonVideoSrc = view.findViewById(R.id.button_video_src);
        buttonResolution = view.findViewById(R.id.buttonResolution);
        buttonCapture = view.findViewById(R.id.buttonCapture);
        buttonAudio = view.findViewById(R.id.button_audio_pub);
        buttonVideo = view.findViewById(R.id.button_video_pub);
        buttonMirror = view.findViewById(R.id.button_mirror);
        buttonScale = view.findViewById(R.id.button_scale);
        buttonAudioCodec = view.findViewById(R.id.buttonCodecAudio);
        buttonVideoCodec = view.findViewById(R.id.buttonCodecVideo);
        buttonPublish = view.findViewById(R.id.buttonPublish);

        // Set static button actions
        linearLayoutVideo.setOnClickListener(this::toggleCon);
        switchDirection.setOnCheckedChangeListener(this::toggleAscend);
        switchDirection.setChecked(ascending);
        buttonRefresh.setOnClickListener(this::refreshMediaSources);
        buttonAudioSrc.setOnClickListener(this::toggleAudioSrc);
        buttonVideoSrc.setOnClickListener(this::toggleVideoSrc);
        buttonResolution.setOnClickListener(this::toggleResolution);
        buttonAudio.setOnClickListener(this::toggleAudio);
        buttonVideo.setOnClickListener(this::toggleVideo);
        buttonMirror.setOnClickListener(this::toggleMirror);
        buttonScale.setOnClickListener(this::toggleScale);
        buttonAudioCodec.setOnClickListener(viewButton -> toggleCodec(viewButton, true));
        buttonVideoCodec.setOnClickListener(viewButton -> toggleCodec(viewButton, false));
    }

    @Override
    public void onResume() {
        super.onResume();
        displayPubVideo();
        setUI();
    }

    @Override
    public void onPause() {
        Utils.stopDisplayVideo(linearLayoutVideo, TAG);
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(keyConVisible, conVisible);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    //**********************************************************************************************
    // Select/Switch videoSource, capability.
    //**********************************************************************************************

    /**
     * Switch audio source to the next one in the list of VideoSource.
     *
     * @param view
     */
    private void toggleAudioSrc(View view) {
        String logTag = "[toggle][Source][Audio] ";
        String error = mcManager.switchAudioSource(ascending);
        if (error != null) {
            Utils.makeSnackbar(logTag, error, this);
        } else {
            Utils.makeSnackbar(logTag, "OK. " + mcManager.getAudioSourceName(), this);
        }
        setUI();
    }

    /**
     * Switch video source to the next one in the list of VideoSource.
     *
     * @param view
     */
    private void toggleVideoSrc(View view) {
        String logTag = "[toggle][Source][Video] ";
        try {
            String error = mcManager.switchVideoSource(ascending);
            if (error != null) {
                Utils.makeSnackbar(logTag, error, this);
            } else {
                Utils.makeSnackbar(logTag, "OK. " + mcManager.getVideoSourceName(), this);
            }
        } catch (IllegalStateException e) {
            logD(TAG, logTag + "Only switched selected camera. " +
                    "No camera is actually capturing now.");
        }
        setUI();
    }

    /**
     * Switch resolutions to the next one in the list of Capabilities.
     *
     * @param view
     */
    private void toggleResolution(View view) {
        String logTag = "[toggle][Resolution] ";
        try {
            mcManager.switchCapability(ascending);
            Utils.makeSnackbar(logTag, "OK. " + mcManager.getCapabilityName(), this);
        } catch (IllegalStateException e) {
            logD(TAG, logTag + "Failed! Error:" + e + "");
        }
        setUI();
    }

    //**********************************************************************************************
    // Capture
    //**********************************************************************************************

    void refreshMediaSources(View view){
        mcManager.refreshMediaLists();
    }

    void onStartCaptureClicked(View captureButton) {
        Log.d(TAG, "Start Capture clicked.");
        displayPubVideo();
        mcManager.startAudioVideoCapture();
        Log.d(TAG, "Started media capture.");
        Log.d(TAG, "Started display video.");
        setUI();
    }

    private void onStopCaptureClicked(View view) {
        Log.d(TAG, "Stop Capture clicked.");
        mcManager.stopAudioVideoCapture();
        Log.d(TAG, "Stopped media capture.");
        Utils.stopDisplayVideo(linearLayoutVideo, TAG);
        setUI();
    }

    //**********************************************************************************************
    // Mute / unmute audio / video.
    //**********************************************************************************************

    /**
     * Mute or unmute audio by switching audio state to the opposite of the current state.
     *
     * @param view
     */
    private void toggleAudio(View view) {
        AudioTrack track = mcManager.getPubAudioTrack();
        if (track != null) {
            track.setEnabled(!mcManager.isPubAudioEnabled());
            mcManager.setPubAudioEnabled(!mcManager.isPubAudioEnabled());
            setUI();
        }
    }

    /**
     * Mute or unmute video by switching video state to the opposite of the current state.
     *
     * @param view
     */
    private void toggleVideo(View view) {
        VideoTrack track = mcManager.getPubVideoTrack();
        if (track != null) {
            track.setEnabled(!mcManager.isPubVideoEnabled());
            mcManager.setPubVideoEnabled(!mcManager.isPubVideoEnabled());
            setUI();
        }
    }

    //**********************************************************************************************
    // Render
    //**********************************************************************************************

    private void displayPubVideo() {
        String tag = "[displayPubVideo] ";
        // Display video if not already displayed.
        if (linearLayoutVideo.getChildCount() == 0) {
            VideoRenderer pubRenderer = mcManager.getPubRenderer();
            // Ensure our renderer is not attached to another parent view.
            Utils.removeFromParentView(pubRenderer, TAG);
            // Finally, add our renderer to our frame layout.
            linearLayoutVideo.addView(pubRenderer);
            logD(TAG, tag + "Added renderer for display.");
        } else {
            logD(TAG, tag + "Already displaying renderer.");
        }
    }

    /**
     * Set the ScalingType of the current videoRenderer to the next one in the list of
     * {@link RendererCommon.ScalingType}.
     * If at end of range of ScalingType, cycle to start of the other end.
     *
     * @param view
     */
    private void toggleScale(View view) {
        String logTag = "[Scale][Toggle][Pub] ";
        String log = "";
        if (linearLayoutVideo != null) {
            RendererCommon.ScalingType type = mcManager.switchScaling(ascending, true);
            if (type != null) {
                log += "Switched to " + type + ".";
            } else {
                log += "Failed! SDK could not switch.";
            }
        } else {
            log += "Failed! linearLayout not available!";
        }
        makeSnackbar(logTag, log, this);
        setUI();
    }

    /**
     * Mirror the rendered video locally.
     */
    private void toggleMirror(View view) {
        String logTag = "[Mirror][Pub] ";
        String log;
        if (linearLayoutVideo != null) {
            mcManager.switchMirror(true);
        }
        log = mcManager.isPubMirrored() + ".";
        makeSnackbar(logTag, log, this);
        setUI();
    }

    private void setButtonMirrorText() {
        String mirror = "Mirror:";
        if(mcManager.isPubMirrored()){
            mirror += "T";
        } else {
            mirror += "F";
        }
        buttonMirror.setText(mirror);
    }

    //**********************************************************************************************
    // Publish
    //**********************************************************************************************

    private void onStartPublishClicked(View view) {
        Log.d(TAG, "Start Publish clicked.");
        mcManager.pubConnect();
        setUI();
    }

    private void onStopPublishClicked(View view) {
        Log.d(TAG, "Stop Publish clicked.");
        mcManager.stopPublish();
        setUI();
    }

    /**
     * Switch codec to the next one in the list of Codecs.
     *
     * @param view
     * @param isAudio
     */
    private void toggleCodec(View view, boolean isAudio) {
        try {
            mcManager.switchCodec(ascending, isAudio);
        } catch (IllegalStateException e) {
            logD(TAG, "[toggle][Codec] Failed! Error:" + e + "");
        }
        setUI();
    }

    //**********************************************************************************************
    // UI Control
    //**********************************************************************************************

    /**
     * Toggle the visibility of UI controls.
     *
     * @param view
     */
    private void toggleCon(View view) {
        if (linearLayoutCon == null) {
            return;
        }
        conVisible = !conVisible;
        displayCon();
    }

    /**
     * Set the UI controls to visible or not depending on {@link #conVisible}.
     * The current scaling of the videoView will be applied again.
     */
    private void displayCon() {
        int visibility;
        if (conVisible) {
            visibility = View.VISIBLE;
        } else {
            visibility = View.GONE;
        }
        linearLayoutCon.setVisibility(visibility);
        mcManager.applyScaling(true);
    }

    /**
     * Switch the direction of cycling through indices, for controls that
     * allow switching to the next index.
     *
     * @param buttonView
     * @param isChecked
     */
    private void toggleAscend(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            ascending = true;
            buttonView.setText("->");
        } else {
            ascending = false;
            buttonView.setText("<-");
        }
    }

    /**
     * Set the states of UIs, including capture and publish buttons,
     * based on current capture and publish states.
     * Capture state:
     * - Cannot change when publishing.
     * Publish state:
     * - Must first capture and connect before publish.
     * Must be run on UI thread.
     */
    void setUI() {

        if (this.getView() == null) {
            return;
        }

        displayCon();

        textView.setText("Token: " + mcManager.getTokenPub(CURRENT) +
                "\nStream: " + mcManager.getStreamNamePub(CURRENT));
        buttonAudioSrc.setText(mcManager.getAudioSourceName());
        buttonVideoSrc.setText(mcManager.getVideoSourceName());
        buttonResolution.setText(mcManager.getCapabilityName());
        buttonScale.setText(mcManager.getScalingName(true));
        setButtonMirrorText();
        buttonAudioCodec.setText(mcManager.getCodecName(true));
        buttonVideoCodec.setText(mcManager.getCodecName(false));

        boolean readyToPublish = false;
        boolean canChangeCapture = false;
        if (mcManager.getCapState() == MillicastManager.CaptureState.IS_CAPTURED) {
            readyToPublish = true;
        }
        if (mcManager.getPubState() == MillicastManager.PublisherState.DISCONNECTED) {
            canChangeCapture = true;
        }

        if (canChangeCapture) {
            buttonAudioSrc.setEnabled(true);
            buttonAudioCodec.setEnabled(true);
            buttonVideoCodec.setEnabled(true);
            switch (mcManager.getCapState()) {
                case NOT_CAPTURED:
                    buttonCapture.setText(R.string.startCapture);
                    buttonCapture.setOnClickListener(this::onStartCaptureClicked);
                    buttonCapture.setEnabled(true);
                    setMuteButtons(false);
                    break;
                case TRY_CAPTURE:
                    buttonCapture.setText(R.string.tryCapture);
                    buttonCapture.setEnabled(false);
                    setMuteButtons(false);
                    break;
                case IS_CAPTURED:
                    buttonCapture.setText(R.string.stopCapture);
                    buttonCapture.setOnClickListener(this::onStopCaptureClicked);
                    buttonCapture.setEnabled(true);
                    setMuteButtons(true);
                    break;
            }
        } else {
            buttonAudioSrc.setEnabled(false);
            buttonAudioCodec.setEnabled(false);
            buttonVideoCodec.setEnabled(false);
            buttonCapture.setEnabled(false);
        }

        if (!readyToPublish) {
            if (MillicastManager.PublisherState.DISCONNECTED == mcManager.getPubState()) {
                buttonPublish.setText(R.string.notPublishing);
                buttonPublish.setEnabled(false);
                return;
            }
        }

        switch (mcManager.getPubState()) {
            case DISCONNECTED:
                buttonPublish.setText(R.string.startPublish);
                buttonPublish.setOnClickListener(this::onStartPublishClicked);
                buttonPublish.setEnabled(true);
                break;
            case CONNECTING:
            case CONNECTED:
                buttonPublish.setText(R.string.tryPublish);
                buttonPublish.setEnabled(false);
                break;
            case PUBLISHING:
                buttonPublish.setText(R.string.stopPublish);
                buttonPublish.setOnClickListener(this::onStopPublishClicked);
                buttonPublish.setEnabled(true);
                break;
        }
        setMuteButtons(true);
    }

    /**
     * Set the states of mute buttons.
     *
     * @param isCaptured
     */
    private void setMuteButtons(boolean isCaptured) {
        if (isCaptured) {
            buttonAudio.setEnabled(true);
            buttonVideo.setEnabled(true);
            if (mcManager.isPubAudioEnabled()) {
                buttonAudio.setText(R.string.muteAudio);
            } else {
                buttonAudio.setText(R.string.unmuteAudio);
            }
            if (mcManager.isPubVideoEnabled()) {
                buttonVideo.setText(R.string.muteVideo);
            } else {
                buttonVideo.setText(R.string.unmuteVideo);
            }
        } else {
            buttonAudio.setEnabled(false);
            buttonVideo.setEnabled(false);
            buttonAudio.setText(R.string.noAudio);
            buttonVideo.setText(R.string.noVideo);
        }
    }
}