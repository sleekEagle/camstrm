package com.example.camstrm;


import static android.hardware.camera2.CaptureResult.LENS_FOCUS_DISTANCE;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.SessionConfiguration;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.util.Range;
import android.util.SizeF;
import android.widget.TextView;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

public class Camera2Service extends Service {
    protected static final int CAMERA_CALIBRATION_DELAY = 5000;
    protected static final String TAG = "camera2Service";
    protected static final String TAG1 = "camera2Service_info";
    protected static final String TAG2 = "camera2Service_capture";
    protected static final String TAG3 = "singlecapture";

    protected static final int CAMERACHOICE = CameraCharacteristics.LENS_FACING_FRONT;
    protected static long cameraCaptureStartTime;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession session;
    protected ImageReader imageReader;
    protected ImageReader imageReader2;
    protected CameraCharacteristics camCharacteristics;
    protected int seq=0;
    int ONGOING_NOTIFICATION_ID=1;
    private static final int ID_SERVICE = 101;
    private static String camid;
    private static String operation;
    private static int lenseState;
    private static float minFdist;
    private static float fdist;
    private static int minFPS=20;
    private static int maxFPS=30;
    //desired f dists (in m) = [0.1,0.5,0.9,1.3,1.7]
    //the units are in diopters = 1/fDist
    private static List<Float> fDistList=Arrays.asList(10.0f,2.0f,1.1f,0.76f,0.58f);

    protected CameraDevice.StateCallback cameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            Log.d(TAG3, "CameraDevice.StateCallback onOpened camera = "+camera.getId());
            cameraDevice = camera;
            imageReader = ImageReader.newInstance(10, 10, ImageFormat.JPEG, 1 /* images buffered */);
            imageReader.setOnImageAvailableListener(onImageAvailableListener, null);
            try {
                cameraDevice.createCaptureSession(Arrays.asList(imageReader.getSurface()), sessionStateCallback, null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            Log.w(TAG, "CameraDevice.StateCallback onDisconnected");
            cameraDevice = camera;
            closeCameraDevice();
        }

        @Override
        public void onError(@NonNull CameraDevice camera,
                            int error) {
            Log.e(TAG, "CameraDevice.StateCallback onError " + error);
        }
    };

