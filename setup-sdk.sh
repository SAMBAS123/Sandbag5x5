#!/usr/bin/env bash
# Helper script to create local.properties for Sandbag5x5
# Run this from the project root after you have installed the Android SDK.

set -e

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEFAULT_SDK="$HOME/Android/Sdk"

echo "Sandbag5x5 SDK setup helper"
echo "============================"
echo

if [ -f "$PROJECT_ROOT/local.properties" ]; then
  echo "local.properties already exists:"
  cat "$PROJECT_ROOT/local.properties"
  echo
  read -p "Overwrite it? [y/N] " -n 1 -r
  echo
  if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Aborted."
    exit 0
  fi
fi

echo "Common SDK location: $DEFAULT_SDK"
read -p "Enter your Android SDK path (or press Enter to use $DEFAULT_SDK): " SDK_PATH

if [ -z "$SDK_PATH" ]; then
  SDK_PATH="$DEFAULT_SDK"
fi

if [ ! -d "$SDK_PATH" ]; then
  echo "WARNING: Directory does not exist yet: $SDK_PATH"
  echo "You must install the Android command line tools first."
  echo ""
  echo "Run these commands (see full details in README.md):"
  echo "  sudo apt update && sudo apt install -y wget unzip"
  echo "  mkdir -p ~/Android/Sdk/cmdline-tools"
  echo "  cd /tmp"
  echo "  wget -O commandlinetools.zip https://dl.google.com/android/repository/commandlinetools-linux-14742923_latest.zip"
  echo "  unzip -q commandlinetools.zip -d ~/Android/Sdk/cmdline-tools"
  echo "  mv ~/Android/Sdk/cmdline-tools/cmdline-tools ~/Android/Sdk/cmdline-tools/latest"
  echo ""
  echo "Then re-run this script and press Enter to accept the default path."
fi

# ARM64 detection
if [ "$(uname -m)" = "aarch64" ] || [ "$(uname -m)" = "arm64" ]; then
  echo ""
  echo "NOTE: This machine is aarch64 (ARM64)."
  echo "You will also need QEMU user emulation for the Android build tools (aapt2 etc.):"
  echo "  sudo apt install -y qemu-user-static binfmt-support"
  echo "  sudo update-binfmts --enable"
  echo "See the ARM64 section in README.md for the full explanation and build commands."
fi

echo "sdk.dir=$SDK_PATH" > "$PROJECT_ROOT/local.properties"
echo
echo "Created $PROJECT_ROOT/local.properties with:"
cat "$PROJECT_ROOT/local.properties"
echo
echo "Next steps:"
echo "  1. Make sure the following are installed in your SDK:"
echo "       platforms;android-34"
echo "       build-tools;34.0.0"
echo "       platform-tools"
echo "  2. Accept licenses if you haven't:"
echo "       export ANDROID_HOME=\"$SDK_PATH\""
echo "       yes | \$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses"
echo "  3. Build:"
echo "       ./build.sh            # recommended helper (handles env + qemu + cache issues)"
echo "       ./build.sh clean"
echo
echo "Done. You can now try building."
