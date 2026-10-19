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

## Session-varied question sampling (v1.2+)

Each chapter's `questions` list is now a POOL, not a fixed script: every underlying
concept ("dimension") has 2 authored question variants. `QuestionSessionSelector`
builds each new quiz session by:

1. Grouping the pool by `dimension` -- guarantees every session still covers the
   same breadth of concepts (one question per dimension), not a random subset.
2. Picking, per dimension, a variant that was NOT used in the immediately previous
   session for that chapter (tracked in-memory by `SessionHistory`), falling back to
   any variant if everything in that dimension has already been seen.
3. Keeping the Socratic opening question first and sorting the rest by ascending
   difficulty, preserving the existing "leading question first, then harder" design.

This is deliberately separate from "Review missed questions" on the results screen,
which intentionally re-shows the exact question + explanation the student got wrong
(remediation of a specific mistake), not a resampled variant.

`SessionHistory` is in-memory only (resets if the app process is killed) -- swap it
for DataStore/Room if session history needs to survive app restarts. See the project
conversation history for the underlying best-practice references (table of
specifications / Bloom's taxonomy tagging, spaced-repetition exposure control,
testing-effect research, and automatic question generation surveys) that motivated
this design.

## Swahili Dictionary (v1.8+)

The in-app "Swahili Dictionary" is a **standalone, chapter-independent, and
MONOLINGUAL** reference dictionary -- Swahili words are defined IN SWAHILI
(the way Oxford/Cambridge define English words in English), not just
translated to their English equivalent. It is backed by a bundled dataset of
**16,156 unique Swahili headwords**, extracted from the Swahili-language
Wiktionary (sw.wiktionary.org). A bonus English translation is attached to
~1,350 entries where a separate bilingual source also had that word, but
most entries intentionally only have the Swahili definition. See
`THIRD_PARTY_NOTICES.md` for the full attribution and license
(CC BY-SA 4.0 + GFDL for the Wiktionary content; GPLv2+ for the bonus
English translations).

It is reachable identically from the Home screen ("Kamusi" button), the Reading
screen (top bar), and the Quiz screen (header) -- opening the exact same full
dictionary every time, not a per-chapter word list. It opens as a dialog
overlay rather than a navigation route, so opening/closing it does not
interrupt the reading countdown timer or reset in-progress quiz state (hearts,
XP, streak).

The dictionary data is parsed once from `assets/swahili_dictionary.json`
(`data/SwahiliDictionaryRepository.kt`) and cached in memory; the search box
filters the full ~16,000-word list by headword, Swahili definition, or bonus
English translation as you type, debounced ~150ms to stay responsive.

**Search UX (v1.9+):** search results show a plain tappable list of matching
headwords only (word + part of speech), modeled on how the Oxford Dictionary
app's search works. Tapping a word navigates to a dedicated detail screen
with the full Swahili definition and bonus English translation, rather than
expanding every result inline. Both screens live inside the same dialog
overlay; the back arrow on the detail screen returns to the search list
(preserving your search text), and the back arrow on the search screen
closes the dictionary.

**v1.0-v1.5 note:** earlier releases had a small, hand-authored, per-chapter
glossary of ~10 words tied to each reading chapter's vocabulary. That was
replaced in v1.6 with a standalone dictionary after feedback that the
dictionary should be a general reference tool, not scoped to lesson content.

**v1.6-v1.7 note:** those releases used a bilingual Swahili-English
*translation* dictionary (FreeDict, 2,096 words) as the primary data source --
so every word was only defined via its English equivalent, not explained in
Swahili. v1.8 replaced that with the monolingual Wiktionary-derived dataset
above, after feedback that a dictionary "like Oxford/Cambridge" should define
words in their own language, and that the word count was too small.

**Data quality note:** this is real third-party dictionary data (not
AI-generated or hand-guessed), extracted from a volunteer-edited wiki, not an
institutionally reviewed dictionary like Oxford/Cambridge. Coverage is strong
for everyday vocabulary but noticeably thinner for specialized/technical terms
(e.g. some geography/science vocabulary used in this app's own chapters is
still not covered). Treat it as a solid general-purpose reference rather than
an authoritative source for formal writing.
