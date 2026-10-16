package com.rtvvoice.app.ui.feed

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.rtvvoice.app.data.api.Article
import com.rtvvoice.app.databinding.ItemArticleBinding
import com.rtvvoice.app.util.Dates

/** Article cards: image, rubric, title, summary and relative time. */
class ArticlesAdapter(
    private val listener: Listener,
) : ListAdapter<Article, ArticlesAdapter.Holder>(DIFF) {

    interface Listener {
        fun onArticleClicked(article: Article)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemArticleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(getItem(position))
    }

    fun submit(articles: List<Article>) = submitList(articles)

    inner class Holder(private val binding: ItemArticleBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(article: Article) {
            binding.title.text = article.title
            binding.summary.text = article.summary.orEmpty()
            binding.summary.visibility =
                if (article.summary.isNullOrBlank()) View.GONE else View.VISIBLE
            binding.rubric.text = article.sectionTitle.orEmpty()
            binding.rubric.visibility =
                if (article.sectionTitle.isNullOrBlank()) View.GONE else View.VISIBLE
            binding.time.text = Dates.relative(Dates.parse(article.time))

            binding.image.visibility = if (article.image.isNullOrBlank()) View.GONE else View.VISIBLE
            article.image?.let { binding.image.load(it) { crossfade(true) } }

            binding.root.setOnClickListener { listener.onArticleClicked(article) }
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<Article>() {
            override fun areItemsTheSame(oldItem: Article, newItem: Article) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Article, newItem: Article) = oldItem == newItem
        }
    }
}
