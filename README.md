# SelfLearn Quiz App (Prototype)

A gamified "read a chapter, then get quizzed" self-learning Android app prototype,
built to validate the concept for Alexander Kivaisi's accessibility/low-resource-language
NLP learning tools research before further investment. This prototype is English-only;
Swahili content is planned for a later iteration.

## Concept

1. **Home screen** -- lists sample chapters (Geography: *Rivers and Erosion*; Science:
   *The Water Cycle*), each with hardcoded ~350-450 word textbook-style reading content
   and a matching 5-question set.
2. **Timer setup screen** -- lets a parent/teacher/instructor set the reading time
   (default 5 minutes) before the student starts reading.
3. **Reading screen** -- shows the chapter text with a live countdown timer at the top.
   **Quiz-unlock rule used in this prototype: the "Start Quiz" button unlocks when EITHER
   the timer reaches zero OR the student scrolls to the bottom of the text, whichever
   happens first.**
4. **Quiz screen** -- gamified question sequence per chapter:
   - Question 1 is always an open-ended Socratic/leading question. The student types a
     free-text answer, then sees a model answer/explanation and self-assesses with
     "Not quite / Sort of / Got it!" (no heart lost either way).
   - Remaining questions mix multiple-choice and true/false, increasing in difficulty,
     each with instant correct/incorrect feedback and an explanation tied back to the text.
   - Game mechanics: 3 hearts (lose one per wrong MC/TF answer; quiz ends early with an
     encouraging "Try Again" screen if hearts hit 0), XP per correct answer (10 XP, +5 XP
     streak bonus once a streak reaches 3 in a row), hearts row + XP badge + streak badge
     + progress bar always visible during the quiz.
5. **Results screen** -- final score, XP earned, best streak, an encouraging message
   tuned to performance, and a "Review missed questions" expandable list with explanations.

## Tech

- Kotlin + Jetpack Compose + Material3, single module, min SDK 24, target/compile SDK 34.
- Navigation via `androidx.navigation:navigation-compose`.
- All chapter/question content is hardcoded in-memory in
  `app/src/main/java/com/kivaisi/selflearn/data/ContentRepository.kt` as plain Kotlin
  data classes (`Chapter`, `Question`). No backend, no database. This is intentionally
  organized so a later version can swap in AI-generated `Question` lists from a chapter's
  text without touching the UI or quiz engine.
- Debug build is scoped to `arm64-v8a` only to keep the sideload APK small.

## Build (headless, no Android Studio)

```
export JAVA_HOME=<path to JDK 17>
export ANDROID_HOME=<path to Android SDK>
export PATH=$JAVA_HOME/bin:<path to gradle 8.9>/bin:$PATH
./gradlew assembleDebug   # or: gradle assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

## Design choices not fully specified in the original brief

- **Quiz-unlock gating rule**: timer-reaches-zero OR scroll-to-bottom (whichever first),
  not an AND of both.
- XP: 10 per correct answer, +5 bonus once a streak of 3+ correct in a row is active.
- Self-assessed Socratic question never costs a heart; "Got it!" counts toward streak/XP,
  "Not quite"/"Sort of" resets the streak but doesn't cost a heart.
- Icons are Compose Material vector icons (Terrain/Science/Favorite/Bolt/etc.), no emoji.
- Adaptive app icon: a simple purple background with a yellow open-book glyph.
