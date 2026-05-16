package org.openprojectx.gradle.mirror.core

import java.net.URI

object MirrorMatcher {
    fun matches(mirrorOf: String, repositoryId: String, repositoryUrl: String? = null): Boolean {
        val tokens = mirrorOf.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (tokens.isEmpty()) {
            return false
        }

        if (tokens.any { it.startsWith("!") && tokenMatches(it.drop(1), repositoryId, repositoryUrl) }) {
            return false
        }

        return tokens
            .filterNot { it.startsWith("!") }
            .any { tokenMatches(it, repositoryId, repositoryUrl) }
    }

    fun firstMatchingMirror(
        mirrors: List<MirrorConfig>,
        repositoryId: String,
        repositoryUrl: String? = null,
    ): MirrorConfig? = mirrors.firstOrNull { matches(it.mirrorOf, repositoryId, repositoryUrl) }

    private fun tokenMatches(token: String, repositoryId: String, repositoryUrl: String?): Boolean =
        when (token) {
            "*" -> true
            "external:*" -> repositoryUrl?.let(::isExternalRepository) ?: (repositoryId != "local")
            "external:http:*" -> repositoryUrl?.let { isExternalRepository(it) && URI.create(it).scheme == "http" } ?: false
            else -> token == repositoryId
        }

    private fun isExternalRepository(repositoryUrl: String): Boolean {
        val uri = URI.create(repositoryUrl)
        val scheme = uri.scheme?.lowercase()
        val host = uri.host?.lowercase()

        return scheme != "file" &&
            host != null &&
            host != "localhost" &&
            host != "127.0.0.1" &&
            host != "::1"
    }
}
