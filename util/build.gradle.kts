plugins {
    id("moira.java-conventions")
}

dependencies {
    compileOnly(libs.junit)
    implementation(libs.picocli)
    implementation(libs.docker.java)
    implementation(libs.docker.java.transport.httpclient5)
    testImplementation(libs.junit)
}


tasks.withType<Jar>().configureEach {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest {
        attributes(
            "Manifest-Version" to "1.0",
            "Main-Class" to "moira.util.cli.MoiraUtil"
        )
    }

    from(
        configurations.runtimeClasspath.get()
            .filter { !it.name.startsWith("junit") }
            .map { if (it.isDirectory) it else zipTree(it) }
    )

    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
}
