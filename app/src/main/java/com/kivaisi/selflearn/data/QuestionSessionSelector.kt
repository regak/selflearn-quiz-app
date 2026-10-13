package com.kivaisi.selflearn.data

/**
 * Remembers which question variant was used per dimension, per chapter, on
 * the last session -- so the next NEW session can prefer a different
 * variant for the same dimension instead of silently repeating it.
 *
 * In-memory singleton: this resets if the app process is killed. That's an
 * acceptable prototype tradeoff; swap the backing store for DataStore/Room
 * if session history needs to survive app restarts.
 *
 * IMPORTANT: this history is only updated by [QuestionSessionSelector] when
 * starting a NEW quiz session. The "review missed questions" flow on the
 * results screen does NOT go through this -- it intentionally re-shows the
 * exact question the student got wrong, because its job is remediation of
 * that specific mistake, not sampling variety.
 */
object SessionHistory {
    // chapterId -> (dimension -> last-used question id)
    private val lastUsedByChapter: MutableMap<String, MutableMap<String, String>> = mutableMapOf()

    fun lastUsedQuestionId(chapterId: String, dimension: String): String? =
        lastUsedByChapter[chapterId]?.get(dimension)

    fun recordSession(chapterId: String, selected: List<Question>) {
        val perDimension = lastUsedByChapter.getOrPut(chapterId) { mutableMapOf() }
        selected.forEach { question ->
            perDimension[question.dimension] = question.id
        }
    }

    /** Test hook / "reset for a new student" affordance -- clears all remembered history. */
    fun clearAll() {
        lastUsedByChapter.clear()
    }
}

/**
 * Builds the question set for ONE new quiz session from a chapter's full
 * question pool.
 *
 * Strategy (stratified sampling + exposure control):
 * 1. Group the pool by [Question.dimension] -- this guarantees every
 *    session still covers the same breadth of concepts (one question per
 *    dimension), rather than randomly favoring easy/over-represented ones.
 * 2. Within each dimension, prefer a variant that was NOT used in the
 *    immediately previous session for this chapter (falls back to any
 *    variant if the dimension only has one, or all have been seen).
 * 3. Keep the opening question Socratic (dimension "socratic_intro") and
 *    sort the rest by ascending difficulty, matching the app's existing
 *    "leading question first, then increasing difficulty" design.
 *
 * This keeps retrieval varied across sessions (testing the same CONCEPT via
 * a different ITEM) rather than either (a) replaying an identical quiz
 * every time, which mostly measures memorization of the specific
 * question-answer pair, or (b) fully randomizing, which can accidentally
 * skip whole dimensions in a given session.
 */
object QuestionSessionSelector {

    fun selectNewSession(chapter: Chapter, random: kotlin.random.Random = kotlin.random.Random.Default): List<Question> {
        val byDimension: Map<String, List<Question>> = chapter.questions.groupBy { it.dimension }

        val chosen: List<Question> = byDimension.map { (dimension, variants) ->
            val lastUsedId = SessionHistory.lastUsedQuestionId(chapter.id, dimension)
            val unseen = variants.filter { it.id != lastUsedId }
            val pool = if (unseen.isNotEmpty()) unseen else variants
            pool[random.nextInt(pool.size)]
        }

        val socratic = chosen.filter { it.type == QuestionType.SOCRATIC_OPEN }
        val rest = chosen.filterNot { it.type == QuestionType.SOCRATIC_OPEN }
            .sortedBy { it.difficulty }

        val session = socratic + rest

        SessionHistory.recordSession(chapter.id, session)
        return session
    }
}
