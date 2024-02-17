package com.havok.decoy

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.TextView
import android.widget.Button
import android.widget.EditText
import android.widget.TableRow
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
// UDP server
import java.net.DatagramSocket
import java.net.DatagramPacket
import java.net.InetAddress

// kotlin coroutines
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch



class MainActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private lateinit var accelerometerRow: TableRow
    private lateinit var gyroscopeRow: TableRow
    private lateinit var sendButton: Button
    private var isSending = false
    private var isSerialSending = false
    private val udpPort = 4440
    private var udpSocket: DatagramSocket?=null
    private lateinit var editIP: EditText
    private lateinit var serialButton: Button
    private lateinit var lineFolButton: Button
    private lateinit var puzzleButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        accelerometerRow = findViewById(R.id.accelerometerData)
        gyroscopeRow = findViewById(R.id.gyroscopeData)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        sendButton = findViewById(R.id.udp_send_button)
        editIP = findViewById(R.id.editIP)
        lineFolButton = findViewById(R.id.line_detector_button)
        puzzleButton = findViewById(R.id.opencv_puzzle)
        serialButton = findViewById(R.id.serial_send_button)
        serialButton.setOnClickListener {
            if (isSerialSending) {
                serialButton.text = "SERIAL SEND"
            } else {
                serialButton.text = "SERIAL STOP"
            }
            isSerialSending = !isSerialSending
        }
        puzzleButton.setOnClickListener {
            val intent = Intent(this, Puzzle15Activity::class.java)
            startActivity(intent)
        }
        lineFolButton.setOnClickListener {
            val intent = Intent(this, LineDetector::class.java)
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
            val accelerationX = String.format("X: %.2f", event.values[0])
            val accelerationY = String.format("Y: %.2f", event.values[1])
            val accelerationZ = String.format("Z: %.2f", event.values[2])
            // Update UI with accelerometer data
            updateTableLayout(accelerometerRow, "Accelerometer", event.values.map(formatValue))
            GlobalScope.launch(Dispatchers.IO){
                if (isSending) {
                    val data = String.format("ACC %s, %s, %s", accelerationX, accelerationY, accelerationZ)
                        .toByteArray()
                    val address = InetAddress.getByName(editIP.text.toString())
                    val packet = DatagramPacket(data, data.size, address, udpPort)
                    udpSocket?.send(packet)
                }
            }
        } else if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
            val gyroX = String.format("X: %.2f", event.values[0])
            val gyroY = String.format("Y: %.2f", event.values[1])
            val gyroZ = String.format("Z: %.2f", event.values[2])
            // Update UI with gyroscope data
            updateTableLayout(gyroscopeRow, "Gyroscope", event.values.map(formatValue))
            GlobalScope.launch(Dispatchers.IO) {
                if (isSending) {
                    val data = String.format("GYR %s, %s, %s", gyroX, gyroY, gyroZ).toByteArray()
                    val address = InetAddress.getByName(editIP.text.toString())
                    val packet = DatagramPacket(data, data.size, address, udpPort)
                    udpSocket?.send(packet)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not implemented
    }
    private fun updateTableLayout(tableRow: TableRow, sensorType: String, values: List<String>) {
        val x = tableRow.getChildAt(0) as? TextView
        val y = tableRow.getChildAt(1) as? TextView
        val z = tableRow.getChildAt(2) as? TextView

        x?.text = values[0]
        y?.text = values[1]
        z?.text = values[2]
    }

    private fun startSending() {
        udpSocket = DatagramSocket()
        isSending = true
        sendButton.text = "STOP UDP"
    }
    private fun stopSending() {
        udpSocket?.close()
        isSending=false
        sendButton.text = "SEND UDP"
    }
}
