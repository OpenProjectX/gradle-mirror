plugins {
    id("buildsrc.convention.kotlin-jvm")
}


dependencies {
    api(libs.snakeYaml)
    testImplementation(libs.junitJupiter)
    testRuntimeOnly(libs.junitPlatformLauncher)
}
