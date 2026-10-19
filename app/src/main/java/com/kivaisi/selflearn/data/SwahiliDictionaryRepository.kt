package com.kivaisi.selflearn.data

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * One headword from the bundled standalone, MONOLINGUAL Swahili dictionary
 * -- the Swahili word is defined IN SWAHILI (e.g. "maji" -> "kioevu kisicho
 * na rangi wala harufu kinachopatikana kiasili"), the same way Oxford or
 * Cambridge defines English words in English, rather than a translation
 * dictionary that only maps the word to its English equivalent.
 *
 * [englishMeaning] is an OPTIONAL bonus field, only populated when a
 * separate bilingual source also had that headword -- most entries will
 * have an empty [englishMeaning] and that's expected, not a bug.
 *
 * [example] is ALWAYS populated (100% coverage), but [isExampleGenerated]
 * tells you whether it's REAL usage pulled from a real source (Tatoeba's
 * open sentence corpus or sw.wiktionary.org's own quotation lines -- about
 * 14.5% of entries) or a TEMPLATE-GENERATED placeholder sentence built from
 * the word and its definition (the other ~85.5%, since no free Swahili
 * example-sentence corpus covers the full dictionary). The UI must label
 * generated examples as such -- never present them as authentic usage.
 *
 * This is real third-party dictionary data (sw.wiktionary.org, CC BY-SA
 * 4.0 + GFDL; Tatoeba, CC BY 2.0 FR -- see THIRD_PARTY_NOTICES.md), not
 * content authored for a specific chapter.
 */
data class DictionaryEntry(
    val headword: String,
    val partOfSpeech: String,
    val swahiliDefinition: String,
    val englishMeaning: String,
    val example: String,
    val isExampleGenerated: Boolean
)

/**
 * Backed by a pre-built, indexed SQLite database bundled as
 * assets/swahili_dictionary.db (~16,000 headwords), replacing the earlier
 * JSON + in-memory-List approach (v1.0-v1.9). This fixes two problems that
 * approach had at this data size:
 *
 * 1. SLOW OPEN: the old code parsed a 2MB JSON file into ~16,000 Kotlin
 *    objects synchronously on the UI thread the first time the dictionary
 *    was opened, which visibly froze the app. Here, opening the dialog
 *    only ever queries the small number of rows actually needed (a 60-row
 *    preview, or up to 50 search results) -- there is no "load everything
 *    into memory" step at all, and every query runs on [Dispatchers.IO].
 * 2. IMPRECISE SEARCH: the old code did a single `contains()` check across
 *    headword + definition + English with no ranking or cap, so an exact
 *    headword match could get buried under loosely-related substring hits
 *    inside long definition text. [search] below runs four ranked tiers
 *    (exact headword -> prefix -> headword-contains -> definition/English
 *    contains), highest-priority tier first, de-duplicated, capped at
 *    [DEFAULT_LIMIT] results -- the word you typed exactly now always
 *    sorts to the top.
 *
 * (FTS5 full-text search was considered as an even more scalable option,
 * but plain indexed columns + tiered LIKE queries are used instead for
 * guaranteed compatibility back to minSdk 24 -- FTS5 availability depends
 * on the device's bundled SQLite build and isn't guaranteed on all Android
 * versions/vendors, while plain B-tree indexes are universally supported
 * and, at ~16,000 rows, comfortably fast either way.)
 *
 * SCHEMA VERSIONING: the bundled DB's `PRAGMA user_version` is bumped
 * whenever a released APK's schema changes (added example columns in
 * schema version 2). [AssetDbOpenHelper.ensureCopied] checks the
 * already-installed copy's user_version against
 * [REQUIRED_SCHEMA_VERSION] and re-copies from assets if it's stale --
 * otherwise a user upgrading from an older release would keep their old
 * on-device copy forever (first-run-only copy logic) and crash querying
 * columns that don't exist in it yet.
 */
object SwahiliDictionaryRepository {
    private const val DB_ASSET_NAME = "swahili_dictionary.db"
    private const val REQUIRED_SCHEMA_VERSION = 2
    private const val DEFAULT_LIMIT = 50
    private const val PREVIEW_LIMIT = 60
    private const val COLUMNS = "headword, pos, def_sw, en, example, example_is_generated"

    @Volatile
    private var db: SQLiteDatabase? = null

    @Volatile
    private var cachedTotalCount: Int? = null

    /** Call early (e.g. app/Home-screen startup) to warm the DB connection
     * in the background before the user opens the dictionary, so the first
     * real open has zero wait. Safe to call multiple times. */
    suspend fun warmUp(context: Context) {
        withContext(Dispatchers.IO) {
            getDb(context)
        }
    }

    suspend fun totalCount(context: Context): Int {
        cachedTotalCount?.let { return it }
        return withContext(Dispatchers.IO) {
            cachedTotalCount?.let { return@withContext it }
            val database = getDb(context)
            val count = database.rawQuery("SELECT COUNT(*) FROM entries", null).use { c ->
                if (c.moveToFirst()) c.getInt(0) else 0
            }
            cachedTotalCount = count
            count
        }
    }

