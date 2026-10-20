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

**Example sentences (v2.2+, revised in v2.3 and v2.4):** every entry also
has an example sentence, flagged as either real or auto-generated (see
"Example sentences" in `THIRD_PARTY_NOTICES.md` for full detail):
- **9,676 entries (59.9%)** have a REAL example sentence, pulled from the
  Tatoeba open sentence corpus (CC BY 2.0 FR), sw.wiktionary.org's own
  quotation lines, or Swahili Wikipedia (sw.wikipedia.org, CC BY-SA 4.0),
  length- and topic-screened for a student audience.
- **6,480 entries (40.1%)** have a TEMPLATE-GENERATED SCENARIO sentence
  (e.g. `Mwalimu alitumia neno "{headword}" wakati wa somo la Kiswahili
  leo.` -- "The teacher used the word '{headword}' during today's
  Swahili lesson") placing the word in a plausible classroom/reading
  context, since no free Swahili example-sentence corpus covers every
  headword. Clearly labeled in the Detail screen as auto-generated, with
  a small disclaimer, and must never be mistaken for authentic native
  usage.
- **v2.2 note:** the first generated-fallback version just restated the
  definition as a sentence (e.g. "Neno X linamaanisha: ..."), which
  wasn't a real usage example at all -- v2.3 replaced that wording with
  the scenario-style templates above, and separately added Swahili
  Wikipedia as a third real-sentence source.
- **v2.4 note:** a post-build QA pass caught 138 spurious "real" matches
  from Wikipedia -- 5 were bound-morpheme headwords (e.g. "-kubwa") that
  can never appear as standalone words in real text, so the match was an
  accidental punctuation artifact; 134 still had leftover MediaWiki
  markup fragments. Both were reverted to the generated fallback instead
  of being shown as real sentences.

A full backup of the word/definition/example/translation data is kept as
`swahili_dictionary_backup.csv` and `.xlsx` in the repository root, for
reference or re-import outside the app.

It is reachable identically from the Home screen ("Kamusi" button), the Reading
screen (top bar), and the Quiz screen (header) -- opening the exact same full
dictionary every time, not a per-chapter word list. It opens as a dialog
overlay rather than a navigation route, so opening/closing it does not
interrupt the reading countdown timer or reset in-progress quiz state (hearts,
XP, streak).

The dictionary data is served from a pre-built, pre-indexed **SQLite
database** bundled as `assets/swahili_dictionary.db`
(`data/SwahiliDictionaryRepository.kt`), not an in-memory JSON-parsed list
(see "Dictionary architecture (v2.0)" below).

**Search UX (v1.9+):** search results show a plain tappable list of matching
headwords only (word + part of speech), modeled on how the Oxford Dictionary
app's search works. Tapping a word navigates to a dedicated detail screen
with the full Swahili definition and bonus English translation, rather than
expanding every result inline. Both screens live inside the same dialog
overlay; the back arrow on the detail screen returns to the search list
(preserving your search text), and the back arrow on the search screen
closes the dictionary.

**Dictionary architecture (v2.0):** v1.6-v1.9 loaded the full dictionary by
parsing a 2MB JSON file into ~16,000 Kotlin objects synchronously on the UI
thread the first time the dialog opened, which visibly froze the app for a
moment, and ranked search matches by raw list order with no relevance
tiering or result cap, so an exact headword match could get buried under
loosely-related substring hits inside long definition text. v2.0 replaced
this with a pre-built, pre-indexed SQLite database
(`assets/swahili_dictionary.db`):
- Opening the dictionary never loads the whole dataset into memory --
  it queries only the rows actually needed (a 60-row preview, or up to 50
  ranked search results), and every query runs on a background thread
  (`Dispatchers.IO`) with a loading spinner shown while in flight.
- The Home screen warms up the database connection in the background as
  soon as it appears (`SwahiliDictionaryRepository.warmUp`), so by the time
  a student taps "Kamusi" the connection is usually already open.
- Search results are now ranked in four priority tiers -- exact headword
  match, headword starts-with, headword contains, then
  definition/English-contains last -- so the exact word you typed always
  sorts to the top, capped at 50 results per query.
- Plain indexed columns + tiered `LIKE` queries were used instead of
  SQLite's FTS5 full-text search extension, specifically for guaranteed
  compatibility down to minSdk 24 (FTS5 availability depends on the
  device's bundled SQLite build and isn't guaranteed on all Android
  versions/vendors); at ~16,000 rows this is comfortably fast either way.

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
