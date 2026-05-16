package org.openprojectx.gradle.mirror.core

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MirrorMatcherTest {
    @Test
    fun `matches wildcard except explicit exclusions`() {
        assertTrue(MirrorMatcher.matches("*,!local", "central", "https://repo.maven.apache.org/maven2"))
        assertFalse(MirrorMatcher.matches("*,!local", "local", "file:/home/user/.m2/repository"))
    }

    @Test
    fun `matches external repositories`() {
        assertTrue(MirrorMatcher.matches("external:*", "central", "https://repo.maven.apache.org/maven2"))
        assertFalse(MirrorMatcher.matches("external:*", "local", "file:/home/user/.m2/repository"))
        assertFalse(MirrorMatcher.matches("external:*", "local-test", "http://localhost:8081/repository"))
    }

    @Test
    fun `matches exact repository ids`() {
        assertTrue(MirrorMatcher.matches("central,google", "central", "https://repo.maven.apache.org/maven2"))
        assertFalse(MirrorMatcher.matches("central,google", "gradlePluginPortal", "https://plugins.gradle.org/m2"))
    }
}
