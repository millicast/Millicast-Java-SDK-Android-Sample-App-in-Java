package com.millicast.android_app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Switch;

import androidx.annotation.IdRes;
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
    private Spinner spinnerCapability;

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

        if (savedInstanceState == null) {
            // Will only lock if it's the first time.
            mcManager.setCameraLock(true);
        }

        populateSpinner(mcManager.getVideoSourceList(), R.id.video_source_list,
                mcManager.getVideoSourceIndex(), (int position) -> {
                    mcManager.setVideoSourceIndex(position, true);
                    spinnerCapability.setAdapter(new ArrayAdapter<>(getContext(),
                            android.R.layout.simple_spinner_item,
                            mcManager.getCapabilityList()));
                    spinnerCapability.setSelection(mcManager.getCapabilityIndex());
                });

        spinnerCapability = view.findViewById(R.id.capability_list);
        populateSpinner(mcManager.getCapabilityList(), spinnerCapability,
                mcManager.getCapabilityIndex(), (int position) -> {
                    mcManager.setCapabilityIndex(position);
                });

        Spinner spinnerAudioSource = view.findViewById(R.id.audio_source_list);
        populateSpinner(mcManager.getAudioSourceList(), spinnerAudioSource,
                mcManager.getAudioSourceIndex(), (int position) -> {
                    if (position == mcManager.getAudioSourceIndex()) {
                        return;
                    }
                    if (!mcManager.setAudioSourceIndex(position)) {
                        spinnerAudioSource.setSelection(mcManager.getAudioSourceIndex());
                        Utils.makeSnackbar("AudioSource cannot be changed now!", this);
                    }
                });

        Spinner spinnerAudioCodec = view.findViewById(R.id.audio_codec_list);
        populateSpinner(mcManager.getCodecList(true), spinnerAudioCodec,
                mcManager.getAudioCodecIndex(), (int position) -> {
                    if (position == mcManager.getAudioCodecIndex()) {
                        return;
                    }
                    if (!mcManager.setCodecIndex(position, true)) {
                        spinnerAudioCodec.setSelection(mcManager.getAudioCodecIndex());
                        Utils.makeSnackbar("AudioCodec cannot be changed now!", this);
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
                        Utils.makeSnackbar("VideoCodec cannot be changed now!", this);
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
                        Utils.makeSnackbar("AudioPlayback cannot be changed now!", this);
                    }
                });

        Switch switchNdiVideoOutput = view.findViewById(R.id.ndiVideoOutput);
        switchNdiVideoOutput.setChecked(mcManager.isNdiVideoRequested());
        switchNdiVideoOutput.setOnClickListener((viewSwitch -> {
            Switch switchNdi = (Switch) viewSwitch;
            mcManager.setNdiVideo(switchNdi.isChecked());
        }));
    }


    private <T> void populateSpinner(ArrayList<T> items, @IdRes int id, int selected, OnItemSelected listener) {
        ArrayAdapter<T> arrayAdapter = new ArrayAdapter<T>(getContext(),
                android.R.layout.simple_spinner_item,
                items);
        Spinner spinner = getView().findViewById(id);
        spinner.setAdapter(arrayAdapter);
        spinner.setOnItemSelectedListener(itemSelectedGenerator(listener));
        spinner.setSelection(selected);
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

}