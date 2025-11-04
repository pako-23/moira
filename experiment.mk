.PHONY: all
all: conflicts

define mvn_exec
@if test -f mvnw; then \
	JAVA_HOME=$(JAVA_HOME) ./mvnw $(1); \
else \
	JAVA_HOME=$(JAVA_HOME) $$(MVN_HOME)/bin/mvn $(1); \
fi
endef

target:
	$(call mvn_exec,compile test-compile)

classpath: | target
	$(call mvn_exec,dependency:build-classpath -DincludeScope=test -Dmdep.outputFile=classpath)

testsuite: | target
	$(call mvn_exec,test)
	@find target/ -name 'TEST*.xml' -print0 | xargs -0 sed -n -e 's/^<testsuite .* name="\([^"]*\)".*$$/\1/p' | sort -u > testsuite

conflicts: testsuite classpath
	$(JAVA_HOME)/bin/java -cp $$(cat classpath):target/classes/:target/test-classes/ \
		-javaagent:$(top_srcdir)/agent/build/libs/agent.jar=NullProfiler \
		-Xbootclasspath/a:$(top_srcdir)/agent/build/libs/agent.jar:$(top_srcdir)/profiler/build/libs/profiler.jar \
		org.junit.runner.JUnitCore $$(cat testsuite | tr '\n' ' ')
