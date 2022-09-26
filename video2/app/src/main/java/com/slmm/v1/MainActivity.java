package com.slmm.v1;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.longdo.mjpegviewer.MjpegView;

import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    final static String RTSP_URL = "http://192.168.15.150:5000/mjpeg";

    private MediaPlayer _mediaPlayer;
    private SurfaceHolder _surfaceHolder;

    private VideoView _videoView;
    private MjpegView viewer;
    private ImageView _img;
    private TextView _txt;

    private ImageLabeler labeler;

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
        viewer = (MjpegView) findViewById(R.id.mjpegview);
        viewer.setMode(MjpegView.MODE_FIT_WIDTH);
        viewer.setAdjustHeight(true);
        viewer.setSupportPinchZoomAndPan(true);
        viewer.setUrl("http://192.168.15.150:5000/mjpeg");
        viewer.startStream();


        _img = findViewById(R.id.preview);
        _txt = findViewById(R.id.txtOutput);

//when user leaves application
        Button btn = findViewById(R.id.button);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap _bmp = loadBitmapFromView(viewer);
                _img.setImageBitmap(_bmp);
                InputImage image = InputImage.fromBitmap(_bmp, 0);


                labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS);

                labeler.process(image).addOnSuccessListener( labels ->
                        {
                            String outputTxt = "";
                            for (ImageLabel label: labels ) {
                                String texto = label.getText();
                                String confidence = String.valueOf(label.getConfidence());
                                outputTxt += texto + " : "+ confidence +"\n";
                            }
                            _txt.setText(outputTxt);
                        }


                ).addOnFailureListener( e->{ System.out.println("erro");});
            }
        });
        Button btn2 = findViewById(R.id.button2);
        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap _bmp = loadBitmapFromView(viewer);

                InputImage image = InputImage.fromBitmap(_bmp, 0);


                ObjectDetectorOptions options =
                        new ObjectDetectorOptions.Builder()
                                .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
                                .enableMultipleObjects()
                                .build();

                ObjectDetector objectDetector = ObjectDetection.getClient(options);
                objectDetector.process(image)
                        .addOnSuccessListener( detectedObjects -> {

                            _img.setImageBitmap(drawBox(detectedObjects, _bmp));

                            for (DetectedObject obj: detectedObjects ) {
                                Rect rect = obj.getBoundingBox();
                                Bitmap croppedBitmap = Bitmap.createBitmap(
                                        _bmp, rect.left, rect.top, rect.width(), rect.height());


                                InputImage image1 = InputImage.fromBitmap(croppedBitmap, 0);

                                labeler.process(image1)
                                        .addOnSuccessListener(labels ->
                                        {
                                            String outputTxt = "";
                                            for (ImageLabel label : labels) {
                                                String texto = label.getText();
                                                String confidence = String.valueOf(label.getConfidence());
                                                outputTxt += texto + " : " + confidence + "\n";
                                            }
                                            _txt.setText(outputTxt);
                                        })
                                        .addOnFailureListener(e -> {
                                            System.out.println("erro");
                                        });
                            }
                        })
                        .addOnFailureListener( e -> {});

            }
        });

    }

    
    public static Bitmap drawBox(List<DetectedObject> objects, Bitmap bmp){

        Bitmap _bmp = bmp.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(_bmp);
        int thisLabel = 0;
        for (DetectedObject obj : objects){
            Rect rect = obj.getBoundingBox();
            Paint paint = new Paint();
            paint.setColor(Color.RED);
            paint.setStyle(Paint.Style.STROKE);
            paint.setTextSize(22.0f);
            paint.setStrokeWidth(2.0f);
            paint.setAntiAlias(true);
            canvas.drawRect(rect,paint);
            canvas.drawText(String.valueOf(thisLabel),
                        rect.left,
                        rect.top, paint );
            thisLabel++;
        }
        return _bmp;
    }


    public static Bitmap loadBitmapFromView(View v) {
        Bitmap b = Bitmap.createBitmap( v.getLayoutParams().width, v.getLayoutParams().height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        v.layout(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
        v.draw(c);
        return b;
    }

    @Override
    public void onDestroy(){
        viewer.stopStream();
        super.onDestroy();

    }



}