# Third-Party Dictionary Data

The standalone Swahili dictionary bundled in this app
(`app/src/main/assets/swahili_dictionary.db`, a pre-built SQLite database --
see README.md "Dictionary architecture") is **monolingual** -- each Swahili
headword is defined IN SWAHILI (e.g. "maji" -> "kioevu kisicho na rangi wala
harufu kinachopatikana kiasili"), the same way Oxford or Cambridge define
English words in English, rather than only giving an English translation.

## Primary source (definitions)

Extracted from the **Swahili-language Wiktionary** (sw.wiktionary.org,
"Wikamusi"), using the `swwiktionary-latest-pages-articles.xml.bz2` database
dump (dumps.wikimedia.org). The extraction parses each article's `==
{{sw}} ==` / `==Kiswahili==` section and pulls the first one or two numbered
glosses, cleaning MediaWiki markup (templates, links, bold/italic markers).

- Pages scanned: 107,157 (namespace 0 / main content pages)
- Pages with a Swahili-language definition section: 17,920
- Final cleaned, de-duplicated monolingual entries: **16,156**

Project homepage: https://sw.wiktionary.org/
Dump source: https://dumps.wikimedia.org/swwiktionary/

## Secondary source (bonus English translations only)

For entries where a separate bilingual dictionary also had the same
headword, an optional English translation is attached as a bonus
(`en` field) -- most entries do NOT have this field populated, and that is
expected, not a bug, since the point of this dictionary is the Swahili
definition. The bilingual source for this bonus field is the FreeDict
project's merged Swahili-English (2,675 headwords) and English-Swahili
(1,450 headwords) dictionaries:
https://github.com/freedict/fd-dictionaries (`swh-eng/` and `eng-swh/`),
https://freedict.org/.

## Example sentences

Every entry has an `example` field, but it comes from one of two very
different places, flagged by `example_is_generated` (shown in-app as
"SENTENSI HALISI" / real sentence, vs "SENTENSI YA MFANO -- IMETUNGWA" /
auto-generated example):

- **Real usage (2,349 entries, 14.5%):** pulled from the **Tatoeba**
  open sentence corpus (4,583 Swahili sentences, CC BY 2.0 FR,
  https://tatoeba.org/, dump via https://downloads.tatoeba.org/) by
  matching whole words against each headword, plus 86 entries that had
  their own native example/quotation line (`#:`) directly in the
  sw.wiktionary.org source (same license as the definitions above).
- **Generated placeholders (13,807 entries, 85.5%):** no free,
  dictionary-scale Swahili example-sentence corpus exists, so the
  remaining entries get a template-filled sentence built from the
  headword and its own definition (e.g. `Neno "{headword}" linamaanisha:
  {definition}.`), produced by a one-off Python script, NOT by a live AI
  model call. These are clearly labeled in the app as auto-generated and
  must never be presented as authentic native usage.

## License

- **Wiktionary content** (the primary monolingual Swahili definitions,
  and the 86 native example quotations) is dual-licensed under
  **Creative Commons Attribution-ShareAlike 4.0** (CC BY-SA 4.0) and the
  **GNU Free Documentation License** (GFDL), per the Wikimedia
  Foundation's standard terms for all Wiktionary editions.
- **Tatoeba content** (2,263 of the real example sentences) is
  distributed under **Creative Commons Attribution 2.0 France**
  (CC BY 2.0 FR), per https://tatoeba.org/en/downloads.
- **FreeDict content** (the bonus English translations) is distributed
  under the **GNU General Public License, version 2.0 or later** (GPLv2+).
  The full license text is in `THIRD_PARTY_DICTIONARY_LICENSE.txt` in the
  root of this repository; the Wiktionary CC BY-SA 4.0 text is in
  `THIRD_PARTY_WIKTIONARY_LICENSE.txt`.
- The ~85.5% of example sentences that are template-generated are NOT
  third-party content -- they're produced by this project's own build
  script from the (third-party) headword + definition, and carry no
  separate license obligation beyond the data they're built from.

Per FSF guidance on "mere aggregation," bundling this data alongside
non-GPL/non-CC application code does not place the application code itself
under those licenses, since the dictionary is plain data (no executable
code) distributed unmodified in format alongside the app rather than linked
into it. If you redistribute `swahili_dictionary.db` itself, the
Wiktionary-derived content must stay under CC BY-SA 4.0 / GFDL, the Tatoeba
sentences under CC BY 2.0 FR, and the FreeDict-derived bonus translations
under GPLv2+, with this attribution preserved.

## Data quality note

This is real, independently compiled dictionary data (not AI-generated or
hand-guessed by this project) for definitions and the 14.5% of real example
sentences, extracted from a volunteer-edited wiki and an open sentence
corpus. It is NOT an institutionally reviewed dictionary like Oxford or
Cambridge -- coverage is strong for everyday vocabulary but noticeably
thinner for specialized/technical terms (e.g. specific geography/science
vocabulary used in this app's reading chapters may not be covered). Some
entries may be informal, incomplete, or imperfectly glossed. The 85.5% of
example sentences that are auto-generated are placeholders meant to
illustrate the word in a sentence frame, not real native usage -- treat the
whole dictionary as a solid general-purpose reference, not an authoritative
source for formal writing.
