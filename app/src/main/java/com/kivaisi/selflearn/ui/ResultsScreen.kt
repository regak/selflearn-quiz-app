package com.kivaisi.selflearn.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kivaisi.selflearn.model.AnsweredQuestion
import com.kivaisi.selflearn.model.QuizResultHolder
import com.kivaisi.selflearn.ui.theme.AccentGreen
import com.kivaisi.selflearn.ui.theme.AccentRed
import com.kivaisi.selflearn.ui.theme.AccentYellow
import com.kivaisi.selflearn.ui.theme.PrimaryPurple

@Composable
fun ResultsScreen(onBackHome: () -> Unit, onRetryChapter: (String) -> Unit) {
    val result = QuizResultHolder.lastResult ?: run {
        onBackHome()
        return
    }
    var showReview by remember { mutableStateOf(false) }

    val encouragement = when {
        result.ranOutOfHearts -> "Good effort! Review the material and try again -- you'll nail it next time."
        result.correctCount == result.answered.size -> "Perfect run! You really understood this chapter."
        result.correctCount.toFloat() / result.answered.size.toFloat() >= 0.6f -> "Nice work! You're getting the hang of this."
        else -> "Good start! A little more review will make this stick."
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        item { Spacer(modifier = Modifier.height(32.dp)) }
        item {
            Icon(
                imageVector = Icons.Filled.EmojiEvents,
                contentDescription = "Trophy",
                tint = AccentYellow,
                modifier = Modifier.size(72.dp)
            )
        }
        item {
            Text(
                text = "Quiz Complete!",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = result.chapterTitle,
                fontSize = 15.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    label = "Score",
                    value = "${result.correctCount}/${result.answered.size}",
                    color = PrimaryPurple,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "XP Earned",
                    value = "${result.finalXp}",
                    color = AccentYellow,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Best Streak",
                    value = "${result.bestStreak}x",
                    color = AccentGreen,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryPurple.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Hearts left", tint = AccentRed)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = encouragement,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (result.missed.isNotEmpty()) {
            item {
                OutlinedButton(
                    onClick = { showReview = !showReview },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Review missed questions (${result.missed.size})")
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = if (showReview) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            if (showReview) {
                items(result.missed) { missed ->
                    MissedQuestionCard(missed)
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { onRetryChapter(result.chapterId) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
            ) {
                Text("Retry This Chapter")
            }
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onBackHome,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Back to Home")
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun MissedQuestionCard(missed: AnsweredQuestion) {
    Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(missed.question.prompt, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(6.dp))
            val explanationText = missed.question.explanation.ifBlank { missed.question.modelAnswer }
            Text(explanationText, fontSize = 13.sp, color = Color.Gray, lineHeight = 18.sp)
        }
    }
}
