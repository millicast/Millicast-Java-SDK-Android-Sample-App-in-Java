package com.millicast.android_app;

import static com.millicast.android_app.MCStates.CaptureState.REFRESH_SOURCE;
import static com.millicast.android_app.Utils.logD;
import static com.millicast.android_app.Utils.populateSpinner;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.millicast.AudioSource;
import com.millicast.VideoCapabilities;
import com.millicast.VideoSource;

import java.util.ArrayList;

/**
 * UI for Media Settings.
 */
public class SettingsMediaFragment extends Fragment {
    public static final String TAG = "SetMediaFragment";
    private final MillicastManager mcMan;
    private Switch switchAudioOnly;
    private Switch switchNdiAudioOutput;
    private Switch switchNdiVideoOutput;
    private Button buttonRefresh;
    private Spinner spinnerAudioSource;
    private Spinner spinnerVideoSource;
    private Spinner spinnerCapability;
    private Spinner spinnerAudioCodec;
    private Spinner spinnerVideoCodec;
    private Spinner spinnerAudioPlayback;

    public SettingsMediaFragment() {
        mcMan = MillicastManager.getSingleInstance();
        // Set this view into listeners/handlers
        mcMan.setViewSetMedia(this);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            // Will only lock if it's the first time.
            mcMan.setCameraLock(true);
        }
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_set_media, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState == null) {
            // Will only lock if it's the first time.
            mcMan.setCameraLock(true);
        }

        // Initialize views
        switchAudioOnly = view.findViewById(R.id.switch_audio_only_setmedia);
        switchNdiAudioOutput = view.findViewById(R.id.ndi_output_audio);
        switchNdiVideoOutput = view.findViewById(R.id.ndi_output_video);
        buttonRefresh = view.findViewById(R.id.button_refresh_set);
        spinnerAudioSource = view.findViewById(R.id.audio_source_list);
        spinnerVideoSource = view.findViewById(R.id.video_source_list);
        spinnerCapability = view.findViewById(R.id.capability_list);
        spinnerAudioCodec = view.findViewById(R.id.audio_codec_list);
        spinnerVideoCodec = view.findViewById(R.id.video_codec_list);
        spinnerAudioPlayback = view.findViewById(R.id.audio_playback_list);

        // Set actions
        // Populate Audio Source Spinner.
        populateSpinner(mcMan.getAudioSourceList(), spinnerAudioSource,
                mcMan.getAudioSourceIndex(), getContext(), (int position) -> {
                    String logTag = "[Spinner][Audio] ";
                    int index = mcMan.getAudioSourceIndex();
                    int size = spinnerAudioSource.getAdapter().getCount();
                    if (position == index) {
                        logD(TAG, logTag + "Not setting as index is the same.");
                        return;
                    }
                    if (REFRESH_SOURCE == mcMan.getCapState()) {
                        logD(TAG, logTag + "Not setting as sources are refreshing.");
                        return;
                    }
                    logD(TAG, logTag + "Setting at position:" + position + "...");
                    if (!mcMan.setAudioSourceIndex(position)) {
                        if (index >= size) {
                            logD(TAG, logTag + "Unable to set index as it is beyond the list!");
                        } else {
                            spinnerAudioSource.setSelection(index);
                        }
                        Utils.makeSnackbar(logTag, "AudioSource cannot be changed now!", this);
                    } else {
                        logD(TAG, logTag + "OK.");
                    }
                });

        // Populate Video Source Spinner.
        populateSpinner(mcMan.getVideoSourceList(), spinnerVideoSource,
                mcMan.getVideoSourceIndex(), getContext(), (int position) -> {
                    String logTag = "[Spinner][Video] ";
                    int indexVideo = mcMan.getVideoSourceIndex();
                    int sizeVideo = spinnerVideoSource.getAdapter().getCount();
                    if (position == indexVideo) {
                        logD(TAG, logTag + "Not setting as index is the same.");
                        return;
                    }
                    if (REFRESH_SOURCE == mcMan.getCapState()) {
                        logD(TAG, logTag + "Not setting as sources are refreshing.");
                        return;
                    }
                    logD(TAG, logTag + "Setting at position:" + position + "...");
                    String error = mcMan.setVideoSourceIndex(position, true);
                    // Set a new list and selection for Capability spinner
                    if (error == null) {
                        logD(TAG, logTag + "OK. Now reloading Capability spinner.");
                        spinnerCapability.setAdapter(new ArrayAdapter<>(getContext(),
                                android.R.layout.simple_spinner_item,
                                mcMan.getCapabilityList()));
                        int indexCap = mcMan.getCapabilityIndex();
                        int sizeCap = spinnerCapability.getAdapter().getCount();
                        if (indexCap >= sizeCap) {
                            logD(TAG, logTag + "Unable to set index as it is beyond the list!");
                        } else {
                            spinnerCapability.setSelection(indexCap);
                        }
                    } else {
                        if (indexVideo >= sizeVideo) {
                            logD(TAG, logTag + "Unable to set index as it is beyond the list!");
                        } else {
                            spinnerVideoSource.setSelection(indexVideo);
                        }

                        Utils.makeSnackbar(logTag, error, this);
                    }
                });

        // Populate Video Capability Spinner.
        populateSpinner(mcMan.getCapabilityList(), spinnerCapability,
                mcMan.getCapabilityIndex(), getContext(), (int position) -> {
                    String logTag = "[Spinner][Capability] ";
                    if (REFRESH_SOURCE == mcMan.getCapState()) {
                        logD(TAG, logTag + "Not setting as sources are refreshing.");
                        return;
                    }
                    logD(TAG, logTag + "Setting at position:" + position + ".");
                    mcMan.setCapabilityIndex(position);
                });

        // Populate Audio Codec Spinner.
        populateSpinner(mcMan.getCodecList(true), spinnerAudioCodec,
                mcMan.getAudioCodecIndex(), getContext(), (int position) -> {
                    String logTag = "[Spinner][Codec][Audio] ";
                    int index = mcMan.getAudioCodecIndex();
                    int size = spinnerAudioCodec.getAdapter().getCount();
                    if (position == index) {
                        return;
                    }
                    logD(TAG, logTag + "Setting at position:" + position + "...");
                    if (!mcMan.setCodecIndex(position, true)) {
                        if (index >= size) {
                            logD(TAG, logTag + "Unable to set index as it is beyond the list!");
                        } else {
                            spinnerAudioCodec.setSelection(index);
                        }
                        Utils.makeSnackbar(logTag, "AudioCodec cannot be changed now!", this);
                    } else {
                        logD(TAG, logTag + "OK.");
                    }
                });

        // Populate Video Codec Spinner.
        populateSpinner(mcMan.getCodecList(false), spinnerVideoCodec,
                mcMan.getVideoCodecIndex(), getContext(), (int position) -> {
                    String logTag = "[Spinner][Codec][Video] ";
                    int index = mcMan.getVideoCodecIndex();
                    int size = spinnerVideoCodec.getAdapter().getCount();
                    if (position == index) {
                        return;
                    }
                    logD(TAG, logTag + "Setting at position:" + position + "...");
                    if (!mcMan.setCodecIndex(position, false)) {
                        if (index >= size) {
                            logD(TAG, logTag + "Unable to set index as it is beyond the list!");
                        } else {
                            spinnerVideoCodec.setSelection(index);
                        }
                        Utils.makeSnackbar(logTag, "VideoCodec cannot be changed now!", this);
                    } else {
                        logD(TAG, logTag + "OK.");
                    }
                });

        // Populate Audio Playback Spinner.
        populateSpinner(mcMan.getAudioPlaybackList(), spinnerAudioPlayback,
                mcMan.getAudioPlaybackIndex(), getContext(), (int position) -> {
                    String logTag = "[Spinner][Playback][Audio] ";
                    int index = mcMan.getAudioPlaybackIndex();
                    int size = spinnerAudioPlayback.getAdapter().getCount();
                    if (position == index) {
                        return;
                    }
                    if (REFRESH_SOURCE == mcMan.getCapState()) {
                        logD(TAG, logTag + "Not setting as sources are refreshing.");
                        return;
                    }
                    logD(TAG, logTag + "Setting at position:" + position + "...");
                    if (!mcMan.setAudioPlaybackIndex(position)) {
                        if (index >= size) {
                            logD(TAG, logTag + "Unable to set index as it is beyond the list!");
                        } else {
                            spinnerAudioPlayback.setSelection(index);
                        }
                        Utils.makeSnackbar(logTag, "AudioPlayback cannot be changed now!", this);
                    } else {
                        logD(TAG, logTag + "OK.");
                    }
                });

        /**
         * Warning: This feature is not functional yet. See warning for
         * {@link MillicastManager#enableNdiOutput} for more details.
         */
        switchNdiAudioOutput.setChecked(mcMan.isNdiOutputEnabled(true));
        switchNdiAudioOutput.setOnClickListener((viewSwitch -> {
            String logTag = "[Switch][Ndi] ";
            Switch switchNdi = (Switch) viewSwitch;
            boolean checked = switchNdi.isChecked();
            if (checked != mcMan.isNdiOutputEnabled(true)) {
                mcMan.enableNdiOutput(checked, true, null);
                switchNdi.setChecked(mcMan.isNdiOutputEnabled(true));
                Utils.makeSnackbar(logTag, "WARNING: This feature does not work yet!\nPlease see MillicastManager.enableNdiOutput for more details.", this);
            }
        }));

        switchNdiVideoOutput.setChecked(mcMan.isNdiOutputEnabled(false));
        switchNdiVideoOutput.setOnClickListener((viewSwitch -> {
            Switch switchNdi = (Switch) viewSwitch;
            boolean checked = switchNdi.isChecked();
            mcMan.enableNdiOutput(checked, false, null);
            switchNdi.setChecked(mcMan.isNdiOutputEnabled(false));
        }));

        switchAudioOnly.setOnCheckedChangeListener(this::toggleAudioOnly);
        switchAudioOnly.setChecked(mcMan.isAudioOnly());
        buttonRefresh.setOnClickListener(this::refreshMedia);
    }

    @Override
    public void onResume() {
        super.onResume();
        setUI();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    /**
     * Switch the AudioOnly mode.
     *
     * @param buttonView
     * @param isChecked  Specifies if AudioOnly mode is true (on) or false (off).
     */
    private void toggleAudioOnly(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            buttonView.setText(R.string.audioOnlyT);
            mcMan.setAudioOnly(true);
        } else {
            buttonView.setText(R.string.audioOnlyF);
            mcMan.setAudioOnly(false);
        }
        setUI();
    }

    /**
     * Refresh the currently available lists of audio and video sources.
     * This can only be done if audio and video are not currently captured.
     *
     * @param view
     */
    private void refreshMedia(View view) {
        String logTag = "[Refresh][Reload][Media] ";
        if (!mcMan.refreshMediaSourceLists()) {
            Utils.makeSnackbar(logTag, "Audio and video sources cannot be refreshed now!", this);
            return;
        }
        Utils.makeSnackbar(logTag, "Audio and video sources refreshing now...", this);
        setUI();
    }

    public void setUI() {
        String logTag = "[UI][Set] ";
        if (this.getView() == null) {
            return;
        }

        if (mcMan.isAudioOnly()) {
            switchAudioOnly.setText(R.string.audioOnlyT);
        } else {
            switchAudioOnly.setText(R.string.audioOnlyF);
        }

        spinnerAudioSource.setEnabled(true);
        if (mcMan.isAudioOnly()) {
            spinnerVideoSource.setEnabled(false);
            spinnerCapability.setEnabled(false);
        } else {
            spinnerVideoSource.setEnabled(true);
            spinnerCapability.setEnabled(true);
        }

        // Reload Media sources.
        if (REFRESH_SOURCE != mcMan.getCapState()) {
            logD(TAG, logTag + "Reloading media sources.");
            reloadSpinnerAudioSource();
            reloadSpinnerVideoSource();
            reloadSpinnerCapabilityList();
        } else {
            logD(TAG, logTag + "Not reloading media sources as they are refreshing.");
        }

        switch (mcMan.getCapState()) {
            case NOT_CAPTURED:
                buttonRefresh.setEnabled(true);
                buttonRefresh.setText(R.string.refreshSources);
                switchAudioOnly.setEnabled(true);
                break;
            case TRY_CAPTURE:
            case IS_CAPTURED:
                buttonRefresh.setEnabled(false);
                buttonRefresh.setText(R.string.refreshSources);
                switchAudioOnly.setEnabled(false);
                break;
            case REFRESH_SOURCE:
                spinnerAudioSource.setEnabled(false);
                spinnerVideoSource.setEnabled(false);
                spinnerCapability.setEnabled(false);
                buttonRefresh.setEnabled(false);
                buttonRefresh.setText(R.string.refreshingSources);
                switchAudioOnly.setEnabled(false);
                break;
        }
    }

    /**
     * Reload the audio source list and reset selection based on current index.
     */
    private void reloadSpinnerAudioSource() {
        String logTag = "[Reload][Spinner][Audio] ";
        ArrayList<AudioSource> list = mcMan.getAudioSourceList();
        int index = mcMan.getAudioSourceIndex();
        spinnerAudioSource.setAdapter(new ArrayAdapter<>(mcMan.getContext(),
                android.R.layout.simple_spinner_item,
                list));
        int size = spinnerAudioSource.getAdapter().getCount();
        if (index >= size) {
            logD(TAG, logTag + "Unable to set index as it is beyond the list!");
            return;
        }
        spinnerAudioSource.setSelection(index);
        logD(TAG, logTag + "Audio sources reloaded.");
    }

    /**
     * Reload the video source list and reset selection based on current index.
     */
    private void reloadSpinnerVideoSource() {
        String logTag = "[Reload][Spinner][Video] ";
        ArrayList<VideoSource> list = mcMan.getVideoSourceList();
        int index = mcMan.getVideoSourceIndex();
        spinnerVideoSource.setAdapter(new ArrayAdapter<>(mcMan.getContext(),
                android.R.layout.simple_spinner_item,
                list));
        int size = spinnerVideoSource.getAdapter().getCount();
        if (index >= size) {
            logD(TAG, logTag + "Unable to set index as it is beyond the list!");
            return;
        }
        spinnerVideoSource.setSelection(index);
        logD(TAG, logTag + "Video sources reloaded.");
    }

    /**
     * Reload the Capability list and reset selection based on current index.
     */
    private void reloadSpinnerCapabilityList() {
        String logTag = "[Reload][Spinner][Cap] ";
        ArrayList<VideoCapabilities> list = mcMan.getCapabilityList();
        int index = mcMan.getCapabilityIndex();
        spinnerCapability.setAdapter(new ArrayAdapter<>(mcMan.getContext(),
                android.R.layout.simple_spinner_item,
                list));
        int size = spinnerCapability.getAdapter().getCount();
        if (index >= size) {
            logD(TAG, logTag + "Unable to set index as it is beyond the list!");
            return;
        }
        spinnerCapability.setSelection(index);
        logD(TAG, logTag + "Capabilities reloaded.");
    }
}