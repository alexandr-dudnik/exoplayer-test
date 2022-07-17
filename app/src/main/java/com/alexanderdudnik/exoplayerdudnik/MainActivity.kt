package com.alexanderdudnik.exoplayerdudnik

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
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
    private lateinit var exoPlayer : ExoPlayer
    private lateinit var binding: ActivityMainBinding

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

    @SuppressLint("MissingPermission")
    private fun setupLocationListener() {
        val locationManager: LocationManager = (getSystemService(LOCATION_SERVICE) as? LocationManager)?:return
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1_000L, 10f, locationListener)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupPlayer()

        locationRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        )

        bindViews()
    }

    private fun setupPlayer() {
        exoPlayer =  ExoPlayer.Builder(this).build()

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
        }
    }
}