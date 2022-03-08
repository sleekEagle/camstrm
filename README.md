# camstrm
Stream camera feed of Android device to computer via ADB

## what does this do 
this repo contain and Android app and a python3 script that runs on a computer.
Android app access the camera of the Android device and sends image frames over TCP connection over ADB.
pytho3 script access these images from the TCP port and display them and stores it as a video at the same time.

## how to use
1. git clone https://github.com/sleekEagle/camstrm.git
2. turn on the Android device
3. establish connection over ADB (USB or WiFi)
4. make sure the TCP port number on both the Android app and the python3 script are the same.
assuming it is 9600,
5. perform port forwarding with the command 
   adb forward tcp:9600 tcp:9600
6. start the Android app first from the device
7. goto the project dir
8. make sure the path is correct (to store the video) 
9. execute the python script
	python3 write_vid.py 

