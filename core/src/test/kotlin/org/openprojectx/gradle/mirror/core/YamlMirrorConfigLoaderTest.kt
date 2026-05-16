package org.openprojectx.gradle.mirror.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class YamlMirrorConfigLoaderTest {
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
}
