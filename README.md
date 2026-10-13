# AttendX

An Android attendance tracker for students. Add your subjects, tap **Mark Present** or **Mark Absent** after each class, and AttendX warns you before any subject dips under your target (default 75%). Built for the exact problem college students live with — attendance rules.

## Features

- Add / edit / delete subjects (name, short code, classes per week, target %)
- One-tap **Mark Present** / **Mark Absent** counters per subject
- Live attendance percentage with color-coded status:
  - 🟢 **SAFE** — at or above target + 5%
  - 🟡 **AT RISK** — within 5% of the target
  - 🔴 **BELOW TARGET** — under the target
- "You can still miss N more classes" line — computed from the math, not a guess
- Fully offline: data stored locally with Room
- Material3 DayNight theme (follows system light/dark mode)

## How to run

This project was written on a machine without the Android SDK, so it has **not** been compiled or device-tested here. To run it:

1. Open Android Studio and choose **Open**, then select this project folder.
2. Let Gradle sync (it downloads the Kotlin/Room/Material3 dependencies automatically).
3. Press **Run** (▶) — use an emulator (AVD) or a physical device with USB debugging enabled.

## Tech stack

- Kotlin 1.9.22
- Room 2.6.1 (persistence)
- Material3 components, ViewBinding, RecyclerView
- Kotlin Coroutines + Flow for reactive UI updates
- minSdk 24, targetSdk 34

## Project structure

```
app/src/main/java/com/attendx/app/
├── data/
│   ├── Subject.kt        # Room entity (id, name, code, classesPerWeek,
│   │                     #   targetPercent, presentCount, absentCount)
│   ├── SubjectDao.kt     # Flow<List<Subject>> getAll + CRUD suspend fns
│   └── AppDatabase.kt    # Room database ("attendx_db")
├── logic/
│   └── AttendanceCalculator.kt  # Pure-Kotlin % / status / can-miss math
└── ui/
    ├── MainActivity.kt               # Subject list + FAB
    ├── SubjectAdapter.kt             # RecyclerView rows with status colors
    ├── AddEditSubjectDialogFragment.kt  # Add/edit form dialog
    └── SubjectDetailActivity.kt      # Stats, present/absent buttons,
                                     #   progress bar, edit/delete menu
app/src/main/res/
├── layout/   activity_main, activity_subject_detail, item_subject,
│             dialog_add_subject
├── menu/     menu_subject_detail (Edit / Delete)
└── values/   strings, colors (safe/at-risk/below), themes (M3 DayNight)
```

## APK download

The repo builds a debug APK automatically on every push via a GitHub Actions workflow at `.github/workflows/build-apk.yml`. That workflow file is added separately through the GitHub web UI (it cannot be committed here). Once it has run, the APK is published to a rolling release:

```
https://github.com/entity-coder/2026-10-13/releases/download/apk-latest/AttendX-v1.0.apk
```

(Android will show an "unknown developer" warning on install — that's normal for a debug-signed APK.)

## Interview notes

**What the project is:** AttendX is an offline Android attendance tracker. A student adds subjects with a target percentage, then marks present/absent each day; the app shows each subject's live percentage with a SAFE / AT RISK / BELOW TARGET status and tells them how many more classes they can afford to miss before dropping under target.

**Why Room over SharedPreferences/JSON file:** The data is relational (a list of subjects, each with several fields, updated frequently), and Room gives typed queries, compile-time SQL validation, and — most importantly — a `Flow<List<Subject>>` from the DAO, so the list and detail screens update reactively with zero manual refresh logic. SharedPreferences would need hand-rolled serialization and no observable stream.

**Why the kapt version is declared explicitly:** Applying `org.jetbrains.kotlin.kapt` without a version in the plugins block silently breaks the GitHub Actions build (the Room annotation processor then can't resolve its Kotlin version). Declaring `version "1.9.22"` alongside the Kotlin plugin keeps the processor and compiler in lockstep, which we learned the hard way on an earlier project.

**Trade-offs / limitations:**
- Attendance is two counters per subject, not per-day records — simpler code and DB, but no date history (can't answer "what was my attendance on Oct 3?").
- No sync or cloud — fully offline-first, data stays on the device; uninstalling wipes it.
- The "can miss N classes" figure assumes future classes are all attended or all missed; it doesn't account for planned leaves or timetable changes.

**How I'd extend it:** Per-date attendance records (calendar view + streaks), weekly timetable with class-time reminder notifications, attendance trend charts per subject, CSV export of the records, and per-subject targets per class type (theory vs. lab).
