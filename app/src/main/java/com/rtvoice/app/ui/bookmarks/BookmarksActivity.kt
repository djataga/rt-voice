package com.rtvvoice.app.ui.bookmarks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rtvvoice.app.R
import com.rtvvoice.app.data.api.Article
import com.rtvvoice.app.data.db.BookmarksDb
import com.rtvvoice.app.databinding.ActivityBookmarksBinding
import com.rtvvoice.app.databinding.ItemBookmarkBinding
import com.rtvvoice.app.ui.article.ArticleActivity
import com.rtvvoice.app.util.Dates

/** Saved articles; each keeps its HTML so it can be read and listened to offline. */
class BookmarksActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookmarksBinding
    private lateinit var db: BookmarksDb
    private val adapter = BookmarksAdapter(
        onClick = { ArticleActivity.start(this, it.article) },
        onRemove = { bookmark ->
            db.remove(bookmark.article.id)
            refresh()
            Toast.makeText(this, R.string.bookmark_removed, Toast.LENGTH_SHORT).show()
        },
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookmarksBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setTitle(R.string.bookmarks_title)
        }
        db = BookmarksDb.get(this)
        binding.list.layoutManager = LinearLayoutManager(this)
        binding.list.adapter = adapter
        refresh()
    }

    private fun refresh() {
        val items = db.all()
        adapter.submitList(items)
        binding.empty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        binding.list.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

private class BookmarksAdapter(
    val onClick: (BookmarksDb.Bookmark) -> Unit,
    val onRemove: (BookmarksDb.Bookmark) -> Unit,
) : ListAdapter<BookmarksDb.Bookmark, BookmarksAdapter.Holder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemBookmarkBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class Holder(private val binding: ItemBookmarkBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(bookmark: BookmarksDb.Bookmark) {
            val article = bookmark.article
            binding.title.text = article.title
            binding.meta.text = Dates.relative(Dates.parse(article.time)).ifBlank { bookmark.editionCode }
            binding.root.setOnClickListener { onClick(bookmark) }
            binding.remove.setOnClickListener { onRemove(bookmark) }
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<BookmarksDb.Bookmark>() {
            override fun areItemsTheSame(old: BookmarksDb.Bookmark, new: BookmarksDb.Bookmark) =
                old.article.id == new.article.id

            override fun areContentsTheSame(old: BookmarksDb.Bookmark, new: BookmarksDb.Bookmark) =
                old.article == new.article
        }
    }
}
