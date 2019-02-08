# Kitfox
Kitfox is a voice assistant for the web.


<img src="http://tola.me.uk/files/kitfox_screenshot.png" width="320" />

## Installation

### Android Mobile

* Clone this GitHub project onto a desktop computer and import the project into [Android Studio](https://developer.android.com/studio/index.html)
* [Enable adb debugging](https://developer.android.com/studio/command-line/adb) on your mobile device so that you can side-load the app
* Plug your mobile device into USB and click "OK" on the "Allow USB debugging" dialog which should appear
* Select the "armMobileDebug" build variant from the Build Variants panel in Android Studio
* Click the "Run App" button (green play button) to build and launch the app on your mobile device, picking it from the list

### Android Things

This application can be used as the main system app on [Android Things](https://developer.android.com/things/sdk/index.html).

* Flash Android Things onto a developer board (e.g. [flash onto a Raspberry Pi 3](https://developer.android.com/things/hardware/raspberrypi.html) with the [official Raspberry Pi touchscreen](https://www.raspberrypi.org/products/raspberry-pi-touch-display/) or [RasPad](https://www.kickstarter.com/projects/35410622/raspad-raspberry-pi-tablet-for-your-creative-proje) touchscreen connected), boot it up and connect it to your local network via Ethernet or WiFi
* Clone this GitHub project onto a desktop computer and import the project into [Android Studio](https://developer.android.com/studio/index.html)
* Connect your desktop computer to the development board using `adb connect {ip address}` where `{ip address}` is the IP address displayed on the screen of your development board
* Select the "armSmart_displayDebug" build variant from the Build Variants panel in Android Studio
* Click the "Run App" button to deploy the app to your development board over adb

## Tips
### Set Display Density

The Raspberry Pi will report the same display density (240dpi) for any screen plugged into it, which may make the UI appear too large or too small.

You can set the display density on the command line using:
```
$ adb shell
$ wm density <new density>
```
where <new density> is your desired display density. For the official Raspberry Pi 7" touch screen I recommend:

```
$ wm density 142
```

### Flip Display

How to flip the display on a Raspberry Pi touchscreen (needed for some cases which mount the display upside down).

Plug the SD card into an SD card reader and mount the boot partition (you may need to create the `/mnt/sd` directory first and replace `disk2s1` with correct device reference for your computer):
```
$ sudo mount -t msdos /dev/disk2s1 /mnt/sd
```

Edit config.txt:
```
$ cd /mnt/sd
$ sudo nano config.txt
```

Add line:
```
lcd_rotate=2
```

Unmount the SD card:
```
$ sudo umount /mnt/sd
```

Alternatively you can flip the orientation in AndroidManifest.xml, but it will only apply to the Kitfox app:
```
<activity android:name=".MainActivity" android:screenOrientation="reverseLandscape">
```