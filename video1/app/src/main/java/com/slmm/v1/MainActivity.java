package com.slmm.v1;

import static java.lang.Math.max;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Pair;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.VideoView;

import com.google.mlkit.common.model.LocalModel;
import com.google.mlkit.vision.objects.ObjectDetectorOptionsBase;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;
import com.longdo.mjpegviewer.MjpegView;
import com.google.mlkit.vision.objects.ObjectDetectorOptionsBase.DetectorMode;

import java.io.IOException;
//import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions;


public class MainActivity extends AppCompatActivity implements MediaPlayer.OnPreparedListener,
        SurfaceHolder.Callback {
    private static final String TAG = "StillImageActivity";
    final static String RTSP_URL = "http://192.168.15.150:5000/mjpeg";

    private MediaPlayer _mediaPlayer;
    private SurfaceHolder _surfaceHolder;

    private VideoView _videoView;
    private MjpegView viewer;
    private ImageView _img;
    private Bitmap _bitmap;

    private ImageView preview;
    private GraphicOverlay graphicOverlay;

    private VisionImageProcessor imageProcessor;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
/*
        // Set up a full-screen black window.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window window = getWindow();
        window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        window.setBackgroundDrawableResource(android.R.color.black);

        setContentView(R.layout.activity_main);
*/
        // Configure the view that renders live video.
      /*  SurfaceView surfaceView =
                (SurfaceView) findViewById(R.id.surfaceView);
        _surfaceHolder = surfaceView.getHolder();
        _surfaceHolder.addCallback(this);
        _surfaceHolder.setFixedSize(320, 240);
*/
     /*   _videoView = (VideoView) findViewById(R.id.vv1);
        Uri source = Uri.parse(RTSP_URL);

        try {
            // Specify the IP camera's URL and auth headers.
            _videoView.setVideoURI(source);
            _videoView.start();
        }
        catch (Exception e) {}
*/
        preview = findViewById(R.id.preview);
        graphicOverlay = findViewById(R.id.graphic_overlay);

        viewer = (MjpegView) findViewById(R.id.mjpegview);
        viewer.setMode(MjpegView.MODE_FIT_WIDTH);
        viewer.setAdjustHeight(true);
        viewer.setSupportPinchZoomAndPan(true);
        viewer.setUrl("http://192.168.15.150:5000/mjpeg");
        viewer.startStream();


        //_img = findViewById(R.id.imageView);
//when user leaves application
        Button btn = findViewById(R.id.button);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                _bitmap = loadBitmapFromView(viewer);
          //      _img.setImageBitmap(_bitmap);

                createImageProcessor();
                tryReloadAndDetectInImage();
            }
        });
        Button btn2 = findViewById(R.id.button2);
        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                _bitmap = loadBitmapFromView(viewer);
                //      _img.setImageBitmap(_bitmap);

                createImageProcessor2();
                tryReloadAndDetectInImage();
            }
        });


    }

    public static Bitmap loadBitmapFromView(View v) {
        Bitmap b = Bitmap.createBitmap( v.getLayoutParams().width, v.getLayoutParams().height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        v.layout(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
        v.draw(c);
        return b;
    }


    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        createImageProcessor();
        tryReloadAndDetectInImage();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (imageProcessor != null) {
            imageProcessor.stop();
        }
    }

    @Override
    public void onDestroy(){
        viewer.stopStream();
        super.onDestroy();
        if (imageProcessor != null) {
            imageProcessor.stop();
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        _mediaPlayer.start();
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder sh) {
        _mediaPlayer = new MediaPlayer();
        _mediaPlayer.setDisplay(_surfaceHolder);

        Context context = getApplicationContext();
       // Map<String, String> headers = getRtspHeaders();
        Uri source = Uri.parse(RTSP_URL);

        try {
            // Specify the IP camera's URL and auth headers.
            _mediaPlayer.setDataSource(context, source);//, headers);

            // Begin the process of setting up a video stream.
            _mediaPlayer.setOnPreparedListener(this);
            _mediaPlayer.prepareAsync();
        }
        catch (Exception e) {}
    }


    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        _mediaPlayer.release();
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }


    private void createImageProcessor() {
        if (imageProcessor != null) {
            imageProcessor.stop();
        }
        try {
             Log.i(TAG, "Using Object Detector Processor");
            ObjectDetectorOptionsBase objectDetectorOptions =
                getObjectDetectorOptions(
                    this,
                    R.string.pref_key_still_image_object_detector_enable_multiple_objects,
                    R.string.pref_key_still_image_object_detector_enable_classification,
                    ObjectDetectorOptions.SINGLE_IMAGE_MODE);
             imageProcessor = new ObjectDetectorProcessor(this, objectDetectorOptions);
        } catch (Exception e) {
            Log.e(TAG, "Can not create image processor: " + e);
            Toast.makeText(
                       getApplicationContext(),
                      "Can not create image processor: " + e.getMessage(),
                       Toast.LENGTH_LONG)
                    .show();
        }
    }

    private void createImageProcessor2() {
        if (imageProcessor != null) {
            imageProcessor.stop();
        }
        try {
            imageProcessor = new FaceDetectorProcessor(this);
        } catch (Exception e) {
            Log.e(TAG, "Can not create image processor: " + e);
            Toast.makeText(
                            getApplicationContext(),
                            "Can not create image processor: " + e.getMessage(),
                            Toast.LENGTH_LONG)
                    .show();
        }
    }

    private static ObjectDetectorOptions getObjectDetectorOptions(
            Context context,
            @StringRes int prefKeyForMultipleObjects,
            @StringRes int prefKeyForClassification,
            @DetectorMode int mode) {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        boolean enableMultipleObjects =
                sharedPreferences.getBoolean(context.getString(prefKeyForMultipleObjects), false);
        boolean enableClassification =
                sharedPreferences.getBoolean(context.getString(prefKeyForClassification), true);

        ObjectDetectorOptions.Builder builder =
                new ObjectDetectorOptions.Builder().setDetectorMode(mode);
        if (enableMultipleObjects) {
            builder.enableMultipleObjects();
        }
        if (enableClassification) {
            builder.enableClassification();
        }
        return builder.build();
    }

    private void tryReloadAndDetectInImage() {
        Log.d(TAG, "Try reload and detect image");
        try {

            Bitmap imageBitmap = _bitmap;
            if (imageBitmap == null) {
                return;
            }

            // Clear the overlay first
            graphicOverlay.clear();



            preview.setImageBitmap(imageBitmap);

            if (imageProcessor != null) {
                graphicOverlay.setImageSourceInfo(
                        imageBitmap.getWidth(), imageBitmap.getHeight(), /* isFlipped= */ false);
                imageProcessor.processBitmap(imageBitmap, graphicOverlay);
            } else {
                Log.e(TAG, "Null imageProcessor, please check adb logs for imageProcessor creation error");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error retrieving saved image");
        }
    }
}