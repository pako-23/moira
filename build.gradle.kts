plugins {
    id("jacoco-report-aggregation")
    id("moira.java-conventions")
}

dependencies {
    jacocoAggregation(project(":agent"))
    jacocoAggregation(project(":moira"))
    jacocoAggregation(project(":util"))
}

tasks.check {
    dependsOn(tasks.named<JacocoReport>("testCodeCoverageReport"))
}
