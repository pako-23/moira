plugins {
    id("testdep.java-conventions")
}

dependencies {
    compileOnly("junit:junit:4.13.2")
    implementation("org.ow2.asm:asm-commons:9.8")
    implementation("org.ow2.asm:asm:9.8")
    testImplementation("org.ow2.asm:asm-util:9.8")
}

tasks.named<Jar>("jar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest {
        attributes(
            "Manifest-Version" to "1.0",
            "Premain-Class" to "ch.usi.inf.moira.agent.Agent",
            "Can-Redefine-Classes" to "true",
            "Can-Retransform-Classes" to "true"
        )
    }

    from(
        configurations.runtimeClasspath.get()
            .map { if (it.isDirectory) it else zipTree(it) }
    )
}
