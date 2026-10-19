package com.kivaisi.selflearn.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
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
// app look: dark background, bold search bar, gold-bordered detail cards).
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
 * Two-screen flow (mirroring Oxford Dictionary's app UX): search shows a
 * plain tappable list of matching headwords only; tapping one navigates to
 * a dedicated detail screen with the full Swahili definition, part of
 * speech, and bonus English translation. Both screens live inside the same
 * Dialog overlay (not a nav route), so opening/closing the whole dictionary
 * does not interrupt the Reading screen's countdown timer or reset
 * in-progress Quiz state (hearts/XP/streak/current question).
 */
@Composable
fun SwahiliDictionaryDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var selectedEntry by remember { mutableStateOf<DictionaryEntry?>(null) }
    var totalCount by remember { mutableStateOf(0) }
    var filtered by remember { mutableStateOf<List<DictionaryEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Debounce: only re-query ~150ms after the user stops typing, instead
    // of on every single keystroke.
    var debouncedQuery by remember { mutableStateOf("") }
    LaunchedEffect(query) {
        delay(150)
        debouncedQuery = query
    }
    val isBrowsingPreview = debouncedQuery.isBlank()

    // Fetch the total word count once -- used only for the "N of 16,156
    // words" label, never for loading the whole dictionary into memory.
    LaunchedEffect(Unit) {
        totalCount = SwahiliDictionaryRepository.totalCount(context)
    }

    // Every query (the no-text preview, and every search) runs on
    // Dispatchers.IO inside the repository and only ever touches the
    // handful of rows actually needed (<=60 preview rows, <=50 ranked
    // search results) -- there is no "parse everything into memory first"
    // step, which is what made opening the dictionary feel slow before.
    LaunchedEffect(debouncedQuery) {
        isLoading = true
        filtered = if (debouncedQuery.isBlank()) {
            SwahiliDictionaryRepository.browsePreview(context)
        } else {
            SwahiliDictionaryRepository.search(context, debouncedQuery)
        }
        isLoading = false
    }

    val backgroundBrush = remember { Brush.verticalGradient(listOf(DictNavyTop, DictNavyBottom)) }

    Dialog(
        onDismissRequest = {
            // Back button inside the detail screen should return to the
            // search list, not close the whole dictionary.
            if (selectedEntry != null) {
                selectedEntry = null
            } else {
                onDismiss()
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
        ) {
            val entry = selectedEntry
            if (entry != null) {
                DictionaryDetailScreen(
                    entry = entry,
                    onBack = { selectedEntry = null }
                )
            } else {
                DictionarySearchScreen(
                    totalCount = totalCount,
                    filtered = filtered,
                    isLoading = isLoading,
                    isBrowsingPreview = isBrowsingPreview,
                    query = query,
                    onQueryChange = { query = it },
                    onDismiss = onDismiss,
                    onEntryClick = { selectedEntry = it }
                )
            }
        }
    }
}

@Composable
private fun DictionarySearchScreen(
    totalCount: Int,
    filtered: List<DictionaryEntry>,
    isLoading: Boolean,
    isBrowsingPreview: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onEntryClick: (DictionaryEntry) -> Unit
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
            text = "$totalCount Kiswahili words, standalone from any chapter",
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
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                )
            }
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Filled.Clear, contentDescription = "Clear search", tint = Color.Gray)
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = when {
                isLoading -> "Searching..."
                isBrowsingPreview -> "Showing first ${filtered.size} of $totalCount words -- type to search"
                else -> "${filtered.size} result${if (filtered.size == 1) "" else "s"} -- tap a word for its full definition"
            },
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.55f),
            modifier = Modifier.padding(start = 24.dp, bottom = 8.dp)
        )

        // --- Results: plain tappable list, like a real dictionary app's
        // search results -- just the headword + part of speech. Tapping
        // navigates to the full-definition detail screen. ---
        if (isLoading && filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        } else if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No matching words found.",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filtered, key = { it.headword }) { entry ->
                        DictionaryListRow(entry = entry, onClick = { onEntryClick(entry) })
                        HorizontalDivider(color = Color(0xFFE6E6E6), thickness = 1.dp)
                    }
                    item { Spacer(modifier = Modifier.height(4.dp)) }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun DictionaryListRow(entry: DictionaryEntry, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.headword,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF15294F)
            )
            if (entry.partOfSpeech.isNotBlank()) {
                Text(
                    text = entry.partOfSpeech,
                    fontSize = 11.sp,
                    fontStyle = FontStyle.Italic,
                    color = Color.Gray
                )
            }
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = Color.LightGray
        )
    }
}

/**
 * Full-definition detail screen for one dictionary entry -- navigated to
 * from tapping a search result row, the way Oxford Dictionary's app opens
 * a dedicated page per word instead of expanding inline.
 */
@Composable
private fun DictionaryDetailScreen(entry: DictionaryEntry, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back to search", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Swahili Dictionary",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.85f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .border(2.dp, DictCardBorder, RoundedCornerShape(20.dp))
                    .padding(vertical = 22.dp, horizontal = 20.dp)
            ) {
                Text(
                    text = entry.headword,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                if (entry.partOfSpeech.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = entry.partOfSpeech,
                        fontSize = 14.sp,
                        fontStyle = FontStyle.Italic,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))

                DictionarySection(label = "MAANA (KISWAHILI)", body = entry.swahiliDefinition, accentColor = Color(0xFF1E7A46))

                if (entry.englishMeaning.isNotBlank()) {
                    Spacer(modifier = Modifier.height(18.dp))
                    DictionarySection(label = "ENGLISH", body = entry.englishMeaning, accentColor = Color.Gray, italicBody = true)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DictionarySection(label: String, body: String, accentColor: Color, italicBody: Boolean = false) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = accentColor,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = body,
            fontSize = 16.sp,
            lineHeight = 23.sp,
            fontStyle = if (italicBody) FontStyle.Italic else FontStyle.Normal,
            textAlign = TextAlign.Center,
            color = Color(0xFF444444),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
