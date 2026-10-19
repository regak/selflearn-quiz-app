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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kivaisi.selflearn.data.GlossaryTerm

// Navy "offline dictionary" palette, deliberately distinct from the rest of
// the app's purple/green/yellow game theme -- this screen is meant to read
// like a dedicated reference-book tool (modeled on a standalone offline
// English-dictionary app's look: dark background, bold search bar, gold
// bordered definition cards).
private val DictNavyTop = Color(0xFF15294F)
private val DictNavyBottom = Color(0xFF0A1830)
private val DictSearchBorder = Color(0xFFE5433B)
private val DictCardBorder = Color(0xFFF0B429)

/**
 * Swahili Dictionary lookup. Shown as a Dialog overlay (not a nav route)
 * so it can be opened from the Home, Reading, or Quiz screens without
 * tearing down that screen's live state (chapter list, countdown timer,
 * hearts/XP/streak).
 *
 * [terms] is just a flat list -- callers decide what to show: a single
 * chapter's glossary (Reading/Quiz screens) or every chapter's glossary
 * merged together (Home screen "Kamusi" button). When [terms] spans more
 * than one [GlossaryTerm.chapterTitle], a row of filter chips appears so
 * the student can narrow the list down to one chapter.
 */
@Composable
fun GlossaryDialog(title: String, subtitle: String, terms: List<GlossaryTerm>, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var selectedChapter by remember { mutableStateOf<String?>(null) }

    val chapterOptions = remember(terms) {
        terms.map { it.chapterTitle }.distinct().filter { it.isNotBlank() }
    }

    val filtered = remember(query, terms, selectedChapter) {
        terms
            .filter { selectedChapter == null || it.chapterTitle == selectedChapter }
            .filter {
                query.isBlank() ||
                    it.term.contains(query, ignoreCase = true) ||
                    it.englishDefinition.contains(query, ignoreCase = true) ||
                    it.swahili.contains(query, ignoreCase = true)
            }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(DictNavyTop, DictNavyBottom)))
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
                        text = title,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Filled.MenuBook, contentDescription = null, tint = Color.White.copy(alpha = 0.8f))
                }
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.65f),
                    modifier = Modifier.padding(start = 56.dp, bottom = 12.dp)
                )

                // --- Chapter filter chips (only shown for the merged, multi-chapter view) ---
                if (chapterOptions.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilterPill(
                            label = "ALL WORDS",
                            selected = selectedChapter == null,
                            onClick = { selectedChapter = null }
                        )
                        chapterOptions.forEach { chapterTitle ->
                            FilterPill(
                                label = chapterTitle.uppercase(),
                                selected = selectedChapter == chapterTitle,
                                onClick = { selectedChapter = chapterTitle }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                } else {
                    Spacer(modifier = Modifier.height(6.dp))
                }

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
                                "Search a word (English or Kiswahili)...",
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
                Spacer(modifier = Modifier.height(16.dp))

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
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(bottom = 20.dp)
                    ) {
                        items(filtered) { entry ->
                            GlossaryEntryCard(entry)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) Color.White else Color.White.copy(alpha = 0.12f))
            .border(
                width = if (selected) 0.dp else 1.dp,
                color = Color.White.copy(alpha = 0.4f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) DictNavyTop else Color.White
        )
    }
}

@Composable
private fun GlossaryEntryCard(entry: GlossaryTerm) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(2.dp, DictCardBorder, RoundedCornerShape(16.dp))
            .padding(vertical = 16.dp, horizontal = 14.dp)
    ) {
        Text(
            text = entry.term,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        GlossarySection(label = "Meaning", body = entry.englishDefinition)

        if (entry.example.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            GlossarySection(label = "Example", body = entry.example)
        }

        Spacer(modifier = Modifier.height(12.dp))
        GlossarySection(label = "Kiswahili", body = entry.swahili, accent = true)
    }
}

@Composable
private fun GlossarySection(label: String, body: String, accent: Boolean = false) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = if (accent) Color(0xFF1E7A46) else Color.Black,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = body,
            fontSize = 14.sp,
            lineHeight = 19.sp,
            textAlign = TextAlign.Center,
            color = Color(0xFF555555),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
