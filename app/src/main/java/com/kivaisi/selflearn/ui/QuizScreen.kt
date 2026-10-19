package com.kivaisi.selflearn.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
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
import com.kivaisi.selflearn.data.ContentRepository
import com.kivaisi.selflearn.data.Question
import com.kivaisi.selflearn.data.QuestionSessionSelector
import com.kivaisi.selflearn.data.QuestionType
import com.kivaisi.selflearn.model.AnsweredQuestion
import com.kivaisi.selflearn.model.QuizResult
import com.kivaisi.selflearn.model.QuizResultHolder
import com.kivaisi.selflearn.ui.theme.AccentGreen
import com.kivaisi.selflearn.ui.theme.AccentRed
import com.kivaisi.selflearn.ui.theme.AccentYellow
import com.kivaisi.selflearn.ui.theme.PrimaryPurple

private const val MAX_HEARTS = 3
private const val XP_PER_CORRECT = 10
private const val STREAK_BONUS_THRESHOLD = 3
private const val STREAK_BONUS_XP = 5

@Composable
fun QuizScreen(chapterId: String, onFinished: () -> Unit, onRetry: () -> Unit) {
    val chapter = ContentRepository.findChapter(chapterId) ?: return
    // A fresh question set is sampled every time this screen is newly
    // composed (i.e. every new quiz session for this chapter -- including
    // "Retry This Chapter" from the results screen), so the next attempt
    // varies which dimension-variant is asked instead of repeating the
    // identical five questions. remember(chapter.id) ties this to the
    // lifetime of this composable instance, not to recomposition within a
    // session, so hearts/XP/currentIndex changes don't re-roll the set.
    val questions = remember(chapter.id) { QuestionSessionSelector.selectNewSession(chapter) }

    var currentIndex by remember { mutableStateOf(0) }
    var hearts by remember { mutableStateOf(MAX_HEARTS) }
    var xp by remember { mutableStateOf(0) }
    var streak by remember { mutableStateOf(0) }
    var bestStreak by remember { mutableStateOf(0) }
    val answered = remember { mutableStateListOf<AnsweredQuestion>() }
    var outOfHearts by remember { mutableStateOf(false) }
    var showGlossary by remember { mutableStateOf(false) }

    fun finish() {
        QuizResultHolder.lastResult = QuizResult(
            chapterId = chapter.id,
            chapterTitle = chapter.title,
            finalXp = xp,
            bestStreak = bestStreak,
            heartsRemaining = hearts,
            ranOutOfHearts = outOfHearts,
            answered = answered.toList()
        )
        onFinished()
    }

    if (outOfHearts) {
        OutOfHeartsScreen(onRetry = onRetry)
        return
    }

    if (currentIndex >= questions.size) {
        LaunchedEffect(Unit) { finish() }
        return
    }

    val question = questions[currentIndex]

    Column(modifier = Modifier.fillMaxSize()) {
        QuizHeader(
            hearts = hearts,
            xp = xp,
            streak = streak,
            progress = currentIndex.toFloat() / questions.size.toFloat(),
            questionNumber = currentIndex + 1,
            totalQuestions = questions.size,
            onDictionaryClick = { showGlossary = true }
        )

        Box(modifier = Modifier.weight(1f)) {
            // key() forces Compose to treat each question as a fresh subtree, so
            // per-question remembered state (selected option, revealed/submitted
            // flags) never leaks into the next question.
            key(question.id) {
            QuestionBody(
                question = question,
                chapterText = chapter.content,
                onAnswered = { wasCorrect, freeText, selfAssessment ->
                    answered.add(
                        AnsweredQuestion(
                            question = question,
                            wasCorrect = wasCorrect,
                            userFreeTextAnswer = freeText,
                            selfAssessment = selfAssessment
                        )
                    )
                    if (wasCorrect) {
                        streak += 1
                        if (streak > bestStreak) bestStreak = streak
                        var gained = XP_PER_CORRECT
                        if (streak >= STREAK_BONUS_THRESHOLD) gained += STREAK_BONUS_XP
                        xp += gained
                    } else {
                        streak = 0
                        if (question.type != QuestionType.SOCRATIC_OPEN) {
                            hearts -= 1
                        }
                    }
                },
                onContinue = {
                    if (hearts <= 0) {
                        outOfHearts = true
                    } else {
                        currentIndex += 1
                    }
                }
            )
            }
        }
    }

    if (showGlossary) {
        GlossaryDialog(
            title = "Swahili Dictionary",
            subtitle = chapter.title,
            terms = chapter.glossary,
            onDismiss = { showGlossary = false }
        )
    }
}

