package org.openprojectx.gradle.mirror.core

data class GradleMirrorConfig(
    val repositories: List<RepositoryConfig> = emptyList(),
    val mirrors: List<MirrorConfig> = emptyList(),
)
