plugins {
    java
    id("com.diffplug.spotless")
    id("jacoco-report-aggregation")
    jacoco
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.7.1")
    testImplementation("org.mockito:mockito-inline:4.2.0")
    testImplementation("org.mockito:mockito-junit-jupiter:4.2.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

jacoco {
    toolVersion = "0.8.13"
}

tasks.named<Test>("test") {
    useJUnitPlatform()

    testLogging {
        events("passed")
    }
}

tasks.jacocoTestReport {
    enabled = false
}

spotless {
    java {
        googleJavaFormat()
        removeUnusedImports()
        importOrder()
    }
}
