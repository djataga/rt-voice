package com.rtvvoice.app.ui.watch

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.rtvvoice.app.R
import com.rtvvoice.app.data.api.LiveChannel
import com.rtvvoice.app.databinding.FragmentWatchBinding
import com.rtvvoice.app.databinding.ItemChannelBinding
import com.rtvvoice.app.ui.player.PlayerActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/** Watch tab: 24/7 live channels (RT, RTD) streamed over HLS. */
class WatchFragment : Fragment() {

    private var _binding: FragmentWatchBinding? = null
    private val binding get() = _binding!!
    private val state = MutableStateFlow<State>(State.Idle)
    private val adapter = ChannelsAdapter { channel ->
        channel.hlsUrl?.let { url ->
            PlayerActivity.start(requireContext(), url, channel.title, image = null, live = true)
        }
    }

    sealed class State {
        data object Idle : State()
        data object Loading : State()
        data class Ready(val channels: List<LiveChannel>) : State()
        data object Error : State()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWatchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.adapter = adapter
        binding.retry.setOnClickListener { load() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                state.collect { render(it) }
            }
        }
        if (state.value is State.Idle) load()
    }

    private fun render(value: State) {
        binding.progress.visibility = if (value is State.Loading) View.VISIBLE else View.GONE
        when (value) {
            is State.Ready -> {
                binding.error.visibility = View.GONE
                binding.empty.visibility = if (value.channels.isEmpty()) View.VISIBLE else View.GONE
                adapter.submitList(value.channels)
            }
            is State.Error -> {
                if (adapter.itemCount == 0) binding.error.visibility = View.VISIBLE
                binding.empty.visibility = View.GONE
            }
            else -> Unit
        }
    }

    private fun load() {
        state.value = State.Loading
        viewLifecycleOwner.lifecycleScope.launch {
            state.value = runCatching {
                com.rtvvoice.app.data.api.NewsRepository.liveChannels()
            }.fold(
                { channels -> State.Ready(channels) },
                { State.Error },
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

private class ChannelsAdapter(
    val onPlay: (LiveChannel) -> Unit,
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
            binding.root.setOnClickListener { onPlay(channel) }
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<LiveChannel>() {
            override fun areItemsTheSame(old: LiveChannel, new: LiveChannel) = old.id == new.id
            override fun areContentsTheSame(old: LiveChannel, new: LiveChannel) = old == new
        }
    }
}
