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

import com.millicast.devices.track.AudioTrack;
import com.millicast.VideoRenderer;
import com.millicast.devices.track.VideoTrack;
import com.millicast.subscribers.state.LayerData;

import org.webrtc.RendererCommon;

import static android.media.AudioManager.MODE_IN_COMMUNICATION;
import static android.media.AudioManager.MODE_NORMAL;
import static com.millicast.android_app.MCTypes.Source.CURRENT;
import static com.millicast.android_app.Utils.logD;
import static com.millicast.android_app.Utils.makeSnackbar;
import static com.millicast.android_app.Utils.populateSpinner;

import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * UI for subscribing.
 */
public class SubscribeFragment extends Fragment {
    public static final String TAG = "SubscribeFragment";

    private final MillicastManager mcMan;
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
        this.mcMan = MillicastManager.getSingleInstance();

        // Set this view into listeners/handlers
        mcMan.setViewSub(this);

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
        mcMan.loadViewSource(null, false);
        mcMan.loadViewSubLayer();
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
        AudioTrack track = mcMan.getAudioTrackSub();
        if (track != null) {
            track.setEnabled(!mcMan.isAudioEnabledSub());
            mcMan.setAudioEnabledSub(!mcMan.isAudioEnabledSub());
            setUI();
        }
    }

    /**
     * Mute or unmute video by switching video state to the opposite of the current state.
     *
     * @param view
     */
    private void toggleVideo(View view) {
        VideoTrack track = mcMan.getVideoTrackSub();
        if (track != null) {
            track.setEnabled(!mcMan.isVideoEnabledSub());
            mcMan.setVideoEnabledSub(!mcMan.isVideoEnabledSub());
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
            RendererCommon.ScalingType type = mcMan.switchScaling(ascending, false);
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
        mcMan.connectSub();
        setUI();
    }

    private void displaySubVideo() {
        String logTag = "[displaySubVideo] ";

        // Display video if not already displayed.
        if (linearLayoutVideo.getChildCount() == 0) {
            // Get remote video renderer.
            VideoRenderer subRenderer = mcMan.getRendererSub();
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
        mcMan.stopSub();
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
        mcMan.applyScaling(false);
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
        AudioManager audioManager = (AudioManager) mcMan.getMainActivity().getSystemService(Context.AUDIO_SERVICE);
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
            currentSource = mcMan.getSourceIdAudioSub();
            source = mcMan.getSourceProjected(true);
            spinner = spinnerSourceAudio;
            textView = textViewSourceAudio;
            label = R.string.source_audio;
        } else {
            logTagLoad += "[Video] ";
            currentSource = mcMan.getSourceIdVideoSub();
            source = mcMan.getSourceProjected(false);
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

        populateSpinner(spinnerList, spinner, lambda, mcMan.getContext(), (int position) -> {
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
            if (!mcMan.projectSource(sourceId, isAudio)) {

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
                mcMan.projectSource("", isAudio);
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

        Optional<LayerData> currentLayer = mcMan.getLayerData();
        logLoad = "Current Source:" + mcMan.getSourceIdVideoSub() + " Layer:" +
                SourceInfo.getLayerStr(currentLayer, true) + ".";
        logD(TAG, logTagLoad + logLoad);

        populateSpinner(spinnerList, spinner, lambda, mcMan.getContext(), (int position) -> {
            String logTagSet = "[View][Layer][Spinner][Set] ";
            AtomicReference<String> logSet = new AtomicReference<>();
            String layerId = spinnerList.get(position);
            logSet.set("Selected Pos: " + position + " LayerId:" + layerId + ".");
            logD(TAG, logTagSet + logSet);

            // Select the layer and if not possible
            mcMan.selectLayer(layerId)
                    .then(success -> {
                        logD(TAG, logTagSet + "OK");
                    })
                    .error(e -> {
                        // Reset to currently selected layerId if possible.
                        logSet.set("Failed! Resetting to currently selected layerId...");
                        logD(TAG, logTagSet + logSet);

                        int previousIndex = lambda.getSelectedIndex(spinnerList);
                        if (previousIndex >= 0) {
                            String resetSelection = spinnerList.get(previousIndex);
                            spinner.setSelection(previousIndex);
                            logSet.set("OK. Spinner reset to:" + resetSelection + ".");
                            logD(TAG, logTagSet + logSet);
                            return;
                        }

                        // Otherwise reset to automatic selection.
                        logSet.set("Failed! Resetting to automatic selection...");
                        logD(TAG, logTagSet + logSet);

                        String resetLayerId = "";
                        mcMan.selectLayer("");
                        int resetIndex = spinnerList.indexOf(resetLayerId);
                        if (resetIndex > 0) {
                            spinner.setSelection(resetIndex);
                            logSet.set("OK. Spinner reset to index:" + resetLayerId + ".");
                        } else {
                            logSet.set("Failed! Unable to find index of resetLayerId(" + resetLayerId +
                                    ") as it is not in the current spinnerList:" + spinnerList + ".");
                        }
                        logD(TAG, logTagSet + logSet);
                    });
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

        textViewStream.setText("Account: " + mcMan.getAccountId(CURRENT) +
                " Stream: " + mcMan.getStreamNameSub(CURRENT));
        buttonScale.setText(mcMan.getScalingName(false));

        switch (mcMan.getSubState()) {
            case DISCONNECTED:
                setAudioMode(false);
                mcMan.loadViewSource(null, false);
                mcMan.loadViewSubLayer();
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
                if (mcMan.isAudioEnabledSub()) {
                    buttonAudio.setText(R.string.muteAudio);
                } else {
                    buttonAudio.setText(R.string.unmuteAudio);
                }
                if (mcMan.isVideoEnabledSub()) {
                    buttonVideo.setText(R.string.muteVideo);
                } else {
                    buttonVideo.setText(R.string.unmuteVideo);
                }
                break;
        }
    }
}