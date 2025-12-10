plugins {
    id("moira.java-conventions")
}

dependencies {
    compileOnly(libs.junit)
    implementation(libs.picocli)
    testImplementation(libs.junit)
}


tasks.withType<Jar>().configureEach {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(
        configurations.runtimeClasspath.get()
            .filter { it.name.startsWith("picocli") }
            .map { if (it.isDirectory) it else zipTree(it) }
    )

}
