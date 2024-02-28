package com.havok.decoy

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.TextView
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.widget.Toast

// UDP server
import java.net.DatagramSocket
import java.net.DatagramPacket
import java.net.InetAddress

// kotlin coroutines
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException


class MainActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private lateinit var accelerometerDatX: TextView
    private lateinit var accelerometerDatY: TextView
    private lateinit var accelerometerDatZ: TextView
    private lateinit var gyroscopeDatX: TextView
    private lateinit var gyroscopeDatY: TextView
    private lateinit var gyroscopeDatZ: TextView
    private lateinit var sendButton: Button
    private var isSending = false
    private var isSerialSending = false
    private lateinit var editIP: EditText
    private var currentIP = "0.0.0.0"
    private lateinit var serialButton: Button
    private lateinit var lineFolButton: Button
    private lateinit var puzzleButton: Button
    private lateinit var serviceIntent: Intent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        accelerometerDatX = findViewById(R.id.accelerometerDataX)
        accelerometerDatY = findViewById(R.id.accelerometerDataY)
        accelerometerDatZ = findViewById(R.id.accelerometerDataZ)
        gyroscopeDatX = findViewById(R.id.gyroscopeDataX)
        gyroscopeDatY = findViewById(R.id.gyroscopeDataY)
        gyroscopeDatZ = findViewById(R.id.gyroscopeDataZ)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        sendButton = findViewById(R.id.udp_send_button)
        editIP = findViewById(R.id.editIP)
        lineFolButton = findViewById(R.id.line_detector_button)
        puzzleButton = findViewById(R.id.opencv_puzzle)
        serialButton = findViewById(R.id.serial_send_button)
        serviceIntent = Intent(this, UDPSendService::class.java)
        serialButton.setOnClickListener {
            if (isSerialSending) {
                serialButton.text = "SSRL"
            } else {
                serialButton.text = "ESRL"
            }
            isSerialSending = !isSerialSending
        }
        puzzleButton.setOnClickListener {
            val intent = Intent(this, Puzzle15Activity::class.java)
            startActivity(intent)
        }
        lineFolButton.setOnClickListener {
            val intent = Intent(this, LineDetector::class.java)
            intent.putExtra("UDP_SERVER_IP", editIP.text.toString())
            startActivity(intent)
        }

        sendButton.setOnClickListener {
            if(!isSending) {
                startSending()
            } else {
                stopSending()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        gyroscope?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }



    override fun onSensorChanged(event: SensorEvent) {
        val formatValue: (Float)->String={"%.2f".format(it)}
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            // Update UI with accelerometer data
            updateLayout("ACC", event.values.map(formatValue))
        } else if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
            // Update UI with gyroscope data
            updateLayout("GYR", event.values.map(formatValue))
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not implemented
    }
    private fun updateLayout(sensorType: String, values: List<String>) {
        if (sensorType=="ACC") {
            val x = accelerometerDatX
            val y = accelerometerDatY
            val z = accelerometerDatZ
            x.text = String.format("%s", values[0])
            y.text = String.format("%s", values[1])
            z.text = String.format("%s", values[2])
        } else {
            val x = gyroscopeDatX
            val y = gyroscopeDatY
            val z = gyroscopeDatZ
            x.text = String.format("%s", values[0])
            y.text = String.format("%s", values[1])
            z.text = String.format("%s", values[2])
        }

    }

    private fun startSending() {
        val ip = editIP.text.toString()
        if (isValidIPAddress(ip)) {
            isSending = true
            currentIP = ip
            serviceIntent.putExtra("UDP_IP_ADDRESS", currentIP)
            startService(serviceIntent)
            sendButton.text = "EUDP"
        } else {
            Toast.makeText(this, "Please Enter a valid IP", Toast.LENGTH_SHORT).show()
        }
    }
    private fun stopSending() {
        stopService(serviceIntent)
        isSending=false
        sendButton.text = "SUDP"
    }

    private fun isValidIPAddress(ip: String): Boolean {
        val parts = ip.split(".")
        if (parts.size!=4) return false
        for (part in parts) {
            val num = part.toIntOrNull()?: return false
            if (num<0|| num>255) return false
            if (part.length>1 && part.startsWith("0")) return false
        }
        return true
    }
}
