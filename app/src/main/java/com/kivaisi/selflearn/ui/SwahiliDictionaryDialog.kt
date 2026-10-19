package com.kivaisi.selflearn.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kivaisi.selflearn.data.DictionaryEntry
import com.kivaisi.selflearn.data.SwahiliDictionaryRepository
import kotlinx.coroutines.delay

// Navy "offline dictionary" palette -- deliberately distinct from the rest
// of the app's purple/green/yellow game theme, so this screen reads like a
// dedicated reference-book tool (modeled on a standalone offline-dictionary
// app look: dark background, bold search bar, gold-bordered cards).
private val DictNavyTop = Color(0xFF15294F)
private val DictNavyBottom = Color(0xFF0A1830)
private val DictSearchBorder = Color(0xFFE5433B)
private val DictCardBorder = Color(0xFFF0B429)

/**
 * Standalone Swahili dictionary -- NOT tied to any chapter, and
 * MONOLINGUAL: a Swahili word is defined IN SWAHILI (like Oxford/Cambridge
 * define English words in English), not just translated to its English
 * equivalent. Backed by a bundled ~16,000-headword dataset extracted from
 * sw.wiktionary.org (see data/SwahiliDictionaryRepository and
 * THIRD_PARTY_NOTICES.md). A bonus English translation is shown when a
 * separate bilingual source also had that word, but it is secondary --
 * most entries only have the Swahili definition, which is expected.
 * Opened identically from the Home, Reading, and Quiz screens.
 *
 * Shown as a Dialog overlay (not a nav route) so opening/closing it does
 * not interrupt the Reading screen's countdown timer or reset in-progress
 * Quiz state (hearts/XP/streak/current question).
 */
@Composable
fun SwahiliDictionaryDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val allEntries = remember { SwahiliDictionaryRepository.load(context) }
    var query by remember { mutableStateOf("") }

    // Debounce: only re-filter ~150ms after the user stops typing, instead
    // of on every single keystroke. Filtering itself is cheap for ~2,100
    // entries, but recomputing LazyColumn's key map for a changed result
    // set on every character (see the `key = { it.headword }` below) adds
    // up on lower-end devices; debouncing cuts how often that work happens.
    var debouncedQuery by remember { mutableStateOf("") }
    LaunchedEffect(query) {
        delay(150)
        debouncedQuery = query
    }

    // With no search term, don't render/measure all ~2,100 entries --
    // that's wasted work most students never need. Show a capped
    // alphabetical preview instead and ask them to type to search further.
    val filtered = remember(debouncedQuery, allEntries) {
        if (debouncedQuery.isBlank()) {
            allEntries.take(60)
        } else {
            allEntries.filter {
                it.headword.contains(debouncedQuery, ignoreCase = true) ||
                    it.swahiliDefinition.contains(debouncedQuery, ignoreCase = true) ||
                    it.englishMeaning.contains(debouncedQuery, ignoreCase = true)
            }
        }
    }
    val isBrowsingPreview = debouncedQuery.isBlank()

    val backgroundBrush = remember { Brush.verticalGradient(listOf(DictNavyTop, DictNavyBottom)) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // --- Top bar ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 20.dp, top = 20.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Close", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Swahili Dictionary",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Filled.MenuBook, contentDescription = null, tint = Color.White.copy(alpha = 0.8f))
                }
                Text(
                    text = "${allEntries.size} Kiswahili words, standalone from any chapter",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.65f),
                    modifier = Modifier.padding(start = 56.dp, bottom = 14.dp)
                )

                // --- Search bar ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color.White)
                        .border(2.dp, DictSearchBorder, RoundedCornerShape(28.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Search, contentDescription = "Search", tint = DictNavyTop)
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text(
                                "Search a Kiswahili word...",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        )
                    }
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear search", tint = Color.Gray)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (isBrowsingPreview) {
                        "Showing first ${filtered.size} of ${allEntries.size} words -- type to search"
                    } else {
                        "${filtered.size} result${if (filtered.size == 1) "" else "s"}"
                    },
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.55f),
                    modifier = Modifier.padding(start = 24.dp, bottom = 8.dp)
                )

                // --- Results ---
                if (filtered.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "No matching words found.",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 20.dp)
                    ) {
                        items(filtered, key = { it.headword }) { entry ->
                            DictionaryEntryCard(entry)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DictionaryEntryCard(entry: DictionaryEntry) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(2.dp, DictCardBorder, RoundedCornerShape(16.dp))
            .padding(vertical = 14.dp, horizontal = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = entry.headword,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            if (entry.partOfSpeech.isNotBlank()) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = entry.partOfSpeech,
                    fontSize = 12.sp,
                    fontStyle = FontStyle.Italic,
                    color = Color.Gray
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "MAANA (KISWAHILI)",
            fontSize = 12.sp,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = Color(0xFF1E7A46),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = entry.swahiliDefinition,
            fontSize = 14.sp,
            lineHeight = 19.sp,
            textAlign = TextAlign.Center,
            color = Color(0xFF555555),
            modifier = Modifier.fillMaxWidth()
        )
        if (entry.englishMeaning.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "ENGLISH",
                fontSize = 11.sp,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = Color.Gray,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = entry.englishMeaning,
                fontSize = 13.sp,
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center,
                color = Color(0xFF888888),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
