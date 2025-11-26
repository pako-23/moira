rootProject.name = "testdep"

pluginManagement {
    repositories {
        gradlePluginPortal()
    }

    plugins {
        id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
        id("com.diffplug.spotless") version "8.0.0"
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

include("profiler")
include("agent")
include("runner")
