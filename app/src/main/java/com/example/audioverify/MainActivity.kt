package com.example.audioverify

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import java.net.DatagramSocket
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.*
import android.util.Log
import androidx.core.app.ActivityCompat
import java.io.IOException
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.UnknownHostException

val myPort: Int = TODO()                //Your port that you want to listen to
var socket = DatagramSocket(myPort)

val deviceIP: String = TODO()           //The IP of the device you want to send to (input as a string)
val devicePort: Int = TODO()            //The port of the device you want to send to

class MainActivity : AppCompatActivity() {

    //    var receiveSocket = DatagramSocket(6012)
    lateinit var recorder: AudioRecord

    private val sampleRate = 44100     //The sampling rate. Found that it's best to keep the sampling rate the same for both receiving and sending
    private val channelConfig = AudioFormat.CHANNEL_IN_STEREO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    private var status: Boolean = true

    lateinit var startButton: Button
    lateinit var stopButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ActivityCompat.requestPermissions(this,
            arrayOf("Manifest.permission.RECORD_AUDIO"), PackageManager.PERMISSION_GRANTED)

        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)

        startButton.setOnClickListener{startListener()}
        stopButton.setOnClickListener{stopListener()}

        receiver()
    }

    private fun receiver() {
        class recthread: Thread() {
            @SuppressLint("MissingPermission")
            override fun run() {

//                val intRecordSampleRate = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC)
                val intRecordSampleRate = sampleRate
                val minBufSize = AudioRecord.getMinBufferSize(AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC), AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT)

                val audioTrack = AudioTrack(AudioManager.STREAM_MUSIC,
                    intRecordSampleRate,
                    AudioFormat.CHANNEL_IN_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBufSize,
                AudioTrack.MODE_STREAM)

                val buffer = ByteArray(minBufSize)

                audioTrack.playbackRate = intRecordSampleRate

                audioTrack.play()

                while (true) {
                    val receivePacket = DatagramPacket(buffer, buffer.size)

                    socket.receive(receivePacket)
                    val sentence = receivePacket.data

                    audioTrack.write(receivePacket.data, 0, receivePacket.length)

                    System.out.println("Received: " +sentence)
                }
            }
        }

        val thread = Thread(recthread())
        thread.start()
    }

    private fun stopListener() {
        status = false
    }

    private fun startListener() {
        status = true
        startStreaming()
    }

    private fun startStreaming() {
        class streamThread: Thread() {
            @SuppressLint("MissingPermission")
            override fun run() {
                try {
                    val sendSocket = DatagramSocket()
                    Log.d("VS", "Socket Created")

                    val minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
                    val buffer = ByteArray(minBufSize)

                    Log.d("VS", "Buffer created of size " + minBufSize)
                    var packet: DatagramPacket

                    val destination = InetAddress.getByName(deviceIP)
                    Log.d("VS", "Address retrieved")

                    recorder = AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        sampleRate,
                        channelConfig,
                        audioFormat,
                        minBufSize
                    )

                    Log.d("VS", recorder.state.toString())

                    recorder.startRecording()

                    while(status == true) {

                        packet = DatagramPacket(buffer, buffer.size, destination, devicePort)

                        sendSocket.send(packet)

                    }

                } catch(e: UnknownHostException){
                    Log.e("VS", "UnknownHostException")
                } catch (e: IOException) {
                    Log.e("VS", "IOException")
                    e.printStackTrace()
                }
            }
        }
        val thread = Thread(streamThread())
        thread.start()
    }
}