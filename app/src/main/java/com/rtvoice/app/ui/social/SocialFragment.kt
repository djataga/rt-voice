package com.rtvvoice.app.ui.social

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.rtvvoice.app.R
import com.rtvvoice.app.RTVoiceApp
import com.rtvvoice.app.data.RssItem
import com.rtvvoice.app.data.SocialCatalog
import com.rtvvoice.app.data.SocialRepository
import com.rtvvoice.app.data.api.Article
import com.rtvvoice.app.databinding.FragmentSocialBinding
import com.rtvvoice.app.databinding.ItemSocialAccountBinding
import com.rtvvoice.app.databinding.ItemSocialPostBinding
import com.rtvvoice.app.ui.article.ArticleActivity
import com.rtvvoice.app.ui.web.LiveWebActivity
import com.rtvvoice.app.util.Dates
import com.rtvvoice.app.util.RssParser
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Social tab: RT's official account links (opened in the in-app browser —
 * X and Facebook have no free data API) and a native feed built from RT's
 * public RSS mirrors. Tapping a post opens it in the article reader with
 * voice support, using the article id embedded in the RSS link.
 */
class SocialFragment : Fragment() {

    private var _binding: FragmentSocialBinding? = null
    private val binding get() = _binding!!
    private val accountsAdapter = AccountsAdapter(::openAccount)
    private val postsAdapter = PostsAdapter(::openPost)
    private val state = MutableStateFlow<State>(State.Idle)

    private var feeds: List<com.rtvvoice.app.data.SocialFeed> = emptyList()
    private var selectedFeed: com.rtvvoice.app.data.SocialFeed? = null
    private var loadJob: Job? = null

    sealed class State {
        data object Idle : State()
        data object Loading : State()
        data class Ready(val items: List<RssItem>) : State()
        data object Error : State()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSocialBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.accounts.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.accounts.adapter = accountsAdapter
        accountsAdapter.submit(SocialCatalog.ACCOUNTS)

