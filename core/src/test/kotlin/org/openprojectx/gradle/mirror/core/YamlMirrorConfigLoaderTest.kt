package org.openprojectx.gradle.mirror.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class YamlMirrorConfigLoaderTest {
    @TempDir
    lateinit var tempDir: File

    @Test
    fun `loads repositories mirrors credentials and placeholders`() {
        val config = YamlMirrorConfigLoader(
            environment = mapOf("MIRROR_USER" to "alice"),
            systemProperties = mapOf("mirror.password" to "secret"),
        ).load(
            """
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
                  username: ${'$'}{env.MIRROR_USER}
                  password: ${'$'}{system.mirror.password}
            """.trimIndent(),
        )

        assertEquals("central", config.repositories.single().id)
        assertEquals(false, config.repositories.single().snapshots)
        assertEquals("company", config.mirrors.single().id)
        assertEquals("*,!local", config.mirrors.single().mirrorOf)
        assertEquals("alice", config.mirrors.single().credentials?.username)
        assertEquals("secret", config.mirrors.single().credentials?.password)
    }

    @Test
    fun `loads unprefixed placeholders from sibling secrets file after environment`() {
        tempDir.resolve(".secrets.properties").writeText(
            """
            repo1_username=secret-user
            repo1_password=secret-password
            shared=secret-shared
            """.trimIndent(),
        )
        val configFile = tempDir.resolve("gradle-mirror.yaml")
        configFile.writeText(
            """
            mirrors:
              - id: company
                url: https://repo.example.com/maven
                mirrorOf: "*"
                credentials:
                  username: ${'$'}{repo1_username}
                  password: ${'$'}{repo1_password}
            repositories:
              - id: central
                url: ${'$'}{shared}
            """.trimIndent(),
        )

        val config = YamlMirrorConfigLoader(
            environment = mapOf("shared" to "https://env.example.com/maven"),
            systemProperties = mapOf("repo1_password" to "system-password"),
        ).load(configFile)

        assertEquals("secret-user", config.mirrors.single().credentials?.username)
        assertEquals("secret-password", config.mirrors.single().credentials?.password)
        assertEquals("https://env.example.com/maven", config.repositories.single().url)
    }
}
