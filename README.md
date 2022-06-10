The Android Java Sample App (SA) demonstrates how the Millicast Java SDK can be used in an Android project to publish/subscribe to/from the Millicast Platform.

# Millicast SDK:
- To use this SA, select an appropriate Millicast SDK from our list of [releases](https://github.com/millicast/millicast-native-sdk/releases).

# Opening the SA
1. The SA can be opened with Android Studio (AS).
1. In AS, choose to "Open an Existing Project".
1. Select the SA folder and open it.

# Install the Millicast Java SDK
## Ways to add the SDK
- There are currently two ways to add the Millicast Java SDK, as shown below.
- Proceed with **only one** of these ways at any one time.

### Add SDK AAR file manually.
1. Download the selected version of SDK file from the list of release indicated above.
1. On your filesystem, place your Millicast SDK in the project folder "***MillicastSDK***" (already created).
   - This is the default folder for our MillicastSDK Module.
1. In AS, at the Project window (on the left), select the Android view (top left of panel) if not already selected.
1. Select and open the Gradle Script (***MillicastSDK/build.gradle***) of the Module MillicastSDK.
   - This file specifies the configuration for adding the Millicast SDK, including it's file name.
   - Edit the following line if required:
    `artifacts.add("default", file('MillicastSDK.aar'))`
   - In this line:
        - "MillicastSDK.aar" is the filename of your MillicastSDK.
        - Please update this name based on the actual filename of your MillicastSDK.

### Add the SDK via Maven from GitHub Packages
#### SA usage
- The required gradle settings listed below are mainly for reference, as they have already been set up in the SA.
- The only action required to use the SA is to provide the credentials of a working GitHub account in the ***sa.properties*** file:
   - `githubUsername`
        - The **username** of the GitHub account to use.
   - `githubPat`
        - GitHub user's **personal access token (PAT)** with a `read:packages` scope.
- Please refer to the ***sa.properties*** file for more details.
#### Gradle settings
- The following have already been set up in the SA.
- If you wish to add the SDK to your own app, you can follow the following steps.
- Maven details:
   - Url: https://maven.pkg.github.com/millicast/maven
   - Group ID: com.millicast
   - Artifact ID: millicast-sdk-android
- In ***app/build.gradle***, add:
   - A maven repository for the Millicast Maven on GitHub Packages
        - For example:
            ``` gradle
            repositories {
                // Millicast SDK via Maven from GitHub Packages
                maven {
                    name = "GitHubPackages"
                    url = uri("https://maven.pkg.github.com/millicast/maven")
                    credentials {
                        username = githubUsername
                        password = githubPat
                    }
                }
            }
            ```
        - Where `githubUsername` and `githubPat` are those mentioned in the previous section.
   - A dependency line to use the Millicast SDK
        - To use the latest version of Millicast SDK:
            ``` gradle
            dependencies {
                implementation 'com.millicast:millicast-sdk-android'
            }
            ```
        - To use a specific version of Millicast SDK, for example 1.1.3:
            ``` gradle
            dependencies {
                implementation 'com.millicast:millicast-sdk-android:1.1.3'
            }
            ```
## Load the SDK
- Perform a Gradle Sync to load the Millicast SDK.
   - On AS, click on the button "Sync Project with Gradle Files".
- If not properly synced, AS may show numerous errors regarding unresolved symbols and methods from the Millicast SDK.

# Run the SA
1. Before running the SA, it is recommended to populate the Millicast credentials in the ***Constants.java*** file.
   - It is also possible to enter or change the credentials when the SA is running, at the Settings - Millicast page.
   - When a credential is different from what is currently applied in the SDK, it is highlighted in grey.
1. On AS, click on the "Run 'app'" button to run the SA on the device of your choice.

# Provide required App permissions
- After the SA is installed, go to Android's Settings (via Permission manager or otherwise) and provide permissions required by the SA.
- If this is not done, the SA may crash when capture is started.

# To publish using the SA
1. To publish video, a device with a camera is required. Simulators (e.g. Android Virtual Devices) may need to be configured with a simulated camera before being able to publish video.
1. Ensure the publishing credentials are populated.
   - This can be done at the Settings - Millicast page.
1. If a Publish sourceId is desired:
   - Enter the sourceId in its field (beside Publish stream name).
   - Turn the SourceId switch to on.
     - Publish sourceId will not be enabled if this switch is off, even if the SourceId field is filled.
1. Go to the Publish page.
1. If desired, tap on the camera description and/or resolution description to cycle through the available cameras and resolutions.
   - Toggle the switch labelled "->" or "<-" to change the direction of cycle.
   - For more media related settings, you may go to the Settings - Media page.
1. Tap on REFRESH to update the currently available list of audio and video sources.
1. Tap "START CAPTURE" to start capturing on the selected camera, at the selected resolution.
   - When the capturing is successful, the local video can be seen on the screen.
1. If desired to switch to another camera, tap on the button showing the selected camera.
1. To mute/unmute audio or video, toggle the respective buttons.
   - This affects both the captured/published media, as well as the subscribed media on Subscriber(s).
1. Tap on Mirror to toggle the mirroring of the local video view.
1. Tap "START PUBLISH" to publish the captured video to Millicast.
1. To stop publishing, click "STOP PUBLISH".

# To subscribe using the SA
1. Ensure the subscribing credentials are populated.
1. If desired to show subscribed audio / video via NDI outputs, go to Settings - Media and select "ndi output" (Audio playback device), and toggle switch "NDI output - Video" to on.
1. Go to the Subscribe page.
1. Click "START SUBSCRIBE" to start subscribing to the Millicast stream specified at the Settings - Millicast page.
1. To mute/unmute audio or video, toggle the respective buttons.
   - This affects only the subscribed media on this Subscriber, and not on other Subscriber(s) or the Publisher.
1. The list of available sources are presented in 2 spinners, one each for audio and video sources.
   - Select the desired audio/video sourceId from the spinner and it would be projected for streaming in the current view.
   - If the default/main source with no sourceId is present, it will be presented as a blank entry that can be selected.
   - When the list of available sources change, it would be reflected in the label above the appropriate spinner.
1. The list of available layers (if any, for e.g. simulcast layers) for the currently projected video source are presented in a spinner.
   - Select the desired layerId from the spinner and it would be selected for streaming in the current view.
   - A blank entry that can be selected represents automatic layer selection by Millicast.
   - Layer information of the projected video source is updated periodically from Millicast and the spinner updates dynamically accordingly.
     - This may sometimes result in the active layers disappearing and appearing.
1. To stop subscribing, click "STOP SUBSCRIBE".

# Miscellaneous
- Tap on video view to hide/unhide UI controls.
- Tap on the scale button (appears by default as "FIT") to scale the local rendering of video, cycling between using 3 possible scaling options.
- The local video view of Publisher is mirrored by default for front facing camera(s).
  - This is to acheive a more natural mirror like effect for the Publisher locally.
  - Mirroring effect is only local and does not affect remote view.
- If the publisher settings requires a secure viewer, a valid Subscriber Token has to be set in the Settings - Millicast page.
  - If a secured viewer is **not** required:
    - The following values for the Subscriber Token field are acceptable:
      - Completely blank (no white spaces).
      - A valid Subscriber Token.
    - Any other values will result in failure to connect.