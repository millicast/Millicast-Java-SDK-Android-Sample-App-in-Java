package com.millicast.android_app;

import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.millicast.AudioTrack;
import com.millicast.VideoRenderer;
import com.millicast.VideoTrack;

import static android.media.AudioManager.MODE_IN_COMMUNICATION;
import static android.media.AudioManager.MODE_NORMAL;
import static com.millicast.android_app.MillicastManager.Source.CURRENT;
import static com.millicast.android_app.Utils.logD;

public class SubscribeFragment extends Fragment {
    public static final String TAG = "SubscribeFragment";

    private final MillicastManager mcManager;
    private FrameLayout frameLayout;
    private TextView textView;
    private Button buttonSubscribe;
    private Button buttonAudio;
    private Button buttonVideo;

    private boolean ascending = true;

    public SubscribeFragment() {
        this.mcManager = MillicastManager.getSingleInstance();

        // Set this view into listeners/handlers
        mcManager.setSubView(this);

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

        frameLayout = view.findViewById(R.id.frame_layout_sub);
        textView = view.findViewById(R.id.text_view_sub);

        buttonSubscribe = view.findViewById(R.id.button_subscribe);
        buttonAudio = view.findViewById(R.id.button_audio_sub);
        buttonVideo = view.findViewById(R.id.button_video_sub);

        // Set dynamic button actions
        buttonSubscribe.setOnClickListener(this::onStartSubscribeClicked);
        buttonAudio.setOnClickListener(this::toggleAudio);
        buttonVideo.setOnClickListener(this::toggleVideo);

        if (!mcManager.isRicohTheta(CURRENT)) {
            mcManager.enableSubStats(true); // Collect Subscriber's rtc stats during the call
        }
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
     * Mute or unmute audio by switching audio state to the opposite of the current state.
     *
     * @param view
     */
    private void toggleAudio(View view) {
        AudioTrack track = mcManager.getSubAudioTrack();
        if (track != null) {
            track.setEnabled(!mcManager.isSubAudioEnabled());
            mcManager.setSubAudioEnabled(!mcManager.isSubAudioEnabled());
            setUI();
        }
    }

    /**
     * Mute or unmute video by switching video state to the opposite of the current state.
     *
     * @param view
     */
    private void toggleVideo(View view) {
        VideoTrack track = mcManager.getSubVideoTrack();
        if (track != null) {
            track.setEnabled(!mcManager.isSubVideoEnabled());
            mcManager.setSubVideoEnabled(!mcManager.isSubVideoEnabled());
            setUI();
        }
    }

    private void onStartSubscribeClicked(View view) {
        Log.d(TAG, "Start Subscribe clicked.");
        displaySubVideo();
        mcManager.subConnect();
        setUI();
    }

    private void displaySubVideo() {
        String tag = "[displaySubVideo] ";

        // Display video if not already displayed.
        if (frameLayout.getChildCount() == 0) {
            // Get remote video renderer.
            VideoRenderer subRenderer = mcManager.getSubRenderer();
            // Ensure our renderer is not attached to another parent view.
            Utils.removeFromParentView(subRenderer, TAG);
            // Finally, add our renderer to our frame layout.
            frameLayout.addView(subRenderer);
            logD(TAG, tag + "Added renderer for display.");
        } else {
            logD(TAG, tag + "Already displaying renderer.");
        }
    }

    private void onStopSubscribeClicked(View view) {
        Log.d(TAG, "Stop Subscribe clicked.");
        mcManager.stopSubscribe();
        Utils.stopDisplayVideo(frameLayout, TAG);
        setUI();
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
     * Set the state of UIs, including subscribe button,
     * based on current subscribe state.
     * Must be run on UI thread.
     */
    void setUI() {

        if (buttonSubscribe == null) {
            return;
        }

        textView.setText("Account: " + mcManager.getAccountId(CURRENT) +
                " Stream: " + mcManager.getStreamNameSub(CURRENT));

        switch (mcManager.getSubState()) {
            case DISCONNECTED:
                setAudioMode(false);
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
                if (mcManager.isSubAudioEnabled()) {
                    buttonAudio.setText(R.string.muteAudio);
                } else {
                    buttonAudio.setText(R.string.unmuteAudio);
                }
                if (mcManager.isSubVideoEnabled()) {
                    buttonVideo.setText(R.string.muteVideo);
                } else {
                    buttonVideo.setText(R.string.unmuteVideo);
                }
                break;
        }
    }
}