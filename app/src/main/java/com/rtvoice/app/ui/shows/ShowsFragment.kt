package com.rtvvoice.app.ui.shows

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.rtvvoice.app.data.api.Show
import com.rtvvoice.app.databinding.FragmentShowsBinding
import kotlinx.coroutines.launch

/** Shows tab: the program catalog of the current edition. */
class ShowsFragment : Fragment(), ShowsAdapter.Listener {

    private var _binding: FragmentShowsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ShowsViewModel by viewModels()
    private val adapter = ShowsAdapter(this)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentShowsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.adapter = adapter
        binding.refresh.setOnRefreshListener { viewModel.load() }
        binding.retry.setOnClickListener { viewModel.load() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { render(it) }
            }
        }
        if (viewModel.state.value is ShowsViewModel.State.Idle) viewModel.load()
    }

    private fun render(state: ShowsViewModel.State) {
        binding.progress.visibility =
            if (state is ShowsViewModel.State.Loading) View.VISIBLE else View.GONE
        binding.refresh.isRefreshing = false
        when (state) {
            is ShowsViewModel.State.Ready -> {
                binding.error.visibility = View.GONE
                binding.list.visibility = View.VISIBLE
                adapter.submit(state.shows)
            }
            is ShowsViewModel.State.Error -> {
                if (adapter.itemCount == 0) {
                    binding.error.visibility = View.VISIBLE
                    binding.list.visibility = View.GONE
                }
            }
            else -> Unit
        }
    }

    override fun onShowClicked(show: Show) {
        EpisodesActivity.start(requireContext(), show.id, show.title)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
