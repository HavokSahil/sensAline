package com.havok.decoy

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.widget.EditText
import java.net.DatagramSocket
import java.net.DatagramPacket
import java.net.InetAddress
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
class UdpSenderService: Service() {
    private var isSending = false
    private var isSerialSending = false
    private val udpPort = 4440
    private var udpSocket: DatagramSocket?=null
    public val addrIP = "";
    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }
}