package com.rtvvoice.app.ui.news

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.rtvvoice.app.data.api.Section
import com.rtvvoice.app.databinding.FragmentNewsBinding
import com.rtvvoice.app.ui.feed.FeedFragment
import com.rtvvoice.app.ui.feed.FeedViewModel

/** News tab: section tabs ("Latest", "World News", ...) with per-section feeds. */
class NewsFragment : Fragment() {

    private var _binding: FragmentNewsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FeedViewModel by viewModels({ requireActivity() })
    private var sections: List<Section> = listOf(Section("latest", "Latest"))

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNewsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.sections.observe(viewLifecycleOwner) { result ->
            result.onSuccess { list ->
                if (list.isNotEmpty()) sections = list
                binding.progress.hide()
                rebuildTabs()
            }.onFailure {
                binding.progress.hide()
                if (binding.pager.adapter == null) rebuildTabs()
            }
        }
        if (viewModel.sections.value == null) {
            binding.progress.show()
            viewModel.loadSections()
        } else {
            rebuildTabs()
        }
        // Edition changes are pushed by FeedViewModel's own collector, so the
        // observed LiveData is always current; no forced reload needed here.
    }

    private fun rebuildTabs() {
        binding.pager.adapter = SectionsAdapter(requireActivity())
        TabLayoutMediator(binding.tabs, binding.pager) { tab, position ->
            tab.text = sections[position].title
        }.attach()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private inner class SectionsAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = sections.size

        override fun createFragment(position: Int): Fragment =
            FeedFragment.newInstance(sections[position].id)
    }
}
