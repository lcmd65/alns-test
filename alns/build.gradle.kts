plugins {
    kotlin("jvm") version "2.0.0"
    kotlin("plugin.serialization") version "1.5.31"
}

group = "com.ft.aio.template.adapter.output.web.scrippt"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(8)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"  // Ensure Kotlin targets JVM 8
    }
}