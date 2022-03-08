package com.example.camstrm;


import android.util.Log;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;


public class Server {
    protected static final String TAG = "cam_stream";
    public static volatile  Socket client = null;
    public static volatile  DataOutputStream out;
    public static volatile DataInputStream in;

    public static volatile int byte_len=0;
    public static volatile int height=12;
    public static volatile int width=188;
    public static volatile int seq=0;
    public static volatile ArrayList<ImageData> img_list=new ArrayList<ImageData>();
    public static volatile boolean ready=false;

    public void startServer(){
        Log.i(TAG,"Starting server...");
        new Thread(new ServerStart()).start();
    }

    static class ServerStart implements Runnable {
        public ServerSocket mSocketServer = null;
        public static final int SERVER_PORT = 9600;
        @Override
        public void run() {
            try{
                mSocketServer = new ServerSocket(SERVER_PORT);
            } catch (IOException e) {
                Log.i(TAG, e.getMessage());
            }

            try {
                Log.i(TAG, "connecting...");
                client = mSocketServer.accept();
                Log.i(TAG, "Connected! local port = " + client.getLocalPort());
                Log.i(TAG,String.valueOf(client.isBound()));
                out = new DataOutputStream(client.getOutputStream());
                in = new DataInputStream(new BufferedInputStream(client.getInputStream()));
                Log.i(TAG, "Created data stream");
                out.write(100);
            } catch (IOException e) {
                Log.i(TAG, e.getMessage());
            }


            //wait till the client is ready to receive
            Log.i(TAG, "Waiting till client is ready....");
            try{
                int begin = in.read();
                Log.i(TAG,String.valueOf(begin));
                while(begin!=23){
                    begin=in.read();
                    Log.i(TAG,String.valueOf(begin));
                    ready=true;
                }
            }catch (IOException e) {
                Log.i(TAG, e.getMessage());
            }

            while(true) {
                Log.i(TAG,"list size = " + String.valueOf(img_list.size()));
                if (img_list.size()>0) {
                    try {
                        ImageData id=img_list.remove(0);
                        Log.i(TAG,"list size = " + String.valueOf(img_list.size()));
                        out.write(22);
                        out.writeInt(id.seq);
                        out.writeInt(id.height);
                        out.writeInt(id.width);
                        out.writeInt(id.byte_len);
                        Log.i(TAG,String.valueOf(id.byte_len));
                        out.write(id.data);
                        out.flush();
                        Log.i(TAG,"done writing");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }catch (NullPointerException e){
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
