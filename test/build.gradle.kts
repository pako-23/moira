plugins {
    id("moira.java-conventions")
}

sourceSets {
    create("app")
}

dependencies {
    "appImplementation"(libs.junit)
    implementation(project(":moira"))
    implementation(project(":util"))
    testImplementation(sourceSets.named("app").get().output)
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

    val junit = configurations
            .getByName("appRuntimeClasspath")
            .files
            .filter { it.name.startsWith("junit-") }

    systemProperty(
        "app.classpath",
        (junit + sourceSets.named("app").get().output)
            .joinToString(File.pathSeparator) { it.absolutePath }
    )
}
