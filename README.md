The Android Java Sample App (SA) demonstrates how the Millicast Java SDK can be used in an Android project to publish/subscribe to/from the Millicast Platform.

# Millicast SDK:
- To use this SA, download an appropriate Millicast SDK from our list of [releases](https://github.com/millicast/millicast-native-sdk/tags).
- The recommended SDK version for this SA version is: [1.0.0](https://github.com/millicast/millicast-native-sdk/releases/tag/v1.0.0)

# Build the SA
1. Open the SA in Android Studio (AS).
    1. In AS, choose to "Open an Existing Project".
    1. Select the SA folder and open it.
1. Add the Millicast Java SDK
    1. On your filesystem, place your Millicast SDK in the project folder "MillicastSDK" (already created).
        - This is the default folder for our MillicastSDK Module.
    1. In AS, at the Project window (on the left), select the Android view (top left of panel) if not already selected.
    1. Select and open the Gradle Script (build.gradle) of the Module android-app.MillicastSDK.
        - This file specifies the configuration for adding the Millicast SDK, including it's file name.
        - In the existing line "artifacts.add("default", file('MillicastSDK.aar'))",
            - "MillicastSDK.aar" is the filename of your MillicastSDK.
            - Please update this name based on the actual filename of your MillicastSDK.
1. Perform a Gradle Sync to load the Millicast SDK.
    - On AS, click on the button "Sync Project with Gradle Files".
    - If not properly synced, AS may show numerous errors regarding unresolved symbols and methods from the Millicast SDK.

# Run the SA
1. Before running the SA, it is recommended to populate the Millicast credentials in the Constants.java file.
    - It is also possible to enter or change the credentials when the SA is running, at the Settings - Millicast page.
1. On AS, click on the "Run 'app'" button to run the SA on the device of your choice.

# Provide required App permissions
- After the SA is installed, go to Android's Settings (via Permission manager or otherwise) and provide permissions required by the SA.
- If this is not done, the SA may crash when capture is started.

# To publish using the SA
1. To publish video, a device with a camera is required. Simulators (e.g. Android Virtual Devices) may need to be configured with a simulated camera before being able to publish video.
1. Ensure the publishing credentials are populated.
    - This can be done at the Settings - Millicast page.
1. Go to the Publish page.
1. If desired, tap on the camera description and/or resolution description to cycle through the available cameras and resolutions.
    - Toggle the switch labelled "->" or "<-" to change the direction of cycle.
    - For more media related settings, you may go to the Settings - Media page.
1. Click "START CAPTURE" to start capturing on the selected camera, at the selected resolution.
    - When the capturing is successful, the local video can be seen on the screen.
1. If desired to switch to another camera, click on the button showing the selected camera.
1. To mute/unmute audio or video, toggle the respective buttons.
    - This affects both the captured/published media, as well as the subscribed media on Subscriber(s).
1. Click "START PUBLISH" to publish the captured video to Millicast.
1. To stop publishing, click "STOP PUBLISH".

# To subscribe using the SA
1. Ensure the subscribing credentials are populated.
1. Go to the Subscribe page.
1. Click "START SUBSCRIBE" to start subscribing to the Millicast stream specified at the Settings - Millicast page.
1. To mute/unmute audio or video, toggle the respective buttons.
    - This affects only the subscribed media on this Subscriber, and not on other Subscriber(s) or the Publisher.
1. To stop subscribing, click "STOP SUBSCRIBE".
