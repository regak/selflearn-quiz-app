package com.kivaisi.selflearn.model

import com.kivaisi.selflearn.data.Question

/** Result of answering one question, kept for the "review missed questions" screen. */
data class AnsweredQuestion(
    val question: Question,
    val wasCorrect: Boolean, // for SOCRATIC_OPEN this reflects self-assessment ("got it" = true-ish)
    val userFreeTextAnswer: String? = null,
    val selfAssessment: String? = null // "not quite" / "sort of" / "got it"
)

/** Final outcome of a quiz run, held in memory to pass from QuizScreen to ResultsScreen. */
data class QuizResult(
    val chapterId: String,
    val chapterTitle: String,
    val finalXp: Int,
    val bestStreak: Int,
    val heartsRemaining: Int,
    val ranOutOfHearts: Boolean,
    val answered: List<AnsweredQuestion>
) {
    val missed: List<AnsweredQuestion> get() = answered.filter { !it.wasCorrect }
    val correctCount: Int get() = answered.count { it.wasCorrect }
}

/** Simple in-memory holder to pass the latest quiz result between screens without a DB. */
object QuizResultHolder {
    var lastResult: QuizResult? = null
}