    /** No-query "browse" view: a cheap, capped, alphabetical preview --
     * never loads the whole dictionary into memory. */
    suspend fun browsePreview(context: Context, limit: Int = PREVIEW_LIMIT): List<DictionaryEntry> =
        withContext(Dispatchers.IO) {
            val database = getDb(context)
            database.rawQuery(
                "SELECT $COLUMNS FROM entries ORDER BY headword_lower LIMIT ?",
                arrayOf(limit.toString())
            ).use { it.toEntries() }
        }

    /**
     * Ranked, capped search: exact headword match, then headword
     * starts-with, then headword contains, then definition/English
     * contains -- each tier queried in priority order, de-duplicated by
     * headword, stopping once [limit] results are collected.
     */
    suspend fun search(context: Context, query: String, limit: Int = DEFAULT_LIMIT): List<DictionaryEntry> =
        withContext(Dispatchers.IO) {
            val database = getDb(context)
            val q = query.trim().lowercase()
            if (q.isEmpty()) return@withContext browsePreview(context, limit)

            val results = LinkedHashMap<String, DictionaryEntry>()

            fun runTier(sql: String, args: Array<String>) {
                if (results.size >= limit) return
                database.rawQuery(sql, args).use { cursor ->
                    while (cursor.moveToNext() && results.size < limit) {
                        val headword = cursor.getString(0)
                        if (!results.containsKey(headword)) {
                            results[headword] = cursor.toEntry()
                        }
                    }
                }
            }

            // Tier 0: exact headword match.
            runTier(
                "SELECT $COLUMNS FROM entries WHERE headword_lower = ? ORDER BY headword_lower",
                arrayOf(q)
            )
            // Tier 1: headword starts with the query.
            runTier(
                "SELECT $COLUMNS FROM entries WHERE headword_lower LIKE ? ORDER BY headword_lower",
                arrayOf("$q%")
            )
            // Tier 2: headword contains the query anywhere.
            runTier(
                "SELECT $COLUMNS FROM entries WHERE headword_lower LIKE ? ORDER BY headword_lower",
                arrayOf("%$q%")
            )
            // Tier 3 (lowest priority): query only found in the definition
            // or bonus English translation, not the headword itself.
            runTier(
                "SELECT $COLUMNS FROM entries WHERE def_sw LIKE ? OR en LIKE ? ORDER BY headword_lower",
                arrayOf("%$q%", "%$q%")
            )

            results.values.toList()
        }

    private fun Cursor.toEntries(): List<DictionaryEntry> {
        val list = ArrayList<DictionaryEntry>(count)
        while (moveToNext()) list.add(toEntry())
        return list
    }

    private fun Cursor.toEntry(): DictionaryEntry = DictionaryEntry(
        headword = getString(0),
        partOfSpeech = getString(1) ?: "",
        swahiliDefinition = getString(2) ?: "",
        englishMeaning = getString(3) ?: "",
        example = getString(4) ?: "",
        isExampleGenerated = getInt(5) != 0
    )

    private fun getDb(context: Context): SQLiteDatabase {
        db?.let { return it }
        synchronized(this) {
            db?.let { return it }
            val helper = AssetDbOpenHelper(context)
            val opened = helper.readableDatabase
            db = opened
            return opened
        }
    }

    /** Copies the pre-built, pre-indexed dictionary database out of
     * assets/ into the app's private database directory on first run (or
     * after an app update that bumped the schema -- see
     * [REQUIRED_SCHEMA_VERSION]), then opens it read-only on every later
     * call. SQLite cannot open a database file directly from inside an
     * APK's compressed assets, so a real filesystem copy is required. */
    private class AssetDbOpenHelper(private val context: Context) :
        SQLiteOpenHelper(context, DB_ASSET_NAME, null, 1) {

        override fun onCreate(db: SQLiteDatabase) {
            // Not used -- the database file is pre-built and copied in
            // getReadableDatabase() below.
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            // Not used -- see ensureCopied(), which re-copies a stale
            // on-device file based on the bundled DB's own
            // PRAGMA user_version instead of this framework callback.
        }

        override fun getReadableDatabase(): SQLiteDatabase {
            ensureCopied()
            return SQLiteDatabase.openDatabase(
                context.getDatabasePath(DB_ASSET_NAME).path,
                null,
                SQLiteDatabase.OPEN_READONLY
            )
        }

        private fun ensureCopied() {
            val destFile = context.getDatabasePath(DB_ASSET_NAME)
            if (destFile.exists() && destFile.length() > 0 && isUpToDate(destFile)) return
            destFile.parentFile?.mkdirs()
            val tempFile = File(destFile.parentFile, "$DB_ASSET_NAME.tmp")
            context.assets.open(DB_ASSET_NAME).use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            // delete() first -- renameTo() can silently fail to replace an
            // existing destination file on some filesystems/API levels.
            destFile.delete()
            tempFile.renameTo(destFile)
        }

        /** True if the already-copied on-device file's schema version is
         * current, so we don't re-copy a multi-megabyte asset on every
         * single app launch -- only when it's actually stale (e.g. left
         * over from an older release that bundled a different schema) or
         * unreadable/corrupted. */
        private fun isUpToDate(destFile: File): Boolean = try {
            SQLiteDatabase.openDatabase(destFile.path, null, SQLiteDatabase.OPEN_READONLY).use { existing ->
                existing.version >= REQUIRED_SCHEMA_VERSION
            }
        } catch (e: Exception) {
            false
        }
    }
}
