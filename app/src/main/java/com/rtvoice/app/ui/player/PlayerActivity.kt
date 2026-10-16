package com.rtvvoice.app.ui.player

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.rtvvoice.app.R
import com.rtvvoice.app.databinding.ActivityPlayerBinding

/**
 * Full-screen video player for show episodes (MP4) and live channels (HLS).
 * Playback is bound to this activity; audio focus is handled by the player.
 */
class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private var player: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.title.text = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        binding.liveBadge.visibility =
            if (intent.getBooleanExtra(EXTRA_LIVE, false)) View.VISIBLE else View.GONE
        binding.status.text = getString(R.string.player_buffering)

        val url = intent.getStringExtra(EXTRA_URL) ?: run { finish(); return }
        initializePlayer(url)
    }

    private fun initializePlayer(url: String) {
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(10_000)
            .build()
            .also { exo ->
                binding.playerView.player = exo
                exo.setMediaItem(MediaItem.fromUri(url))
                exo.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        binding.status.setText(
                            when (playbackState) {
                                Player.STATE_BUFFERING -> R.string.player_buffering
                                Player.STATE_READY -> R.string.player_ready
                                Player.STATE_ENDED -> R.string.player_ended
                                else -> 0
                            }
                        )
                        binding.status.visibility =
                            if (playbackState == Player.STATE_READY) View.GONE else View.VISIBLE
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        Toast.makeText(this@PlayerActivity, R.string.player_error, Toast.LENGTH_LONG).show()
                        binding.status.setText(R.string.player_error)
                        binding.status.visibility = View.VISIBLE
                    }
                })
                exo.prepare()
                exo.playWhenReady = true
            }
    }

    override fun onStop() {
        super.onStop()
        // Video is activity-bound; drop the decoder while in the background.
        player?.playWhenReady = false
    }

    override fun onDestroy() {
        binding.playerView.player = null
        player?.release()
        player = null
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_URL = "url"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_LIVE = "live"

        fun start(context: Context, url: String, title: String, image: String?, live: Boolean = false) {
            context.startActivity(
                Intent(context, PlayerActivity::class.java)
                    .putExtra(EXTRA_URL, url)
                    .putExtra(EXTRA_TITLE, title)
                    .putExtra(EXTRA_LIVE, live)
            )
        }
    }
}
