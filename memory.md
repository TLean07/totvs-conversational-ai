# Memory — ChallengeTotvs

## Overview
<!-- Describe the project scope and goals here -->

## Architecture decisions
<!-- Key decisions made during development -->

## Current state
<!-- What has been done, what is in progress, what is next -->

## JSON transcription multi-record import

### Problem

The previous TOTVS JSON importer intentionally aggregated every JSON object into one `Transcricao` document. This mixed independent meetings and made listing, history, filters, dashboard metrics, and per-meeting analysis incorrect.

### Supported formats

- A root array of meeting objects.
- A root object containing a meeting list under `transcricoes` or any other key.
- A single meeting object.
- Nested objects containing arrays of meeting objects.
- Multiple top-level JSON objects separated by whitespace/newlines, as used by `ANON_transcricao.json`.

The parser treats a meeting object as one transcription. It never splits the text by speaker or by individual speech.

### Detection and mapping

`TotvsTranscriptionJsonParser` reads the upload as a stream. Root arrays are consumed item by item, which avoids loading the complete 48 MB sample into memory. Nested arrays are detected when their objects contain `ANON_TRANSCRICAO`.

Each valid item maps:

- `ID_MEETING` to `meetingId`.
- `DT_MEETING` to `meetingDate` and, when valid, `dataReuniao`.
- `FORMATO_MEETING` to `format`.
- `STATUS_MEETING` to `meetingStatus` (the existing `status` remains the application processing status).
- `DURACAO_MEETING` to `duration` and `duracaoMinutos`.
- `CODT` to `codt`.
- `FLG_EXTERNO` to `external`.
- `DT_CRIACAO` to `createdAtSource`.
- `ANON_TRANSCRICAO` to `transcriptionText` and the existing `textoOriginal` field.
- Upload metadata to `sourceFileName`, `importBatchId`, and `importedAt`.

Missing `ID_MEETING` values receive a `generated-<uuid>` identifier and set `missingMeetingId=true`. Invalid source dates are preserved as strings, reported as item warnings, and do not stop the batch.

### Duplicate rule and result

`meetingId` has a sparse unique MongoDB index. The importer also checks the repository and IDs already seen in the same file before saving. Existing IDs are skipped by default.

The endpoint keeps the existing route and multipart upload flow. Its response now includes `success`, `sourceFile`, `batchId`, `found`, `imported`, `skipped`, `failed`, and one status item per detected meeting. Legacy counters remain in the response for compatibility.

### Verification

Automated tests cover a single object, a three-item root array, a list under an arbitrary root key, a database duplicate, and an invalid item mixed with valid items.

The real `ANON_transcricao.json` file was tested through the streaming importer on June 15, 2026:

- File size: 48,441,117 bytes.
- Meetings detected: 1,174.
- Unique `ID_MEETING` values imported into an empty mocked repository: 1,126.
- Duplicate occurrences skipped within the file: 48.
- Invalid/empty `ANON_TRANSCRICAO` items: 0.

### Limitations

- Duplicate handling defaults to skip; overwrite mode is not implemented.
- The detailed response contains one item per meeting, so very large imports produce a correspondingly large response.
- Existing MongoDB documents remain readable because all new fields are additive. Automatic index creation must be allowed so the sparse unique `meetingId` index is created.
