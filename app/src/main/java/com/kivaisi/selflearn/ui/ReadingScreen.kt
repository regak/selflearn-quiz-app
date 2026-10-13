package com.kivaisi.selflearn.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kivaisi.selflearn.data.ContentRepository
import com.kivaisi.selflearn.ui.theme.AccentGreen
import com.kivaisi.selflearn.ui.theme.PrimaryPurple
import kotlinx.coroutines.delay

/**
 * Gating rule chosen for this prototype: the Start Quiz button unlocks when
 * EITHER the countdown timer reaches zero OR the student scrolls to the
 * bottom of the chapter text -- whichever happens first.
 */
@Composable
fun ReadingScreen(chapterId: String, timerSeconds: Int, onStartQuiz: () -> Unit) {
    val chapter = ContentRepository.findChapter(chapterId) ?: return
    var secondsLeft by remember { mutableStateOf(timerSeconds) }
    val scrollState = rememberScrollState()

    LaunchedEffect(chapterId) {
        while (secondsLeft > 0) {
            delay(1000)
            secondsLeft -= 1
        }
    }

    val scrolledToBottom by remember {
        derivedStateOf {
            scrollState.value > 0 && scrollState.value >= scrollState.maxValue - 20
        }
    }

    val unlocked = secondsLeft <= 0 || scrolledToBottom

    Column(modifier = Modifier.fillMaxSize()) {
        // Top timer bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = chapter.subject.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryPurple
                )
                Text(
                    text = chapter.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (unlocked) Icons.Filled.CheckCircle else Icons.Filled.Timer,
                    contentDescription = "Timer",
                    tint = if (unlocked) AccentGreen else PrimaryPurple
                )
                Spacer(modifier = Modifier.width(6.dp))
                val mins = secondsLeft / 60
                val secs = secondsLeft % 60
                Text(
                    text = if (unlocked && secondsLeft <= 0) "Ready!" else String.format("%d:%02d", mins, secs),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (unlocked) AccentGreen else PrimaryPurple
                )
            }
        }
        LinearProgressIndicator(
            progress = { if (timerSeconds == 0) 1f else 1f - (secondsLeft.toFloat() / timerSeconds.toFloat()).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .padding(20.dp)
            ) {
                Text(
                    text = chapter.content,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    color = Color(0xFF2B2B2B)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "-- End of chapter --",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            if (!unlocked) {
                Text(
                    text = "Finish the timer or scroll to the end to unlock the quiz.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            Button(
                onClick = onStartQuiz,
                enabled = unlocked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
            ) {
                Text("Start Quiz", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
