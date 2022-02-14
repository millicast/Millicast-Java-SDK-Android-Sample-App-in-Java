# Release Notes
This file documents the release notes for each version of the Millicast Java SDK Android Sample App (SA) in Java.
SA APIs refer to public methods provided by the SA class, MillicastManager.
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
  - Miror: Mirror the local rendering of captured video.
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