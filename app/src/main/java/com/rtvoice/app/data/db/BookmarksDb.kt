package com.rtvvoice.app.data.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.google.gson.Gson
import com.rtvvoice.app.data.api.Article

/**
 * Bookmarks store, mirroring the original app's `DatabaseManager` schema
 * (`bookmarks` table keyed by article id, with the article JSON and the raw
 * article HTML kept for offline reading).
 */
class BookmarksDb private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    private val gson = Gson()

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE bookmarks (" +
                "article_id TEXT PRIMARY KEY NOT NULL, " +
                "item TEXT NOT NULL, " +
                "html TEXT NOT NULL, " +
                "edition TEXT NOT NULL)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS bookmarks")
        onCreate(db)
    }

    fun add(article: Article, html: String, editionCode: String) {
        val values = ContentValues().apply {
            put(COL_ID, article.id)
            put(COL_ITEM, gson.toJson(article))
            put(COL_HTML, html)
            put(COL_EDITION, editionCode)
        }
        writableDatabase.insertWithOnConflict(TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun remove(articleId: String) {
        writableDatabase.delete(TABLE, "$COL_ID = ?", arrayOf(articleId))
    }

    fun contains(articleId: String): Boolean =
        readableDatabase.rawQuery(
            "SELECT 1 FROM $TABLE WHERE $COL_ID = ? LIMIT 1", arrayOf(articleId)
        ).use { it.count > 0 }

    /** All bookmarks, newest first. */
    fun all(): List<Bookmark> {
        val out = mutableListOf<Bookmark>()
        readableDatabase.query(
            TABLE, null, null, null, null, null, "rowid DESC"
        ).use { c ->
            while (c.moveToNext()) {
                out += Bookmark(
                    article = gson.fromJson(c.getString(c.getColumnIndexOrThrow(COL_ITEM)), Article::class.java),
                    html = c.getString(c.getColumnIndexOrThrow(COL_HTML)),
                    editionCode = c.getString(c.getColumnIndexOrThrow(COL_EDITION)),
                )
            }
        }
        return out
    }

    fun getHtml(articleId: String): String? =
        readableDatabase.query(
            TABLE, arrayOf(COL_HTML), "$COL_ID = ?", arrayOf(articleId), null, null, null
        ).use { c -> if (c.moveToFirst()) c.getString(0) else null }

    data class Bookmark(val article: Article, val html: String, val editionCode: String)

    companion object {
        private const val DB_NAME = "rt_voice.db"
        private const val DB_VERSION = 1
        private const val TABLE = "bookmarks"
        private const val COL_ID = "article_id"
        private const val COL_ITEM = "item"
        private const val COL_HTML = "html"
        private const val COL_EDITION = "edition"

        @Volatile
        private var instance: BookmarksDb? = null

        fun get(context: Context): BookmarksDb =
            instance ?: synchronized(this) {
                instance ?: BookmarksDb(context).also { instance = it }
            }
    }
}