        binding.posts.layoutManager = LinearLayoutManager(requireContext())
        binding.posts.adapter = postsAdapter
        binding.refresh.setOnRefreshListener { selectedFeed?.let { load(it, force = true) } }
        binding.retry.setOnClickListener { selectedFeed?.let { load(it, force = true) } }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                state.collect { render(it) }
            }
        }

        val edition = (requireActivity().application as RTVoiceApp).currentEdition()
        feeds = SocialCatalog.feedsFor(edition)
        buildFeedChips()
        if (state.value is State.Idle && feeds.isNotEmpty()) {
            selectFeed(feeds.first())
        }
    }

    private fun buildFeedChips() {
        binding.chips.removeAllViews()
        if (feeds.size <= 1) {
            binding.chips.visibility = View.GONE
            return
        }
        binding.chips.visibility = View.VISIBLE
        feeds.forEach { feed ->
            val chip = com.google.android.material.chip.Chip(requireContext())
            chip.text = feed.title
            chip.isCheckable = true
            chip.setOnClickListener { selectFeed(feed) }
            binding.chips.addView(chip)
        }
        binding.chips.check(firstOrNullId())
    }

    private fun firstOrNullId(): Int =
        if (binding.chips.childCount > 0) (binding.chips.getChildAt(0) as com.google.android.material.chip.Chip).id
        else View.NO_ID

    private fun selectFeed(feed: com.rtvvoice.app.data.SocialFeed) {
        selectedFeed = feed
        for (i in 0 until binding.chips.childCount) {
            val chip = binding.chips.getChildAt(i) as? com.google.android.material.chip.Chip ?: continue
            chip.isChecked = chip.text == feed.title
        }
        load(feed)
    }

    private fun load(feed: com.rtvvoice.app.data.SocialFeed, force: Boolean = false) {
        if (force) SocialRepository.clear()
        loadJob?.cancel()
        state.value = State.Loading
        loadJob = viewLifecycleOwner.lifecycleScope.launch {
            val result = runCatching { SocialRepository.feed(feed.url) }
            state.value = result.fold(
                { items -> if (items.isEmpty()) State.Error else State.Ready(items) },
                { State.Error },
            )
        }
    }

    private fun render(value: State) {
        binding.progress.visibility = if (value is State.Loading && postsAdapter.itemCount == 0) {
            View.VISIBLE
        } else {
            View.GONE
        }
        binding.refresh.isRefreshing = value is State.Loading && postsAdapter.itemCount > 0
        when (value) {
            is State.Ready -> {
                binding.error.visibility = View.GONE
                binding.posts.visibility = View.VISIBLE
                postsAdapter.submit(value.items)
            }
            is State.Error -> {
                if (postsAdapter.itemCount == 0) {
                    binding.error.visibility = View.VISIBLE
                    binding.posts.visibility = View.GONE
                }
            }
            else -> Unit
        }
    }

    /** Platforms with an official Android app: prefer the app over the site. */
    private val APP_PACKAGES = mapOf(
        "telegram" to "org.telegram.messenger",
        "vk" to "com.vtostudio.lite",
    )

    private fun openAccount(account: com.rtvvoice.app.data.SocialAccount) {
        val appPackage = APP_PACKAGES[account.name.lowercase()]
        if (appPackage != null && openInApp(account.url, appPackage)) return
        LiveWebActivity.start(requireContext(), account.url, account.name)
    }

    /**
     * t.me previews render as an interstitial (or not at all) on some
     * networks, and VK's own app gives the full community experience — so
     * for these platforms try the installed app first; if absent (or the
     * link fails) fall back to the in-app browser.
     */
    private fun openInApp(url: String, appPackage: String): Boolean {
        val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .setPackage(appPackage)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            startActivity(appIntent)
            true
        } catch (_: android.content.ActivityNotFoundException) {
            false
        }
    }

    private fun openPost(item: RssItem) {
        // Prefer the native reader: the RSS link embeds the API article id.
        val articleId = RssParser.articleId(item.link)
        if (articleId != null) {
            ArticleActivity.start(
                requireContext(),
                Article(id = articleId, title = item.title, url = item.link),
            )
        } else {
            LiveWebActivity.start(requireContext(), item.link, item.title)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        loadJob?.cancel()
        _binding = null
    }
}

private class AccountsAdapter(
    val onClick: (com.rtvvoice.app.data.SocialAccount) -> Unit,
) : ListAdapter<com.rtvvoice.app.data.SocialAccount, AccountsAdapter.Holder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemSocialAccountBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(getItem(position))
    }

    fun submit(accounts: List<com.rtvvoice.app.data.SocialAccount>) = submitList(accounts)

    inner class Holder(private val binding: ItemSocialAccountBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(account: com.rtvvoice.app.data.SocialAccount) {
            binding.name.text = account.name
            binding.root.setOnClickListener { onClick(account) }
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<com.rtvvoice.app.data.SocialAccount>() {
            override fun areItemsTheSame(old: com.rtvvoice.app.data.SocialAccount, new: com.rtvvoice.app.data.SocialAccount) =
                old.name == new.name

            override fun areContentsTheSame(old: com.rtvvoice.app.data.SocialAccount, new: com.rtvvoice.app.data.SocialAccount) =
                old == new
        }
    }
}

private class PostsAdapter(
    val onClick: (RssItem) -> Unit,
) : ListAdapter<RssItem, PostsAdapter.Holder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemSocialPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(getItem(position))
    }

    fun submit(items: List<RssItem>) = submitList(items)

    inner class Holder(private val binding: ItemSocialPostBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: RssItem) {
            binding.title.text = item.title
            binding.time.text = Dates.relative(item.publishedAt)
            binding.image.visibility = if (item.thumbnail.isNullOrBlank()) View.GONE else View.VISIBLE
            item.thumbnail?.let { binding.image.load(it) { crossfade(true) } }
            binding.root.setOnClickListener { onClick(item) }
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<RssItem>() {
            override fun areItemsTheSame(old: RssItem, new: RssItem) = old.link == new.link
            override fun areContentsTheSame(old: RssItem, new: RssItem) = old == new
        }
    }
}
