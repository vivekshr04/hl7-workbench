# Development setup

## Required

- JDK 17 or later, including `javac`, `jar` and `jpackage`
- Apache Maven 3.9 or later

## Verify

```bash
java -version
javac -version
jpackage --version
mvn -version
```

## Build

```bash
mvn clean test package
```

## Run

```bash
mvn exec:java
```

## macOS release

```bash
./scripts/build-mac-release.command
```

Homebrew is not required. Maven can be installed from the official Apache binary distribution and its `bin` directory added to `PATH`.
