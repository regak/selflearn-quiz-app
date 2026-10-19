package com.kivaisi.selflearn.data

import android.content.Context
import org.json.JSONArray

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
 * This is real third-party dictionary data (sw.wiktionary.org, CC BY-SA
 * 4.0 + GFDL -- see THIRD_PARTY_NOTICES.md), not content authored for a
 * specific chapter.
 */
data class DictionaryEntry(
    val headword: String,
    val partOfSpeech: String,
    val swahiliDefinition: String,
    val englishMeaning: String
)

/**
 * Loads the bundled standalone Swahili dictionary from
 * assets/swahili_dictionary.json (~16,000 headwords, each defined in
 * Swahili). This dictionary is intentionally NOT tied to any chapter --
 * it's a general-purpose reference, like a standalone pocket dictionary,
 * available from the Home, Reading, and Quiz screens alike.
 *
 * Parsed once per process and cached in memory.
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
                        swahiliDefinition = obj.optString("def_sw"),
                        englishMeaning = obj.optString("en")
                    )
                )
            }
            cached = entries
            return entries
        }
    }
}
