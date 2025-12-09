plugins {
    id("moira.java-conventions")
    id("jacoco-report-aggregation")
}

dependencies {
    jacocoAggregation(project(":agent"))
    jacocoAggregation(project(":moira"))
    jacocoAggregation(project(":util"))
}

tasks.check {
    dependsOn(tasks.named<JacocoReport>("testCodeCoverageReport"))
}
