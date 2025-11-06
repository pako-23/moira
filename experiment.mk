.PHONY: all
all: doi-conflicts.txt doi-profile.svg obj-conflicts.txt obj-profile.svg

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

obj-conflicts.txt obj-traces.txt: testsuite classpath
	@$(JAVA_HOME)/bin/java -cp $$(cat classpath):target/classes/:target/test-classes/ \
		-javaagent:$(top_srcdir)/agent/build/libs/agent.jar=ObjectProfiler \
		-agentpath:$(top_srcdir)/experiments//lightweight-java-profiler/build-64/liblagent.so \
		-Xbootclasspath/a:$(top_srcdir)/agent/build/libs/agent.jar:$(top_srcdir)/profiler/build/libs/profiler.jar \
		org.junit.runner.JUnitCore $$(cat testsuite | tr '\n' ' ')
	@mv conflicts obj-conflicts.txt
	@mv traces.txt obj-traces.txt

doi-conflicts.txt doi-traces.txt: testsuite classpath
	@$(JAVA_HOME)/bin/java -cp $$(cat classpath):target/classes/:target/test-classes/ \
		-javaagent:$(top_srcdir)/agent/build/libs/agent.jar=DOIProfiler \
		-agentpath:$(top_srcdir)/experiments//lightweight-java-profiler/build-64/liblagent.so \
		-Xbootclasspath/a:$(top_srcdir)/agent/build/libs/agent.jar:$(top_srcdir)/profiler/build/libs/profiler.jar \
		org.junit.runner.JUnitCore $$(cat testsuite | tr '\n' ' ')
	@mv conflicts doi-conflicts.txt
	@mv traces.txt doi-traces.txt

%-profile.svg: %-traces.txt
	@$(top_srcdir)/experiments/FlameGraph/stackcollapse-ljp.awk $^ | $(top_srcdir)/experiments/FlameGraph/flamegraph.pl > $@

.PHONY: clean
clean:
	rm -f doi-conflicts.txt doi-profile.svg obj-conflicts.txt obj-profile.svg
