# Sandbag 5x5

Brutally simple local-first Android workout tracker for a **fixed 70 lb sandbag**.

Inspired by the "open app → do the work → close app" philosophy of StrongLifts 5x5. No accounts. No cloud. No ads. No bullshit.

## Core Idea

- Two alternating workouts: **A** and **B**
- Fixed sandbag weight (70 lb) — progression happens via reps, sets, and rest times
- Large tappable targets. Complete a set → rest timer → repeat
- At the end of the session you answer one question: **"How did that feel?"**
  - **Easy** → +1 rep per set (or +rounds for carries) + reduce rest by ~10s
  - **Solid** → same targets next time (build consistency)
  - **Brutal** → same targets next time
  - **Failed** → drop 1 rep per set (or 1 set)

The app automatically alternates A/B and persists your current targets + full history locally with Room.

## Workouts

### Workout A
- Sandbag clean to shoulder — 5×3 each side
- Bear hug squat — 5×5
- Sandbag floor press / push-up — 5×5-10
- Bent-over sandbag row — 5×8
- Bear hug carry — 3 rounds

### Workout B
- Sandbag shouldering — 10 minutes alternating sides (timed)
- Front-loaded reverse lunge — 3×5 each leg
- Ground-to-over-shoulder — 5×5
- Sandbag pull-through / plank drag — 3×10
- Carry finisher — 5 minutes (timed)

## Tech

- 100% Kotlin
- Jetpack Compose + Material 3
- Room for local persistence (sessions, set logs, live targets)
- No external dependencies beyond AndroidX + coroutines
- Single Activity + Compose Navigation

## Project Structure (key parts)

```
app/
├── src/main/java/com/sandbagstrong/
│   ├── MainActivity.kt
│   ├── SandbagApplication.kt
│   ├── data/
│   │   ├── WorkoutRepository.kt
│   │   └── db/ (Entities, Daos, AppDatabase with seeder)
│   ├── domain/ (WorkoutType, Exercise, Program, FeelRating)
│   ├── ui/
│   │   ├── WorkoutViewModel.kt
│   │   ├── components/ (BigButton, ExerciseCard, RestTimerDialog)
│   │   └── screens/ (Home, Workout, History, FeelRating)
│   └── ui/theme/
├── build.gradle.kts
└── ...
```

## Build & Run

### Recommended (Android Studio)

1. Clone / copy the `Sandbag5x5` folder.
2. Open the project in **Android Studio** (Ladybug / newer recommended).
3. Let Gradle sync (it will download the wrapper + dependencies the first time).
4. Select a device or emulator (API 26+).
5. Run ▶️ `app`.

### Command line (after first Studio sync or with gradle installed)

**Recommended on this machine (especially arm64 / Jetson-style boards):**

```bash
cd /home/doogie/Sandbag5x5
./build.sh                # Normal build (auto-sets env + QEMU if needed)
./build.sh installDebug   # After a successful build, install to device/emulator
```

The `build.sh` helper does the following automatically:
- Detects aarch64 and enables QEMU user emulation (required for aapt2 and other x86_64 tools)
- Sets `ANDROID_HOME` + `PATH` if they look reasonable
- Stops the Gradle daemon and clears the transforms cache before "clean" builds (this fixes the recurring `aar-metadata.properties` / navigation errors)
- Passes any extra arguments straight through to the real `./gradlew`

Other useful modes:
```bash
./build.sh clean          # Safe clean + build (recommended after cache problems)
./build.sh fullclean      # Nuclear option: also refreshes dependencies
./build.sh setup          # Just ensure QEMU + print SDK reminders
```

You can still use raw gradle if you prefer:
```bash
./gradlew assembleDebug
```

The first build will take a while (downloads Gradle + Compose + AndroidX artifacts).

### Common Build Error: "SDK location not found"

If you see this:

```
Could not determine the dependencies of task ':app:compileDebugJavaWithJavac'.
> SDK location not found. Define a valid SDK location with an ANDROID_HOME
  environment variable or by setting the sdk.dir path in your project's
  local.properties file...
```

**Fix (Linux):**

1. Install the Android SDK (easiest way = Android Studio).

   - Download Android Studio from https://developer.android.com/studio
   - Or on many Linux distros:
     ```bash
     sudo snap install android-studio --classic
     ```
   - Launch Android Studio → it will download the SDK for you.

2. Find your SDK path (common locations):
   ```bash
   echo "$HOME/Android/Sdk"
   ls "$HOME/Android/Sdk" 2>/dev/null || echo "Not there yet"
   ```

3. Tell the project where the SDK is.

   **Easiest:** Run the included helper (from the project root):

   ```bash
   cd /home/doogie/Sandbag5x5
   ./setup-sdk.sh
   ```

   Or manually:

   ```bash
   cd /home/doogie/Sandbag5x5
   echo "sdk.dir=$HOME/Android/Sdk" > local.properties
   ```

   (Adjust the path if your SDK is somewhere else, e.g. `/opt/android-sdk` or a custom location.)

