# Contributing

Thanks for improving Gradle Mirror. Keep changes focused and covered by tests where behavior changes.

## Development Setup

Use Java 17 or newer. The build configures a JVM toolchain for Java 17.

Run tests with:

```bash
env GRADLE_USER_HOME=/data/.gradle ./gradlew test --no-configuration-cache
```

Use `--no-configuration-cache` while developing plugin behavior. The root build currently applies release tooling that can report configuration-cache limitations unrelated to plugin correctness.

## Project Layout

| Path | Purpose |
| --- | --- |
| `core/src/main/kotlin` | YAML config model, loader, and Maven-style mirror matching. |
| `core/src/test/kotlin` | Unit tests for config loading and matching. |
| `plugin/src/main/kotlin` | Gradle plugin and extension. |
| `plugin/src/test/kotlin` | Gradle TestKit functional tests. |

## Contribution Guidelines

Before opening a pull request:

1. Add or update tests for behavior changes.
2. Run `./gradlew test --no-configuration-cache`.
3. Keep YAML schema changes backward compatible when possible.
4. Document new configuration fields in `README.md`.
5. Avoid unrelated formatting or publishing changes.

## Testing Guidance

Use unit tests in `core` for:

- `mirrorOf` pattern matching
- YAML parsing
- placeholder resolution
- validation behavior

Use Gradle TestKit tests in `plugin` for:

- applying the plugin from `build.gradle.kts`
- applying the plugin from `settings.gradle.kts`
- repository replacement behavior
- credentials and content filters

## YAML Compatibility

The plugin intentionally follows Maven-style mirror concepts. If you change `mirrorOf` behavior, include tests for exact repository names, wildcards, exclusions, and external repository patterns.

## Release Notes

For user-visible changes, include a short summary that covers:

- new YAML fields or behavior
- compatibility notes
- migration steps, if any
