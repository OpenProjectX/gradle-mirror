package org.openprojectx.gradle.mirror.core

data class RepositoryConfig(
    val id: String,
    val url: String,
    val credentials: CredentialsConfig? = null,
    val allowInsecureProtocol: Boolean = false,
    val releases: Boolean = true,
    val snapshots: Boolean = true,
)
