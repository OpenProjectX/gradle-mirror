plugins {
    id("buildsrc.convention.kotlin-jvm")
    `java-gradle-plugin`
}


dependencies {
    implementation(project(":core"))
    testImplementation(libs.junitJupiter)
    testImplementation(gradleTestKit())
    testRuntimeOnly(libs.junitPlatformLauncher)
}

gradlePlugin {
    plugins {
        create("gradleMirror") {
            id = "org.openprojectx.gradle.mirror"
            implementationClass = "org.openprojectx.gradle.mirror.plugin.GradleMirrorPlugin"
            displayName = "Gradle Mirror"
            description = "Configures Gradle repositories from YAML using Maven-style mirrorOf rules."
        }
    }
}
