// Top-level build file where you can add configuration options common to all sub-projects/modules.

// The R8 code shrinker that ships with Android Gradle Plugin 8.5.2 has a bug that makes the compiled code depend on
// the number of CPU cores of the build machine, which breaks reproducible builds (F-Droid compares its own build with
// the released APK byte for byte). R8 8.8 and newer fixed it, so a newer R8 is pinned here.
// See https://f-droid.org/docs/Reproducible_Builds/
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools:r8:8.8.34")
    }
}

plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}
