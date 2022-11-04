# Release Notes
This file documents the release notes for each version of the Millicast Java SDK Android Sample App (SA) in Java.
SA APIs refer to public methods provided by the SA class, MillicastManager.

## 1.4.1 (2022-11-04)
Upgraded to use SDK 1.4.1, synced Android & iOS SA, improved logging and improved documentation.
### Major changes
- Upgraded to new SDK 1.4.1.
- Greatly synchronised MillicastManager code between Android and iOS SAs, including:
  - Variable, state and method names.
  - Init, Publisher & Subscriber connect/disconnect, start/stop methods.
  - Methods categorised into similar sections.
- Improved logcat printing of long strings, for e.g. stats reports.
- Every class now has a brief documentation on its purpose.
### Fixed
- N.A.
### Known issues
- As before.

## 1.3.0 (2022-08-16)
Upgraded to use SDK 1.3.0, updated SA logo, and updated readme.
### Major changes
- Upgraded to new SDK 1.3.0.
- Updated SA logo.
- Updated ***README.md*** for Settings usage.
### Fixed
- N.A.
### Known issues
- As before.

## 1.2.0 (2022-06-10)
Upgraded to use SDK 1.2.0, added multisource sourceId & layer selection usage.
### Major changes
- Upgraded to new SDK 1.2.0.
- Publisher able to specify sourceId for source published.
- Subscriber able to select and project any active source received.
- Subscriber able to select any active layer from the video source currently projected.
### Fixed
- N.A.
### Known issues
- As before.

## 1.1.2 (2022-04-27)
Upgraded to use SDK 1.1.4, fixed Subscribe Token usage.
### Major changes
- Upgraded to new SDK 1.1.4.
### Fixed
- Fixed Subscribe Token usage.
  - Added UI and logic for Subscribe Token usage.
  - Added work around for current SDK issue where credentials are not set when a Subscribe Token was provided.
### Known issues
- As before.

## 1.1.1 (2022-02-14)
Upgraded to use SDK 1.1.3, added Maven installation of SDK, release configs and R8 rules.
### Major changes
- Upgraded to new SDK 1.1.3.
- Added Maven installation of SDK.
  - Both the existing manual addition of AAR and the Maven installation of SDK are supported.
  - Refer to ***README.md*** for usage details.
- Added release signing config and build type in ***app/build.gradle***.
- Added ***sa.properties*** file where various SA related setting can be added and used in the ***build.gradle*** files.
- Added R8 rules in ***proguard-rules.pro*** that enables SDK to work even after R8 minify and resource shrinking.
- Updated Gradle & Android Gradle Plugin to version 7+.
- Updated SA dependencies in ***app/build.gradle***.
- Update JavaDoc generation settings.
### Fixed
- N.A.
### Known issues
- As before.

## 1.1.0 (2021-12-16)
Upgraded to features of SDK 1.1, improved handling of NDI input/output, and improved UI control and display for video and settings.
### Major changes
- Upgraded to new features of SDK 1.1:
  - New WebRTC Stats API
  - New Option for Client & Publisher classes.
- Switching VideoSource after capturing will only allow switching between device cameras and not with NDI.
- New SA API switchScaling to scale published or subscribe local video.
  - Scaling of video is set to ASPECT FIT by default.
- New SA API switchMirror to mirror local video views.
  - By default front facing camera(s) will be mirrored.
- New UI controls for Publish view:
  - Refresh: Refresh available audio and video sources.
  - Mirror: Mirror the local rendering of captured video.
- New UI controls for both Publish & Subscribe views:
  - Tap on video view to hide/unhide UI controls.
  - Scale: Scale the local rendering of video using 3 possible scaling options.
- Improved Media Settings options.
### Fixed
- Occasional crashes when switching views.
- Stopping publish while subscribing may lead to an error.
- Crash when using NDI due to 0 Capabilities List size.
### Known issues
- Higher resolutions may be slow in starting capture, or sometimes fail to be captured.
  - This might be device dependent.

## 1.0.0 (2021-11-08)
This is the first release of the Millicast Java SDK Android Sample App (SA) in Java.
### Known issues
- Higher resolutions may be slow in starting capture, or sometimes fail to be captured.
  - This might be device dependent.
- Stopping publish while subscribing may lead to an error.