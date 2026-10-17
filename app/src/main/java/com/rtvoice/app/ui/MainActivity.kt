package com.rtvvoice.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.rtvvoice.app.R
import com.rtvvoice.app.RTVoiceApp
import com.rtvvoice.app.data.Edition
import com.rtvvoice.app.data.api.NewsRepository
import com.rtvvoice.app.databinding.ActivityMainBinding
import com.rtvvoice.app.ui.bookmarks.BookmarksActivity
import com.rtvvoice.app.ui.news.NewsFragment
import com.rtvvoice.app.ui.shows.ShowsFragment
import com.rtvvoice.app.ui.social.SocialFragment
import com.rtvvoice.app.ui.watch.WatchFragment

/**
 * Top-level screen with three destinations (bottom navigation): News (section
 * tabs per edition), Shows and Watch (live channels). The edition is switched
 * from the toolbar spinner, mirroring the original app's drawer switcher.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var spinnerReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        setupEditionSpinner()
        setupBottomNav()

        if (savedInstanceState == null) {
            binding.bottomNav.selectedItemId = R.id.nav_news
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
        spinner.setSelection(Edition.ALL.indexOfFirst { it.code == current.code }.coerceAtLeast(0), false)
        spinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                // The first callback is the layout pass reflecting the persisted
                // selection — without this guard it rewrites the edition to English.
                if (!spinnerReady) {
                    spinnerReady = true
                    return
                }
                val edition = Edition.ALL[position]
                val app = application as RTVoiceApp
                if (edition.code == app.currentEdition().code) return
                app.prefs.edition = edition
                // Rebuild the Retrofit service and signal every cached view model.
                com.rtvvoice.app.data.api.NewsRepository.switchEdition(edition)
                onNavigationSelected(binding.bottomNav.selectedItemId, forceRecreate = true)
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

    private fun setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            onNavigationSelected(item.itemId, forceRecreate = false)
        }
    }

    private fun onNavigationSelected(itemId: Int, forceRecreate: Boolean): Boolean {
        val fragment: Fragment = when (itemId) {
            R.id.nav_shows -> ShowsFragment()
            R.id.nav_watch -> WatchFragment()
            R.id.nav_social -> SocialFragment()
            else -> NewsFragment()
        }
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            if (forceRecreate && supportFragmentManager.findFragmentById(R.id.container) != null) {
                // Replace with a fresh instance so feeds refetch for the new edition.
                replace(R.id.container, fragment.javaClass, null)
            } else {
                replace(R.id.container, fragment)
            }
        }
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        // The toolbar is dark; untinted menu icons are near-black and wash out.
        for (i in 0 until menu.size()) menu.getItem(i).icon?.setTintList(
            android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
        )
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
}
