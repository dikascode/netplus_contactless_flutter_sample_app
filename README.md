# NetPOS Contactless Flutter Sample

This Flutter project demonstrates the implementation of the `netplus/contactless_sdk`, providing functionalities for contactless card reading and payment processing using Netplus Payment Client.

## Key Features

- Contactless card reading.
- Payment processing with Netplus Payment Client.

## Important Dependencies

The following dependencies are crucial for the project:

```gradle
implementation 'com.github.netplusTeam:contactless-sdk:1.0.21'
implementation 'com.github.netplusTeam:netplus-epms:1.0.23'
implementation 'com.github.netplusTeam:NetposNibssClient:1.0.13'
```

These dependencies are responsible for enabling contactless card reading and integrating Netplus Payment Client.

Prerequisites
An NFC-enabled device is required to run the app.
Minimum SDK version: 23
Target SDK version: 33
Kotlin KAPT: id 'kotlin-kapt'

Setup Instructions
Activity Theme Override: Due to a Theme.AppCompat bug triggered by an activity in the SDK, the theme of the activity must be overridden in the AndroidManifest.xml:

```xml
<activity android:name="com.netpluspay.contactless.sdk.ui.NfcActivity"
          android:theme="@style/AppTheme" />
```

The AppTheme should be defined in styles.xml with AppCompat as the parent:

```xml
<style name="AppTheme" parent="Theme.AppCompat.Light.NoActionBar">
```


MainActivity Configuration: The MainActivity in Flutter should extend FlutterFragmentActivity to fully implement ActivityResultLauncher:

```kotlin
class MainActivity : FlutterFragmentActivity() {}
```

Method Channel Implementation: Define and implement a method channel in Flutter for optimal communication between Flutter and native Android code:

```flutter
static const platform = MethodChannel('netplus/contactless_sdk');
```

## And in Android:

```kotlin
override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
    super.configureFlutterEngine(flutterEngine)
    MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
    .setMethodCallHandler { call, result ->
        flutterResult = result
        handleMethodCall(call)
    }
}
```


## Prerequisites
An NFC-enabled device is required to run the app.
Minimum SDK version: 23
Target SDK version: 33
Kotlin KAPT: id 'kotlin-kapt'


## Getting Started
This project is a starting point for a Flutter application. To get started, clone the repository and run the following commands:

```bash
flutter pub get
flutter run
```



