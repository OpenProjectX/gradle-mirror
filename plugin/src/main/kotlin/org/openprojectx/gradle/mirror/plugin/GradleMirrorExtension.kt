package org.openprojectx.gradle.mirror.plugin

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class GradleMirrorExtension @Inject constructor(objects: ObjectFactory) {
    val configFile: RegularFileProperty = objects.fileProperty()
    val replaceExisting: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
    val configureAllProjects: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
}
