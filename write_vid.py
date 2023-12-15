#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Mon Nov  1 15:51:50 2021

@author: sleekeagle
"""
import socket
from PIL import Image
import cv2
import io
import numpy as np
from datetime import datetime
import os
import traceback
import argparse



# TCP_IP = '127.0.0.1'
TCP_PORT = 9600
seq_all=0
dropped_imgs=0

# if(sys.platform=="win32"):
#     adbpath="C:\\Users\\lahir\\adb\\platform-tools_r33.0.3-windows\\platform-tools\\adb.exe"
# #if linux
# else:
#     adbpath="adb"

def connect(TCP_IP):
    read_int=-1
    print("waiting for connection....")
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    try:
        s.connect((TCP_IP, TCP_PORT))
        print('connection established. Waiting for ACK')
        s.settimeout(5) 
        data=s.recv(1)
        read_int=int.from_bytes(data,"big")
        if read_int==100:
            print('connected')
            return s
    except KeyboardInterrupt:
        print("Ctrl+C pressed. Exiting safely.")
        s.close()
        return -1
    except Exception as e:
        print(e)
        s.close()
        return -1        

    # while(read_int!=100):
    #     try:
            
            
    #         print('connection established. Waiting for ACK')
    #         data=s.recv(1)
    #         read_int=int.from_bytes(data,"big")
    #         print('received ACK')
    #     except KeyboardInterrupt:
    #         print("Ctrl+C pressed. Exiting safely.")
    #         s.close()
    #         return -1
    #     except Exception as e:
    #         print(e)
    # print("connected")
    # return s
    
    
def get_next_image(s):
    global seq_all,dropped_imgs
    try:
        d_type=int.from_bytes(s.recv(1),"big")
        fdist=int.from_bytes(s.recv(4),"big")/100
        if(d_type!=22):
            return -1
        seq=int.from_bytes(s.recv(4),"big")
        height=int.from_bytes(s.recv(4),"big")
        width=int.from_bytes(s.recv(4),"big")
        size=int.from_bytes(s.recv(4),"big")
        if(size>1000000):
            return -1
        img=bytearray()
        #print('received bytes : '+str(size))
        while(size>0):
            read_len=min(size,1024)
            data = s.recv(read_len)
            size -= len(data)
            img+=data
            
        image = Image.open(io.BytesIO(img))
        img_ar=np.array(image)
        if(seq_all==0):
            seq_all=seq
        elif(not(seq==(seq_all+1))):
            dropped_imgs+=1
        seq_all=seq

    except:
        print(traceback.format_exc())
        return -1
    return img_ar,fdist
    
# #make sure the app is installed on the phone
# print("port forwarding....")
# ret=os.system(adbpath+" forward tcp:9600 tcp:9600")
# if(ret==-1):
#     print("port forwarding error. Exiting...")
#     quit()
# print("return value from OS = "+str(ret))

# #stopping the app (we need to restart)
# ret=os.system(adbpath + " shell am force-stop com.example.camstrm")
# if(ret==-1):
#     print("error when trying to stop the app. Exiting...")
#     quit()

# #start the app
# print("starting the camstream app...")
# ret=os.system(adbpath+" shell am start -n com.example.camstrm/com.example.camstrm.MainActivity --es operation " +str(args.operation)+" --es camid " +str(args.camera)
# +" --es dynamiclense " +str(args.dyn))
# if(ret==-1):
#     print("Error when starting app with adb. Exiting...")
#     quit()
# print("return value from OS = "+str(ret))

def read_imgs(TCP_PORT):
    total_imgs=0

    now = datetime.now()
    dt_string = now.strftime("%d-%m-%Y-%H-%M-%S")
    s=connect(TCP_PORT)
    operation=args.operation
    camera=args.camera
    init_str=f'23,{operation},{camera},{-1}'
    s.send(bytes(init_str, 'utf-8'))

    #send operation mode and cameraID
    # begin = struct.pack('!i', 23)
    # s.send(begin)
    # operation = struct.pack('!i', 0)
    # s.send(operation)
    # camera = struct.pack('!i', 12)
    # s.send(camera)

    #get_next_image(s)
    #quit()

    print("reading the first image...")
    image=-1
    while(type(image)!=tuple):
        try:
            image=get_next_image(s)
            print(type(image))
            height,width,layers=image[0].shape 
            if(args.savetype==0 or args.savetype==1):
                if(not(os.path.exists(args.savedir))):
                    os.makedirs(args.savedir)
            if(args.savetype==0):
                fourcc = cv2.VideoWriter_fourcc('M','J','P','G')
                video = cv2.VideoWriter(args.savedir + dt_string+'.avi', fourcc, 30, (width, height))
            elif(args.savetype==1):
                now_ = datetime.now()
                dt_string_ = now_.strftime("%d-%m-%Y-%H-%M-%S.%f")
                RGB_img_first = cv2.cvtColor(image[0], cv2.COLOR_BGR2RGB)
                print(RGB_img_first.shape)
                ret=cv2.imwrite(args.savedir+dt_string_+'_'+str(image[1])+'.jpg', RGB_img_first)
                print('save path : '+args.savedir+dt_string_+'_'+str(image[1])+'.jpg')
                print("saved : "+str(ret))
            if(type(image)==tuple):
                total_imgs+=1 
        except KeyboardInterrupt:
            print("Ctrl+C pressed. Exiting safely.")
            break
        except:
            print(traceback.format_exc())

    print("done reading the first image")
    print('images = ' +str(total_imgs))

    while((args.nimgs==-1) or (total_imgs<args.nimgs)):
        try:
            image=get_next_image(s)
            if(type(image)==tuple):
                total_imgs+=1 
                RGB_img = cv2.cvtColor(image[0], cv2.COLOR_BGR2RGB)
                if(args.display==1):
                    desired_width = 400
                    desired_height = 300
                    resized_image = cv2.resize(RGB_img, (desired_width, desired_height))
                    cv2.imshow('frame',resized_image)
                if(args.savetype==0):  
                    video.write(RGB_img)
                    print("wrote video frame")
                elif(args.savetype==1):
                    now_ = datetime.now()
                    dt_string_ = now_.strftime("%d-%m-%Y-%H-%M-%S.%f") 
                    ret=cv2.imwrite(args.savedir+dt_string_+'_'+str(image[1])+'.jpg', RGB_img)
                    print('save path : '+args.savedir+dt_string_+'_'+str(image[1])+'.jpg')
                    print("saved : "+str(ret))
                if (cv2.waitKey(1) & 0xFF == ord('q')):
                    break
        except KeyboardInterrupt:
            print("Ctrl+C pressed. Exiting safely.")
            break
        except:
            print(traceback.format_exc())


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='camstrm')
    parser.add_argument('--operation', type=int ,default=3,
                        help='0: video stream with auto focus 1: single image capture 2: focal stacking video 3: video stream with fixed focus defualt=0')
    parser.add_argument('--camera', type=int ,default=0, help='camera ID, default=0')
    parser.add_argument('--display', type=int ,default=0, help='0: Do not display the image 1: Display the image, default=0')
    parser.add_argument('--nimgs', type=int ,default=-1, help='number of images to capture. -1 to capture until manually quit. Default=-1')
    parser.add_argument('--dyn', type=int ,default=1, help='Shoud the camera not wait till the lese is stationary (a.k.a dynamic lense) ? (Android property CameraMetadata.LENS_STATE_STATIONARY),Default=1')
    parser.add_argument('--savetype', type=int ,default=2, help='0: save as avi video file 1: save as images with timestamp and focal length 2: Do not save anything, Default=2')
    parser.add_argument('--savedir', type=str ,default='C:\\Users\\lahir\\data\\CPR_experiment\\test\\smartglass\\', help='save directory for the video. In windows add two \s in the path')
    parser.add_argument('--ip', type=str ,default='192.168.0.100',
                        help='IP address of the Android device')
    args = parser.parse_args()

    if(args.savetype==0 or args.savetype==1):
        if(args.savedir==''):
            print('savedir cannot be empty if you want to save the data.')
            quit() 
    read_imgs(args.ip)

# print("done") 
# print('dropped images='+str(dropped_imgs))
# if(args.savetype==0):
#     video.release()
# cv2.destroyAllWindows()

# print("Stopping the camstream app...")
# ret=os.system(adbpath+" shell am force-stop  com.example.camstrm")
# if(ret==-1):
#     print("Error when stopping the app with adb. Exiting...")
#     quit()
# print("return value from OS = "+str(ret))

