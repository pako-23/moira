plugins {
    java
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }

    consistentResolution {
        useCompileClasspathVersions()
    }
}

dependencies {
    implementation(libs.junit)
}
