package com.millicast.android_app;

import static com.millicast.android_app.Utils.getArrayStr;
import static com.millicast.android_app.Utils.logD;

import androidx.annotation.NonNull;

import com.millicast.LayerData;
import com.millicast.Subscriber;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

/**
 * Information about a subscribed media source.
 * This includes: Media sourceId, lists of audio and video trackIds, and lists of layers.
 */
public class SourceInfo {

    public static final String TAG = "SourceInfo";
    private static final String LAYER_SEPARATOR = ", ";
    private String sourceId;
    private String[] trackIdAudioList;
    private String[] trackIdVideoList;
    /**
     * Map of layerId : {@link LayerData} of currently active Layers.
     * LayerId is obtained via {@link #getLayerId}.
     */
    private HashMap<String, LayerData> layerActiveMap = null;
    /**
     * An array of currently active {@link LayerData} for this Source.
     */
    private LayerData[] layerActiveList = null;
    /**
     * A defined String representation (via {@link #getLayerListStr}) of the current {@link #layerActiveList},
     * that can be quickly used for comparison when trying to set a new {@link #layerActiveList}.
     */
    private String layerListStr = "";

    /**
     * The layerId {from @link #getLayerId} of the selected Layer.
     * The default value, an empty String (""), indicates automatic layer selection
     * by the Millicast platform, provided there is at least 1 active layer.
     */
    private String layerActiveId = "";

    public SourceInfo(String sourceId, String[] trackIdAudioList, String[] trackIdVideoList) {
        this.sourceId = sourceId;
        this.trackIdAudioList = trackIdAudioList;
        this.trackIdVideoList = trackIdVideoList;
    }

    //**********************************************************************************************
    // APIs
    //**********************************************************************************************

    /**
     * Checks if this Source has at least 1 audioTrack.
     *
     * @return
     */
    public boolean hasAudio() {
        if (trackIdAudioList == null || trackIdAudioList.length == 0) {
            return false;
        }
        return true;
    }

    /**
     * Checks if this Source has at least 1 videoTrack.
     *
     * @return
     */
    public boolean hasVideo() {
        if (trackIdVideoList == null || trackIdVideoList.length == 0) {
            return false;
        }
        return true;
    }

    /**
     * Get an ArrayList of {@link com.millicast.Subscriber.ProjectionData} for all the
     * audio or video tracks of this MediaInfo.
     *
     * @param mid      The mid onto which this source is requested to be projected onto.
     * @param forAudio
     * @return
     */
    public ArrayList<Subscriber.ProjectionData> getProjectionData(String mid, boolean forAudio) {
        String[] trackList = trackIdAudioList;
        String mediaType = "audio";

        if (!forAudio) {
            trackList = trackIdVideoList;
            mediaType = "video";
        }

        ArrayList<Subscriber.ProjectionData> result = null;
        if (trackList == null || trackList.length < 1) {
            return result;
        }

        result = new ArrayList<>();

        for (String trackId : trackList) {
            Subscriber.ProjectionData data = new Subscriber.ProjectionData();
            data.trackId = trackId;
            data.media = mediaType;
            data.mid = mid;

            result.add(data);
        }

        return result;
    }

    /**
     * Set the {@link #layerActiveMap} of this Source if this list differs from the last one set.
     * If set, the selected {@link #layerActiveId} would be reset to default ("").
     *
     * @param layerActiveList
     * @return True if a new {@link #layerActiveList} was set, and false otherwise.
     */
    public boolean setLayerActiveList(LayerData[] layerActiveList) {
        String logTag = "[Layer][List][Set] ";
        this.layerActiveList = layerActiveList;
        String newStr = getLayerListStr(layerActiveList);
        // Check if this is a new list of Layers.
        if (layerListStr.equals(newStr)) {
            // If it is the same existing list, do nothing.
            logD(TAG, logTag + "NOT setting list as it already exists: " +
                    layerListStr + ".");
            return false;
        }

        // If this is a new list of Layers, set all associated items.
        // Set the new layerListStr.
        layerListStr = newStr;
        this.layerActiveList = layerActiveList;
        // Reset the selected layer.
        layerActiveId = "";
        // Set the layerMap.
        setLayerActiveMap(layerActiveList);
        logD(TAG, logTag + "OK. Layers:\n" + layerListStr + ".\nlayerMap:" + layerActiveMap +
                ", selected layerId:" + layerActiveId + ".");
        return true;
    }

