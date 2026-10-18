package com.rtvvoice.app.ui.watch

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rtvvoice.app.R
import com.rtvvoice.app.RTVoiceApp
import com.rtvvoice.app.data.LiveCatalog
import com.rtvvoice.app.data.LiveChannel
import com.rtvvoice.app.data.LiveSourceKind
import com.rtvvoice.app.databinding.FragmentWatchBinding
import com.rtvvoice.app.databinding.ItemChannelBinding
import kotlinx.coroutines.launch
import com.rtvvoice.app.ui.player.PlayerActivity
import com.rtvvoice.app.ui.web.LiveWebActivity

/**
 * Watch tab: 24/7 live channels for the current edition. Each channel offers
 * its sources — HLS plays in the in-app player, site players (Rumble, Odysee,
 * VK) open in an in-app WebView.
 */
class WatchFragment : Fragment() {

    private var _binding: FragmentWatchBinding? = null
    private val binding get() = _binding!!
    private val adapter = ChannelsAdapter(::onChannelClicked)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWatchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.adapter = adapter
        val edition = (requireActivity().application as RTVoiceApp).currentEdition()
        val channels = LiveCatalog.forEdition(edition)
        adapter.submitList(channels)
        binding.empty.visibility = if (channels.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun onChannelClicked(channel: LiveChannel) {
        when (channel.sources.size) {
            0 -> Unit
            1 -> open(channel, channel.sources.first())
            else -> {
                val labels = channel.sources.map { it.label }.toTypedArray()
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.watch_choose_source, channel.title))
                    .setItems(labels) { _, which -> open(channel, channel.sources[which]) }
                    .show()
            }
        }
    }

    private fun open(channel: LiveChannel, source: com.rtvvoice.app.data.LiveSource) {
        when (source.kind) {
            LiveSourceKind.HLS -> PlayerActivity.start(
                requireContext(), source.url, channel.title, image = null, live = true,
            )
            LiveSourceKind.RUMBLE -> openRumble(channel, source)
            LiveSourceKind.WEB -> LiveWebActivity.start(requireContext(), source.url, channel.title)
        }
    }

    /**
     * Rumble embeds carry the direct HLS playlist; resolve it and play in the
     * native player. If resolution fails (network, layout change), fall back
     * to the web page.
     */
    private fun openRumble(channel: LiveChannel, source: com.rtvvoice.app.data.LiveSource) {
        Toast.makeText(requireContext(), R.string.watch_resolving_rumble, Toast.LENGTH_SHORT).show()
        viewLifecycleOwner.lifecycleScope.launch {
            val streamUrl = runCatching {
                com.rtvvoice.app.util.RumbleResolver.resolveLiveStream(source.url)
            }.getOrNull()
            if (streamUrl != null) {
                PlayerActivity.start(requireContext(), streamUrl, channel.title, image = null, live = true)
            } else {
                Toast.makeText(requireContext(), R.string.watch_rumble_fallback, Toast.LENGTH_LONG).show()
                LiveWebActivity.start(requireContext(), source.url, channel.title)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

private class ChannelsAdapter(
    val onClick: (LiveChannel) -> Unit,
) : ListAdapter<LiveChannel, ChannelsAdapter.Holder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemChannelBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class Holder(private val binding: ItemChannelBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(channel: LiveChannel) {
            binding.title.text = channel.title
            binding.sources.text =
                channel.sources.joinToString(" · ") { it.label }
            binding.root.setOnClickListener { onClick(channel) }
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<LiveChannel>() {
            override fun areItemsTheSame(old: LiveChannel, new: LiveChannel) = old.id == new.id
            override fun areContentsTheSame(old: LiveChannel, new: LiveChannel) = old == new
        }
    }
}
