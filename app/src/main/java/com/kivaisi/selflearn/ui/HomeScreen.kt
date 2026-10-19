package com.kivaisi.selflearn.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kivaisi.selflearn.data.Chapter
import com.kivaisi.selflearn.data.ContentRepository
import com.kivaisi.selflearn.data.SwahiliDictionaryRepository
import com.kivaisi.selflearn.ui.theme.AccentGreen
import com.kivaisi.selflearn.ui.theme.AccentYellow
import com.kivaisi.selflearn.ui.theme.PrimaryPurple

@Composable
fun HomeScreen(onChapterClick: (String) -> Unit) {
    var showGlossary by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Warm up the dictionary's SQLite connection (copy-from-assets + open)
    // in the background as soon as Home appears, so by the time the
    // student taps "Kamusi" the database is already open and the first
    // query returns instantly instead of paying that cost on first open.
    LaunchedEffect(Unit) {
        SwahiliDictionaryRepository.warmUp(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "SelfLearn Quiz",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryPurple
                )
                Text(
                    text = "Read a chapter, then test yourself!",
                    fontSize = 15.sp,
                    color = Color.Gray
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            // Prominent "Swahili Dictionary" button, reachable from the home
            // screen before a chapter is even picked. Opens the standalone,
            // chapter-independent dictionary (not tied to lesson content).
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(PrimaryPurple.copy(alpha = 0.12f))
                    .clickable { showGlossary = true }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Translate,
                    contentDescription = "Swahili Dictionary",
                    tint = PrimaryPurple,
                    modifier = Modifier.size(26.dp)
                )
                Text(
                    text = "Kamusi",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryPurple
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Choose a chapter",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            items(ContentRepository.chapters) { chapter ->
                ChapterCard(chapter = chapter, onClick = { onChapterClick(chapter.id) })
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    if (showGlossary) {
        SwahiliDictionaryDialog(onDismiss = { showGlossary = false })
    }
}

@Composable
private fun ChapterCard(chapter: Chapter, onClick: () -> Unit) {
    val accent = if (chapter.subject == "Geography") AccentGreen else AccentYellow
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(accent.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (chapter.subject == "Geography") Icons.Filled.Terrain else Icons.Filled.Science,
                    contentDescription = chapter.subject,
                    tint = PrimaryPurple,
                    modifier = Modifier.size(30.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chapter.subject.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryPurple
                )
                Text(
                    text = chapter.title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${chapter.sessionQuestionCount} questions * ~${chapter.content.split(" ").size} words",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}
