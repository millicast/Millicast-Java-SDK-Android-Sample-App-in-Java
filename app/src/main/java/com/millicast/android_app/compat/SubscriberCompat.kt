package com.millicast.android_app.compat

import android.util.Log
import com.millicast.Core
import com.millicast.clients.state.ConnectionState
import com.millicast.devices.track.AudioTrack
import com.millicast.devices.track.TrackType
import com.millicast.devices.track.VideoTrack
import com.millicast.subscribers.Credential
import com.millicast.subscribers.Option
import com.millicast.subscribers.ProjectionData
import com.millicast.subscribers.SubscriberListener
import com.millicast.subscribers.SubscriberState
import com.millicast.subscribers.state.ActivityStream
import com.millicast.subscribers.state.LayerData
import com.millicast.subscribers.state.SubscriptionState
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.util.Optional


class SubscriberCompat(subscriberListener: SubscriberListener) : CompatBase() {
    val subscriber = Core.createSubscriber();
    val listener = subscriberListener;
    var credentials = CompatSubscriberCreds();
    var options = CompatSubscriberOptions();
    var state = SubscriberState();
    fun connect() =  promise {
        stateChange();
        activityChange()
        layersChange()
        onTrack();
        onLayers();
        onVad();
        subscriber.setCredentials(credentials.get());
        subscriber.connect()
    }
    fun stateChange() = scope.launch {
        // given a known subscriber object
        subscriber.state
                .distinctUntilChanged()
                .collect { newState: SubscriberState ->
                    // and then perform any operation that fit our logic
                    // here just logging those
                    Log.d("[Kotlin]", "new state received $newState")
                    if(!newState.connectionState.equals(state.connectionState)){
                        when (newState.connectionState){
                            ConnectionState.Connected -> listener.onConnected()
                            ConnectionState.Connecting -> {}
                            ConnectionState.Default -> {}
                            ConnectionState.Disconnected -> listener.onDisconnected()
                            is ConnectionState.DisconnectedError -> {
                                val error = newState.connectionState as ConnectionState.DisconnectedError;
                                listener.onConnectionError(error.httpCode,error.reason);
                            }
                            ConnectionState.Disconnecting -> {}
                        }
                    }
                    if(newState.subscriptionState != state.subscriptionState){
                        when(newState.subscriptionState){
                            SubscriptionState.Default -> {}
                            is SubscriptionState.Error -> {
                                val error = newState.subscriptionState as SubscriptionState.Error;
                                listener.onSubscribedError(error.reason);
                            }
                            SubscriptionState.Stopped -> listener.onStopped()
                            SubscriptionState.Subscribed -> listener.onSubscribed()
                        }
                    }

                    if (newState.viewers != state.viewers){
                        listener.onViewerCount(newState.viewers);
                    }

                    state = newState;
                }
    }

    fun activityChange() = scope.launch {
        subscriber.activity.distinctUntilChanged().collect{
            newActivity ->
            when(newActivity){
                is ActivityStream.Active -> newActivity.sourceId?.let { Optional.of(it) }?.let { listener.onActive(newActivity.streamId,newActivity.track, it) }
                is ActivityStream.Inactive -> newActivity.sourceId?.let { Optional.of(it) }?.let { listener.onInactive(newActivity.streamId,Optional.of(newActivity.sourceId!!)) }
            }
    }
    }

    fun layersChange() = scope.launch {
        subscriber.layers.distinctUntilChanged().collect{
            newLayers ->
                listener.onLayers(newLayers.mid,newLayers.activeLayers,newLayers.inactiveLayersEncodingIds)
        }
    }

    private fun onTrack() = scope.launch {
            subscriber.track.collect() { holder ->
                if(holder.track.kind.equals(TrackType.Audio))
                    listener.onTrack(holder.track as AudioTrack, Optional.of(holder.mid.orEmpty()));
                if(holder.track.kind.equals(TrackType.Video))
                    listener.onTrack(holder.track as VideoTrack, Optional.of(holder.mid.orEmpty()));
            }
        }

    private fun onLayers() = scope.launch {
        subscriber.layers.distinctUntilChanged().collect{
            layer ->
            listener.onLayers(layer.mid,layer.activeLayers, layer.inactiveLayersEncodingIds)
        }
    }

    fun onVad() = scope.launch {
        subscriber.vad.distinctUntilChanged().collect{
            vad ->
            vad.sourceId?.let { Optional.of(it) }?.let { listener.onVad(vad.mid, it) }
        }
    }
    fun select(layerData: LayerData) = promise { subscriber.select(layerData) }

    fun project(sourceId: String, projectionData: ArrayList<ProjectionData?>) =
            promise {subscriber.project(sourceId,projectionData)}

    fun addRemoteTrack(trackType: TrackType) = promise{ subscriber.addRemoteTrack(trackType) }

    fun getMid(trackId: String) = promise { subscriber.getMid(trackId) }

    fun getStats(interval: Boolean) = promise { subscriber.enableStats(interval) }

    fun isConnected() = promise { subscriber.isConnected }

    fun release() = subscriber.release();

    fun subscribe() = promise {subscriber.subscribe(options.get())}

    fun unsubscribe() = promise { subscriber.unsubscribe() }

    fun isSubscribed() = subscriber.isSubscribed;
}

class CompatSubscriberOptions(){
    var stereo : Boolean = true;
    fun get() : Option {
        return Option(
                stereo = this.stereo
        )
    }
}


class CompatSubscriberCreds() {
    var apiUrl = "";
    var accountId = ""
    var streamName = "";
    var token = "";

    fun get() = Credential(
            streamName=this.streamName,
            accountId = this.accountId,
            token = if (this.token.equals("")) null else this.token,
            apiUrl = this.apiUrl
    );
}