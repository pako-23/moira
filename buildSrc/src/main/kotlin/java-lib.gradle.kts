plugins {
    id("java-library")
    jacoco
    id("com.diffplug.spotless")
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.7.1")
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

    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
}

spotless {
    java {
        googleJavaFormat()
        removeUnusedImports()
        importOrder()
    }
}
