package com.millicast.android_app;

/**
 * Enums for Millicast related states that help ensure Millicast SDK APIs are called in valid ways.
 */
public class MCStates {

    /**
     * States for video capturing.
     */
    enum CaptureState {
        NOT_CAPTURED,
        TRY_CAPTURE,
        IS_CAPTURED
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
