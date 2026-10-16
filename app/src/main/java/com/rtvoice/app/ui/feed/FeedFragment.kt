package com.rtvvoice.app.ui.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.rtvvoice.app.data.api.Article
import com.rtvvoice.app.databinding.FragmentFeedBinding
import com.rtvvoice.app.ui.article.ArticleActivity

/** Swipe-refreshable list of articles for one section. */
class FeedFragment : Fragment(), ArticlesAdapter.Listener {

    companion object {
        private const val ARG_SECTION = "section"

        fun newInstance(sectionId: String) = FeedFragment().apply {
            arguments = Bundle().apply { putString(ARG_SECTION, sectionId) }
        }
    }

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FeedViewModel by viewModels({ requireActivity() })
    private val adapter = ArticlesAdapter(this)
    private val sectionId: String get() = requireArguments().getString(ARG_SECTION, "latest")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.adapter = adapter
        binding.refresh.setOnRefreshListener { viewModel.loadArticles(sectionId) }
        binding.retry.setOnClickListener { viewModel.loadArticles(sectionId) }

        viewModel.articlesFor(sectionId).observe(viewLifecycleOwner) { result ->
            binding.refresh.isRefreshing = false
            result.onSuccess { articles ->
                binding.error.visibility = View.GONE
                binding.list.visibility = View.VISIBLE
                adapter.submit(articles)
            }.onFailure {
                if (adapter.itemCount == 0) {
                    binding.error.visibility = View.VISIBLE
                    binding.list.visibility = View.GONE
                }
            }
        }

        if (viewModel.articlesFor(sectionId).value == null) {
            viewModel.loadArticles(sectionId)
        }
    }

    override fun onArticleClicked(article: Article) {
        ArticleActivity.start(requireContext(), article)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
