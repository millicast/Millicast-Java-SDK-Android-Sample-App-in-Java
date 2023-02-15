package com.millicast.android_app;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.activity.result.contract.ActivityResultContracts;

import java.util.ArrayList;

import com.millicast.AudioTrack;
import com.millicast.VideoRenderer;
import com.millicast.VideoTrack;

import org.webrtc.RendererCommon;

import static com.millicast.android_app.MCStates.*;
import static com.millicast.android_app.MCTypes.Source.CURRENT;
import static com.millicast.android_app.MillicastManager.keyConVisible;
import static com.millicast.android_app.Utils.logD;
import static com.millicast.android_app.Utils.makeSnackbar;

/**
 * UI for publishing.
 */
public class PublishFragment extends Fragment {
    public static final String TAG = "PublishFragment";

    private MillicastManager mcMan;
    private LinearLayout linearLayoutVideo;
    private LinearLayout linearLayoutCon;
    private TextView textViewPub;
    private TextView textViewAudioOnly;
    private Switch switchDirection;
    private Switch switchAudioOnly;
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

    // Launch a dialog to ask for required permission(s).
    private final ActivityResultLauncher<String[]> cameraMicPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                String logTag = "[Perm][Result] ";
                String log = "";
                Boolean permGranted = false;
                int audioResult = result.get(Manifest.permission.RECORD_AUDIO) ? PackageManager.PERMISSION_GRANTED : PackageManager.PERMISSION_DENIED;
                if (mcMan.isAudioOnly()) {
                    log = "AudioOnly: ";
                    if (audioResult == PackageManager.PERMISSION_GRANTED) {
                        permGranted = true;
                        log += "Permission granted.";
                    } else {
                        log += "Permission NOT granted!";
                    }
                } else {
                    log = "AudioVideo: ";
                    int cameraResult = result.get(Manifest.permission.CAMERA) ? PackageManager.PERMISSION_GRANTED : PackageManager.PERMISSION_DENIED;
                    if (audioResult == PackageManager.PERMISSION_GRANTED &&
                            cameraResult == PackageManager.PERMISSION_GRANTED) {
                        permGranted = true;
                        log += "Permissions granted.";
                    } else {
                        log += "Permission(s) NOT granted!";
                    }
                }
                logD(TAG, logTag + log);
                if (permGranted) {
                    // Permission is granted, continue with the code
                    onStartCaptureClickedApproved();
                    return;
                }
                // Warn user that permission(s) required to publish is/are still missing.
                String msg = "Publishing can only start after microphone ";
                if (!mcMan.isAudioOnly()) {
                    msg += "and camera permissions have ";
                } else {
                    msg += "permission has ";
                }
                msg += "been given.\nIn order to publish, please allow the required permission at the next permission dialog or manually allow them at the device's Settings > Apps menu.";
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setMessage(msg);
                builder.setNeutralButton("OK.", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
            });

    public PublishFragment() {
        mcMan = MillicastManager.getSingleInstance();

        // Set this view into listeners/handlers
        mcMan.setViewPub(this);

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
            mcMan.setCameraLock(true);
        } else {
            conVisible = savedInstanceState.getBoolean(keyConVisible);
        }

        // Initialize views
        linearLayoutVideo = view.findViewById(R.id.linear_layout_video_pub);
        linearLayoutCon = view.findViewById(R.id.linear_layout_con_pub);
        textViewPub = view.findViewById(R.id.text_Pub);
        textViewAudioOnly = view.findViewById(R.id.text_audio_only);

        switchDirection = view.findViewById(R.id.switchDirectionPub);
        switchAudioOnly = view.findViewById(R.id.switch_audio_only_pub);

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

        // Set actions
        linearLayoutVideo.setOnClickListener(this::toggleCon);
        switchDirection.setOnCheckedChangeListener(this::toggleAscend);
        switchDirection.setChecked(ascending);
        switchAudioOnly.setOnCheckedChangeListener(this::toggleAudioOnly);
        switchAudioOnly.setChecked(mcMan.isAudioOnly());
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
        String error = mcMan.switchAudioSource(ascending);
        if (error != null) {
            Utils.makeSnackbar(logTag, error, this);
        } else {
            Utils.makeSnackbar(logTag, "OK. " + mcMan.getAudioSourceName(), this);
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
            String error = mcMan.switchVideoSource(ascending);
            if (error != null) {
                Utils.makeSnackbar(logTag, error, this);
            } else {
                Utils.makeSnackbar(logTag, "OK. " + mcMan.getVideoSourceName(), this);
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
            mcMan.switchCapability(ascending);
            Utils.makeSnackbar(logTag, "OK. " + mcMan.getCapabilityName(), this);
        } catch (IllegalStateException e) {
            logD(TAG, logTag + "Failed! Error:" + e + "");
        }
        setUI();
    }

    //**********************************************************************************************
    // Capture
    //**********************************************************************************************

    /**
     * Switch the AudioOnly mode on or off.
     * In AudioOnly mode, no video will be captured.
     * This switch can only be done when no media is currently captured.
     *
     * @param buttonView
     * @param isChecked
     */
    private void toggleAudioOnly(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            textViewAudioOnly.setText(R.string.audioOnlyT);
            mcMan.setAudioOnly(true);
        } else {
            textViewAudioOnly.setText(R.string.audioOnlyF);
            mcMan.setAudioOnly(false);
        }
        setUI();
    }

    private void refreshMediaSources(View view) {
        // Reload the Publish view if possible after media sources are obtained.
        mcMan.refreshMediaSourceLists();
        setUI();
    }

    private void onStartCaptureClicked(View captureButton) {
        Log.d(TAG, "Start Capture clicked.");
        // Check if camera and microphone permissions are already granted.
        if (!hasMediaPermissions()) {
            // Request camera and/or microphone permissions
            String[] permissions = {""};
            ArrayList<String> permList = new ArrayList();
            permList.add(Manifest.permission.RECORD_AUDIO);
            if (!mcMan.isAudioOnly()) {
                permList.add(Manifest.permission.CAMERA);
            }
            permissions = permList.toArray(permissions);
            // Try to ask for the remaining required permission(s).
            cameraMicPermissionLauncher.launch(permissions);
        } else {
            // If permission(s) already granted, proceed to start capture.
            onStartCaptureClickedApproved();
        }
    }

    /**
     * Checks if all the required media permission(s) have been granted.
     * If in {@link MillicastManager#audioOnly AudioOnly} mode, only the {@link Manifest.permission#RECORD_AUDIO} permission is needed.
     * Otherwise, the {@link Manifest.permission#CAMERA} permission is also needed.
     *
     * @return
     */
    private boolean hasMediaPermissions() {
        Boolean audioGranted = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
        Boolean cameraGranted = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        if (mcMan.isAudioOnly()) {
            return audioGranted;
        } else {
            return audioGranted && cameraGranted;
        }
    }

    private void onStartCaptureClickedApproved() {
        displayPubVideo();
        mcMan.startAudioVideoCapture();
        Log.d(TAG, "Started media capture.");
        Log.d(TAG, "Started display video.");
        setUI();
    }

    private void onStopCaptureClicked(View view) {
        Log.d(TAG, "Stop Capture clicked.");
        mcMan.stopAudioVideoCapture();
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
        AudioTrack track = mcMan.getAudioTrackPub();
        if (track != null) {
            track.setEnabled(!mcMan.isAudioEnabledPub());
            mcMan.setAudioEnabledPub(!mcMan.isAudioEnabledPub());
            setUI();
        }
    }

    /**
     * Mute or unmute video by switching video state to the opposite of the current state.
     *
     * @param view
     */
    private void toggleVideo(View view) {
        VideoTrack track = mcMan.getVideoTrackPub();
        if (track != null) {
            track.setEnabled(!mcMan.isVideoEnabledPub());
            mcMan.setVideoEnabledPub(!mcMan.isVideoEnabledPub());
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
            VideoRenderer pubRenderer = mcMan.getRendererPub();
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
            RendererCommon.ScalingType type = mcMan.switchScaling(ascending, true);
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
            mcMan.switchMirror();
        }
        log = mcMan.isMirroredPub() + ".";
        makeSnackbar(logTag, log, this);
        setUI();
    }

    private void setButtonMirrorText() {
        String mirror = "Mirror:";
        if (mcMan.isMirroredPub()) {
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
        mcMan.connectPub();
        setUI();
    }

    private void onStopPublishClicked(View view) {
        Log.d(TAG, "Stop Publish clicked.");
        mcMan.stopPub();
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
            mcMan.switchCodec(ascending, isAudio);
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
        mcMan.applyScaling(true);
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

        textViewPub.setText("Token: " + mcMan.getTokenPub(CURRENT) +
                "\nStream: " + mcMan.getStreamNamePub(CURRENT));
        buttonAudioSrc.setText(mcMan.getAudioSourceName());
        buttonVideoSrc.setText(mcMan.getVideoSourceName());
        buttonResolution.setText(mcMan.getCapabilityName());
        buttonScale.setText(mcMan.getScalingName(true));
        setButtonMirrorText();
        buttonAudioCodec.setText(mcMan.getCodecName(true));
        buttonVideoCodec.setText(mcMan.getCodecName(false));

        boolean readyToPublish = false;
        boolean canChangeCapture = false;
        if (mcMan.getCapState() == CaptureState.IS_CAPTURED) {
            readyToPublish = true;
        }
        if (mcMan.getPubState() == PublisherState.DISCONNECTED) {
            canChangeCapture = true;
        }

        if (canChangeCapture) {
            buttonAudioSrc.setEnabled(true);
            buttonAudioCodec.setEnabled(true);
            buttonVideoSrc.setEnabled(true);
            buttonVideoCodec.setEnabled(true);
            buttonResolution.setEnabled(true);
            switch (mcMan.getCapState()) {
                case NOT_CAPTURED:
                    buttonCapture.setText(R.string.startCapture);
                    buttonCapture.setOnClickListener(this::onStartCaptureClicked);
                    buttonCapture.setEnabled(true);
                    buttonRefresh.setEnabled(true);
                    switchAudioOnly.setEnabled(true);
                    setMuteButtons(false);
                    break;
                case TRY_CAPTURE:
                    buttonCapture.setText(R.string.tryCapture);
                    buttonCapture.setEnabled(false);
                    buttonRefresh.setEnabled(false);
                    switchAudioOnly.setEnabled(false);
                    setMuteButtons(false);
                    break;
                case IS_CAPTURED:
                    buttonCapture.setText(R.string.stopCapture);
                    buttonCapture.setOnClickListener(this::onStopCaptureClicked);
                    buttonCapture.setEnabled(true);
                    buttonRefresh.setEnabled(false);
                    switchAudioOnly.setEnabled(false);
                    setMuteButtons(true);
                    break;
                case REFRESH_SOURCE:
                    buttonAudioSrc.setEnabled(false);
                    buttonVideoSrc.setEnabled(false);
                    buttonResolution.setEnabled(false);
                    buttonCapture.setText(R.string.refreshingSources);
                    buttonCapture.setEnabled(false);
                    buttonRefresh.setEnabled(false);
                    switchAudioOnly.setEnabled(false);
                    setMuteButtons(false);
                    break;
            }
        } else {
            buttonAudioSrc.setEnabled(false);
            buttonAudioCodec.setEnabled(false);
            buttonVideoSrc.setEnabled(false);
            buttonVideoCodec.setEnabled(false);
            buttonResolution.setEnabled(false);
            buttonCapture.setEnabled(false);
            buttonRefresh.setEnabled(false);
            switchAudioOnly.setEnabled(false);
        }

        // Adjust for AudioOnly mode.
        if (mcMan.isAudioOnly()) {
            textViewAudioOnly.setText(R.string.audioOnlyT);
            buttonVideoSrc.setEnabled(false);
            buttonResolution.setEnabled(false);
            buttonVideoCodec.setEnabled(false);
        } else {
            textViewAudioOnly.setText(R.string.audioOnlyF);
        }

        if (!readyToPublish) {
            if (PublisherState.DISCONNECTED == mcMan.getPubState()) {
                buttonPublish.setText(R.string.notPublishing);
                buttonPublish.setEnabled(false);
                return;
            }
        }

        switch (mcMan.getPubState()) {
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
            if (mcMan.isAudioEnabledPub()) {
                buttonAudio.setText(R.string.muteAudio);
            } else {
                buttonAudio.setText(R.string.unmuteAudio);
            }
            if (mcMan.isVideoEnabledPub()) {
                buttonVideo.setText(R.string.muteVideo);
            } else {
                buttonVideo.setText(R.string.unmuteVideo);
            }
            if (mcMan.isAudioOnly()) {
                buttonVideo.setEnabled(false);
                buttonVideo.setText(R.string.noVideo);
            } else {
                buttonVideo.setEnabled(true);
            }
        } else {
            buttonAudio.setEnabled(false);
            buttonVideo.setEnabled(false);
            buttonAudio.setText(R.string.noAudio);
            buttonVideo.setText(R.string.noVideo);
        }
    }
}