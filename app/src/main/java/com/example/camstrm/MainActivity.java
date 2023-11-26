package com.example.camstrm;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class MainActivity extends AppCompatActivity {
    private static final String[] CAMERA_PERMISSION = new String[]{Manifest.permission.CAMERA};
    private static final int CAMERA_REQUEST_CODE = 10;
    protected static final String TAG = "cam_stream";
    protected static final String TAG1 = "cam_stream1";
    private TextView myTextView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        myTextView = (TextView)findViewById(R.id.myTextView);
        //get WiFi address
        String localIpAddress = getLocalIpAddress();
        updateText(localIpAddress);

        getWindow(). addFlags (WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Log.i(TAG1,"in main");

//        String operation = getIntent().getStringExtra("operation");
//        String camid = getIntent().getStringExtra("camid");
//        String dynamiclense = getIntent().getStringExtra("dynamiclense");

        // start server that sends frames to computer over ADB
        Server server=new Server(new Server.OptionsCallback(){
            @Override
            public void onTaskComplete(int operation,int camera) {
                // Handle the result obtained from the thread here
                Log.i(TAG1, "operation: " + operation);
                Log.i(TAG1, "camera: " + camera);
                start_camera(operation,camera);
            }
        });
        server.start();

    }

    private void start_camera(int operation,int camid){
        //check of the user has given permission for this app to use camera
        checkPermissionsOrRequest();
        if (hasCameraPermission()) {
            Intent cameraServiceIntent = new Intent(MainActivity.this, Camera2Service.class);
            Log.i(TAG,"starting cam..");

            // camera apis expect the cameraId to be a string
            // from testing, regular lens = 0, wide angle = 1
//            String idString = Integer.toString(0);
            /*
            operation=2 : focal stacking mode
            operation=0 :  video stream mode (autofocus)
            operation=1 : capture a single image
            * */
            String operation_str=String.valueOf(operation);
            String camid_str=String.valueOf(camid);
            String dynamiclense=String.valueOf(0);
            if(operation_str!=null) cameraServiceIntent.putExtra("operation", operation_str);
            else cameraServiceIntent.putExtra("operation", "0");
            if(camid_str!=null) cameraServiceIntent.putExtra("camid", camid_str);
            else cameraServiceIntent.putExtra("camid", "0");
            if(dynamiclense!=null) cameraServiceIntent.putExtra("dynamiclense", dynamiclense);
            else cameraServiceIntent.putExtra("dynamiclense", "0");

            Log.i(TAG,"starting service...");
            startService(cameraServiceIntent);
            //start service which access the camera and the stream of camera image frames
            //see the class Camera2Service.java class
            ContextCompat.startForegroundService(this, cameraServiceIntent);
        } else {
            //if the user has not granted permission, request it
            requestPermission();
        }

    }

    private void checkPermissionsOrRequest() {
        // The request code used in ActivityCompat.requestPermissions()
        // and returned in the Activity's onRequestPermissionsResult()
        int PERMISSION_ALL = 1;
        String[] permissions = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA,
                Manifest.permission.WAKE_LOCK,
                Manifest.permission.ACCESS_NETWORK_STATE
        };

        if (!hasPermissions(this, permissions)) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_ALL);
        }
    }

    public boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "hasPermissions: no permission for " + permission);
                    return false;
                } else {
                    Log.d(TAG, "hasPermissions: YES permission for " + permission);
                }
            }
        }
        return true;
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED;
    }
    private void requestPermission() {
        ActivityCompat.requestPermissions(
                this,
                CAMERA_PERMISSION,
                CAMERA_REQUEST_CODE
        );
    }

    private void updateText(String newText) {
        if (myTextView != null) {
            myTextView.setText(newText);
        }
        else{
            Log.i(TAG,"null pointer");
        }

    }

    public static String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                // filters out 127.0.0.1 and inactive interfaces
                if (iface.isLoopback() || !iface.isUp())
                    continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    String ip = addr.getHostAddress();
                    // Check if the IP address is in the IPv4 format
                    if (ip.matches("\\d+(\\.\\d+){3}")) {
                        return ip;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}