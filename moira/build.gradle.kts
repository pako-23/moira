plugins {
    id("moira.java-conventions")
}

dependencies {
    compileOnly(libs.junit)
    testImplementation(libs.junit)
    testImplementation(project(":agent"))
}
