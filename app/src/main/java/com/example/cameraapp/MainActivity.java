package com.example.cameraapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;


/* MainActivity class */
public class MainActivity extends AppCompatActivity {

    private Size previewsize;                              // previewsize object
    private Size jpegSizes[] = null;                       // jpegSize array object
    private TextureView textureView;                       // textureView object
    private CameraDevice cameraDevice;                     // cameraDevice object
    private CaptureRequest.Builder previewBuilder;         // previewBuilder object
    private CameraCaptureSession previewSession;           // previewSession object
    Button getpicture;                                     // getpicture object

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();        // ORIENTATIONS objects


    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {                            // onCreate method
        super.onCreate(savedInstanceState);                                         // savedInstanceState called on  onCreate super
        setContentView(R.layout.activity_main);                                     // setContentView with activity_main layout

        textureView = (TextureView) findViewById(R.id.textureview);                 // textureview from xml assigned to textureView

        textureView.setSurfaceTextureListener(surfaceTextureListener);              // surfaceTextureListener set on textureView

        getpicture = (Button) findViewById(R.id.getpicture);                        // getpicture assigned view getpicture button xml

        getpicture.setOnClickListener(new View.OnClickListener() {                  // getpicture setOnClickListener
            @Override
            public void onClick(View v) {                                           // onClick method
                getPicture();                                                       // getPicture method called
            }
        });
    }

    void getPicture() {                                                             // getPicture method
        if (cameraDevice == null) {                                                 // check if cameraDevice is available
            return;
        }
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);    // create CameraManager object and assign CAMERA_SERVICE

        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());   // CameraCharacteristics object and assignment


            if (characteristics != null) {   // if characteristics not empty

                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);  // set image format
            }


            int width = 640, heigth = 480;
            if (jpegSizes != null && jpegSizes.length > 0) {  // if jpegSizes not empty
                width = jpegSizes[0].getWidth();
                heigth = jpegSizes[0].getHeight();
            }
            ImageReader reader = ImageReader.newInstance(width, heigth, ImageFormat.JPEG, 1);  // ImageReader object assigned new instance

            List<Surface> outputSurfaces = new ArrayList<Surface>(2);   // outputSurfaces ArrayList object
            outputSurfaces.add(reader.getSurface());                                 // outputSurfaces added ImageReader object
            outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));        // outputSurfaces added surface textureView


            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);  // captureBuilder object assigned
            captureBuilder.addTarget(reader.getSurface());                            // captureBuilder assigned target surface
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);  // captureBuilder set CONTROL_MODE

            int rotation = getWindowManager().getDefaultDisplay().getRotation();    // rotation object assigned

            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));    // captureBuilder set capture request

            ImageReader.OnImageAvailableListener imageAvailableListener = new ImageReader.OnImageAvailableListener() {   // imageAvailableListener
                @Override
                public void onImageAvailable(ImageReader reader) {           //  onImageAvailable method
                    Image image = null;
                    try {
                        image = reader.acquireLatestImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();

                        byte[] bytes = new byte[buffer.capacity()];

                        buffer.get(bytes);
                        save(bytes);
                    } catch (Exception ee) {

                    } finally {
                        if (image != null)
                            image.close();
                    }
                }

                void save(byte[] bytes) {
                    File file12 = getOutputMediaFile();
                    OutputStream outputStream = null;

                    try {
                        outputStream = new FileOutputStream(file12);
                        outputStream.write(bytes);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            if (outputStream != null)
                                outputStream.close();
                        } catch (Exception e) {
                        }
                    }
                }
            };


            HandlerThread handlerThread = new HandlerThread("takepicture");

            handlerThread.start();

            final Handler handler = new Handler(Looper.getMainLooper());

            reader.setOnImageAvailableListener(imageAvailableListener, handler);

            final CameraCaptureSession.CaptureCallback previewSSession = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
                    super.onCaptureStarted(session, request, timestamp, frameNumber);
                }

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);

                    startCamera();
                }
            };

            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        session.capture(captureBuilder.build(), previewSSession, handler);
                    } catch (Exception e) {

                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                }
            }, handler);
        } catch (Exception e) {

        }
    }

    public void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String camerId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(camerId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            previewsize = map.getOutputSizes(SurfaceTexture.class)[0];
            manager.openCamera(camerId, stateCallback, null);
        } catch (Exception e) {
        }
    }

    private TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    private CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            startCamera();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {

        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {

        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        if (cameraDevice != null) {
            cameraDevice.close();
        }
    }

    void startCamera() {
        if (cameraDevice == null || !textureView.isAvailable() || previewsize == null) {
            return;
        }

        SurfaceTexture texture = textureView.getSurfaceTexture();
        if (texture == null) {
            return;
        }

        texture.setDefaultBufferSize(previewsize.getWidth(), previewsize.getHeight());
        Surface surface = new Surface(texture);

        try {
            previewBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        } catch (Exception e) {
        }

        previewBuilder.addTarget(surface);

        try {
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    previewSession = session;
                    getChangedPreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                }

            }, null);

        } catch (Exception e) {

        }


    }
            public void getChangedPreview(){
        if (cameraDevice == null) {
            return;
        }
                previewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                            HandlerThread thread = new HandlerThread("changed preview");
                            thread.start();

                            Handler handler = new Handler(thread.getLooper());

                            try {
                                previewSession.setRepeatingRequest(previewBuilder.build(), null, handler);
                                }catch (Exception e){}

                                }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;

        }

        return super.onOptionsItemSelected(item);
}

private static File getOutputMediaFile() {
        File mediaStorageDir = new File(Environment.getExternalStorageState(), "MyCameraApp");

        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");

                return null;
            }
        }

        // Create media file name

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        File mediaFile;
        mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");

        return mediaFile;
            }
}