ENABLE_PROFILE := $(filter yes,$(PROFILE))

all: doi-conflicts.txt obj-conflicts.txt $(if $(ENABLE_PROFILE),doi-profile.svg obj-profile.svg doi-only-profile.svg,)

define mvn_exec
@if test -f mvnw; then \
	JAVA_HOME=$(JAVA_HOME) ./mvnw $(1); \
else \
	JAVA_HOME=$(JAVA_HOME) $(MVN_HOME)/bin/mvn $(1); \
fi
endef

define java_exec
if test $$($(JAVA_HOME)/bin/java -version 2>&1 | head -n 1 | awk -F '"' '{print $$2}' | cut -d. -f1) -ge 9; then \
	$(JAVA_HOME)/bin/java -Xss2m --add-opens java.base/java.util=ALL-UNNAMED --add-exports java.base/sun.security.jca=ALL-UNNAMED $(1); \
else \
	$(JAVA_HOME)/bin/java -Xss2m $(1); \
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
	start_time="$$(date -u +%s)" ; \
	$(call java_exec,-cp $$(cat classpath):target/classes/:target/test-classes/ \
		-javaagent:$(top_srcdir)/agent/build/libs/agent.jar \
		$(if $(ENABLE_PROFILE),-agentpath:$(top_srcdir)/experiments/lightweight-java-profiler/build-64/liblagent.so,) \
		-Xbootclasspath/a:$(top_srcdir)/agent/build/libs/agent.jar \
		-Dagent.profiler.name=ObjectProfiler \
		-Dagent.profiler.filename=obj-conflicts.txt \
		org.junit.runner.JUnitCore $$(cat testsuite | tr '\n' ' ')) && \
	echo "obj-profiler: $$(expr "$$(date -u +%s)" - "$$start_time")" >> running-times
	$(if $(ENABLE_PROFILE),@mv traces.txt obj-traces.txt,)

doi-conflicts.txt $(if $(ENABLE_PROFILE),doi-traces.txt,): testsuite classpath obj-conflicts.txt
	start_time="$$(date -u +%s)" ; \
	$(JAVA_HOME)/bin/java -Xss2m -cp $$(cat classpath):target/classes/:target/test-classes/ \
		-javaagent:$(top_srcdir)/agent/build/libs/agent.jar \
		$(if $(ENABLE_PROFILE),-agentpath:$(top_srcdir)/experiments/lightweight-java-profiler/build-64/liblagent.so,) \
		-Xbootclasspath/a:$(top_srcdir)/agent/build/libs/agent.jar \
		-Dagent.profiler.name=DOIProfiler \
		-Dagent.profiler.filename=doi-conflicts.txt \
		-Dagent.profiler.filter.filename=obj-conflicts.txt \
		org.junit.runner.JUnitCore $$(cat testsuite | tr '\n' ' ') && \
	echo "doi-profiler: $$(expr "$$(date -u +%s)" - "$$start_time")" >> running-times
	$(ifeq $(ENABLE_PROFILE),@mv traces.txt doi-traces.txt,)

doi-only-conflicts.txt $(if $(ENABLE_PROFILE),doi-only-traces.txt,): testsuite classpath
	start_time="$$(date -u +%s)" ; \
	$(JAVA_HOME)/bin/java -Xss2m -cp $$(cat classpath):target/classes/:target/test-classes/ \
		-javaagent:$(top_srcdir)/agent/build/libs/agent.jar \
		$(if $(ENABLE_PROFILE),-agentpath:$(top_srcdir)/experiments/lightweight-java-profiler/build-64/liblagent.so,) \
		-Xbootclasspath/a:$(top_srcdir)/agent/build/libs/agent.jar \
		-Dagent.profiler.name=DOIProfiler \
		-Dagent.profiler.filename=doi-only-conflicts.txt \
		org.junit.runner.JUnitCore $$(cat testsuite | tr '\n' ' ') && \
	echo "doi-only-profiler: $$(expr "$$(date -u +%s)" - "$$start_time")" >> running-times
	$(ifeq $(ENABLE_PROFILE),@mv traces.txt doi-only-traces.txt,)


%-profile.svg: %-traces.txt
	@$(top_srcdir)/experiments/FlameGraph/stackcollapse-ljp.awk $^ | $(top_srcdir)/experiments/FlameGraph/flamegraph.pl > $@

.PHONY: clean
clean:
	- rm -f running-times
	- rm -f doi-conflicts.txt
	- rm -f doi-profile.svg
	- rm -f obj-conflicts.txt
	- rm -f obj-profile.svg
	- rm -f doi-traces.txt
	- rm -f obj-traces.txt
