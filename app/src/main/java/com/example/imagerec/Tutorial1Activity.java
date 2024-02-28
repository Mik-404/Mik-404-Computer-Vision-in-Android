package com.example.imagerec;

import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfRect2d;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Rect2d;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.dnn.Dnn;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.dnn.Net;
import org.opencv.imgproc.Imgproc;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class Tutorial1Activity extends CameraActivity implements CvCameraViewListener2 {
    private static final String TAG = "OCVSample::Activity";
    private CameraBridgeViewBase mOpenCvCameraView;
    private Net model;
    private boolean make_dirt;

    private String [] labels = new String[] {"person", "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck", "boat", "traffic light", "fire hydrant", "stop sign", "parking meter", "bench", "bird", "cat", "dog", "horse", "sheep", "cow", "elephant", "bear", "zebra", "giraffe", "backpack", "umbrella", "handbag", "tie", "suitcase", "frisbee", "skis", "snowboard", "sports ball", "kite", "baseball bat", "baseball glove", "skateboard", "surfboard", "tennis racket", "bottle", "wine glass", "cup", "fork", "knife", "spoon", "bowl", "banana", "apple", "sandwich", "orange", "broccoli", "carrot", "hot dog", "pizza", "donut", "cake", "chair", "couch", "potted plant",
            "bed", "dining table", "toilet", "tv", "laptop", "mouse", "remote", "keyboard", "cell phone", "microwave", "oven", "toaster", "sink", "refrigerator", "book", "clock", "vase", "scissors", "teddy bear", "hair drier",
            "toothbrush"};
    private Scalar [] colors = new Scalar[80];


    public Tutorial1Activity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        for (int i = 0; i < colors.length; i++) {
            colors[i] = new Scalar((int) (Math.random() * 255), (int) (Math.random() * 255), (int) (Math.random() * 255));
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial1_activity_java_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        model = Dnn.readNetFromONNX(loadFileFromResource(R.raw.yolov8s));
    }
    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }
    @Override
    public void onResume()
    {
        super.onResume();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.enableView();
    }
    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(mOpenCvCameraView);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }
    @Override
    public void onCameraViewStarted(int width, int height) {
    }
    @Override
    public void onCameraViewStopped() {
    }
    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        if (make_dirt) {
            make_dirt = false;
            Mat obj_img = new Mat();

            Imgproc.cvtColor(inputFrame.rgba(), obj_img, Imgproc.COLOR_RGBA2BGR);
            int w = obj_img.width();
            int h = obj_img.height();
            double scale_x = w / 640.0;
            double scale_y = h / 640.0;
            Mat blob = Dnn.blobFromImage(obj_img, 1.0/255.0, new Size (640, 640));

            model.setInput(blob);

            Mat out = model.forward(model.getUnconnectedOutLayersNames().get(0));
            Core.transpose(out.reshape(1,(int)out.total() / 8400), out);


            ArrayList<Rect2d> save_b1 = new ArrayList<Rect2d>();
            ArrayList<Float> save_s1 = new ArrayList<Float>();
            ArrayList<Integer> save_i1 = new ArrayList<Integer>();


            for (int i = 0; i < out.size().height; i++) {
                Core.MinMaxLocResult res = Core.minMaxLoc(out.row(i).colRange(4,(int)out.size().width));
                if (res.maxVal >= 0.25) {
                    Rect2d box = new Rect2d(out.get(i,0)[0] - (0.5 * out.get(i,2)[0]), out.get(i,1)[0] - (0.5 * out.get(i,3)[0]), out.get(i,2)[0],out.get(i,3)[0]);
                    save_b1.add(box);
                    save_s1.add((float)res.maxVal);
                    save_i1.add((int)res.maxLoc.x);
                }
            }
            Rect2d[] save_b2 = new Rect2d[save_b1.size()];
            float[] save_s2 = new float[save_s1.size()];
            int[] save_i2 = new int[save_i1.size()];

            for (int i = 0; i < save_b1.size(); i++) {
                save_b2[i] = save_b1.get(i);
                save_s2[i] = save_s1.get(i);
                save_i2[i] = save_i1.get(i);
            }
            MatOfRect2d boxes = new MatOfRect2d(save_b2);
            MatOfFloat scores = new MatOfFloat(save_s2);
            MatOfInt indices = new MatOfInt(save_i2);
            MatOfInt result = new MatOfInt();

            Dnn.NMSBoxes(boxes, scores, (float) 0.25, (float)0.45, result, (float)0.5);

            Imgproc.cvtColor(inputFrame.rgba(), obj_img, Imgproc.COLOR_BGR2RGB);
            for (int i = 0; i < result.size().height; i++) {
                int index = (int)result.get(i, 0)[0];
                Rect2d box = new Rect2d (boxes.get(index, 0));
                Rect box2 = new Rect (new double []{box.x * scale_x, box.y * scale_y, box.width * scale_x, box.height* scale_y});

                int class_obj = (int) indices.get(index,0)[0];

                Imgproc.rectangle(obj_img, box2, colors[class_obj],4);
                Imgproc.putText(obj_img, labels[class_obj], new Point(box2.x + 10, box2.y + 50), Imgproc.FONT_HERSHEY_TRIPLEX, 1.7, colors[class_obj]);
            }

            Bitmap image = Bitmap.createBitmap(obj_img.cols(),
                    obj_img.rows(), Bitmap.Config.RGB_565);
            Utils.matToBitmap(obj_img, image);
            String a = saveToInternalStorage(image);
            Intent returnIntent = new Intent();
            returnIntent.putExtra("result",a);
            setResult(1,returnIntent);
            finish();
        }
        return inputFrame.rgba();
    }

    public void funcOnClick (View v){
        make_dirt = true;
    }

    private String saveToInternalStorage(Bitmap bitmapImage){
        ContextWrapper cw = new ContextWrapper(getApplicationContext());

        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);

        File mypath=new File(directory,"profile.jpg");

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mypath);
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return directory.getAbsolutePath();
    }

    private MatOfByte loadFileFromResource(int id) {
        byte[] buffer;
        try {
            // load cascade file from application resources
            InputStream is = getResources().openRawResource(id);
            int size = is.available();
            buffer = new byte[size];
            int bytesRead = is.read(buffer);
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to ONNX model from resources! Exception thrown: " + e);
            (Toast.makeText(this, "Failed to ONNX model from resources!", Toast.LENGTH_LONG)).show();
            return null;
        }
        return new MatOfByte(buffer);
    }
}