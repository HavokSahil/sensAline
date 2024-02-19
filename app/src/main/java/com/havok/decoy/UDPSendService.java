package com.havok.decoy;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPSendService extends Service implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor gyroscopeSensor;
    private Sensor accelerometer;
    private DatagramSocket udpSocket;
    private boolean isSending = false;
    private int udpPort = 4440;
    private String ip = "0.0.0.0";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        try {
            udpSocket = new DatagramSocket();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Toast.makeText(this, "Object Created", Toast.LENGTH_SHORT);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        isSending = true;
        ip = intent.getStringExtra("UDP_IP_ADDRESS").toString();
        sensorManager.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isSending = false;
        sensorManager.unregisterListener(this);
        udpSocket.close();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not needed for this implementation
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            String gyroX = String.format("%.2f", event.values[0]);
            String gyroY = String.format("%.2f", event.values[1]);
            String gyroZ = String.format("%.2f", event.values[2]);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (isSending && !udpSocket.isClosed()) {
                        try {
                            String data = "GYR " + gyroX + ", " + gyroY + ", " + gyroZ;
                            byte[] bytes = data.getBytes();
                            InetAddress address = InetAddress.getByName(ip);
                            DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, udpPort);
                            udpSocket.send(packet);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            String accelerationX = String.format("X: %.2f", event.values[0]);
            String accelerationY = String.format("Y: %.2f", event.values[1]);
            String accelerationZ = String.format("Z: %.2f", event.values[2]);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (isSending && !udpSocket.isClosed()) {
                        try {
                            String data = "ACC " + accelerationX + ", " + accelerationY + ", " + accelerationZ;
                            byte[] bytes = data.getBytes();
                            InetAddress address = InetAddress.getByName(ip);
                            DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, udpPort);
                            udpSocket.send(packet);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        }
    }
}
