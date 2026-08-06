plugins {
    id("moira.java-conventions")
    application
}

dependencies {
    compileOnly(libs.junit)
    implementation(libs.picocli)
    implementation(libs.jna)
    testImplementation(libs.junit)
}

application {
    applicationName = "moira"
    mainClass = "moira.util.cli.MoiraUtil"
}
