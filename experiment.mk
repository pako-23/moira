runs := 1 2 3 4 5 6 7 8 9 10
experiments := obj doi doi-only
experiment_files := $(foreach exp,$(experiments),$(exp)-conflicts.txt $(exp)-verified.txt)

all: $(foreach run,$(runs),$(foreach file,$(experiment_files),run-$(run)/$(file)))

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

%obj-conflicts.txt: testsuite classpath
	mkdir -p $(dir $@) ; \
	start_time="$$(date -u +%s)" ; \
	$(call java_exec,-cp $$(cat classpath):target/classes/:target/test-classes/:$(top_srcdir)/moira/build/libs/moira.jar \
		-javaagent:$(top_srcdir)/agent/build/libs/agent.jar \
		-Xbootclasspath/a:$(top_srcdir)/agent/build/libs/agent.jar \
		-Dmoira.profiler.name=ObjectProfiler \
		-Dmoira.profiler.filename=$@ \
		moira.Moira $$(cat testsuite | tr '\n' ' ')) && \
	echo "obj-profiler: $$(expr "$$(date -u +%s)" - "$$start_time")" >> running-times

%doi-conflicts.txt: %obj-conflicts.txt testsuite classpath
	mkdir -p $(dir $@) ; \
	start_time="$$(date -u +%s)" ; \
	$(call java_exec,-cp $$(cat classpath):target/classes/:target/test-classes/:$(top_srcdir)/moira/build/libs/moira.jar \
		-javaagent:$(top_srcdir)/agent/build/libs/agent.jar \
		-Xbootclasspath/a:$(top_srcdir)/agent/build/libs/agent.jar \
		-Dmoira.profiler.name=DOIProfiler \
		-Dmoira.profiler.filename=$@ \
		-Dmoira.profiler.filter.filename=$< \
		moira.Moira $$(cat testsuite | tr '\n' ' ')) && \
	echo "doi-profiler: $$(expr "$$(date -u +%s)" - "$$start_time")" >> running-times

%doi-only-conflicts.txt: testsuite classpath
	mkdir -p $(dir $@) ; \
	start_time="$$(date -u +%s)" ; \
	$(call java_exec,-cp $$(cat classpath):target/classes/:target/test-classes/:$(top_srcdir)/moira/build/libs/moira.jar \
		-javaagent:$(top_srcdir)/agent/build/libs/agent.jar \
		-Xbootclasspath/a:$(top_srcdir)/agent/build/libs/agent.jar \
		-Dmoira.profiler.name=DOIProfiler \
		-Dmoira.profiler.filename=$@ \
		moira.Moira $$(cat testsuite | tr '\n' ' ')) && \
	echo "doi-only-profiler: $$(expr "$$(date -u +%s)" - "$$start_time")" >> running-times

%-verified.txt: %-conflicts.txt
	already_done="$$(find . -name '*-verified.txt' -print0 | grep -vFz "$@" | xargs -0 sort | uniq)" ; \
	while read -r pair; do \
		if echo "$$already_done" | grep -cFq "$$pair"; then \
			echo "$$already_done" | grep -F "$$pair" >> $@; \
			continue; \
		fi ; \
		first="$$(echo "$$pair" | cut -f1 -d' ')"; \
		second="$$(echo "$$pair" | cut -f2 -d' ')"; \
		ordered="$$($(call java_exec,-cp $$(cat classpath):target/classes/:target/test-classes/:$(top_srcdir)/util/build/libs/util.jar moira.util.cli.MoiraUtil verify $$first $$second) | grep OK)" ; \
		reversed="$$($(call java_exec,-cp $$(cat classpath):target/classes/:target/test-classes/:$(top_srcdir)/util/build/libs/util.jar moira.util.cli.MoiraUtil verify $$second $$first) | grep OK)" ; \
		if test "$$ordered" = "$$reversed"; then \
			echo "$$pair -> INVALID" >> $@; \
		else \
			echo "$$pair -> VALID" >> $@; \
		fi; \
	done < $^


.PHONY: clean
clean:
	- rm -f running-times
	- rm -f $(experiment_files)
	- rm -rf $(foreach run,$(runs),run-$(run))
	- rm -f $(foreach exp,$(experiments),$(exp)-profile.svg $(exp)-traces.txt $(exp)-conflicts.txt)

.PHONY: profile
profile: $(foreach exp,$(experiments),$(exp)-profile.svg $(exp)-traces.txt $(exp)-conflicts.txt)

%-profile.svg: %-traces.txt
	@$(top_srcdir)/experiments/FlameGraph/stackcollapse-ljp.awk $^ | $(top_srcdir)/experiments/FlameGraph/flamegraph.pl > $@

obj-conflicts.txt obj-traces.txt &: testsuite classpath
	$(call java_exec,-cp $$(cat classpath):target/classes/:target/test-classes/:$(top_srcdir)/moira/build/libs/moira.jar \
		-agentpath:$(top_srcdir)/experiments/lightweight-java-profiler/$(shell basename $(JAVA_HOME))/liblagent.so=file=obj-traces.txt \
		-javaagent:$(top_srcdir)/agent/build/libs/agent.jar \
		-Xbootclasspath/a:$(top_srcdir)/agent/build/libs/agent.jar \
		-Dmoira.profiler.name=ObjectProfiler \
		-Dmoira.profiler.filename=obj-conflicts.txt \
		moira.Moira $$(cat testsuite | tr '\n' ' '))

doi-conflicts.txt doi-traces.txt &: obj-conflicts.txt testsuite classpath
	$(call java_exec,-cp $$(cat classpath):target/classes/:target/test-classes/:$(top_srcdir)/moira/build/libs/moira.jar \
		-agentpath:$(top_srcdir)/experiments/lightweight-java-profiler/$(shell basename $(JAVA_HOME))/liblagent.so=file=doi-traces.txt \
		-javaagent:$(top_srcdir)/agent/build/libs/agent.jar \
		-Xbootclasspath/a:$(top_srcdir)/agent/build/libs/agent.jar \
		-Dmoira.profiler.name=DOIProfiler \
		-Dmoira.profiler.filename=doi-conflicts.txt \
		-Dmoira.profiler.filter.filename=obj-conflicts.txt \
		moira.Moira $$(cat testsuite | tr '\n' ' '))

doi-only-conflicts.txt doi-only-traces.txt &: testsuite classpath
	$(call java_exec,-cp $$(cat classpath):target/classes/:target/test-classes/:$(top_srcdir)/moira/build/libs/moira.jar \
		-agentpath:$(top_srcdir)/experiments/lightweight-java-profiler/$(shell basename $(JAVA_HOME))/liblagent.so=file=doi-only-traces.txt \
		-javaagent:$(top_srcdir)/agent/build/libs/agent.jar \
		-Xbootclasspath/a:$(top_srcdir)/agent/build/libs/agent.jar \
		-Dmoira.profiler.name=DOIProfiler \
		-Dmoira.profiler.filename=doi-only-conflicts.txt \
		moira.Moira $$(cat testsuite | tr '\n' ' '))
