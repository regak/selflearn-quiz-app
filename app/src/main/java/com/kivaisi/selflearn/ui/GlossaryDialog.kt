package com.kivaisi.selflearn.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kivaisi.selflearn.data.Chapter
import com.kivaisi.selflearn.data.GlossaryTerm
import com.kivaisi.selflearn.ui.theme.AccentGreen
import com.kivaisi.selflearn.ui.theme.AccentYellow
import com.kivaisi.selflearn.ui.theme.PrimaryPurple

/**
 * Swahili Dictionary lookup for a chapter's key vocabulary. Shown as a
 * Dialog overlay (not a nav route) so it can be opened from the Reading
 * screen without tearing down that screen's live countdown timer, and
 * from the Quiz screen without disturbing hearts/XP/streak state.
 */
@Composable
fun GlossaryDialog(chapter: Chapter, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, chapter.id) {
        if (query.isBlank()) {
            chapter.glossary
        } else {
            chapter.glossary.filter {
                it.term.contains(query, ignoreCase = true) ||
                    it.englishDefinition.contains(query, ignoreCase = true) ||
                    it.swahili.contains(query, ignoreCase = true)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            color = Color.White
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp, 20.dp, 12.dp, 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Translate, contentDescription = "Dictionary", tint = PrimaryPurple)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Swahili Dictionary", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(chapter.title, fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    placeholder = { Text("Search a word (English or Kiswahili)...") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (filtered.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "No matching words found.",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        items(filtered) { entry ->
                            GlossaryCard(entry)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlossaryCard(entry: GlossaryTerm) {
    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(entry.term, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PrimaryPurple)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                entry.englishDefinition,
                fontSize = 13.sp,
                color = Color(0xFF444444)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(AccentGreen.copy(alpha = 0.12f))
                    .padding(10.dp)
            ) {
                Column {
                    Text(
                        "KISWAHILI",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentGreen
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(entry.swahili, fontSize = 13.sp, lineHeight = 18.sp)
                }
            }
        }
    }
}
