package com.rtvvoice.app.ui.shows

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.rtvvoice.app.data.api.Show
import com.rtvvoice.app.databinding.ItemShowBinding

class ShowsAdapter(
    private val listener: Listener,
) : ListAdapter<Show, ShowsAdapter.Holder>(DIFF) {

    interface Listener {
        fun onShowClicked(show: Show)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemShowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(getItem(position))
    }

    fun submit(shows: List<Show>) = submitList(shows)

    inner class Holder(private val binding: ItemShowBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(show: Show) {
            binding.title.text = show.title
            val image = show.thumbnailUrl ?: show.imageUrl
            binding.image.visibility = if (image.isNullOrBlank()) View.GONE else View.VISIBLE
            image?.let { binding.image.load(it) { crossfade(true) } }
            binding.root.setOnClickListener { listener.onShowClicked(show) }
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<Show>() {
            override fun areItemsTheSame(old: Show, new: Show) = old.id == new.id
            override fun areContentsTheSame(old: Show, new: Show) = old == new
        }
    }
}
