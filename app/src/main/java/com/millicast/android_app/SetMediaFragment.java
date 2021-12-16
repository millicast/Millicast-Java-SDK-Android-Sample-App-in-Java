package com.millicast.android_app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 */
public class SetMediaFragment extends Fragment {
    public static final String TAG = "SetMediaFragment";
    private final MillicastManager mcManager;
    private Button buttonRefresh;
    private Spinner spinnerCapability;
    private Spinner spinnerVideoSource;
    private Spinner spinnerAudioSource;

    public SetMediaFragment() {
        mcManager = MillicastManager.getSingleInstance();
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
            mcManager.setCameraLock(true);
        }
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_set_media, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String logTag = "[Setting][Spinner] ";

        if (savedInstanceState == null) {
            // Will only lock if it's the first time.
            mcManager.setCameraLock(true);
        }

        buttonRefresh = view.findViewById(R.id.button_refresh_set);
        buttonRefresh.setOnClickListener(this::refreshMedia);

        spinnerAudioSource = view.findViewById(R.id.audio_source_list);
        populateSpinner(mcManager.getAudioSourceList(false), spinnerAudioSource,
                mcManager.getAudioSourceIndex(), (int position) -> {
                    if (position == mcManager.getAudioSourceIndex()) {
                        return;
                    }
                    if (!mcManager.setAudioSourceIndex(position)) {
                        spinnerAudioSource.setSelection(mcManager.getAudioSourceIndex());
                        Utils.makeSnackbar(logTag, "AudioSource cannot be changed now!", this);
                    }
                });

        spinnerVideoSource = view.findViewById(R.id.video_source_list);
        populateSpinner(mcManager.getVideoSourceList(false), spinnerVideoSource,
                mcManager.getVideoSourceIndex(), (int position) -> {
                    if (position == mcManager.getVideoSourceIndex()) {
                        return;
                    }
                    String error = mcManager.setVideoSourceIndex(position, true);
                    if (error == null) {
                        spinnerCapability.setAdapter(new ArrayAdapter<>(getContext(),
                                android.R.layout.simple_spinner_item,
                                mcManager.getCapabilityList()));
                        spinnerCapability.setSelection(mcManager.getCapabilityIndex());
                    } else {
                        spinnerVideoSource.setSelection(mcManager.getVideoSourceIndex());
                        Utils.makeSnackbar(logTag, error, this);
                    }
                });

        spinnerCapability = view.findViewById(R.id.capability_list);
        populateSpinner(mcManager.getCapabilityList(), spinnerCapability,
                mcManager.getCapabilityIndex(), (int position) -> {
                    mcManager.setCapabilityIndex(position);
                });

        Spinner spinnerAudioCodec = view.findViewById(R.id.audio_codec_list);
        populateSpinner(mcManager.getCodecList(true), spinnerAudioCodec,
                mcManager.getAudioCodecIndex(), (int position) -> {
                    if (position == mcManager.getAudioCodecIndex()) {
                        return;
                    }
                    if (!mcManager.setCodecIndex(position, true)) {
                        spinnerAudioCodec.setSelection(mcManager.getAudioCodecIndex());
                        Utils.makeSnackbar(logTag, "AudioCodec cannot be changed now!", this);
                    }
                });

        Spinner spinnerVideoCodec = view.findViewById(R.id.video_codec_list);
        populateSpinner(mcManager.getCodecList(false), spinnerVideoCodec,
                mcManager.getVideoCodecIndex(), (int position) -> {
                    if (position == mcManager.getVideoCodecIndex()) {
                        return;
                    }
                    if (!mcManager.setCodecIndex(position, false)) {
                        spinnerVideoCodec.setSelection(mcManager.getVideoCodecIndex());
                        Utils.makeSnackbar(logTag, "VideoCodec cannot be changed now!", this);
                    }
                });

        Spinner spinnerAudioPlayback = view.findViewById(R.id.audio_playback_list);
        populateSpinner(mcManager.getAudioPlaybackList(), spinnerAudioPlayback,
                mcManager.getAudioPlaybackIndex(), (int position) -> {
                    if (position == mcManager.getAudioPlaybackIndex()) {
                        return;
                    }
                    if (!mcManager.setAudioPlaybackIndex(position)) {
                        spinnerAudioPlayback.setSelection(mcManager.getAudioPlaybackIndex());
                        Utils.makeSnackbar(logTag, "AudioPlayback cannot be changed now!", this);
                    }
                });

        /**
         * Warning: This feature is not functional yet. See warning for
         * {@link MillicastManager#enableNdiOutput} for more details.
         */
        Switch switchNdiAudioOutput = view.findViewById(R.id.ndi_output_audio);
        switchNdiAudioOutput.setChecked(mcManager.isNdiOutputEnabled(true));
        switchNdiAudioOutput.setOnClickListener((viewSwitch -> {
            Switch switchNdi = (Switch) viewSwitch;
            boolean checked = switchNdi.isChecked();
            if (checked != mcManager.isNdiOutputEnabled(true)) {
                mcManager.enableNdiOutput(checked, true, null);
                switchNdi.setChecked(mcManager.isNdiOutputEnabled(true));
                Utils.makeSnackbar(logTag, "WARNING: This feature does not work yet!\nPlease see MillicastManager.enableNdiOutput for more details.", this);
            }
        }));

        Switch switchNdiVideoOutput = view.findViewById(R.id.ndi_output_video);
        switchNdiVideoOutput.setChecked(mcManager.isNdiOutputEnabled(false));
        switchNdiVideoOutput.setOnClickListener((viewSwitch -> {
            Switch switchNdi = (Switch) viewSwitch;
            boolean checked = switchNdi.isChecked();
            mcManager.enableNdiOutput(checked, false, null);
            switchNdi.setChecked(mcManager.isNdiOutputEnabled(false));
        }));
    }

    private <T> void populateSpinner(ArrayList<T> items, Spinner spinner, final int selected, OnItemSelected listener) {
        ArrayAdapter<T> arrayAdapter = new ArrayAdapter<T>(getContext(),
                android.R.layout.simple_spinner_item,
                items);
        spinner.setAdapter(arrayAdapter);
        spinner.setOnItemSelectedListener(itemSelectedGenerator(listener));
        spinner.setSelection(selected);
    }

    private interface OnItemSelected {
        void onItemSelected(int position);
    }

    private AdapterView.OnItemSelectedListener itemSelectedGenerator(OnItemSelected lambda) {
        return new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                lambda.onItemSelected(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        };
    }

    private void refreshMedia(View view) {
        mcManager.refreshMediaLists();
        spinnerAudioSource.setAdapter(new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item,
                mcManager.getAudioSourceList(false)));
        spinnerVideoSource.setAdapter(new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item,
                mcManager.getVideoSourceList(false)));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

}