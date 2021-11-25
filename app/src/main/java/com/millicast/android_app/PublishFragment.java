package com.millicast.android_app;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.millicast.AudioTrack;
import com.millicast.VideoRenderer;
import com.millicast.VideoTrack;

import static com.millicast.android_app.MillicastManager.Source.CURRENT;
import static com.millicast.android_app.Utils.logD;

public class PublishFragment extends Fragment {
    public static final String TAG = "PublishFragment";

    private final MillicastManager mcManager;
    private FrameLayout frameLayout;
    private TextView textView;
    private Switch switchDirection;
    private Button buttonCamera;
    private Button buttonResolution;
    private Button buttonAudioCodec;
    private Button buttonVideoCodec;
    private Button buttonPublish;
    private Button buttonCapture;
    private Button buttonAudio;
    private Button buttonVideo;

    private boolean ascending = true;

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

        frameLayout = view.findViewById(R.id.frame_layout_pub);
        textView = view.findViewById(R.id.textViewPub);

        switchDirection = view.findViewById(R.id.switchDirectionPub);
        buttonCamera = view.findViewById(R.id.buttonCamera);
        buttonResolution = view.findViewById(R.id.buttonResolution);
        buttonAudioCodec = view.findViewById(R.id.buttonCodecAudio);
        buttonVideoCodec = view.findViewById(R.id.buttonCodecVideo);
        buttonPublish = view.findViewById(R.id.buttonPublish);
        buttonCapture = view.findViewById(R.id.buttonCapture);
        buttonAudio = view.findViewById(R.id.button_audio_pub);
        buttonVideo = view.findViewById(R.id.button_video_pub);

        // Set static button actions
        switchDirection.setOnCheckedChangeListener(this::toggleAscend);
        switchDirection.setChecked(ascending);
        buttonCamera.setOnClickListener(this::toggleCamera);
        buttonResolution.setOnClickListener(this::toggleResolution);
        buttonAudioCodec.setOnClickListener(viewButton -> toggleCodec(viewButton, true));
        buttonVideoCodec.setOnClickListener(viewButton -> toggleCodec(viewButton, false));
        buttonAudio.setOnClickListener(this::toggleAudio);
        buttonVideo.setOnClickListener(this::toggleVideo);

        if (savedInstanceState == null) {
            // Will only lock if it's the first time.
            mcManager.setCameraLock(true);
            if (mcManager.isRicohTheta(CURRENT)) {
                mcManager.overrideBWE(40000000);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        displayPubVideo();
        setUI();
    }

    @Override
    public void onPause() {
        Utils.stopDisplayVideo(frameLayout, TAG);
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    void onStartCaptureClicked(View captureButton) {
        Log.d(TAG, "Start Capture clicked.");
        mcManager.startAudioVideoCapture();
        Log.d(TAG, "Started media capture.");
        displayPubVideo();
        Log.d(TAG, "Started display video.");
        setUI();
    }

    private void displayPubVideo() {
        String tag = "[displayPubVideo] ";
        // Display video if not already displayed.
        if (frameLayout.getChildCount() == 0) {
            VideoRenderer pubRenderer = mcManager.getPubRenderer();
            // Ensure our renderer is not attached to another parent view.
            Utils.removeFromParentView(pubRenderer, TAG);
            // Finally, add our renderer to our frame layout.
            frameLayout.addView(pubRenderer);
            logD(TAG, tag + "Added renderer for display.");
        } else {
            logD(TAG, tag + "Already displaying renderer.");
        }
    }

    private void onStopCaptureClicked(View view) {
        Log.d(TAG, "Stop Capture clicked.");
        mcManager.stopAudioVideoCapture();
        Log.d(TAG, "Stopped media capture.");
        Utils.stopDisplayVideo(frameLayout, TAG);
        setUI();
    }

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
     * Switch camera to the next one in the list of VideoSource.
     *
     * @param view
     */
    private void toggleCamera(View view) {
        try {
            mcManager.switchVideoSource(ascending);
        } catch (IllegalStateException e) {
            logD(TAG, "[toggleCamera] Only switched selected camera. " +
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
        try {
            mcManager.switchCapability(ascending);
        } catch (IllegalStateException e) {
            logD(TAG, "[toggle][Resolution] Failed! Error:" + e + "");
        }
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
     * Set the states of UIs, including capture and publish buttons,
     * based on current capture and publish states.
     * Capture state:
     * - Cannot change when publishing.
     * Publish state:
     * - Must first capture and connect before publish.
     * Must be run on UI thread.
     */
    void setUI() {

        if (buttonCapture == null || buttonPublish == null ||
                buttonCamera == null || buttonResolution == null || buttonAudioCodec == null || buttonVideoCodec == null) {
            return;
        }

        textView.setText("Token: " + mcManager.getPublishingToken(CURRENT) +
                " Stream: " + mcManager.getStreamNamePub(CURRENT));
        buttonCamera.setText(mcManager.getVideoSourceName());
        buttonResolution.setText(mcManager.getCapabilityName());
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