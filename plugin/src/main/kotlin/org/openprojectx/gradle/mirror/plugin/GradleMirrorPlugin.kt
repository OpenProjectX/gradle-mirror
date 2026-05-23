package org.openprojectx.gradle.mirror.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.artifacts.repositories.PasswordCredentials
import org.gradle.api.initialization.Settings
import org.gradle.api.model.ObjectFactory
import org.openprojectx.gradle.mirror.core.CredentialsConfig
import org.openprojectx.gradle.mirror.core.GradleMirrorConfig
import org.openprojectx.gradle.mirror.core.MirrorConfig
import org.openprojectx.gradle.mirror.core.MirrorMatcher
import org.openprojectx.gradle.mirror.core.RepositoryConfig
import org.openprojectx.gradle.mirror.core.YamlMirrorConfigLoader
import java.io.File
import java.net.URI
import java.util.Properties
import javax.inject.Inject

class GradleMirrorPlugin @Inject constructor(
    private val objects: ObjectFactory,
) : Plugin<Any> {
    override fun apply(target: Any) {
        when (target) {
            is Project -> applyToProject(target)
            is Settings -> applyToSettings(target)
            else -> error("Gradle Mirror plugin can only be applied to a Project or Settings instance.")
        }
    }

    private fun applyToProject(project: Project) {
        val extension = project.extensions.create(
            "gradleMirror",
            GradleMirrorExtension::class.java,
            objects,
        )
        extension.configFile.convention(project.layout.projectDirectory.file(DEFAULT_CONFIG_FILE))

        project.afterEvaluate {
            val configFile = extension.configFile.asFile.get()
            exposeSecretProperties(project, configFile)
            val config = load(configFile)
            val projects = if (extension.configureAllProjects.get()) {
                project.rootProject.allprojects
            } else {
                setOf(project)
            }

            projects.forEach { configuredProject ->
                configureRepositories(
                    repositories = configuredProject.repositories,
                    config = config,
                    replaceExisting = extension.replaceExisting.get(),
                    uriFactory = configuredProject::uri,
                )
            }
        }
    }

    private fun applyToSettings(settings: Settings) {
        val extension = settings.extensions.create(
            "gradleMirror",
            GradleMirrorExtension::class.java,
            objects,
        )
        extension.configFile.convention(settings.layout.settingsDirectory.file(DEFAULT_CONFIG_FILE))

        settings.gradle.settingsEvaluated {
            val configFile = extension.configFile.asFile.get()
            exposeSecretProperties(settings, configFile)
            val config = load(configFile)

            settings.dependencyResolutionManagement { dependencyResolution ->
                dependencyResolution.repositories { repositories ->
                    configureRepositories(
                        repositories = repositories,
                        config = config,
                        replaceExisting = extension.replaceExisting.get(),
                        uriFactory = { settingsUri(settings, it) },
                    )
                }
            }

            settings.pluginManagement { pluginManagement ->
                pluginManagement.repositories { repositories ->
                    configureRepositories(
                        repositories = repositories,
                        config = config,
                        replaceExisting = extension.replaceExisting.get(),
                        uriFactory = { settingsUri(settings, it) },
                    )
                }
            }
        }
    }

    private fun load(configFile: File): GradleMirrorConfig =
        if (configFile.isFile) {
            YamlMirrorConfigLoader().load(configFile)
        } else {
            GradleMirrorConfig()
        }

    private fun exposeSecretProperties(project: Project, configFile: File) {
        val secrets = loadSecretProperties(configFile)
        secrets.forEach { (key, value) ->
            project.extensions.extraProperties.set(key, value)
        }
    }

    private fun exposeSecretProperties(settings: Settings, configFile: File) {
        val secrets = loadSecretProperties(configFile)
        secrets.forEach { (key, value) ->
            settings.extensions.extraProperties.set(key, value)
            settings.gradle.extensions.extraProperties.set(key, value)
        }
    }

    private fun loadSecretProperties(configFile: File): Map<String, String> {
        val secretsFile = configFile.parentFile.resolve(DEFAULT_SECRETS_FILE)
        if (!secretsFile.isFile) {
            return emptyMap()
        }

        val properties = Properties()
        secretsFile.inputStream().use(properties::load)
        return properties.stringPropertyNames().associateWith(properties::getProperty)
    }

    private fun configureRepositories(
        repositories: RepositoryHandler,
        config: GradleMirrorConfig,
        replaceExisting: Boolean,
        uriFactory: (String) -> URI,
    ) {
        val desiredRepositories = linkedMapOf<String, RepositorySpec>()

        if (replaceExisting) {
            repositories.filterIsInstance<MavenArtifactRepository>()
                .filter { existingRepository ->
                    MirrorMatcher.firstMatchingMirror(
                        config.mirrors,
                        existingRepository.name,
                        existingRepository.url.toString(),
                    ) != null
                }
                .forEach { existingRepository ->
                    val mirror = MirrorMatcher.firstMatchingMirror(
                        config.mirrors,
                        existingRepository.name,
                        existingRepository.url.toString(),
                    ) ?: return@forEach

                    repositories.remove(existingRepository)
                    desiredRepositories.putIfAbsent(mirror.id, RepositorySpec.Mirror(mirror))
                }
        }

        config.repositories.forEach { repository ->
            val mirror = MirrorMatcher.firstMatchingMirror(config.mirrors, repository.id, repository.url)
            val spec = if (mirror == null) {
                RepositorySpec.Repository(repository)
            } else {
                RepositorySpec.Mirror(mirror)
            }
            desiredRepositories.putIfAbsent(spec.id, spec)
        }

        config.mirrors.forEach { mirror ->
            desiredRepositories.putIfAbsent(mirror.id, RepositorySpec.Mirror(mirror))
        }

        desiredRepositories.values.forEach { spec ->
            repositories.findByName(spec.id)?.let(repositories::remove)
            addMavenRepository(repositories, spec, uriFactory)
        }
    }

    private fun addMavenRepository(
        repositories: RepositoryHandler,
        spec: RepositorySpec,
        uriFactory: (String) -> URI,
    ) {
        repositories.maven { repository ->
            repository.name = spec.id
            repository.url = uriFactory(spec.url)
            repository.isAllowInsecureProtocol = spec.allowInsecureProtocol
            applyCredentials(repository, spec.credentials)

            repository.mavenContent { content ->
                when {
                    spec.releases && !spec.snapshots -> content.releasesOnly()
                    spec.snapshots && !spec.releases -> content.snapshotsOnly()
                }
            }
        }
    }

    private fun applyCredentials(repository: MavenArtifactRepository, credentials: CredentialsConfig?) {
        if (credentials == null) {
            return
        }

        repository.credentials(PasswordCredentials::class.java) { passwordCredentials ->
            passwordCredentials.username = credentials.username
            passwordCredentials.password = credentials.password
        }
    }

    private fun settingsUri(settings: Settings, value: String): URI =
        if (value.contains(":")) {
            URI.create(value)
        } else {
            settings.settingsDir.resolve(value).toURI()
        }

    private sealed interface RepositorySpec {
        val id: String
        val url: String
        val credentials: CredentialsConfig?
        val allowInsecureProtocol: Boolean
        val releases: Boolean
        val snapshots: Boolean

        data class Repository(private val config: RepositoryConfig) : RepositorySpec {
            override val id: String = config.id
            override val url: String = config.url
            override val credentials: CredentialsConfig? = config.credentials
            override val allowInsecureProtocol: Boolean = config.allowInsecureProtocol
            override val releases: Boolean = config.releases
            override val snapshots: Boolean = config.snapshots
        }

        data class Mirror(private val config: MirrorConfig) : RepositorySpec {
            override val id: String = config.id
            override val url: String = config.url
            override val credentials: CredentialsConfig? = config.credentials
            override val allowInsecureProtocol: Boolean = config.allowInsecureProtocol
            override val releases: Boolean = config.releases
            override val snapshots: Boolean = config.snapshots
        }
    }

    private companion object {
        const val DEFAULT_CONFIG_FILE = "gradle-mirror.yaml"
        const val DEFAULT_SECRETS_FILE = ".secrets.properties"
    }
}
