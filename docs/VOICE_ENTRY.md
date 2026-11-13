# Voice entry behavior and validation

Voice entry uses the Android device's installed speech recognition service. The app parses the transcript returned by that service and shows a confirmation form; no transaction is saved until the user confirms it.

## Languages and amounts

The manifest declares visibility of `android.speech.RecognitionService` so Android 11+ can discover installed recognition providers. This does not install a provider or grant microphone permission.

The language selector requests English (`en-US`), Urdu (`ur-PK`), or Punjabi (`pa-PK`). Availability, offline support, and transcription quality depend on the installed speech service and its language models. A provider that supports Punjabi only as `pa-IN` may reject the current Punjabi request. The app reports unsupported or unavailable language errors and offers manual entry; it does not silently select a different language.

The parser accepts Western, Arabic-Indic, Persian/Urdu, and Gurmukhi digits. It recognizes common English, Urdu, and Punjabi number words and scales such as hundred, thousand, lakh, and crore. Roman Urdu/Punjabi number words are enabled when the corresponding language is selected. This is a bounded parser, not unrestricted natural-language understanding: dialects, spelling variants, fractions, spoken decimal phrases, and every possible number-word construction are not guaranteed.

Examples covered by regression tests:

| Transcript | Amount | Description |
| --- | --- | --- |
| `shopping 50000` | 50,000.00 | `shopping` |
| `shopping fifty thousand rupees` | 50,000.00 | `shopping` |
| `شاپنگ ۵۰٬۰۰۰ روپے` | 50,000.00 | `شاپنگ` |
| `شاپنگ پچاس ہزار روپے` | 50,000.00 | `شاپنگ` |
| `ਖਰੀਦਦਾਰੀ ੫੦੦੦੦ ਰੁਪਏ` | 50,000.00 | `ਖਰੀਦਦਾਰੀ` |
| `shopping 125.50` | 125.50 | `shopping` |

The recognized monetary phrase and adjacent currency word are removed from the description. Money is converted with exact decimal arithmetic; negative, overflowing, or sub-paisa amounts are rejected.

## Repeated speech and manual edits

Each new utterance updates only the fields it supplies:

- Say `shopping`, then `50000`: the amount is filled and the description/category remain.
- Say `50000`, then `shopping`: the description/category are filled and the amount remains.
- Say `shopping 50000`, then `45000`: the amount is corrected and the description remains.
- Amount-only speech preserves an earlier income/expense selection and relative date.
- Manually edited amount, description, type, and category remain authoritative. Clearing an amount or description lets later speech fill that field again.
- Category changes are limited to the selected transaction type. An unrecognized category leaves the previous compatible selection in place.

The form stays visible during another recognition attempt. Save is unavailable until recognition finishes, preventing a transaction from being saved while a result is still arriving. A failed save keeps the form available and displays an error.

## Ambiguity and dates

Multiple unqualified amounts, such as `500 600` or `five shirts and ten shoes`, do not produce an amount. A single currency-qualified amount can be distinguished from a quantity: `2 shirts 500 rupees` proposes 500.00 and preserves `2 shirts` as the description. Always review the proposal; category matching and transcript interpretation can still be wrong.

Supported relative dates include `today`, `yesterday`, `day before yesterday`, and the supported Urdu/Punjabi equivalents in the parser. The ambiguous Urdu word `پرسوں` follows the existing interpretation of two days ago. A date-only relative utterance changes the date without replacing the description.

Calendar-date patterns such as `13 November 2026`, `2026-11-13`, and `13/11` are excluded from amount extraction. The form blocks saving these entries and directs the user to manual entry to choose a date; it must not silently save them as today. Manual entry opens a separate form, so review and enter the transaction details there. Arbitrary date wording is not supported. When an absolute date was recognized accidentally, a subsequent supported relative date, such as `today`, explicitly replaces that date requirement.

## Automated coverage

`VoiceTransactionParserTest` and `VoiceEntryRegressionTest` exercise parsing and the draft reducer. Coverage includes separate/combined utterances, supported scripts and common number words, exact paisa, manual edit retention, income/date retention, ambiguity, overflow, Unicode description offsets, and preventing calendar-date digits from becoming amounts.

Run with the repository's configured JDK/Android SDK:

```sh
bash gradlew :app:testDebugUnitTest --tests '*VoiceTransactionParserTest' --tests '*VoiceEntryRegressionTest'
```

These are code-level tests with supplied transcripts. They do not test a microphone, a live speech service, or device language-model availability. Consult the current test report for actual pass/fail results.

## Physical-device checks still required

Use a test transaction and delete it afterward. These cases are a checklist, not a claim that live testing has occurred.

1. Grant microphone permission; then repeat with permission denied. Confirm an actionable error and usable manual entry.
2. In each available voice language, say the description first, then the amount. Reverse the order and repeat using one combined phrase. Verify amount, description, category, and type before saving.
3. Speak an amount with paisa; check the actual transcript and confirm no fractional value is lost.
4. Manually edit each proposed field, speak again, and confirm the edited values stay intact. Clear amount/description and confirm speech can refill them.
5. Try a language the speech service does not support or has not downloaded. Verify the language-specific guidance, and verify another supported language/manual entry remains usable.
6. Check offline/network failure, silence, background noise, and repeated microphone taps. Verify the app recovers and cannot save while processing an unfinished result.
7. Dictate an income, then an amount-only correction. Confirm its type and category are retained.
8. Dictate a calendar date. Verify it is not used as money and the form directs the user to manual entry for the date.
9. Confirm a valid transaction once and verify exactly one matching record appears in the transaction list and totals. Reopen voice entry and verify the new draft starts empty.
10. Close the dialog during speech and reopen it. Verify the previous recognizer does not change the new draft or save a transaction.
