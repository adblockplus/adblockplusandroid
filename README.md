Adblock Plus for Android
========================

An Android app that runs a proxy to block ads.

Updating the dependencies
-------------------------

Adblock Plus for Android has dependencies that aren't in this repository.
To update those, call:

    ./ensure_dependencies.py

Buildling with Ant
------------------

### Requirements

- [The Android SDK](http://developer.android.com/sdk)
- [The Android NDK](https://developer.android.com/tools/sdk/ndk)
- [Ant](http://ant.apache.org)

### Building

In the project directory, create the file _local.properties_ and set
_sdk.dir_ and _ndk.dir_ to where you installed it, e.g.:

    sdk.dir = /some/where/sdk
    ndk.dir = /some/where/ndk

Then run:

    ant debug

### Running

Connect an Android device or start the Android Emulator, then run:

    ant debug install

Finally, you can run _Adblock Plus_ from the launcher.

Building with Eclipse
---------------------

### Requirements

- [The Android SDK](http://developer.android.com/sdk)
- [The Android NDK](https://developer.android.com/tools/sdk/ndk)
- [Eclipse](https://www.eclipse.org)
- [Android Developer Tools for Eclipse](http://developer.android.com/tools/sdk/eclipse-adt.html)
  (both _Developer Tools_ and _NDK Plugins_)

### Building

1. Ensure the `NDKROOT` environment variable points to your NDK directory.
2. Select _Import_ in the _File_ menu, then _Existing Android Projects Into Workspace_.
3. Select the project directory (_adblockplusandroid_) as _Root Directory_.
4. Select the projects _Adblock Plus_, _library_ and _android-switch-backport_.
5. Revert any local changes to _.classpath_ in _adblockplusandroid_
   and _adblockplusandroid/submodules/android-switch-backport_.

### Running

1. Connect an Android device or start the Android Emulator.
2. In Eclipse, select the _Adblock Plus_ project, then run it as an
   _Android Application_.
