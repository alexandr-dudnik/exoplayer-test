package com.alexanderdudnik.exoplayerdudnik

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.alexanderdudnik.exoplayerdudnik.databinding.ActivityMainBinding
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.offline.DownloadHelper
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {
    private val mediaItem by lazy { MediaItem.fromUri(getString(R.string.link_to_video)) }
    private lateinit var exoPlayer : ExoPlayer
    private lateinit var binding: ActivityMainBinding

    private val task = object : TimerTask(){
        override fun run() {
            runOnUiThread{
                exoPlayer.playWhenReady = true
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupPlayer()

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