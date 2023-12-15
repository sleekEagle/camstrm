# camstrm
Stream camera feed of Android device to computer via WiFi

## what does this do 
this repo contains an Android app and a python3 script that runs on a computer.
Android app access the camera of the Android device and sends image frames over WiFi using TCP.
python3 script access these images from the TCP port and/or display them and/or stores it as a video/images at the same time.

## Dependencies
1. Pillow 
2. socket 
3. openCV
4. numpy

## how to use
1. git clone https://github.com/sleekEagle/camstrm.git
2. turn on the camstrm app on the android device
3. use the IP address shown in the mobile phone on the python code (as the argument --ip)

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
                        0:video stream with auto focus 1: single image capture 2: focal
                        stacking video 3: video stream with fixed focus
  --camera CAMERA       camera ID
  --savetype SAVETYPE   0:save as avi video file 1:save as images with
                        timestamp
  --savedir SAVEDIR     save directory for the video
  --ip                  IP address of the Android device
```

To run in video streaming mode, with camera 0, save the video as an avi video file, in the path given in --savedir argument
```
python write_vid.py --operation 0 --camera 0 --savetype 0 --savedir /path/to/save/video/file/
```
To quit : Press the key 'q' on the image display window. 
or
CRTL + c on the terminal



## Getting details about the cameras available in this device
1. Connect the Android device to ADB. This can either be over USB or WiFi (turn on WiFi debugging and pair, etc.)
2. Monitor logcat with the TAG "cameradetails"
Use command 
```
adb logcat | grep "cameradetails"
```
For example, Google Pixel 7 Pro gives us the following details:
```
Camera Details....
logic ID: 0 Physics under ID: [2, 3, 4, 5, 6]
scene modes : [0, 1]
facing back?: true
is depth capable ?: false
manual focus ?: true
min focal distance: 9.523809
focus distance calibration : approximately calibrated
num physical = 5
capabilities: [0, 1, 5, 2, 6, 19, 3, 9, 11, 18]
Sensor size: 9.792x7.3728
Focal lengths: [6.81]
Calculated FoV: 1.24665275985497 rad
************************************
logic ID: 1 Physics under ID: [7, 8]
scene modes : [0, 1]
facing back?: false
is depth capable ?: false
manual focus ?: true
min focal distance: 0.0
focus distance calibration : approximately calibrated
num physical = 2
capabilities: [0, 1, 5, 2, 6, 19, 3, 11, 18]
Sensor size: 4.1968x2.98656
Focal lengths: [2.74]
Calculated FoV: 1.3071230616267635 rad
************************************
```