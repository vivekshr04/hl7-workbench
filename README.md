# HL7 Workbench

A Java 17/Swing desktop application for inspecting and comparing HL7 v2 messages locally.

## Features
- Paste and parse HL7 v2 messages
- Segment, field, component, subcomponent and repetition inspection
- Sequence and Set ID navigation
- CSV-driven HL7 descriptions
- Custom Z-segment support
- Source/Target comparison tab
- Field/component/subcomponent highlighting
- Hover paths in comparison
- macOS `.app` and `.dmg` packaging with `jpackage`

## Requirements for development
- JDK 17+
- Maven 3.9+

## Build and run
```bash
mvn clean test package
mvn exec:java
```

Run the built JAR:
```bash
java -jar target/hl7-workbench-1.0.0.jar
```

Build the macOS release on macOS:
```bash
chmod +x scripts/build-mac-release.command
./scripts/build-mac-release.command
```

## Privacy
Messages are processed locally by the desktop application. Do not commit real patient data or confidential messages to the repository.

## Project structure
- `src/main/java`: application sources
- `src/main/resources`: dictionaries and icon
- `src/test/java`: unit tests
- `scripts`: packaging automation
- `docs`: design and contributor documentation

## Code organisation

- `HL7Workbench.java`: application launcher
- `MainWindow.java`: Viewer and Comparison workspaces
- `DictionaryLoader.java`: external/classpath CSV loading
- `AppInfo.java`: product metadata
- `UITheme.java`: shared visual constants

For local use without Maven, double-click `run-local.command` or execute `./run-local.command`.

## Maven setup

Maven is the primary build system for local development, CI and public releases.

Verify the prerequisites:

```bash
java -version
javac -version
mvn -version
jpackage --version
```

If Maven is not installed, install the Apache Maven binary distribution and add its `bin` directory to `PATH`. Homebrew is optional, not required.

## Build and run with Maven

```bash
mvn clean test package
mvn exec:java
```

## Build the self-contained macOS release

```bash
chmod +x scripts/build-mac-release.command
./scripts/build-mac-release.command
```

The release script first runs the Maven tests and package lifecycle, then uses `jpackage` to produce:

```text
dist/HL7 Workbench.app
dist/HL7 Workbench-1.0.0.dmg
```

`run-local.command` remains available as a convenience for local UI testing, but Maven is the authoritative public build.