4. Install the command-line tools + required components.

   Full one-time setup (copy-paste this block):

   ```bash
   # Prerequisites
   sudo apt update
   sudo apt install -y wget unzip

   # Create SDK location and download official command line tools
   mkdir -p ~/Android/Sdk/cmdline-tools
   cd /tmp
   wget -O commandlinetools.zip https://dl.google.com/android/repository/commandlinetools-linux-14742923_latest.zip

   # Extract with the special directory layout required by the tools
   unzip -q commandlinetools.zip -d ~/Android/Sdk/cmdline-tools
   mv ~/Android/Sdk/cmdline-tools/cmdline-tools ~/Android/Sdk/cmdline-tools/latest

   # Set environment variables for this shell session
   export ANDROID_HOME="$HOME/Android/Sdk"
   export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"

   # Accept licenses and install exactly what this project needs
   yes | sdkmanager --licenses
   yes | sdkmanager \
     "platforms;android-34" \
     "build-tools;34.0.0" \
     "platform-tools" \
     "cmdline-tools;latest"

   echo "=== Android SDK ready at $ANDROID_HOME ==="
   ```

   (If the download link above is ever stale, go to https://developer.android.com/studio and look for the "Command line tools only" → Linux download link.)

5. Now point the project at the SDK (you can run the helper or do it manually):

   ```bash
   cd /home/doogie/Sandbag5x5
   ./setup-sdk.sh     # press Enter to accept the default
   # or manually:
   # echo "sdk.dir=$HOME/Android/Sdk" > local.properties
   ```

6. Build:

   ```bash
   ./gradlew clean assembleDebug
   ```

   The first build downloads Gradle + lots of dependencies and can take a long time (10-30+ minutes). Subsequent builds are much faster.

If you open the project in Android Studio it will usually detect the missing pieces and offer to install them automatically (including accepting licenses).

On pure arm64 Linux machines, even Android Studio's embedded tools may need the same qemu-user-static setup above for the build to succeed from the IDE.

**Note on directory:** In your terminal session you did `cd /home/doogie/Sandbag5x5` (correct) then accidentally `cd Sandbag5x5`. You were already in the project root — just run `./gradlew ...` commands directly from there. The `gradlew` script lives at the top level of this folder.

Use `./build.sh` (see below) instead of raw `./gradlew` on this machine — it handles the common arm64 gotchas automatically.

### Special note for ARM64 / aarch64 Linux (Jetson, Raspberry Pi 4/5 arm64, etc.)

If you see:

```
aapt2: 2: Syntax error: "(" unexpected
AAPT2 ... Daemon startup failed
```

or failures transforming `core-1.13.1.aar` / `processDebugResources`, it means you are on an ARM64 machine and the Android build tools (aapt2, etc.) are x86_64 only.

**Fix (manual):**

```bash
sudo dpkg --add-architecture amd64
sudo apt update
sudo apt install -y qemu-user-static binfmt-support libc6:amd64 libstdc++6:amd64
sudo update-binfmts --enable
sudo systemctl restart systemd-binfmt 2>/dev/null || true

cd /home/doogie/Sandbag5x5
rm -rf ~/.gradle/caches/8.9/transforms/
./build.sh clean
```

**Best:** Just use the included helper — it now does the full multiarch + QEMU setup automatically:

```bash
./build.sh clean
# or for stubborn cache problems:
./build.sh fullclean
```

The helper will prompt for sudo the first time it sets up amd64 libraries (this provides the missing `/lib64/ld-linux-x86-64.so.2` that aapt2 needs under emulation).

The first build after enabling QEMU will run the tools under emulation (noticeably slower, but it works). Your machine already had a working `adb` from the distro packages.

This is a very common gotcha on arm64 dev boards. QEMU user emulation + multiarch libc is the standard workaround because Google does not ship official aarch64 Linux versions of the build tools (aapt2, etc.).

## Using the App

1. Open the app → you immediately see **Today's workout** (A or B).
2. Tap **START WORKOUT X**.
3. For each exercise:
   - Read the target.
   - Hit the huge **COMPLETE SET** (or **COMPLETE** for timed finishers).
   - A full-screen rest timer appears (skip if you want).
4. When every set of every exercise is logged, the **FINISH WORKOUT** button appears (or tap it early if you want).
5. Pick **Easy / Solid / Brutal / Failed**.
6. App saves everything, applies the progression rule to the targets for that workout letter, flips to the other workout, and sends you home.

History is available from the home screen ("SEE ALL").

## Persistence & Progression

- All data lives in `sandbag5x5.db` (Room) inside the app's private storage.
- Current targets (sets / reps / rest) for A and B are stored separately and mutated on workout completion.
- Deleting the app or clearing data resets to the original seed program.
- A debug helper `resetAllTargetsToSeed()` exists in the repository (call it from a future settings screen if you add one).

## Philosophy / Constraints

- The only thing that matters is today's session.
- No exercise library, no graphs, no PR tracking beyond what the simple progression does.
- Big text. Big buttons. High contrast. Dark theme.
- If you can't finish a workout in one sitting the state is kept in the ViewModel until you finish or kill the app (simple v1 tradeoff — easy to improve with "in progress session" persistence later).

## License

Do whatever you want with it. Lift the damn sandbag.

---

Built as a minimal, focused example. Open the app. Do the work. Close the app.
