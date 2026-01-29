# Moira

Moira is a dynamic analysis tool designed to uncover hidden
dependencies within JUnit test suites. By leveraging a Java Agent,
Moira monitors execution in real-time to identify shared state and
side effects that cause tests to pass or fail depending on their
execution order.

---

# Getting Started

## Prerequisites
To build Moira, you need to set up an environment with the following
tools:

  * JDK 21 or higher

## Installation

Clone the repository and build Moira as follows:


``` bash
git clone  https://github.com/pako-23/moira.git
cd moira
./gradlew build
```

## Testing

The tool comes with an exhaustive test suite to verify the correct
functioning of the tool.  The test suite can be executed as follows:


``` bash
./gradlew check
```

The build process makes also available some metrics such as the
coverage.  The coverage report in HTML format is available at
`build/reports/jacoco/testCodeCoverageReport/html/index.html`.

## Usage

Once the agent is built, it can be used to detect dependeices among
tests.  The general command structure to execute a test suite with
Moira is the following:


``` bash
java -javaagent:agent/build/libs/agent.jar \
    -Xbootclasspath/a:agent/build/libs/agent.jar \
    -cp <test-suite-classpaht>:agent/build/libs/agent.jar \
    -Dmoira.profiler.name=OnlineProfiler \
    moira.Moira TEST_CLASS...
```

The execution of Moira can be configured using the following
variables:

| Variable                         | Description                                                                                                               |
|:---------------------------------|:-------------------------------------------------------------------------------------------------------------------------:|
| `moira.profiler.name`            | The name of the profiler. The possible values are `OnlineProfiler`, `NaiveProfiler`, `ObjectProfiler` and `NullProfiler`. |
| `moira.profiler.filename`        | A path to a file where all dependent test pairs will be written.                                                          |
| `moira.profiler.filter.filename` | A path to a file containing previously computed dependencies to filter tests to instrument.                               |
