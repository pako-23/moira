plugins {
    id("testdep.java-conventions")
}

dependencies {
    implementation("org.ow2.asm:asm-commons:9.8")
    implementation("org.ow2.asm:asm:9.8")
    testImplementation("org.ow2.asm:asm-util:9.8")
}

tasks.withType<Jar>().configureEach {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest {
        attributes(
            "Manifest-Version" to "1.0",
            "Premain-Class" to "ch.usi.inf.agent.Agent",
            "Can-Redefine-Classes" to "true",
            "Can-Retransform-Classes" to "true"
        )
    }

    from(
        configurations.runtimeClasspath.get()
            .map { if (it.isDirectory) it else zipTree(it) }
    )
}