@Composable
private fun QuizHeader(
    hearts: Int,
    xp: Int,
    streak: Int,
    progress: Float,
    questionNumber: Int,
    totalQuestions: Int,
    onDictionaryClick: () -> Unit
) {
    Column(modifier = Modifier.padding(20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                repeat(MAX_HEARTS) { i ->
                    Icon(
                        imageVector = if (i < hearts) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Heart",
                        tint = AccentRed,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Bolt, contentDescription = "XP", tint = AccentYellow)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "$xp XP", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (streak >= 2) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(PrimaryPurple)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("${streak}x streak", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onDictionaryClick, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Translate, contentDescription = "Swahili Dictionary", tint = PrimaryPurple)
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Question $questionNumber of $totalQuestions",
            fontSize = 12.sp,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun OutOfHeartsScreen(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.FavoriteBorder,
            contentDescription = "Out of hearts",
            tint = AccentRed,
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Out of hearts!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "No worries -- that chapter was tricky. Give it another read and try again, you've got this!",
            fontSize = 15.sp,
            color = Color.Gray,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
        ) {
            Text("Try Again", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/**
 * Renders the body for one question and its instant-feedback / self-assessment flow.
 * onAnswered is called once, right after the student answers (before Continue).
 * onContinue moves to the next question / ends the quiz.
 */
@Composable
private fun QuestionBody(
    question: Question,
    chapterText: String,
    onAnswered: (wasCorrect: Boolean, freeText: String?, selfAssessment: String?) -> Unit,
    onContinue: () -> Unit
) {
    when (question.type) {
        QuestionType.SOCRATIC_OPEN -> SocraticQuestionBody(question, onAnswered, onContinue)
        else -> ChoiceQuestionBody(question, onAnswered, onContinue)
    }
}

@Composable
private fun SocraticQuestionBody(
    question: Question,
    onAnswered: (Boolean, String?, String?) -> Unit,
    onContinue: () -> Unit
) {
    var answerText by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }
    var selfAssessment by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "THINK ABOUT IT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryPurple
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = question.prompt,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (!submitted) {
            OutlinedTextField(
                value = answerText,
                onValueChange = { answerText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .heightIn(min = 120.dp),
                label = { Text("Type your answer") },
                placeholder = { Text("Explain your thinking in your own words...") }
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { submitted = true },
                enabled = answerText.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
            ) {
                Text("Submit Answer")
            }
        } else {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AccentYellow.copy(alpha = 0.18f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Your answer", fontSize = 12.sp, color = Color.Gray)
                    Text(answerText, fontSize = 14.sp)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AccentGreen.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Model answer / explanation", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccentGreen)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(question.modelAnswer, fontSize = 14.sp, lineHeight = 20.sp)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (selfAssessment == null) {
                Text("How close was your answer?", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Not quite", "Sort of", "Got it!").forEach { label ->
                        OutlinedButton(
                            onClick = {
                                selfAssessment = label
                                // Self-assessed Socratic question never costs a heart; "Got it!" counts as correct for streak/XP.
                                onAnswered(label == "Got it!", answerText, label)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(label, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                ) {
                    Text("Continue")
                }
            }
        }
    }
}

@Composable
private fun ChoiceQuestionBody(
    question: Question,
    onAnswered: (Boolean, String?, String?) -> Unit,
    onContinue: () -> Unit
) {
    var selectedIndex by remember { mutableStateOf(-1) }
    var revealed by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = if (question.type == QuestionType.TRUE_FALSE) "TRUE OR FALSE" else "MULTIPLE CHOICE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryPurple
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = question.prompt,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        question.options.forEachIndexed { index, option ->
            val isCorrectOption = index == question.correctIndex
            val isSelected = index == selectedIndex
            val colors = when {
                !revealed -> CardDefaults.cardColors()
                isCorrectOption -> CardDefaults.cardColors(containerColor = AccentGreen.copy(alpha = 0.25f))
                isSelected && !isCorrectOption -> CardDefaults.cardColors(containerColor = AccentRed.copy(alpha = 0.25f))
                else -> CardDefaults.cardColors()
            }
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = colors,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .clickable(enabled = !revealed) {
                        selectedIndex = index
                        revealed = true
                        onAnswered(isCorrectOption, null, null)
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(option, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    if (revealed && isCorrectOption) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = "Correct", tint = AccentGreen)
                    } else if (revealed && isSelected && !isCorrectOption) {
                        Icon(Icons.Filled.Cancel, contentDescription = "Incorrect", tint = AccentRed)
                    }
                }
            }
        }

        if (revealed) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryPurple.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (selectedIndex == question.correctIndex) "Correct!" else "Not quite",
                        fontWeight = FontWeight.Bold,
                        color = if (selectedIndex == question.correctIndex) AccentGreen else AccentRed
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(question.explanation, fontSize = 14.sp, lineHeight = 20.sp)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
            ) {
                Text("Continue")
            }
        }
    }
}
