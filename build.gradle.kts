plugins {
    id("jacoco-report-aggregation")
    id("moira.java-conventions")
}

dependencies {
    jacocoAggregation(project(":agent")) { isTransitive = false }
    jacocoAggregation(project(":moira")) { isTransitive = false }
    jacocoAggregation(project(":util")) { isTransitive = false }
    jacocoAggregation(project(":test")) { isTransitive = false }
}

tasks.check {
    dependsOn(tasks.named<JacocoReport>("testCodeCoverageReport"))
}
