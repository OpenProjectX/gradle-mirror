# Basic Example

This standalone build applies the local `org.openprojectx.gradle.mirror` plugin through `includeBuild("../..")` and reads repository configuration from `gradle-mirror.yaml`.

Run it from the repository root with:

```bash
env GRADLE_USER_HOME=/data/.gradle ./gradlew -p examples/basic printRepositories --no-configuration-cache
```

Expected output includes a repository named `central-mirror`:

```text
central-mirror: ...
```

The example mirrors the logical repository named `central` to `central-mirror`. The mirror URL intentionally points at Maven Central so the example remains runnable without private infrastructure. Some local Gradle environments may rewrite the displayed URL through their own repository mirror configuration.
