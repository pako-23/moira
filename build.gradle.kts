plugins {
    id("testdep.java-conventions")
    id("jacoco-report-aggregation")
}

dependencies {
    jacocoAggregation(project(":agent"))
    jacocoAggregation(project(":profiler"))
}

tasks.check {
    dependsOn(tasks.named<JacocoReport>("testCodeCoverageReport"))
}
