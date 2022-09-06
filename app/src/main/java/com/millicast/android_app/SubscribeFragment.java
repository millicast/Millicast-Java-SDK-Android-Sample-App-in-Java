package com.millicast.android_app;

import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.millicast.AudioTrack;
import com.millicast.LayerData;
import com.millicast.VideoRenderer;
import com.millicast.VideoTrack;

import org.webrtc.RendererCommon;

import static android.media.AudioManager.MODE_IN_COMMUNICATION;
import static android.media.AudioManager.MODE_NORMAL;
import static com.millicast.android_app.MillicastManager.Source.CURRENT;
import static com.millicast.android_app.Utils.logD;
import static com.millicast.android_app.Utils.makeSnackbar;
import static com.millicast.android_app.Utils.populateSpinner;

import java.util.ArrayList;
import java.util.Optional;

/**
 * UI for subscribing.
 */
public class SubscribeFragment extends Fragment {
    public static final String TAG = "SubscribeFragment";

    private final MillicastManager mcManager;
    private LinearLayout linearLayoutVideo;
    private LinearLayout linearLayoutCon;
    private TextView textViewStream;
    private Switch switchDirection;
    private Button buttonSubscribe;
    private Button buttonAudio;
    private Button buttonVideo;
    private TextView textViewSourceAudio;
    private TextView textViewSourceVideo;
    private Spinner spinnerSourceAudio;
    private Spinner spinnerSourceVideo;
    private Spinner spinnerLayer;
    private Button buttonScale;

    private boolean ascending = true;
    private boolean conVisible = true;

    public SubscribeFragment() {
        this.mcManager = MillicastManager.getSingleInstance();

        // Set this view into listeners/handlers
        mcManager.setViewSub(this);

        // Set buttons again in case states changed while subListener had no access to this view.
        setUI();
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_subscribe, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        linearLayoutVideo = view.findViewById(R.id.linear_layout_video_sub);
        linearLayoutCon = view.findViewById(R.id.linear_layout_con_sub);
        textViewStream = view.findViewById(R.id.text_view_stream);

        textViewSourceAudio = view.findViewById(R.id.text_view_source_audio);
        textViewSourceVideo = view.findViewById(R.id.text_view_source_video);
        spinnerSourceAudio = view.findViewById(R.id.spinner_source_audio_sub);
        spinnerSourceVideo = view.findViewById(R.id.spinner_source_video_sub);
        spinnerLayer = view.findViewById(R.id.spinner_layer_sub);

        switchDirection = view.findViewById(R.id.switchDirectionSub);
        buttonSubscribe = view.findViewById(R.id.button_subscribe);
        buttonAudio = view.findViewById(R.id.button_audio_sub);
        buttonVideo = view.findViewById(R.id.button_video_sub);
        buttonScale = view.findViewById(R.id.button_scale_sub);

        // Set static button actions
        linearLayoutVideo.setOnClickListener(this::toggleCon);
        switchDirection.setOnCheckedChangeListener(this::toggleAscend);
        switchDirection.setChecked(ascending);
        buttonScale.setOnClickListener(this::toggleScale);

        // Set dynamic button actions
        buttonSubscribe.setOnClickListener(this::onStartSubscribeClicked);
        buttonAudio.setOnClickListener(this::toggleAudio);
        buttonVideo.setOnClickListener(this::toggleVideo);
        mcManager.loadViewSource(null, false);
        mcManager.loadViewLayer();
    }

    @Override
    public void onResume() {
        super.onResume();
        displaySubVideo();
        setUI();
    }