    protected CameraCaptureSession.CaptureCallback capturecallback=new CameraCaptureSession.CaptureCallback(){
        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result){
            Log.d(TAG2,"capture completed");
            lenseState=result.get(CaptureResult.LENS_STATE);
            if(operation.equals("2")){
                fdist=result.get(CaptureResult.LENS_FOCUS_DISTANCE);
                if(lenseState==CameraMetadata.LENS_STATE_STATIONARY) Log.d(TAG3,"fdist= "+String.valueOf(fdist));
            }
            else if(operation.equals("1")) closeCameraDevice();
        }
    };

    protected CameraCaptureSession.StateCallback sessionStateCallback = new CameraCaptureSession.StateCallback() {

        @Override
        public void onReady(CameraCaptureSession session) {
            Log.d(TAG3,"on ready");
            Camera2Service.this.session = session;
            try {
                //if operation is focal stacking
                if(operation.equals("2")) {
                    List<CaptureRequest> builders = new ArrayList<CaptureRequest>();
                    for (Float fd : fDistList) {
                        CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                        builder.addTarget(imageReader.getSurface());
                        Range<Integer> fpsRange = new Range<>(minFPS, 30);
                        builder.set(CaptureRequest.JPEG_ORIENTATION, 180); // hardcoding orientation for the tomy camera
                        builder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange);
                        builder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_OFF);
                        builder.set(CaptureRequest.LENS_FOCUS_DISTANCE,fd);
                        builders.add(builder.build());
                    }
                    session.setRepeatingBurst(builders, capturecallback, null);
                //stream video - autofocus mode
                }else if(operation.equals("0")){
                    CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                    builder.addTarget(imageReader.getSurface());
                    Range<Integer> fpsRange = new Range<>(minFPS, maxFPS);
                    builder.set(CaptureRequest.JPEG_ORIENTATION, 180); // hardcoding orientation for the tomy camera
                    builder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,fpsRange);
                    session.setRepeatingRequest(builder.build(),capturecallback,null);
                //if we just need a single photo
                }else if(operation.equals("1")){
                    Log.d(TAG3,"capturing image...");
                    CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                    builder.addTarget(imageReader.getSurface());
                    builder.set(CaptureRequest.JPEG_ORIENTATION, 180);
                    session.capture(builder.build(),capturecallback,null);
                }
                //session.capture(createCaptureRequest(), null, null);
                cameraCaptureStartTime = System.currentTimeMillis();
            } catch (CameraAccessException e) {
                Log.e(TAG, e.getMessage());
            }
        }


        @Override
        public void onConfigured(CameraCaptureSession session) {

        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
        }
    };

    protected ImageReader.OnImageAvailableListener onImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        //when images are available from camera they appear in this method
        @Override
        public void onImageAvailable(ImageReader reader) {
            Log.i(TAG1, "on image available");
            //Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            //Log.i(TAG,"displaying " + timestamp);
            Image img = reader.acquireLatestImage();
            ByteBuffer buf = img.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buf.remaining()];
            buf.get(bytes);


            if(Server.client!=null && Server.client.isBound() && Server.ready && lenseState==CameraMetadata.LENS_STATE_STATIONARY){
                Log.d(TAG, "on image available ");
                //Log.d(TAG, "not null");
                if (img != null) {
                    Server.seq+=1;
                    //Log.i(TAG,String.valueOf(bytes.length));
                    //keep the buffer to a fixed max length
                    //encode images as objects of class ImageData
                    ImageData data=new ImageData(seq,img.getHeight(),img.getWidth(),bytes.length,fdist,bytes);
                    if(Server.img_list.size() < 5){
                        Log.d(TAG, "added image to list");
                        //add the ImageData object to a list in the class Server so that the server will send them
                        //through the TCP link over ADB to the computer
                        Server.img_list.add(data);
                    }
                }
            }
            if (img != null) {
                img.close();
            }
            seq+=1;
        }
    };

    public void readyCamera(String cameraId) {
        CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            CameraCharacteristics cameraCharacteristics = manager.getCameraCharacteristics(cameraId);
            minFdist=cameraCharacteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        try {
//            String pickedCamera = getCamera(manager);
            String pickedCamera = cameraId; // using the hardcoded cameraId instead of picking with code
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            manager.openCamera(cameraId, cameraStateCallback, null);
            Log.d(TAG, "imageReader created");
        } catch (CameraAccessException e) {
            Log.e(TAG, e.getMessage());
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public int onStartCommand(Intent intent,
                              int flags,
                              int startId) {
        operation = intent.getStringExtra("operation");
        camid = intent.getStringExtra("camid");
        Log.d(TAG3,"here in start,,,,");
        Log.i(TAG1,"cam id: "+camid);
        Log.i(TAG1,"operation : "+operation);
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            pendingIntent = PendingIntent.getActivity
                    (this, 0, notificationIntent, PendingIntent.FLAG_MUTABLE);
        }
        else
        {
            pendingIntent = PendingIntent.getActivity
                    (this, 0, notificationIntent, PendingIntent.FLAG_ONE_SHOT);
        }

        Notification notification = new NotificationCompat.Builder(this, MyApp.CHANNEL_ID)
                .setContentTitle("Auto Start Service")
                .setContentText(camid)
                .setContentIntent(pendingIntent)
                .build();
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel( MyApp.CHANNEL_ID, MyApp.CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            mNotificationManager.createNotificationChannel(channel);
            new NotificationCompat.Builder(this, MyApp.CHANNEL_ID);
        }

        startForeground(1, notification);
        Log.d(TAG, "onStartCommand flags " + flags + " startId " + startId);
        //String cameraId = intent.getStringExtra("cameraId");
        //print details on all cameras
        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)) {
            CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
            try {
                String[] cameraIdList = manager.getCameraIdList();
                for (String id : cameraIdList) {
                    CameraCharacteristics cameraCharacteristics = manager.getCameraCharacteristics(id);
                    CameraCharacteristics.Key<int[]> scene_modes=cameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES;
                    int[] sm=cameraCharacteristics.get(scene_modes);
                    Set<String> physicalCameraIds = cameraCharacteristics.getPhysicalCameraIds();
                    Log.i(TAG1, "logic ID: " + id + " Physics under ID: " + Arrays.toString(physicalCameraIds.toArray()));
                    Log.i(TAG1, "scene modes : " + Arrays.toString(sm));

                    final int[] cap = cameraCharacteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
                    boolean facingBack=cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)== CameraMetadata.LENS_FACING_BACK;
                    Log.i(TAG1, "facing back?: " + String.valueOf(facingBack));
                    boolean depthCapable=false;
                    boolean manualFocus=false;
                    for (int capability : cap) {
                        boolean capable = capability == CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT;
                        boolean mancapable = capability==CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR;
                        depthCapable=depthCapable || capable;
                        manualFocus=manualFocus||mancapable;
                    }
                    Log.i(TAG1, "is depth capable ?: " + String.valueOf(depthCapable));
                    Log.i(TAG1, "manual focus ?: " + String.valueOf(manualFocus));
                    float min_f=cameraCharacteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);
                    Log.i(TAG1, "min focal distance: " + String.valueOf(min_f));
                    int fcalib=cameraCharacteristics.get(CameraCharacteristics.LENS_INFO_FOCUS_DISTANCE_CALIBRATION);
                    switch(fcalib){
                        case CameraMetadata.LENS_INFO_FOCUS_DISTANCE_CALIBRATION_UNCALIBRATED: Log.i(TAG1,"focus distance calibration : Uncalibrated");
                        break;
                        case CameraMetadata.LENS_INFO_FOCUS_DISTANCE_CALIBRATION_CALIBRATED: Log.i(TAG1,"focus distance calibration : Calibrated");
                        break;
                        case CameraMetadata.LENS_INFO_FOCUS_DISTANCE_CALIBRATION_APPROXIMATE: Log.i(TAG1,"focus distance calibration : approximately calibrated");
                        break;
                    }
                    if(depthCapable){
                        boolean isDepthExclusive=cameraCharacteristics.get(CameraCharacteristics.DEPTH_DEPTH_IS_EXCLUSIVE);
                        Log.i(TAG1,"is depth exclusive ? "+String.valueOf(isDepthExclusive));
                    }
                    //Log.i(TAG1,"is multi? "+String.valueOf(cameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA));
                    Set<String> pids=cameraCharacteristics.getPhysicalCameraIds();
                    Log.i(TAG1,"num physical = "+String.valueOf(pids.size()));
                    Log.i(TAG1,"capabilities: " + Arrays.toString(cap));
                    SizeF sensorSize = cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
                    Log.i(TAG1, "Sensor size: " + sensorSize);
                    float[] focalLengths = cameraCharacteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
                    Log.i(TAG1,"Focal lengths: " +Arrays.toString(focalLengths));
                    if (focalLengths.length > 0) {
                        float focalLength = focalLengths[0];
                        double fov = 2 * Math.atan(sensorSize.getWidth() / (2 * focalLength));
                        Log.i(TAG1, "Calculated FoV: " + fov + " rad");
                    }
                    Log.i(TAG1, "************************************");
                }
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }


        }
        readyCamera(camid);
        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate service");
        super.onCreate();
    }


    @Override
    public void onDestroy() {
        closeCameraDevice();
    }

    void closeCameraDevice() {
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}