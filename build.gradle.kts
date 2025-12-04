plugins {
    id("testdep.java-conventions")
    id("jacoco-report-aggregation")
}

dependencies {
    jacocoAggregation(project(":agent"))
    jacocoAggregation(project(":moira"))
    jacocoAggregation(project(":runner"))
}

tasks.check {
    dependsOn(tasks.named<JacocoReport>("testCodeCoverageReport"))
}
