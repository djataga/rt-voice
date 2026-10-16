package com.rtvvoice.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.rtvvoice.app.R
import com.rtvvoice.app.RTVoiceApp
import com.rtvvoice.app.data.Edition
import com.rtvvoice.app.data.api.Section
import com.rtvvoice.app.databinding.ActivityMainBinding
import com.rtvvoice.app.ui.bookmarks.BookmarksActivity
import com.rtvvoice.app.ui.feed.FeedFragment
import com.rtvvoice.app.ui.feed.FeedViewModel

/**
 * Top-level screen: section tabs ("Latest", "World News", ...) fed by the
 * `news` endpoint of the selected edition. The edition is switched from the
 * toolbar spinner, mirroring the original app's drawer-based edition switcher.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: FeedViewModel by viewModels()
    private var sections: List<Section> = listOf(Section("latest", "Latest"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        setupEditionSpinner()
        observeSections()

        if (savedInstanceState == null) {
            viewModel.loadSections()
        }
    }

    private fun setupEditionSpinner() {
        val spinner = Spinner(this)
        spinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            Edition.ALL.map { it.label },
        )
        val current = (application as RTVoiceApp).currentEdition()
        spinner.setSelection(Edition.ALL.indexOfFirst { it.code == current.code }.coerceAtLeast(0))
        spinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val edition = Edition.ALL[position]
                if (edition.code == (application as RTVoiceApp).currentEdition().code &&
                    binding.pager.adapter != null
                ) return
                viewModel.selectEdition(edition)
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
        }
        binding.toolbar.addView(
            spinner,
            android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
    }

    private fun observeSections() {
        viewModel.sections.observe(this) { result ->
            result.onSuccess { list ->
                if (list.isNotEmpty()) sections = list
                binding.progress.hide()
                rebuildTabs()
            }.onFailure {
                binding.progress.hide()
                if (binding.pager.adapter == null) rebuildTabs()
            }
        }
        viewModel.editionChanged.observe(this) {
            binding.progress.show()
        }
    }

    private fun rebuildTabs() {
        binding.pager.adapter = SectionsAdapter(this)
        binding.pager.offscreenPageLimit = 1
        TabLayoutMediator(binding.tabs, binding.pager) { tab, position ->
            tab.text = sections[position].title
        }.attach()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_bookmarks -> {
            startActivity(Intent(this, BookmarksActivity::class.java))
            true
        }
        R.id.action_about -> {
            AboutDialog().show(supportFragmentManager, "about")
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private inner class SectionsAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = sections.size

        override fun createFragment(position: Int): Fragment =
            FeedFragment.newInstance(sections[position].id)
    }
}
