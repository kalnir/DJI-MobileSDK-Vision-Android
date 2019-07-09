# DJI-MobileSDK-Vision-Android
An app that does computer vision with a DJI drone and an Android mobile device.

This project opens the door of using DJI's consumer drones, to do near-real-time computer vision. It takes advantage of DJI's advanced long distance video transmission link and the Mobile SDK, and demonstrates how to access the frames of the live video feed, do any computer vision and machine learning tricks you like on the mobile device, and take actions based on your needs.

## What you need
1. A DJI drone that supports Mobile SDK: Phantom, Inspire, Mavic, and even the tiny Spark.

1. A laptop with Android Studio installed and an Android mobile device (keep in mind that some of the older devices might be too slow to do the computer vision processing). 

If you would like to use an iOS device instead, checkout [this](https://github.com/SamuelWangDJI/dji-mobilesdk-vision) repository.

This repository uses the binaries provided by OpenCV for Android as well as the Aruco module from OpenCV contrib. You can find the license and code for OpenCV [here](https://github.com/opencv/opencv) and the OpenCV Contrib modules [here](https://github.com/opencv/opencv_contrib).

## Getting Started
### Clone the Project
Clone the project from this git repository.

### Open in Android Studio
To open an existing project in Android Studio, select **File -> New -> Import Project** or select **Open an existing Android Studio Project** from the welcome screen. In the file browser that appears, select the **DJI-MobileSDK-Vision-Android** folder and click **Open**. Once the Gradle Build has finished running, you should be able to build the project. 

### Get Keys
Register for a DJI Developer account [here](https://account.dji.com/register?appId=dji_sdk&locale=en_US) or sign into your existing account. Then [generate an app key](https://developer.dji.com/mobile-sdk/documentation/quick-start/index.html#generate-an-app-key). Make sure to enter "com.dji.mobilesdk.vision" as the package name for your new app.

### Add Keys to your Application
Open the file **DJI-Mobile-Vision-Internal/app/src/main/AndroidManifest.xml** and replace "YOUR KEY HERE" with your generated App Key string.

### Running the App on an Android Mobile Device
In order to run your app on your mobile device, you must have Developer Options enabled from your device settings. You can follow [these instructions](https://developer.android.com/studio/debug/dev-options) to enable them. Next, connect your mobile device to your laptop using a USB cord and allow USB debugging. To run the app, go to your open project in Android Studio and click on the Play button (or go to **Run -> Run app**) and choose your connected deivce in the dialog that comes up.