    /**
     * Get the layerId (from {@link SourceInfo#getLayerId}) list of the current active layers.
     * If there is at least one layer, include an empty String ("") at index 0
     * as the layer automatically selected by Millicast.
     * If there are no active layers, list returned will have size 0.
     *
     * @return
     */
    public ArrayList<String> getLayerActiveIdList() {
        ArrayList<String> list;
        if (layerActiveMap == null || layerActiveMap.size() == 0) {
            list = new ArrayList<>();
            return list;
        }

        list = new ArrayList<>(layerActiveMap.keySet());
        list.add(0, "");
        return list;
    }

    /**
     * The layerId {@link #layerActiveId} of the currently selected Layer.
     * An empty String ("") indicates that the layer is automatically selected by Millicast.
     *
     * @return
     */
    public String getLayerActiveId() {
        return layerActiveId;
    }

    /**
     * Set the layerId {@link #layerActiveId} of the currently selected Layer.
     * An empty String ("") indicates that the layer is automatically selected by Millicast.
     *
     * @param layerId
     * @return True if successful, false if the layerId is not available in the list of layers.
     */
    public boolean setLayerActiveId(String layerId) {
        if (layerId == null) {
            return false;
        }
        if (layerId.equals("")) {
            layerActiveId = layerId;
            return true;
        }
        if (!layerActiveMap.containsKey(layerId)) {
            return false;
        }
        this.layerActiveId = layerId;
        return true;
    }

    /**
     * Get an Optional {@link LayerData} of the selected Layer of this source.
     * An empty Optional indicates that the layer is automatically selected by Millicast.
     *
     * @return The Optional or null if either:
     * - There is no active layer.
     * - The layer specified by the {@link #layerActiveId} cannot be found.
     */
    public Optional<LayerData> getLayerData() {
        String logTag = "[Layer][Active] ";
        Optional<LayerData> layerDataOpt;
        if (layerActiveMap == null) {
            logD(TAG, logTag + "Failed! There is no active Layer!");
            return null;
        }
        if (layerActiveId.equals("")) {
            layerDataOpt = Optional.empty();
            logD(TAG, logTag + getLayerStr(layerDataOpt, true) + ".");
            return layerDataOpt;
        }
        LayerData layerData = layerActiveMap.get(layerActiveId);
        if (layerData == null) {
            logD(TAG, logTag + "Failed! The Layer specified by the layerId cannot be found!");
            return null;
        }
        layerDataOpt = Optional.of(layerData);
        logD(TAG, logTag + getLayerStr(layerDataOpt, true) + ".");
        return layerDataOpt;
    }

    /**
     * Get an Optional {@link LayerData} of the Layer of this source
     * that has the given {@link #layerActiveId}.
     * An empty Optional indicates that the layer is automatically selected by Millicast.
     *
     * @return The Optional or null if the layer specified by the layerId cannot be found.
     */
    public Optional<LayerData> getLayerData(String layerId) {
        String logTag = "[Layer][Active]:" + layerId + " ";
        Optional<LayerData> layerDataOpt;
        if (layerId.equals("")) {
            logD(TAG, logTag + "Automatically selected by Millicast.");
            return Optional.empty();
        }

        LayerData layerData = layerActiveMap.get(layerId);
        if (layerData == null) {
            logD(TAG, logTag + "Failed! The Layer specified by the layerId cannot be found!");
            return null;
        }
        layerDataOpt = Optional.of(layerData);
        logD(TAG, logTag + getLayerStr(layerDataOpt, true) + ".");
        return layerDataOpt;
    }

    @Override
    public String toString() {
        String result = "Source:" + sourceId + " A:[";
        result += getArrayStr(trackIdAudioList, ",", null);
        result += "] V:[";
        result += getArrayStr(trackIdVideoList, ",", null);
        result += "] Layer:[";
        result += layerListStr + "]";
        return result;
    }

