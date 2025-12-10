plugins {
    id("moira.java-conventions")
}

dependencies {
    implementation(libs.asm.commons)
    implementation(libs.asm)
    testImplementation(libs.asm.util)
}

tasks.named<Jar>("jar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest {
        attributes(
            "Manifest-Version" to "1.0",
            "Premain-Class" to "moira.agent.Agent",
            "Can-Redefine-Classes" to "true",
            "Can-Retransform-Classes" to "true"
        )
    }

    from(
        configurations.runtimeClasspath.get()
            .map { if (it.isDirectory) it else zipTree(it) }
    )
}
