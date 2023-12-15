package com.example.camstrm;


import android.util.Log;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
public class Server extends Thread {
    //set this to false if you want to stop this thread
    private volatile boolean isRunning = true;
    public ServerSocket mSocketServer = null;
    public static final int SERVER_PORT = 9600;
    static int operation;
    static int camera;
    protected static final String TAG = "cam_stream";
    protected static final String TAG1 = "cam_stream1";
    public static volatile  Socket client = null;
    public static volatile  DataOutputStream out;
    public static volatile DataInputStream in;

    public static volatile int byte_len=0;
    public static volatile int height=12;
    public static volatile int width=188;
    public static volatile int seq=0;
    public static volatile ArrayList<ImageData> img_list=new ArrayList<ImageData>();
    public static volatile boolean ready=false;
    private OptionsCallback callback;

    public Server(OptionsCallback callback){
        this.callback = callback;
    }

    public void stop_thread(){
        isRunning=false;
    }

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
        String begin="not assigned";
        int operation=-1;
        int camera=-1;
        try{
            byte[] buffer = new byte[20];
            int read = in.read(buffer);
            String msg = new String(buffer, StandardCharsets.UTF_8);
            Log.i(TAG,msg);
            String[] arrOfStr = msg.split(",", 0);
            begin=arrOfStr[0];
            operation=Integer.parseInt(arrOfStr[1]);
            camera=Integer.parseInt(arrOfStr[2]);
        }catch (IOException e) {
            Log.i(TAG, e.getMessage());
        }

        if (callback != null) {
            callback.onTaskComplete(operation,camera);
            ready=true;
        }

        while("23".equals(begin) && isRunning) {
            Log.i(TAG,"list size = " + String.valueOf(img_list.size()));
            if (img_list.size()>0) {
                try {
                    ImageData id=img_list.remove(0);
                    Log.i(TAG1,"list size = " + String.valueOf(img_list.size()));
                    out.write(22);
                    //write focal dist if focal stacking
                    out.writeInt((int)(id.fdist*100));
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
        Log.i(TAG,"stopped thread");
    }
    public interface OptionsCallback {
        void onTaskComplete(int option, int camera);
    }
}

