#!/bin/bash

# Exit immediately when:
# - a command fails (-e)
# - an undefined variable is used (-u)
# - any command in a pipeline fails (pipefail)
set -euo pipefail

# Always run from the project root, regardless of where the script is launched.
cd "$(dirname "$0")/.."

# -----------------------------------------------------------------------------
# Application metadata
# Keep APP_VERSION aligned with the version in pom.xml and AppInfo.java.
# -----------------------------------------------------------------------------
APP_NAME="HL7 Workbench"
APP_VERSION="1.0.0"
MAIN_CLASS="com.eqe.hl7workbench.HL7Workbench"
JAR_NAME="hl7-workbench-${APP_VERSION}.jar"

# -----------------------------------------------------------------------------
# Project paths
# Maven creates the JAR under target/. jpackage reads it from INPUT_DIR.
# -----------------------------------------------------------------------------
ICON_PNG="src/main/resources/com/eqe/hl7workbench/assets/HL7Workbench.png"
BUILD_DIR="target/macos"
INPUT_DIR="$BUILD_DIR/input"
ICONSET_DIR="$BUILD_DIR/HL7Workbench.iconset"
ICNS_FILE="$BUILD_DIR/HL7Workbench.icns"
DIST_DIR="dist"

# Fail early with a clear message when a required command is unavailable.
require_command() {
    command -v "$1" >/dev/null 2>&1 || {
        echo "Required command not found: $1"
        exit 1
    }
}

# -----------------------------------------------------------------------------
# Prerequisite validation
# Maven builds/tests the project. jpackage creates the native macOS packages.
# sips and iconutil are standard macOS utilities used to create the .icns icon.
# -----------------------------------------------------------------------------
require_command mvn
require_command java
require_command jpackage
require_command sips
require_command iconutil

# -----------------------------------------------------------------------------
# Step 1: Build and test using the authoritative Maven lifecycle.
# This compiles all Java sources, runs unit tests, and creates the executable JAR.
# -----------------------------------------------------------------------------
echo "Building and testing with Maven..."
mvn --batch-mode clean test package

# Confirm that Maven produced the JAR expected by the packaging steps.
if [ ! -f "target/$JAR_NAME" ]; then
    echo "Expected Maven artefact was not created: target/$JAR_NAME"
    exit 1
fi

# Confirm that the source PNG required for the macOS application icon exists.
if [ ! -f "$ICON_PNG" ]; then
    echo "Application icon not found: $ICON_PNG"
    exit 1
fi

# -----------------------------------------------------------------------------
# Step 2: Prepare clean packaging folders.
# Removing old output prevents stale files from entering a new release.
# -----------------------------------------------------------------------------
echo "Preparing macOS package input..."
rm -rf "$BUILD_DIR" "$DIST_DIR"
mkdir -p "$INPUT_DIR" "$ICONSET_DIR" "$DIST_DIR"
cp "target/$JAR_NAME" "$INPUT_DIR/"

# -----------------------------------------------------------------------------
# Step 3: Generate the macOS .icns file.
# macOS icons require multiple PNG resolutions in an .iconset directory.
# -----------------------------------------------------------------------------
echo "Creating macOS icon..."
sips -z 16 16 "$ICON_PNG" --out "$ICONSET_DIR/icon_16x16.png" >/dev/null
sips -z 32 32 "$ICON_PNG" --out "$ICONSET_DIR/icon_16x16@2x.png" >/dev/null
sips -z 32 32 "$ICON_PNG" --out "$ICONSET_DIR/icon_32x32.png" >/dev/null
sips -z 64 64 "$ICON_PNG" --out "$ICONSET_DIR/icon_32x32@2x.png" >/dev/null
sips -z 128 128 "$ICON_PNG" --out "$ICONSET_DIR/icon_128x128.png" >/dev/null
sips -z 256 256 "$ICON_PNG" --out "$ICONSET_DIR/icon_128x128@2x.png" >/dev/null
sips -z 256 256 "$ICON_PNG" --out "$ICONSET_DIR/icon_256x256.png" >/dev/null
sips -z 512 512 "$ICON_PNG" --out "$ICONSET_DIR/icon_256x256@2x.png" >/dev/null
sips -z 512 512 "$ICON_PNG" --out "$ICONSET_DIR/icon_512x512.png" >/dev/null
sips -z 1024 1024 "$ICON_PNG" --out "$ICONSET_DIR/icon_512x512@2x.png" >/dev/null
iconutil -c icns "$ICONSET_DIR" -o "$ICNS_FILE"

# -----------------------------------------------------------------------------
# Step 4: Create the self-contained .app bundle.
# jpackage bundles the application JAR and a Java runtime, so users do not need
# Java, Maven, VS Code, or the source project installed to run the application.
# -----------------------------------------------------------------------------
echo "Creating self-contained macOS application..."
jpackage \
    --type app-image \
    --input "$INPUT_DIR" \
    --main-jar "$JAR_NAME" \
    --main-class "$MAIN_CLASS" \
    --name "$APP_NAME" \
    --app-version "$APP_VERSION" \
    --vendor "Vivek Sharma" \
    --description "HL7 Message Viewer and Comparison Workbench" \
    --icon "$ICNS_FILE" \
    --mac-package-identifier "com.eqe.hl7workbench" \
    --mac-package-name "HL7 Workbench" \
    --dest "$DIST_DIR"

# -----------------------------------------------------------------------------
# Step 5: Create the distributable DMG installer.
# The DMG is the recommended artefact to share with other macOS users.
# -----------------------------------------------------------------------------
echo "Creating macOS DMG installer..."
jpackage \
    --type dmg \
    --input "$INPUT_DIR" \
    --main-jar "$JAR_NAME" \
    --main-class "$MAIN_CLASS" \
    --name "$APP_NAME" \
    --app-version "$APP_VERSION" \
    --vendor "Vivek Sharma" \
    --description "HL7 Message Viewer and Comparison Workbench" \
    --icon "$ICNS_FILE" \
    --mac-package-identifier "com.eqe.hl7workbench" \
    --mac-package-name "HL7 Workbench" \
    --dest "$DIST_DIR"

# -----------------------------------------------------------------------------
# Build summary
# -----------------------------------------------------------------------------
echo
echo "Release created successfully:"
echo "  $PWD/$DIST_DIR/$APP_NAME.app"
echo "  $PWD/$DIST_DIR/$APP_NAME-$APP_VERSION.dmg"
