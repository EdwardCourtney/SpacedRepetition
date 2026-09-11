# Spaced Repetition

A personal JavaFX desktop app for tracking study errands and reviewing them on spaced intervals.

## Features

- Add a study errand with `Subject` and `Lesson`.
- Automatically schedules the first review for today.
- Review flow:
  - `Done` marks the task completed and schedules the next interval.
  - `Delay` moves today's task to tomorrow without increasing the repeat count.
  - `Undo` moves a completed task back to today's work.
  - `Delete` removes the task after confirmation.
- `Today's Work` shows tasks due today.
- `Completed` shows only tasks completed today.
- `All Errand` shows every saved task sorted by next review date.
- Duplicate prevention by `Subject + Lesson`.
- A small note area is saved between app launches.
- Local SQLite storage with automatic backups.

## Review Schedule

When you click `Done`, the next review is scheduled from that day:

```text
1st completion -> 3 days later
2nd completion -> 7 days later
3rd completion -> 15 days later
4th completion -> 30 days later
5th completion -> 45 days later
Then +15 more days each time
```

If a task is missed, the app moves it forward when you next open the app so it appears in `Today's Work`.

## Data Storage

The app stores data locally in SQLite.

On Windows:

```text
%LOCALAPPDATA%\SpacedRepetition\data\spaced_repetition.db
```

Backups are kept here:

```text
%LOCALAPPDATA%\SpacedRepetition\backups
```

The app keeps the latest 10 database backups.

## Run From IntelliJ

Open the project in IntelliJ IDEA and run:

```text
com.morrello.spacedrepetition.App
```

The project uses Java 25 and JavaFX 25.

## Build Windows App

Use the included packaging script:

```powershell
powershell -ExecutionPolicy Bypass -File .\package-windows.ps1
```

The packaged app is created at:

```text
dist\SpacedRepetition\SpacedRepetition.exe
```

Keep the whole `dist\SpacedRepetition` folder together. The `.exe` depends on the bundled `app` and `runtime` folders.

## Requirements

- JDK 25 with `jpackage`
- IntelliJ IDEA bundled Maven, or Maven on PATH

The packaging script automatically uses IntelliJ's bundled Maven if normal `mvn` is not available.

## Notes

- `data/` is ignored by Git so local study data is not committed.
- This is a local-first personal app. It does not sync across computers.
