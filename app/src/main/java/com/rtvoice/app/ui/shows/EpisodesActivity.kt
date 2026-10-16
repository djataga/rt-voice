package com.rtvvoice.app.ui.shows

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.rtvvoice.app.R
import com.rtvvoice.app.data.api.ShowEpisode
import com.rtvvoice.app.databinding.ActivityEpisodesBinding
import com.rtvvoice.app.databinding.ItemEpisodeBinding
import com.rtvvoice.app.ui.player.PlayerActivity
import com.rtvvoice.app.util.Dates
import kotlinx.coroutines.launch

/** All episodes of one show; tapping an episode plays it. */
class EpisodesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEpisodesBinding
    private val adapter = EpisodesAdapter { episode ->
        episode.playableUrl?.let { url ->
            PlayerActivity.start(this, url, episode.title, episode.thumbnailUrl ?: episode.image)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEpisodesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = intent.getStringExtra(EXTRA_TITLE) ?: getString(R.string.episodes_title)
        }
        binding.list.layoutManager = LinearLayoutManager(this)
        binding.list.adapter = adapter
        binding.retry.setOnClickListener { load() }
        load()
    }

    private fun load() {
        val showId = intent.getStringExtra(EXTRA_SHOW_ID) ?: return
        binding.progress.visibility = View.VISIBLE
        binding.error.visibility = View.GONE
        lifecycleScope.launch {
            val result = runCatching { com.rtvvoice.app.data.api.NewsRepository.episodes(showId) }
            binding.progress.visibility = View.GONE
            result.onSuccess { episodes ->
                adapter.submitList(episodes)
                binding.empty.visibility = if (episodes.isEmpty()) View.VISIBLE else View.GONE
            }.onFailure {
                if (adapter.itemCount == 0) binding.error.visibility = View.VISIBLE
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    companion object {
        private const val EXTRA_SHOW_ID = "show_id"
        private const val EXTRA_TITLE = "title"

        fun start(context: Context, showId: String, title: String) {
            context.startActivity(
                Intent(context, EpisodesActivity::class.java)
                    .putExtra(EXTRA_SHOW_ID, showId)
                    .putExtra(EXTRA_TITLE, title)
            )
        }
    }
}

private class EpisodesAdapter(
    val onPlay: (ShowEpisode) -> Unit,
) : ListAdapter<ShowEpisode, EpisodesAdapter.Holder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemEpisodeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class Holder(private val binding: ItemEpisodeBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(episode: ShowEpisode) {
            binding.title.text = episode.title
            binding.summary.text = episode.summary?.trim().orEmpty()
            binding.summary.visibility =
                if (episode.summary.isNullOrBlank()) View.GONE else View.VISIBLE
            binding.time.text = Dates.relative(Dates.parse(episode.time))
            val image = episode.thumbnailUrl ?: episode.image
            binding.image.visibility = if (image.isNullOrBlank()) View.GONE else View.VISIBLE
            image?.let { binding.image.load(it) { crossfade(true) } }
            binding.playBadge.visibility =
                if (episode.playableUrl != null) View.VISIBLE else View.GONE
            binding.root.setOnClickListener {
                if (episode.playableUrl != null) onPlay(episode)
            }
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<ShowEpisode>() {
            override fun areItemsTheSame(old: ShowEpisode, new: ShowEpisode) = old.id == new.id
            override fun areContentsTheSame(old: ShowEpisode, new: ShowEpisode) = old == new
        }
    }
}
