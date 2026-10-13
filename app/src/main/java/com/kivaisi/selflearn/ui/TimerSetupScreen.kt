package com.kivaisi.selflearn.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kivaisi.selflearn.data.ContentRepository
import com.kivaisi.selflearn.ui.theme.PrimaryPurple

/**
 * Lets a parent/teacher/instructor set how long the student has to read
 * before the quiz unlocks. Defaults to the chapter's suggested time (5 min).
 */
@Composable
fun TimerSetupScreen(chapterId: String, onStart: (Int) -> Unit) {
    val chapter = ContentRepository.findChapter(chapterId) ?: return
    var minutes by remember { mutableStateOf(chapter.defaultTimerSeconds / 60) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Icon(
            imageVector = Icons.Filled.Timer,
            contentDescription = "Timer",
            tint = PrimaryPurple,
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = chapter.title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Set the reading time (parent/teacher setting)",
            fontSize = 14.sp,
            color = androidx.compose.ui.graphics.Color.Gray
        )
        Spacer(modifier = Modifier.height(32.dp))

        Card(shape = RoundedCornerShape(20.dp)) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { if (minutes > 1) minutes-- }) {
                    Icon(Icons.Filled.Remove, contentDescription = "Decrease minutes")
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "$minutes min",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryPurple
                )
                Spacer(modifier = Modifier.width(16.dp))
                IconButton(onClick = { if (minutes < 30) minutes++ }) {
                    Icon(Icons.Filled.Add, contentDescription = "Increase minutes")
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
        Text(
            text = "The quiz unlocks once the timer reaches zero, or sooner if the student scrolls to the end of the chapter.",
            fontSize = 13.sp,
            color = androidx.compose.ui.graphics.Color.Gray,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = { onStart(minutes * 60) },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
        ) {
            Text("Start Reading", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}
