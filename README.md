# Gradle Mirror

Gradle Mirror is a Gradle plugin that configures Maven repositories from a YAML file. It is intended for teams that want Maven-style mirror configuration, including `mirrorOf` rules and credentials, without repeating repository blocks in every Gradle build.

## Plugin ID

```kotlin
plugins {
    id("org.openprojectx.gradle.mirror") version "0.1.0-SNAPSHOT"
}
```

The plugin can be applied to either a project build script or a settings script.

## YAML Configuration

By default, the plugin reads `gradle-mirror.yaml` from the project directory when applied to a project, or from the settings directory when applied to settings.

```yaml
repositories:
  - id: central
    url: https://repo.maven.apache.org/maven2
    releases: true
    snapshots: false

mirrors:
  - id: company
    url: https://repo.example.com/maven
    mirrorOf: "*,!local"
    credentials:
      username: ${env.MIRROR_USER}
      password: ${env.MIRROR_PASSWORD}
```

### Repository Fields

| Field | Required | Default | Description |
| --- | --- | --- | --- |
| `id` | yes | | Repository name used by Gradle and `mirrorOf` matching. |
| `url` | yes | | Maven repository URL. |
| `credentials.username` | no | | Username for password credentials. |
| `credentials.password` | no | | Password for password credentials. |
| `allowInsecureProtocol` | no | `false` | Allows HTTP repositories when set to `true`. |
| `releases` | no | `true` | Enables release artifacts. |
| `snapshots` | no | `true` | Enables snapshot artifacts. |

`mirrors` use the same fields as repositories and also support `mirrorOf`.

## `mirrorOf` Support

The matcher follows common Maven mirror patterns:

| Pattern | Meaning |
| --- | --- |
| `*` | Match every repository. |
| `external:*` | Match non-local, non-file repositories. |
| `external:http:*` | Match external HTTP repositories. |
| `central` | Match a repository named `central`. |
| `central,google` | Match either named repository. |
| `*,!local` | Match all repositories except `local`. |

When multiple mirrors match, the first mirror in the YAML file wins.

## Project Usage

```kotlin
plugins {
    id("org.openprojectx.gradle.mirror") version "0.1.0-SNAPSHOT"
}

gradleMirror {
    configFile.set(layout.projectDirectory.file("gradle-mirror.yaml"))
    replaceExisting.set(true)
    configureAllProjects.set(true)
}
```

Project extension options:

| Option | Default | Description |
| --- | --- | --- |
| `configFile` | `gradle-mirror.yaml` | YAML config file. |
| `replaceExisting` | `true` | Removes existing Maven repositories that match a configured mirror and adds the mirror. |
| `configureAllProjects` | `true` | Applies repository configuration to all projects from the root project. Set to `false` to configure only the current project. |

## Settings Usage

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

plugins {
    id("org.openprojectx.gradle.mirror") version "0.1.0-SNAPSHOT"
}

gradleMirror {
    configFile.set(layout.settingsDirectory.file("gradle-mirror.yaml"))
    replaceExisting.set(true)
}
```

When applied to settings, the plugin configures both `dependencyResolutionManagement.repositories` and `pluginManagement.repositories`.

## Placeholders

String values support environment and system property placeholders:

```yaml
credentials:
  username: ${env.MIRROR_USER}
  password: ${system.mirror.password}
```

The plugin also reads `.secrets.properties` from the same directory as `gradle-mirror.yaml`. Those values are available to placeholder resolution and are added as Gradle extra properties when the plugin loads the config.

```properties
repo1_username=alice
repo1_password=secret
```

```yaml
credentials:
  username: ${repo1_username}
  password: ${repo1_password}
```

Plain placeholders without a prefix check environment variables first, then `.secrets.properties`, then system properties:

```yaml
url: ${MIRROR_URL}
```

You can force the secrets file source with `secret.`:

```yaml
password: ${secret.repo1_password}
```

Missing placeholders resolve to an empty string.

## Development

Run the test suite with:

```bash
env GRADLE_USER_HOME=/data/.gradle ./gradlew test --no-configuration-cache
```

The project has two modules:

| Module | Purpose |
| --- | --- |
| `core` | YAML parsing, config models, and `mirrorOf` matching. |
| `plugin` | Gradle plugin implementation and functional tests. |

## Examples

See [examples/basic](examples/basic) for a standalone sample build that applies this plugin from the local checkout and prints the configured repositories.

Run it with:

```bash
env GRADLE_USER_HOME=/data/.gradle ./gradlew -p examples/basic printRepositories --no-configuration-cache
```
