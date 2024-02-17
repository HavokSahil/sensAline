package com.havok.decoy;

import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import android.animation.ValueAnimator;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class LineDetector extends CameraActivity implements CvCameraViewListener2 {
    private static final String TAG = "OCVSample::Activity";
    private CameraBridgeViewBase mOpenCvCameraView;
    private Mat frame;
    private Mat mIntermediateMat;
    private Size mSize;
    private List<MatOfPoint> contours;
    private Mat hierarchy;
    private final Scalar lowerBlack = new Scalar(0, 0, 0);
    private final Scalar upperBlack = new Scalar(180, 255, 30);
    public LineDetector() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    private DatagramSocket socket;
    private InetAddress address;
    private final int port = 4440;
    private String udpServerIpAddress = "0.0.0.0";

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);

        udpServerIpAddress = getIntent().getStringExtra("UDP_SERVER_IP");
        try {
            socket = new DatagramSocket();
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }

        //! [ocv_loader_init]
        if (OpenCVLoader.initLocal()) {
            Log.i(TAG, "OpenCV loaded successfully");
        } else {
            Log.e(TAG, "OpenCV initialization failed!");
            (Toast.makeText(this, "OpenCV initialization failed!", Toast.LENGTH_LONG)).show();
            return;
        }
        //! [ocv_loader_init]

        //! [keep_screen]
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //! [keep_screen]

        setContentView(R.layout.activity_line_detector);

        mOpenCvCameraView = findViewById(R.id.tutorial1_activity_java_surface_view);
        mOpenCvCameraView.setMaxFrameSize(640, 480);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
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
        socket.close();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mIntermediateMat = new Mat();
        contours = new ArrayList<>();
        hierarchy = new Mat();

    }

    @Override
    public void onCameraViewStopped() {
        if (mIntermediateMat!= null)
            mIntermediateMat.release();
        mIntermediateMat = null;
    }
    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        contours = new ArrayList<>();
        frame = inputFrame.rgba();
        Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGBA2RGB);
        // Convert frame to HSV
        Imgproc.cvtColor(frame, mIntermediateMat, Imgproc.COLOR_RGB2HSV);
        // Create mask for black color
        Imgproc.GaussianBlur(mIntermediateMat, mIntermediateMat, new Size(9, 9), 0);
        Core.inRange(mIntermediateMat, lowerBlack, upperBlack, mIntermediateMat);
        Imgproc.findContours(mIntermediateMat, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);
        if (!contours.isEmpty()) {
            double maxArea = -1;
            int maxAreaIndex = -1;

            for (int i=0; i<contours.size(); i++) {
                double area = Imgproc.contourArea(contours.get(i));
                if (area>maxArea) {
                    maxArea = area;
                    maxAreaIndex = i;
                }
            }
            Moments M = Imgproc.moments(contours.get(maxAreaIndex));
            if (M.get_m00()!=0) {
                int cx = (int) (M.get_m10()/M.get_m00());
                int cy = (int) (M.get_m01()/M.get_m00());
                sendUDP(cx, cy);
                String text = "CX: "+cx+", CY: "+cy;
                Imgproc.putText(mIntermediateMat, text, new Point(30, 30), Imgproc.FONT_HERSHEY_SIMPLEX, 1, new Scalar(255, 0, 0), 2);
                Imgproc.circle(mIntermediateMat, new Point(cx, cy), 5 , new Scalar(200, 100, 0));
            }
        } else {
            String text = "I dont see a line";
            Imgproc.putText(mIntermediateMat, text, new Point(10, 30), Imgproc.FONT_HERSHEY_SIMPLEX, 1, new Scalar(255, 0, 0), 2);
        }
        hierarchy.release();
        // Apply mask to frame
        return mIntermediateMat;
    }

    private void sendUDP(int cx, int cy) {
        new Thread(() -> {
            try {
                address = InetAddress.getByName(udpServerIpAddress);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            String message = "[L]; CX: "+cx + ", CY: "+cy;
            byte[] sendData = message.getBytes();
            DatagramPacket packet = new DatagramPacket(sendData, sendData.length, address, port);
            try {
                socket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
