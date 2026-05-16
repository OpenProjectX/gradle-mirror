package org.openprojectx.gradle.mirror.core

data class MirrorConfig(
    val id: String,
    val url: String,
    val mirrorOf: String = "*",
    val credentials: CredentialsConfig? = null,
    val allowInsecureProtocol: Boolean = false,
    val releases: Boolean = true,
    val snapshots: Boolean = true,
)
