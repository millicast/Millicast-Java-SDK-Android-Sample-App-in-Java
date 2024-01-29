package com.millicast.android_app;

/**
 * Enums for types used in the Sample App.
 */
public class MCTypes {


    /**
     * The type of {@link com.millicast.publishers.BitrateSettings} referred to.
     */
    public enum Bitrate {
        /**
         * {@link com.millicast.publishers.BitrateSettings#minBitrateKbps}
         */
        MIN,
        /**
         * {@link com.millicast.publishers.BitrateSettings#maxBitrateKbps}
         */
        MAX;
    }

    /**
     * The source of a setting value to read from or to write to.
     */
    public enum Source {
        /**
         * The current value in MillicastManager.
         */
        CURRENT,
        /**
         * The Constants.java file.
         */
        FILE,
        /**
         * Where the device saves value.
         */
        SAVED;
    }

}