    //**********************************************************************************************
    // Static APIs
    //**********************************************************************************************

    /**
     * Get a String representation of an array of {@link LayerData}.
     */
    @NonNull
    public static String getLayerListStr(LayerData[] layerList) {
        return Utils.getArrayStr(layerList, LAYER_SEPARATOR, lambdaLayerToString);
    }

    /**
     * Get a String representation of a {@link LayerData}.
     */
    public static Utils.LambdaToString lambdaLayerToString = new Utils.LambdaToString() {
        @Override
        public <T> String toString(T layerData) {
            LayerData ld = (LayerData) layerData;
            String result = getLayerStr(ld, true);
            return result;
        }
    };

    /**
     * Get the layerId, an unique id that represents the given Layer.
     * An empty Optional indicates that the layer is automatically selected by Millicast.
     *
     * @param layerDataOpt
     * @return
     */
    public static String getLayerId(Optional<LayerData> layerDataOpt) {
        return getLayerStr(layerDataOpt, false);
    }

    /**
     * Get the layerId, an unique id that represents the given Layer.
     *
     * @param layerData
     * @return
     */
    public static String getLayerId(LayerData layerData) {
        return getLayerStr(layerData, false);
    }

    /**
     * Get a String representation of an Optional of {@link LayerData}.
     * An empty Optional indicates that the layer is automatically selected by Millicast.
     * String representation as given in {@link #getLayerStr(LayerData, boolean)}.
     */
    public static String getLayerStr(Optional<LayerData> layerDataOpt, boolean longForm) {
        String name = "";
        if (layerDataOpt == null) {
            name += "N.A.";
            return name;
        }
        if (!layerDataOpt.isPresent()) {
            name += "AUTO";
            return name;
        }

        return getLayerStr(layerDataOpt.get(), longForm);
    }

    /**
     * Get a String representation of a {@link LayerData}.
     * The default representation is:<br/>
     * {@link LayerData#encodingId encodingId}(T:{@link LayerData#temporalLayerId temporalLayerId} S:{@link LayerData#spatialLayerId spatialLayerId})
     * <br/>
     * Long form representation:<br/>
     * {@link LayerData#encodingId encodingId}(T:{@link LayerData#temporalLayerId temporalLayerId}/{@link LayerData#maxTemporalLayerId maxTemporalLayerId} S:{@link LayerData#spatialLayerId spatialLayerId}/{@link LayerData#maxSpatialLayerId maxSpatialLayerId})
     *
     * @param ld       The {@link LayerData} for which to get a String.
     * @param longForm true to use the long form, false otherwise.
     * @return
     */
    public static String getLayerStr(LayerData ld, boolean longForm) {
        String name = "";
        if (ld == null) {
            name += "N.A.";
            return name;
        }

        name += ld.encodingId;
        String temStr = "" + ld.temporalLayerId;
        String spaStr = "" + ld.spatialLayerId;
        if (longForm) {
            String maxTemLayer = "-";
            if (ld.maxTemporalLayerId.isPresent()) {
                maxTemLayer = "" + ld.maxTemporalLayerId.get();
            }
            String maxSpaLayer = "-";
            if (ld.maxSpatialLayerId.isPresent()) {
                maxSpaLayer = "" + ld.maxSpatialLayerId.get();
            }
            temStr += "/" + maxTemLayer;
            spaStr += "/" + maxSpaLayer;
        }
        name += " (T:" + temStr + " S:" + spaStr + ")";
        return name;
    }

    //**********************************************************************************************
    // Private methods
    //**********************************************************************************************

    /**
     * Populate the {@link #layerActiveMap} using the given layerList.
     *
     * @param layerList
     */
    private void setLayerActiveMap(LayerData[] layerList) {
        // Create a new layerMap.
        layerActiveMap = new HashMap<>();
        if (layerList == null || layerList.length == 0) {
            return;
        }
        for (LayerData ld : layerList) {
            String layerId = getLayerId(ld);
            layerActiveMap.put(layerId, ld);
        }
    }
}
