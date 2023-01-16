package com.millicast.android_app;

/**
 * Enums for Millicast related states that help ensure Millicast SDK APIs are called in valid ways.
 */
public class MCStates {

    /**
     * States for video capturing.
     */
    enum CaptureState {
        /**
         * No media is captured. Can start media capture.
         */
        NOT_CAPTURED,
        /**
         * Trying to capture media now. Not allowed to start media capture.
         */
        TRY_CAPTURE,
        /**
         * Media is currently captured. Not allowed to start media capture.
         */
        IS_CAPTURED,
        /**
         * Media source(s) currently being refreshed. Not allowed to start media capture.
         */
        REFRESH_SOURCE;

        // Indicates if audio sources are currently being refreshed.
        public static boolean refreshAudio = false;
        // Indicates if video sources are currently being refreshed.
        public static boolean refreshVideo = false;
    }

    /**
     * States for publishing.
     */
    enum PublisherState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        PUBLISHING
    }

    /**
     * States for subscribing.
     */
    enum SubscriberState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        SUBSCRIBING
    }
}
