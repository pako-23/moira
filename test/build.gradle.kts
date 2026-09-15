plugins {
    id("moira.java-conventions")
}

sourceSets {
    create("app")
}

dependencies {
    implementation(project(":moira"))
    implementation(project(":testapp"))
    implementation(libs.junit)
    testImplementation(libs.picocli)
}

tasks.test {
    dependsOn(project(":agent").tasks.jar)

    systemProperty(
        "moira.agent.path",
        project(":agent")
            .tasks
            .jar
            .flatMap { it.archiveFile }
            .get()
            .asFile
            .absolutePath
    )
}
