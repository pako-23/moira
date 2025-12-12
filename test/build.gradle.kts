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
    testImplementation(libs.junit)
}

tasks.test {
    dependsOn(project(":agent").tasks.jar)
    var agent = project(":agent").tasks.jar.flatMap { it.archiveFile }.get().asFile.absolutePath
    systemProperty("moira.agent.path", agent)
}
