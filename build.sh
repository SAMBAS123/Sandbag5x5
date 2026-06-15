#!/usr/bin/env bash
#
# Sandbag5x5 build helper
#
# This script makes building easier on tricky setups (especially arm64 Linux
# like Jetson / dev boards that need QEMU to run the x86_64 Android build tools).
#
# Usage:
#   ./build.sh                  # Normal build (sets env + qemu if needed)
#   ./build.sh clean            # Clean transforms + gradle clean + build
#   ./build.sh fullclean        # Aggressive: also refresh deps + purge bad caches
#   ./build.sh setup            # Just ensure qemu + remind about SDK
#   ./build.sh assembleDebug    # Pass any normal gradle task
#
# It will:
# - Set ANDROID_HOME and PATH if not already correct
# - Enable QEMU user emulation on aarch64 (required for aapt2 etc.)
# - Stop the Gradle daemon before heavy cleans
# - Clear the transforms cache that frequently causes "aar-metadata.properties"
#   and navigation dependency errors on this platform
#
set -e

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_ROOT"

ARCH=$(uname -m)
echo "==> Architecture: $ARCH"

# ------------------------------------------------------------------
# 1. Locate / set ANDROID_HOME
# ------------------------------------------------------------------
if [ -z "$ANDROID_HOME" ]; then
    for candidate in \
        "$HOME/Android/Sdk" \
        "/opt/android-sdk" \
        "/usr/lib/android-sdk" \
        "$HOME/AndroidStudioProjects/Sdk" \
        "$HOME/android-sdk"
    do
        if [ -d "$candidate" ] && [ -f "$candidate/cmdline-tools/latest/bin/sdkmanager" ]; then
            export ANDROID_HOME="$candidate"
            echo "==> Found ANDROID_HOME at $ANDROID_HOME"
            break
        fi
    done
fi

if [ -z "$ANDROID_HOME" ]; then
    echo "WARNING: ANDROID_HOME is not set and no SDK was auto-detected."
    echo "         Run ./setup-sdk.sh first, or export ANDROID_HOME manually."
else
    export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
    echo "==> ANDROID_HOME=$ANDROID_HOME"
fi

# ------------------------------------------------------------------
# 2. QEMU user emulation for aarch64 (the #1 cause of aapt2 "Syntax error")
# ------------------------------------------------------------------
setup_qemu() {
    if [ "$ARCH" = "aarch64" ] || [ "$ARCH" = "arm64" ]; then
        echo "==> ARM64 detected — ensuring QEMU user emulation + x86_64 libraries for aapt2 etc."
        # Add amd64 architecture so we can install the x86_64 dynamic linker and libc
        sudo dpkg --add-architecture amd64 2>/dev/null || true

        # On arm64 Ubuntu (ports.ubuntu.com), amd64 packages must come from the regular archive.
        # Create a dedicated sources file for amd64 (modern .sources format) to avoid 404s on ports repo.
        if [ ! -f /etc/apt/sources.list.d/amd64.sources ]; then
            echo "    Creating amd64 sources (required on arm64 systems using ports.ubuntu.com)..."
            cat <<'EOF' | sudo tee /etc/apt/sources.list.d/amd64.sources >/dev/null
Types: deb
URIs: http://archive.ubuntu.com/ubuntu/
Suites: noble noble-updates
Components: main restricted universe multiverse
Architectures: amd64
Signed-By: /usr/share/keyrings/ubuntu-archive-keyring.gpg

Types: deb
URIs: http://security.ubuntu.com/ubuntu/
Suites: noble-security
Components: main restricted universe multiverse
Architectures: amd64
Signed-By: /usr/share/keyrings/ubuntu-archive-keyring.gpg
EOF
        fi

        # Pin the existing ports sources to arm64 only so apt doesn't try to fetch amd64 from them (which 404s).
        if ! grep -q 'Architectures: arm64' /etc/apt/sources.list.d/ubuntu.sources 2>/dev/null; then
            echo "    Restricting ubuntu.sources to arm64 architecture..."
            sudo sed -i '/ports.ubuntu.com\/ubuntu-ports\//a \Architectures: arm64' /etc/apt/sources.list.d/ubuntu.sources
        fi

        sudo apt update -qq
        # Install qemu + the amd64 libc (provides /lib64/ld-linux-x86-64.so.2 and friends)
        sudo apt install -y qemu-user-static binfmt-support libc6:amd64 libstdc++6:amd64
        sudo update-binfmts --enable 2>/dev/null || true
        if command -v systemctl >/dev/null 2>&1; then
            sudo systemctl restart systemd-binfmt 2>/dev/null || true
        fi
        echo "    QEMU + amd64 libs should now be active. The dynamic linker error should be gone."

        # Ensure we have a full JDK (not just JRE/headless). AGP needs jlink for androidJdkImage on API 34+.
        if [ ! -x /usr/lib/jvm/java-21-openjdk-arm64/bin/jlink ]; then
            echo "    Installing full openjdk-21-jdk (jlink is required for Android builds)..."
            sudo apt install -y openjdk-21-jdk
        fi

        # Optional: clean up old duplicate Signal source (harmless warning)
        if [ -f /etc/apt/sources.list.d/signal-xenial.list ]; then
            echo "    (Optional) Removing old signal-xenial.list to silence duplicate warnings..."
            sudo rm -f /etc/apt/sources.list.d/signal-xenial.list 2>/dev/null || true
        fi
    fi
}

# ------------------------------------------------------------------
# 3. Safe cache cleaning (fixes the recurring navigation / aar-metadata errors)
# ------------------------------------------------------------------
clean_transforms() {
    echo "==> Cleaning Gradle transforms cache (common source of aar-metadata.properties failures)"
    rm -rf ~/.gradle/caches/8.9/transforms/ 2>/dev/null || true
    ./gradlew --stop 2>/dev/null || true
}

# ------------------------------------------------------------------
# 4. Command handling
# ------------------------------------------------------------------
case "${1:-build}" in
    setup)
        setup_qemu
        echo
        echo "Now make sure the SDK is installed:"
        echo "  ./setup-sdk.sh"
        echo "  (or open the project in Android Studio)"
        ;;

    clean)
        setup_qemu
        clean_transforms
        echo "==> Running gradle clean + assembleDebug"
        ./gradlew clean assembleDebug
        ;;

    fullclean)
        setup_qemu
        clean_transforms
        echo "==> Also purging navigation module cache and using --refresh-dependencies"
        rm -rf ~/.gradle/caches/modules-2/files-2.1/androidx.navigation/navigation-common/2.8.2/ 2>/dev/null || true
        ./gradlew clean assembleDebug --refresh-dependencies
        ;;

    build|*)
        # Default / passthrough mode
        setup_qemu
        if [ "$1" = "build" ]; then shift; fi
        echo "==> Running ./gradlew $*"
        ./gradlew "$@"
        ;;
esac

echo "==> Done. If you still see problems, try:"
echo "    ./build.sh fullclean"
echo "    (then open the project in Android Studio and let it sync)"
