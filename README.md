# camstrm
Stream camera feed of Android device to computer via ADB

## what does this do 
this repo contain and Android app and a python3 script that runs on a computer.
Android app access the camera of the Android device and sends image frames over TCP connection over ADB.
pytho3 script access these images from the TCP port and display them and stores it as a video at the same time.

## Dependencies
1. Pillow 
2. socket 
3. openCV
4. numpy

## how to use
1. git clone https://github.com/sleekEagle/camstrm.git
2. turn on the Android device
3. Turn on developer options and USB debugging (also WiFi debugging if you are using it over WiFi) on the Android device
4. establish connection over ADB (USB or WiFi)
5. make sure the TCP port number on both the Android app and the python3 script are the same.

## Options 
Help 
```
python write_vid.py -h
```
Other options
```
usage: write_vid.py [-h] [--operation OPERATION] [--camera CAMERA]
                    [--savetype SAVETYPE] [--savedir SAVEDIR]

camstrm

optional arguments:
  -h, --help            show this help message and exit
  --operation OPERATION
                        0:video stream 1: single image capture 2: focal
                        stacking video
  --camera CAMERA       camera ID
  --savetype SAVETYPE   0:save as avi video file 1:save as images with
                        timestamp
  --savedir SAVEDIR     save directory for the video
```

To run in video streaming mode, with camera 0, save the video as an avi video file, in the path given in --savedir argument
```
python write_vid.py --operation 0 --camera 0 --savetype 0 --savedir /path/to/save/video/file/
```
To quit : Press Esc on the image display window.


# tips
you can use scrcpy program from https://github.com/Genymobile/scrcpy
to mirror screen of an Android device to the computer, so you do not have to look at the 
device screen while you are working. 
 
## Running apps via ADB
start the adb shell by 
```
adb shell
```

start the app by 
```
am start -n com.example.camstrm/com.example.camstrm.MainActivity
``` 

kill the app by 
```
am force-stop com.example.camstrm
```


## Running ADB via Wi-Fi instead of through USB cable
on the computer type:
```
adb tcpip 5555
adb shell ip addr show wlan0
```
and copy the IP address after the "inet" until the "/". 

on the computer type:
```
adb connect ip-address-of-device:5555
```

You can disconnect the USB cable now
use 
```
adb devices
```
to check if the device is still attached. 


