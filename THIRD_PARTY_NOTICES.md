# Third-Party Dictionary Data

The standalone Swahili<->English dictionary bundled in this app
(`app/src/main/assets/swahili_dictionary.json`) was built by merging and
deduplicating two dictionaries from the FreeDict project:

- **Swahili-English**, version 0.4.4, 2,675 headwords
  compiled by Beata Wojtowicz, corrected by Beata Wojtowicz and Piotr Banski.
- **English-Swahili**, version 0.2.2, 1,450 headwords
  (same FreeDict project).

Source: https://github.com/freedict/fd-dictionaries (`swh-eng/` and `eng-swh/`)
Project homepage: https://freedict.org/

After merging both directions and de-duplicating headwords, the bundled
`swahili_dictionary.json` contains 2,096 unique Swahili headwords with
English meanings and part-of-speech tags.

## License

The FreeDict dictionary data is distributed under the
**GNU General Public License, version 2.0 or later** (GPLv2+). The full
license text is in `THIRD_PARTY_DICTIONARY_LICENSE.txt` in the root of this
repository. Per FSF guidance on "mere aggregation," bundling this GPL data
file alongside non-GPL application code does not place the application code
itself under the GPL, since the dictionary is plain data (no executable
code) distributed unmodified in format alongside the app rather than linked
into it. If you redistribute `swahili_dictionary.json` itself, it must stay
under GPLv2+ terms with this attribution preserved.

## Data quality note

This is real, independently compiled dictionary data (not AI-generated or
hand-guessed by this project), but it is a community-maintained resource,
not an institutionally reviewed dictionary like Oxford or Cambridge. Some
entries may be dated, informal, or imperfectly glossed. Treat it as a solid
general-purpose reference, not an authoritative source for formal writing.
