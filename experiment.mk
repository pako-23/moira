ENABLE_PROFILE := $(filter yes,$(PROFILE))

.PHONY: all
all: doi-conflicts.txt obj-conflicts.txt $(if $(ENABLE_PROFILE),doi-profile.svg obj-profile.svg,)

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

obj-conflicts.txt $(if $(ENABLE_PROFILE),obj-traces.txt,): testsuite classpath
	$(JAVA_HOME)/bin/java -cp $$(cat classpath):target/classes/:target/test-classes/ \
		-javaagent:$(top_srcdir)/agent/build/libs/agent.jar \
		$(if $(ENABLE_PROFILE),-agentpath:$(top_srcdir)/experiments//lightweight-java-profiler/build-64/liblagent.so,) \
		-Xbootclasspath/a:$(top_srcdir)/agent/build/libs/agent.jar \
		-Dagent.profiler.name=ObjectProfiler \
		-Dagent.profiler.filename=obj-conflicts.txt \
		org.junit.runner.JUnitCore $$(cat testsuite | tr '\n' ' ')
	$(if $(ENABLE_PROFILE),@mv traces.txt obj-traces.txt,)

doi-conflicts.txt $(if $(ENABLE_PROFILE),doi-traces.txt,): testsuite classpath obj-conflicts.txt
	$(JAVA_HOME)/bin/java -cp $$(cat classpath):target/classes/:target/test-classes/ \
		-javaagent:$(top_srcdir)/agent/build/libs/agent.jar \
		$(if $(ENABLE_PROFILE),-agentpath:$(top_srcdir)/experiments/lightweight-java-profiler/build-64/liblagent.so,) \
		-Xbootclasspath/a:$(top_srcdir)/agent/build/libs/agent.jar \
		-Dagent.profiler.name=DOIProfiler \
		-Dagent.profiler.filename=doi-conflicts.txt \
		org.junit.runner.JUnitCore $$(cat testsuite | tr '\n' ' ')
	$(ifeq $(ENABLE_PROFILE),@mv traces.txt doi-traces.txt,)

%-profile.svg: %-traces.txt
	@$(top_srcdir)/experiments/FlameGraph/stackcollapse-ljp.awk $^ | $(top_srcdir)/experiments/FlameGraph/flamegraph.pl > $@

.PHONY: clean
clean:
	- rm -f doi-conflicts.txt
	- rm -f doi-profile.svg
	- rm -f obj-conflicts.txt
	- rm -f obj-profile.svg
	- rm -f doi-traces.txt
	- rm -f obj-traces.txt
