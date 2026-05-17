package org.openprojectx.gradle.mirror.core

import org.yaml.snakeyaml.Yaml
import java.io.File
import java.util.Properties

class YamlMirrorConfigLoader(
    private val environment: Map<String, String> = System.getenv(),
    private val secretProperties: Map<String, String> = emptyMap(),
    private val systemProperties: Map<String, String> = System.getProperties()
        .mapKeys { it.key.toString() }
        .mapValues { it.value.toString() },
) {
    fun load(file: File): GradleMirrorConfig {
        require(file.isFile) { "Gradle mirror config file does not exist: ${file.absolutePath}" }
        val secrets = secretProperties + loadSecrets(file.parentFile.resolve(DEFAULT_SECRETS_FILE))
        return copy(secretProperties = secrets).load(file.readText())
    }

    fun load(yamlText: String): GradleMirrorConfig {
        val root = Yaml().load<Any?>(yamlText) ?: return GradleMirrorConfig()
        require(root is Map<*, *>) { "Gradle mirror YAML root must be a mapping." }

        return GradleMirrorConfig(
            repositories = list(root["repositories"]).map(::repository),
            mirrors = list(root["mirrors"]).map(::mirror),
        )
    }

    private fun repository(value: Any?): RepositoryConfig {
        val map = mapping(value)
        return RepositoryConfig(
            id = requiredString(map, "id"),
            url = requiredString(map, "url"),
            credentials = credentials(map["credentials"]),
            allowInsecureProtocol = boolean(map, "allowInsecureProtocol", false),
            releases = boolean(map, "releases", true),
            snapshots = boolean(map, "snapshots", true),
        )
    }

    private fun mirror(value: Any?): MirrorConfig {
        val map = mapping(value)
        return MirrorConfig(
            id = requiredString(map, "id"),
            url = requiredString(map, "url"),
            mirrorOf = string(map, "mirrorOf") ?: "*",
            credentials = credentials(map["credentials"]),
            allowInsecureProtocol = boolean(map, "allowInsecureProtocol", false),
            releases = boolean(map, "releases", true),
            snapshots = boolean(map, "snapshots", true),
        )
    }

    private fun credentials(value: Any?): CredentialsConfig? {
        if (value == null) {
            return null
        }

        val map = mapping(value)
        return CredentialsConfig(
            username = string(map, "username"),
            password = string(map, "password"),
        )
    }

    private fun list(value: Any?): List<Any?> =
        when (value) {
            null -> emptyList()
            is List<*> -> value
            else -> error("Expected a YAML list but found ${value::class.simpleName}.")
        }

    private fun mapping(value: Any?): Map<*, *> =
        value as? Map<*, *> ?: error("Expected a YAML mapping but found ${value?.let { it::class.simpleName } ?: "null"}.")

    private fun requiredString(map: Map<*, *>, key: String): String =
        string(map, key)?.takeIf { it.isNotBlank() } ?: error("Missing required YAML property '$key'.")

    private fun string(map: Map<*, *>, key: String): String? =
        map[key]?.toString()?.let(::resolvePlaceholders)

    private fun boolean(map: Map<*, *>, key: String, defaultValue: Boolean): Boolean =
        map[key]?.toString()?.toBooleanStrictOrNull() ?: defaultValue

    private fun resolvePlaceholders(value: String): String =
        placeholderRegex.replace(value) { match ->
            val expression = match.groupValues[1]
            when {
                expression.startsWith("env.") -> environment[expression.removePrefix("env.")] ?: ""
                expression.startsWith("secret.") -> secretProperties[expression.removePrefix("secret.")] ?: ""
                expression.startsWith("system.") -> systemProperties[expression.removePrefix("system.")] ?: ""
                else -> environment[expression] ?: secretProperties[expression] ?: systemProperties[expression] ?: ""
            }
        }

    private fun copy(secretProperties: Map<String, String>): YamlMirrorConfigLoader =
        YamlMirrorConfigLoader(
            environment = environment,
            secretProperties = secretProperties,
            systemProperties = systemProperties,
        )

    private fun loadSecrets(file: File): Map<String, String> {
        if (!file.isFile) {
            return emptyMap()
        }

        val properties = Properties()
        file.inputStream().use(properties::load)
        return properties.stringPropertyNames().associateWith(properties::getProperty)
    }

    private companion object {
        const val DEFAULT_SECRETS_FILE = ".secrets.properties"
        private val placeholderRegex = Regex("""\$\{([^}]+)}""")
    }
}
