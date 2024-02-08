package com.millicast.android_app.compat

import android.util.Log
import com.millicast.Core
import com.millicast.android_app.PubListener
import com.millicast.clients.ConnectionOptions
import com.millicast.clients.state.ConnectionState
import com.millicast.devices.track.AudioTrack
import com.millicast.devices.track.Track
import com.millicast.devices.track.VideoTrack
import com.millicast.publishers.BitrateSettings
import com.millicast.publishers.Option
import com.millicast.publishers.PublisherState
import com.millicast.publishers.state.PublishingState
import com.millicast.publishers.state.RecordingState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch


class PublisherCompat(pubListener: PubListener) : CompatBase() {

    val publisher = Core.createPublisher();
    val listener = pubListener;
    var credentials = CompatPublisherCredentials();
    var options = CompatPublisherOptions();
    var state = PublisherState();
    fun connect() =  promise {
        stateChange();
        val connOpts = ConnectionOptions(false);
        publisher.enableStats(true)
        publisher.setCredentials(credentials.get());
        onStatsReport()
        publisher.connect(connectionOptions = connOpts);
    }
    fun stateChange() = scope.launch {
        publisher.state
                .distinctUntilChanged()
                .collect { newState: PublisherState ->
                    if(newState.connectionState != state.connectionState){
                        when(newState.connectionState){
                            ConnectionState.Connected -> listener.onConnected()
                            ConnectionState.Connecting -> {}
                            ConnectionState.Default -> {}
                            ConnectionState.Disconnected -> listener.onDisconnected()
                            ConnectionState.Disconnecting -> {}
                            is ConnectionState.DisconnectedError -> {
                                val error = newState.connectionState as ConnectionState.DisconnectedError;
                                listener.onConnectionError(error.httpCode,error.reason);
                            }
                        };
                    }

                    if(newState.publishingState != state.publishingState){
                        when(newState.publishingState){
                            is PublishingState.Error -> {
                                val error = newState.publishingState as PublishingState.Error
                                listener.onPublishingError(error.reason)
                            }
                            PublishingState.Started -> listener.onPublishing()
                            PublishingState.Stopped -> {}
                        }
                    }


                    if(newState.recordingState != state.recordingState){
                        when(newState.recordingState){
                            RecordingState.Started -> listener.onRecordingStarted()
                            RecordingState.StartedFailed -> listener.onFailedToStartRecording()
                            RecordingState.Stopped -> listener.onRecordingStopped()
                            RecordingState.StoppedFailed -> listener.onFailedToStopRecording()
                        }
                    }

                    if(newState.viewers != state.viewers){
                        listener.onViewerCount(newState.viewers);
                    }
                    state = newState;
                }
    }

    fun getStats(interval: Boolean) = promise { publisher.enableStats(interval) }

    fun isConnected() = promise { publisher.isConnected }

    fun isRecording() = state.recordingState == RecordingState.Started;

    fun release() = publisher.release();
    fun addTrack(audioTrackPub: AudioTrack) = promise { publisher.addTrack(audioTrackPub as Track); }

    fun addTrack(videoTrackPub: VideoTrack) = promise { publisher.addTrack(videoTrackPub as Track) }

    fun publish() = promise {
        publisher.setCredentials(credentials.get())
        publisher.publish(options.get());
    }

    fun startRecording() = promise { publisher.recording.start() }

    fun stopRecording() = promise { publisher.recording.stop() }

    fun unpublish() = promise { publisher.unpublish() }

    fun isPublishing() = promise { publisher.isPublishing }

    fun onStatsReport() = scope.launch {
        publisher.rtcStatsReport.distinctUntilChanged().collect{ report -> listener.onStatsReport(report) }
    }
}

class CompatPublisherOptions(){
    var stereo : Boolean = true;
    var recordStream = false;
    var sourceId = "";
    var bitrateSettings = CompatBitrateSettings();
    var audioCodec = "";
    var videoCodec = ""

    fun get() : Option {
        return Option(
                stereo = this.stereo,
                recordStream = this.recordStream,
                sourceId = null,
                bitrateSettings = BitrateSettings(
                        minBitrateKbps = bitrateSettings.minBitrateKbps,
                        maxBitrateKbps = bitrateSettings.maxBitrateKbps,
                        disableBWE = bitrateSettings.disableBWE
                ),
                videoCodec = this.videoCodec,
                audioCodec = this.audioCodec
        )
    }
}

class CompatPublisherCredentials() {
    var apiUrl = "";
    var streamName = "";
    var token = "";

    fun get() = com.millicast.publishers.Credential(
            streamName = this.streamName,
            token = this.token,
            apiUrl = this.apiUrl,

    );
}