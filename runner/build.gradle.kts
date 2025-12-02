plugins {
    id("testdep.java-conventions")
}

dependencies {
    implementation("junit:junit:4.13.2")
    implementation("info.picocli:picocli:4.7.7")
}


tasks.withType<Jar>().configureEach {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(
        configurations.runtimeClasspath.get()
            .filter { it.name.startsWith("picocli") }
            .map { if (it.isDirectory) it else zipTree(it) }
    )

}
