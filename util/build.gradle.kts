plugins {
    id("moira.java-conventions")
    application
}

dependencies {
    compileOnly(libs.junit)
    implementation(libs.picocli)
    implementation(libs.jna)
    testImplementation(project(":testapp"))
    testImplementation(libs.junit)
}

val agentJar = rootProject.project(":agent").tasks.named<Jar>("jar").flatMap { it.archiveFile }

tasks.test {
    dependsOn(agentJar)
}

distributions {
    main {
        contents {
            from(agentJar) {
                into("agent")
            }
        }
    }
}

application {
    applicationName = "moira"
    mainClass = "moira.util.cli.MoiraUtil"
}