    @Override
    public void onPause() {
        setAudioMode(false);
        Utils.stopDisplayVideo(linearLayoutVideo, TAG);
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

    //**********************************************************************************************
    // Render
    //**********************************************************************************************

    /**
     * Mute or unmute audio by switching audio state to the opposite of the current state.
     *
     * @param view
     */
    private void toggleAudio(View view) {
        AudioTrack track = mcManager.getAudioTrackSub();
        if (track != null) {
            track.setEnabled(!mcManager.isAudioEnabledSub());
            mcManager.setAudioEnabledSub(!mcManager.isAudioEnabledSub());
            setUI();
        }
    }

    /**
     * Mute or unmute video by switching video state to the opposite of the current state.
     *
     * @param view
     */
    private void toggleVideo(View view) {
        VideoTrack track = mcManager.getVideoTrackSub();
        if (track != null) {
            track.setEnabled(!mcManager.isVideoEnabledSub());
            mcManager.setVideoEnabledSub(!mcManager.isVideoEnabledSub());
            setUI();
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
        String logTag = "[Scale][Toggle][Sub] ";
        String log = "";
        if (linearLayoutVideo != null) {
            RendererCommon.ScalingType type = mcManager.switchScaling(ascending, false);
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

    //**********************************************************************************************
    // Subscribe
    //**********************************************************************************************

    private void onStartSubscribeClicked(View view) {
        Log.d(TAG, "Start Subscribe clicked.");
        displaySubVideo();
        mcManager.connectSub();
        setUI();
    }

    private void displaySubVideo() {
        String logTag = "[displaySubVideo] ";

        // Display video if not already displayed.
        if (linearLayoutVideo.getChildCount() == 0) {
            // Get remote video renderer.
            VideoRenderer subRenderer = mcManager.getRendererSub();
            // Ensure our renderer is not attached to another parent view.
            Utils.removeFromParentView(subRenderer, TAG);
            // Finally, add our renderer to our frame layout.
            linearLayoutVideo.addView(subRenderer);
            logD(TAG, logTag + "Added renderer for display.");
        } else {
            logD(TAG, logTag + "Already displaying renderer.");
        }
    }

    private void onStopSubscribeClicked(View view) {
        Log.d(TAG, "Stop Subscribe clicked.");
        mcManager.stopSub();
        Utils.stopDisplayVideo(linearLayoutVideo, TAG);
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
     * When streaming starts, set isInCom to true, else set to false.
     *
     * @param isInCom
     */
    private void setAudioMode(boolean isInCom) {
        AudioManager audioManager = (AudioManager) mcManager.getMainActivity().getSystemService(Context.AUDIO_SERVICE);
        int mode = MODE_IN_COMMUNICATION;
        if (!isInCom) {
            mode = MODE_NORMAL;
            audioManager.setSpeakerphoneOn(false);
        } else {
            audioManager.setSpeakerphoneOn(true);
        }
        audioManager.setMode(mode);
    }

    /**
     * Load {@link #spinnerSourceAudio} or {@link #spinnerSourceVideo} with current sources.
     * Set selection to the current selected source, if possible.
     * The default/main source (the latest one published without a sourceId) if present,
     * would be represented by a blank selection.
     *
     * @param spinnerList
     * @param lambda
     * @param isAudio
     * @param changed
     */
    void loadSourceSpinner(ArrayList<String> spinnerList, Utils.GetSelectedIndex lambda, boolean isAudio, boolean changed) {
        String logTagLoad = "[View][Source][Id][Spinner][Load]";
        String logLoad;
        String currentSource;
        SourceInfo source;
        Spinner spinner;
        TextView textView;
        int label;
        if (isAudio) {
            logTagLoad += "[Audio] ";
            currentSource = mcManager.getSourceIdAudioSub();
            source = mcManager.getSourceProjected(true);
            spinner = spinnerSourceAudio;
            textView = textViewSourceAudio;
            label = R.string.source_audio;
        } else {
            logTagLoad += "[Video] ";
            currentSource = mcManager.getSourceIdVideoSub();
            source = mcManager.getSourceProjected(false);
            spinner = spinnerSourceVideo;
            textView = textViewSourceVideo;
            label = R.string.source_video;
        }

        logLoad = "Current Source:" + currentSource + " " + source + ".";
        logD(TAG, logTagLoad + logLoad);

        // Set Spinner labels to reflect new source list.
        if (changed) {
            if (isAudio) {
                textViewSourceAudio.setText(R.string.source_audio_changed);
            } else {
                textViewSourceVideo.setText(R.string.source_video_changed);
            }
        }

        // Set touch listener.
        spinner.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                textView.setText(label);
                return false;
            }
        });

        populateSpinner(spinnerList, spinner, lambda, mcManager.getContext(), (int position) -> {
            String logTagSet = "[View][Source][Id][Spinner][Set]";
            String logSet;
            String sourceId = spinnerList.get(position);
            if (!isAudio) {
                logTagSet += "[Video] ";
            } else {
                logTagSet = "[Audio] ";
            }
            logSet = "Selected Pos: " + position + " Source: " + sourceId + ".";
            logD(TAG, logTagSet + logSet);

            // Select the source and if not possible
            if (!mcManager.projectSource(sourceId, isAudio)) {

                // Reset to currently selected source if possible.
                logSet = "Failed! Resetting to currently selected source...";
                logD(TAG, logTagSet + logSet);

                int previousIndex = lambda.getSelectedIndex(spinnerList);
                if (previousIndex >= 0) {
                    String resetSelection = spinnerList.get(previousIndex);
                    spinner.setSelection(previousIndex);
                    logSet = "OK. Spinner reset to:" + resetSelection + ".";
                    logD(TAG, logTagSet + logSet);
                    return;
                }

                // Otherwise reset to automatic selection.
                logSet = "Failed! Resetting to automatic selection...";
                logD(TAG, logTagSet + logSet);

                String resetSelection = "";
                mcManager.projectSource("", isAudio);
                int resetIndex = spinnerList.indexOf(resetSelection);
                if (resetIndex > 0) {
                    spinner.setSelection(resetIndex);
                    logSet = "OK. Spinner reset to index:" + resetSelection + ".";
                } else {
                    logSet = "Failed! Unable to find index of resetSelection(" + resetSelection +
                            ") as it is not in the current spinnerList:" + spinnerList + ".";
                }
                logD(TAG, logTagSet + logSet);
            } else {
                logSet = "OK.";
                logD(TAG, logTagSet + logSet);
            }
        });
    }

    /**
     * Load {@link #spinnerLayer} with the current video source's layers.
     * Set initial selection to the current selected layerId, if possible.
     * Automatic layer selection by Millicast would be represented by a blank selection.
     *
     * @param spinnerList
     * @param lambda
     */
    void loadLayerSpinner(ArrayList<String> spinnerList, Utils.GetSelectedIndex lambda) {
        String logTagLoad = "[View][Layer][Spinner][Load] ";
        String logLoad;
        final Spinner spinner = spinnerLayer;

        Optional<LayerData> currentLayer = mcManager.getLayerData();
        logLoad = "Current Source:" + mcManager.getSourceIdVideoSub() + " Layer:" +
                SourceInfo.getLayerStr(currentLayer, true) + ".";
        logD(TAG, logTagLoad + logLoad);

        populateSpinner(spinnerList, spinner, lambda, mcManager.getContext(), (int position) -> {
            String logTagSet = "[View][Layer][Spinner][Set] ";
            String logSet;
            String layerId = spinnerList.get(position);
            logSet = "Selected Pos: " + position + " LayerId:" + layerId + ".";
            logD(TAG, logTagSet + logSet);

            // Select the layer and if not possible
            if (!mcManager.selectLayer(layerId)) {

                // Reset to currently selected layerId if possible.
                logSet = "Failed! Resetting to currently selected layerId...";
                logD(TAG, logTagSet + logSet);

                int previousIndex = lambda.getSelectedIndex(spinnerList);
                if (previousIndex >= 0) {
                    String resetSelection = spinnerList.get(previousIndex);
                    spinner.setSelection(previousIndex);
                    logSet = "OK. Spinner reset to:" + resetSelection + ".";
                    logD(TAG, logTagSet + logSet);
                    return;
                }

                // Otherwise reset to automatic selection.
                logSet = "Failed! Resetting to automatic selection...";
                logD(TAG, logTagSet + logSet);

                String resetLayerId = "";
                mcManager.selectLayer("");
                int resetIndex = spinnerList.indexOf(resetLayerId);
                if (resetIndex > 0) {
                    spinner.setSelection(resetIndex);
                    logSet = "OK. Spinner reset to index:" + resetLayerId + ".";
                } else {
                    logSet = "Failed! Unable to find index of resetLayerId(" + resetLayerId +
                            ") as it is not in the current spinnerList:" + spinnerList + ".";
                }
                logD(TAG, logTagSet + logSet);
            } else {
                logSet = "OK.";
                logD(TAG, logTagSet + logSet);
            }
        });
    }


    /**
     * Set the state of UIs, including subscribe button,
     * based on current subscribe state.
     * Must be run on UI thread.
     */
    void setUI() {

        if (this.getView() == null) {
            return;
        }

        displayCon();

        textViewStream.setText("Account: " + mcManager.getAccountId(CURRENT) +
                " Stream: " + mcManager.getStreamNameSub(CURRENT));
        buttonScale.setText(mcManager.getScalingName(false));

        switch (mcManager.getSubState()) {
            case DISCONNECTED:
                setAudioMode(false);
                mcManager.loadViewSource(null, false);
                mcManager.loadViewLayer();
                buttonSubscribe.setText(R.string.startSubscribe);
                buttonSubscribe.setOnClickListener(this::onStartSubscribeClicked);
                buttonSubscribe.setEnabled(true);
                buttonAudio.setEnabled(false);
                buttonVideo.setEnabled(false);
                buttonAudio.setText(R.string.noAudio);
                buttonVideo.setText(R.string.noVideo);
                break;
            case CONNECTING:
            case CONNECTED:
                buttonSubscribe.setText(R.string.trySubscribe);
                buttonSubscribe.setEnabled(false);
                buttonAudio.setEnabled(false);
                buttonVideo.setEnabled(false);
                buttonAudio.setText(R.string.noAudio);
                buttonVideo.setText(R.string.noVideo);
                break;
            case SUBSCRIBING:
                setAudioMode(true);
                buttonSubscribe.setText(R.string.stopSubscribe);
                buttonSubscribe.setOnClickListener(this::onStopSubscribeClicked);
                buttonSubscribe.setEnabled(true);
                buttonAudio.setEnabled(true);
                buttonVideo.setEnabled(true);
                if (mcManager.isAudioEnabledSub()) {
                    buttonAudio.setText(R.string.muteAudio);
                } else {
                    buttonAudio.setText(R.string.unmuteAudio);
                }
                if (mcManager.isVideoEnabledSub()) {
                    buttonVideo.setText(R.string.muteVideo);
                } else {
                    buttonVideo.setText(R.string.unmuteVideo);
                }
                break;
        }
    }
}