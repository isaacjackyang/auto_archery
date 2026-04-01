buildscript {
    repositories {
        google()
        mavenCentral()
    }

    dependencies {
        // Upgrades AGP's built-in Kotlin runtime to match the Compose compiler plugin version.
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.10")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.legacy.kapt) apply false
}
