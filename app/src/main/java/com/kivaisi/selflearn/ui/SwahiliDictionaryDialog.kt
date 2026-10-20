package com.kivaisi.selflearn.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
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
            // Scroll back to the top whenever the result SET changes (new
            // query, not just new characters within the same debounce) --
            // otherwise a scroll position kept from browsing a previous
            // query's longer list can leave the new top-ranked match (e.g.
            // an exact headword hit) scrolled out of view, making it look
            // "missing" even though it's actually the #1 result.
            val listState = rememberLazyListState()
            LaunchedEffect(filtered) {
                listState.scrollToItem(0)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
            ) {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
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

/**
 * One search-result row. Words that have a bundled illustration get a small
 * rounded thumbnail of it on the left (a familiar "contact photo" pattern);
 * the ~16,000 rows without one keep the plain text layout, so the list's
 * rhythm isn't broken by empty placeholder slots.
 */
@Composable
private fun DictionaryListRow(entry: DictionaryEntry, onClick: () -> Unit) {
    val imageResId = rememberIllustrationResId(entry.imageResName)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = if (imageResId != 0) 10.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (imageResId != 0) {
            Image(
                painter = painterResource(id = imageResId),
                contentDescription = null, // decorative: the headword text beside it is the label
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE6E6E6), RoundedCornerShape(10.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.headword,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = DictNavyTop
            )
            val pos = swahiliPartOfSpeech(entry.partOfSpeech)
            if (pos.isNotBlank()) {
                Text(
                    text = pos,
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
 * Full-definition page for one word (navigated to from a search row, the way
 * Oxford's app opens a dedicated page per word).
 *
 * Layout, top to bottom, inside one white card that always fills the screen:
 *  1. Illustration header (only for the ~20 illustrated words).
 *  2. Headword, left-aligned, with pills for the Swahili part of speech and the
 *     bonus English meaning (each pill only when that data exists).
 *  3. "Maana" block: the definition, left-aligned body text.
 *  4. "Mfano" block: the example as a quote with an accent bar, the headword
 *     bolded inside it, and a small Halisi/Imetungwa tag for real vs generated.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DictionaryDetailScreen(entry: DictionaryEntry, onBack: () -> Unit) {
    val imageResId = rememberIllustrationResId(entry.imageResName)

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

        // BoxWithConstraints gives the visible height so the card can be at
        // least that tall (no empty navy band under short entries) while
        // still scrolling for long ones.
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val minCardHeight = maxHeight - 16.dp
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 24.dp, bottomEnd = 24.dp),
                    color = Color.White,
                    shadowElevation = 6.dp,
                    border = BorderStroke(1.dp, DictCardBorder.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = minCardHeight)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (imageResId != 0) {
                            Image(
                                painter = painterResource(id = imageResId),
                                contentDescription = "Illustration of ${entry.headword}",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(230.dp)
                                    .padding(start = 20.dp, end = 20.dp, top = 20.dp)
                            )
                        }

                        Column(
                            modifier = Modifier.padding(
                                start = 22.dp,
                                end = 22.dp,
                                top = if (imageResId != 0) 12.dp else 26.dp,
                                bottom = 28.dp
                            )
                        ) {
                            Text(
                                text = entry.headword,
                                fontSize = 32.sp,
                                lineHeight = 38.sp,
                                fontWeight = FontWeight.Bold,
                                color = DictNavyTop
                            )

                            val pos = swahiliPartOfSpeech(entry.partOfSpeech)
                            if (pos.isNotBlank() || entry.englishMeaning.isNotBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (pos.isNotBlank()) {
                                        InfoPill(text = pos, background = DictNavyTop.copy(alpha = 0.08f), textColor = DictNavyTop)
                                    }
                                    if (entry.englishMeaning.isNotBlank()) {
                                        InfoPill(
                                            text = entry.englishMeaning,
                                            prefix = "EN",
                                            background = DictCardBorder.copy(alpha = 0.18f),
                                            textColor = Color(0xFF6B4A06)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(22.dp))
                            HorizontalDivider(color = Color(0xFFEFEAE0), thickness = 1.dp)
                            Spacer(modifier = Modifier.height(20.dp))

                            SectionHeader(icon = Icons.Filled.MenuBook, title = "Maana", accent = MeaningGreen)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = entry.swahiliDefinition,
                                fontSize = 17.sp,
                                lineHeight = 26.sp,
                                color = Color(0xFF2B2B2B)
                            )

                            Spacer(modifier = Modifier.height(26.dp))

                            val exampleAccent = if (entry.isExampleGenerated) GeneratedAmber else MeaningGreen
                            SectionHeader(
                                icon = Icons.Filled.FormatQuote,
                                title = "Mfano",
                                accent = exampleAccent,
                                tag = if (entry.isExampleGenerated) "Imetungwa" else "Halisi"
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(exampleAccent)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = highlightWord(entry.example, entry.headword),
                                    fontSize = 16.sp,
                                    lineHeight = 24.sp,
                                    fontStyle = FontStyle.Italic,
                                    color = Color(0xFF3A3A3A),
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                            if (entry.isExampleGenerated) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "This example was auto-generated to show the word in use, not taken from real text.",
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

private val MeaningGreen = Color(0xFF1E7A46)
private val GeneratedAmber = Color(0xFFB07A17)

@Composable
private fun SectionHeader(icon: ImageVector, title: String, accent: Color, tag: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = DictNavyTop
        )
        if (tag != null) {
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = tag,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = accent,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(accent.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
private fun InfoPill(text: String, background: Color, textColor: Color, prefix: String? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        if (prefix != null) {
            Text(
                text = prefix,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = textColor.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(text = text, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = textColor)
    }
}

/**
 * Resolves a data-driven illustration name (e.g. "word_baba") to a drawable
 * id, or 0 when there is none. The name lives in the dictionary DB rather
 * than as a compile-time R.drawable reference, so getIdentifier() is needed;
 * it only runs for the ~20 illustrated words and is cached per name.
 */
@Composable
private fun rememberIllustrationResId(name: String): Int {
    val context = LocalContext.current
    return remember(name) {
        if (name.isBlank()) 0
        else context.resources.getIdentifier(name, "drawable", context.packageName)
    }
}

/** Short tags from the bilingual source mapped to the Swahili grammar terms students learn. */
private fun swahiliPartOfSpeech(raw: String): String = when (raw.trim().lowercase()) {
    "" -> ""
    "n" -> "nomino"
    "v" -> "kitenzi"
    "adj" -> "kivumishi"
    "adv" -> "kielezi"
    "pron" -> "kiwakilishi"
    "num" -> "namba"
    "prep" -> "kihusishi"
    "conj" -> "kiunganishi"
    "interj" -> "kihisishi"
    "cop" -> "kishirikishi"
    "prt" -> "kijineno"
    "expr" -> "msemo"
    else -> raw
}

/** Bolds every case-insensitive occurrence of the headword inside the example sentence. */
private fun highlightWord(sentence: String, headword: String): AnnotatedString {
    val needle = headword.trim().trimStart('-')
    if (needle.length < 2) return AnnotatedString(sentence)
    val regex = Regex(Regex.escape(needle), RegexOption.IGNORE_CASE)
    return buildAnnotatedString {
        var last = 0
        for (m in regex.findAll(sentence)) {
            append(sentence.substring(last, m.range.first))
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Normal, color = DictNavyTop)) {
                append(m.value)
            }
            last = m.range.last + 1
        }
        append(sentence.substring(last))
    }
}
