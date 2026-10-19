package com.kivaisi.selflearn.data

import android.content.Context
import org.json.JSONArray

/**
 * One headword from the bundled standalone Swahili<->English dictionary.
 * This is real third-party dictionary data (FreeDict project, GPLv2+ --
 * see THIRD_PARTY_NOTICES.md), not content authored for a specific chapter.
 */
data class DictionaryEntry(
    val headword: String,
    val partOfSpeech: String,
    val meaning: String
)

/**
 * Loads the bundled standalone Swahili dictionary from
 * assets/swahili_dictionary.json (~2,100 headwords). This dictionary is
 * intentionally NOT tied to any chapter -- it's a general-purpose
 * reference, like a standalone pocket dictionary, available from the Home,
 * Reading, and Quiz screens alike.
 *
 * Parsed once per process and cached in memory; parsing ~95KB of JSON is
 * fast enough to do synchronously on first open without a loading spinner.
 */
object SwahiliDictionaryRepository {
    @Volatile
    private var cached: List<DictionaryEntry>? = null

    fun load(context: Context): List<DictionaryEntry> {
        cached?.let { return it }
        synchronized(this) {
            cached?.let { return it }
            val json = context.assets.open("swahili_dictionary.json")
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
            val array = JSONArray(json)
            val entries = ArrayList<DictionaryEntry>(array.length())
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                entries.add(
                    DictionaryEntry(
                        headword = obj.optString("sw"),
                        partOfSpeech = obj.optString("pos"),
                        meaning = obj.optString("en")
                    )
                )
            }
            cached = entries
            return entries
        }
    }
}
