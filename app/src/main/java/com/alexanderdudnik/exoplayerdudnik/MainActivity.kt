package com.alexanderdudnik.exoplayerdudnik

import android.Manifest
import android.annotation.SuppressLint
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.alexanderdudnik.exoplayerdudnik.databinding.ActivityMainBinding
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.offline.DownloadHelper
import java.io.IOException
import java.util.*
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {
    private val mediaItem by lazy { MediaItem.fromUri(getString(R.string.link_to_video)) }
    private val exoPlayer by lazy { ExoPlayer.Builder(this).build() }
    private var locationManager: LocationManager? = null
    private var sensor: SensorManager? = null
    private lateinit var binding: ActivityMainBinding

    //Acceleration parameters
    private var acceleration = 0f
    private var lastAcceleration = 0f
    //Gyroscope
    private var sensorTimestamp: Long = 0
    private var accelerationX = 0f
    private var lastRotationX = 0f
    private var accelerationZ = 0f
    private var lastRotationZ = 0f

    private var locationListener = object: LocationListener{
        override fun onLocationChanged(p0: Location) {
            if (exoPlayer.isPlaying){
                exoPlayer.seekTo(0L)
                exoPlayer.play()
            }
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            //Nothing to do
        }
    }

    private val task = object : TimerTask(){
        override fun run() {
            runOnUiThread{
                exoPlayer.playWhenReady = true
            }
        }
    }

    private val locationRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.any { it.value && (it.key == Manifest.permission.ACCESS_COARSE_LOCATION || it.key == Manifest.permission.ACCESS_FINE_LOCATION) }){
            setupLocationListener()
        }
    }

    private val sensorListener: SensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            // Fetching x,y,z values
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            when(event.sensor.type){
                Sensor.TYPE_ACCELEROMETER -> {
                    //Check for shake
                    val currentAcceleration = sqrt(x * x + y * y + z * z)
                    val delta: Float = currentAcceleration - lastAcceleration
                    lastAcceleration = currentAcceleration
                    acceleration = acceleration * 0.9f + delta
                    if (acceleration > 5f) {
                        if (exoPlayer.isPlaying){
                            exoPlayer.pause()
                        }
                    }
                }
                Sensor.TYPE_GYROSCOPE -> {
                    if (exoPlayer.isPlaying){
                        if (sensorTimestamp != 0L){
                            val isPortrait = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
                            val dt = (event.timestamp - sensorTimestamp)/1_000_000_000.0f
                            lastRotationZ = lastRotationZ*0.9f + dt * z
                            lastRotationX = lastRotationX*0.9f + dt * if (isPortrait) x else y

                            if (lastRotationZ>0.5f){
                                exoPlayer.seekBack()
                            }
                            if (lastRotationZ<-0.5f){
                                exoPlayer.seekForward()
                            }
                            if (lastRotationX>0.5f){
                                exoPlayer.decreaseDeviceVolume()
                            }
                            if (lastRotationX<-0.5f){
                                exoPlayer.increaseDeviceVolume()
                            }
                        }
                        sensorTimestamp = event.timestamp
                    }

                }
            }

        }
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }

    @SuppressLint("MissingPermission")
    private fun setupLocationListener() {
        locationManager = (getSystemService(LOCATION_SERVICE) as? LocationManager)
        locationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1_000L, 10f, locationListener)
    }

    private fun setupSensorListener(){
        sensor = (getSystemService(SENSOR_SERVICE) as? SensorManager)
        sensor?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let{
            sensor?.registerListener(sensorListener, it , SensorManager.SENSOR_DELAY_FASTEST)
        }
        sensor?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)?.let{
            sensor?.registerListener(sensorListener, it , SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bindViews()

    }

    override fun onStart() {
        super.onStart()

        locationRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        )

        setupSensorListener()

        setupPlayer()

    }

    private fun setupPlayer() {
        DownloadHelper.forMediaItem(
            this,
            mediaItem,
        ).prepare(object : DownloadHelper.Callback{
            override fun onPrepared(helper: DownloadHelper) {
                exoPlayer.addMediaItem(mediaItem)
                exoPlayer.prepare()
                helper.release()
            }

            override fun onPrepareError(helper: DownloadHelper, e: IOException) {
                Toast.makeText(
                    this@MainActivity,
                    e.message?:getString(R.string.downloading_error),
                    Toast.LENGTH_LONG
                ).show()
            }

        })

        Timer().schedule(task, 4_000L)
    }

    private fun bindViews(){
        with(binding.playerView){
            useController = false
            player = exoPlayer
            setOnClickListener {
                if (!exoPlayer.isPlaying) {
                    if (exoPlayer.contentPosition >= exoPlayer.contentDuration){
                        exoPlayer.seekTo(0L)
                    }
                    exoPlayer.play()
                }
            }
        }
    }

    override fun onStop() {
        locationManager?.removeUpdates(locationListener)
        sensor?.unregisterListener(sensorListener)
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        binding.playerView.player = null
        super.onStop()
    }
}