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
import time
from datetime import datetime
import struct
import os
import sys
import traceback
import argparse

parser = argparse.ArgumentParser(description='camstrm')
parser.add_argument('--operation', type=int ,default=0, help='0: video stream 1: single image capture 2: focal stacking video')
parser.add_argument('--camera', type=int ,default=0, help='camera ID')
parser.add_argument('--display', type=int ,default=1, help='0: Do not display the image 1: Display the image')
parser.add_argument('--savetype', type=int ,default=2, help='0: save as avi video file 1: save as images with timestamp and focal length 2: Do not save anything')
parser.add_argument('--savedir', type=str ,default='', help='save directory for the video')
args = parser.parse_args()

if(args.savetype==0 or args.savetype==1):
    if(args.savedir==''):
        print('savedir cannot be empty if you want to save the data.')
        quit()

TCP_IP = '127.0.0.1'
TCP_PORT = 9600

if(sys.platform=="win32"):
    adbpath="C:\\Users\\lahir\\adb\\platform-tools_r33.0.3-windows\\platform-tools\\adb.exe"
#if linux
else:
    adbpath="adb"


seq_all=0
dropped_imgs=0

def connect():
    read_int=-1
    print("waiting for connection....")
    while(read_int!=100):
        try:
            s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            s.connect((TCP_IP, TCP_PORT))
            read_int=int.from_bytes(s.recv(1),"big")
        except Exception as e:
            print(e)
    print("connected")
    return s
    
    
def get_next_image(s):
    global seq_all,dropped_imgs
    try:
        d_type=int.from_bytes(s.recv(1),"big")
        fdist=int.from_bytes(s.recv(4),"big")/100
        #print('fdist: '+str(fdist/100))
        if(d_type!=22):
            return -1
        seq=int.from_bytes(s.recv(4),"big")
        height=int.from_bytes(s.recv(4),"big")
        width=int.from_bytes(s.recv(4),"big")
        size=int.from_bytes(s.recv(4),"big")
        if(size>1000000):
            return -1
        img=bytearray()
        print('received bytes : '+str(size))
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
    
#make sure the app is installed on the phone
print("port forwarding....")
ret=os.system(adbpath+" forward tcp:9600 tcp:9600")
if(ret==-1):
    print("port forwarding error. Exiting...")
    quit()
print("return value from OS = "+str(ret))
#stopping the app (we need to restart)
ret=os.system(adbpath + " shell am force-stop com.example.camstrm")
if(ret==-1):
    print("error when trying to stop the app. Exiting...")
    quit()
#start the app
print("starting the camstream app...")
ret=os.system(adbpath+" shell am start -n com.example.camstrm/com.example.camstrm.MainActivity --es operation " +str(args.operation)+" --es camid " +str(args.camera))
if(ret==-1):
    print("Error when starting app with adb. Exiting...")
    quit()
print("return value from OS = "+str(ret))
    
now = datetime.now()
dt_string = now.strftime("%d-%m-%Y-%H-%M-%S")
s=connect()

p = struct.pack('!i', 23)
s.send(p)

#get_next_image(s)
#quit()


image=-1
while(type(image)!=tuple):
    try:
        image=get_next_image(s)
        height,width,layers=image[0].shape 
        if(args.savetype==0 or args.savetype==1):
            if(not(os.path.exists(args.savedir))):
                os.makedirs(args.savedir)
        if(args.savetype==0):
            fourcc = cv2.VideoWriter_fourcc('M','J','P','G')
            video = cv2.VideoWriter(args.savedir + dt_string+'.avi', fourcc, 30, (width, height))
        elif(args.savetype==1):
            now_ = datetime.now()
            dt_string_ = now.strftime("%d-%m-%Y-%H-%M-%S")
            RGB_img_first = cv2.cvtColor(image[0], cv2.COLOR_BGR2RGB)
            cv2.imwrite(args.savedir+str(now_)+'_'+str(image[1])+'.jpg', RGB_img)
    except:
        print(traceback.format_exc())

while(True):
    try:
        image=get_next_image(s)
        if(type(image)==tuple):
            RGB_img = cv2.cvtColor(image[0], cv2.COLOR_BGR2RGB)
            if(args.display==1):
                cv2.imshow('frame',RGB_img)
            if(args.savetype==0):  
                video.write(RGB_img)
            elif(args.savetype==1):
                now_ = datetime.now()
                dt_string_ = now.strftime("%d-%m-%Y-%H-%M-%S") 
                print(args.savedir+str(now_)+'_'+str(image[1])+'.jpg')
                cv2.imwrite(args.savedir+str(now_)+'_'+str(image[1])+'.jpg', RGB_img)
            if (cv2.waitKey(1) & 0xFF == ord('q')):
                break
    except:
        print(traceback.format_exc())

print("done") 
print('dropped images='+str(dropped_imgs))
if(args.savetype==0):
    video.release()
cv2.destroyAllWindows()

print("Stopping the camstream app...")
ret=os.system(adbpath+" shell am force-stop  com.example.camstrm")
if(ret==-1):
    print("Error when stopping the app with adb. Exiting...")
    quit()
print("return value from OS = "+str(ret))

