# Architecture

## Runtime classes

- `HL7Workbench` is the stable application entry point.
- `MainWindow` contains the proven Viewer and Comparison UI behaviour.
- `AppInfo` owns name, version and author metadata.
- `DictionaryLoader` loads CSV dictionaries from the filesystem during development or from the packaged classpath.
- `UITheme` provides shared constants for future UI components.

## Why MainWindow remains cohesive

This release is a behaviour-preserving refactor. The former all-in-one `HL7Workbench` class was split so the launcher, metadata, dictionary loading and visual constants have clear ownership. Viewer and Comparison event handlers remain together in `MainWindow` because separating tightly coupled Swing listeners and models in the same change would create unnecessary regression risk.

Future extractions should move one tested feature at a time into `ViewerPanel`, `ComparisonPanel`, parsing services and comparison services. Each extraction should preserve screenshots, path generation, dictionary lookup, highlighting and macOS packaging behaviour.
