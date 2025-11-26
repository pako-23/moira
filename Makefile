EXPERIMENTS_DIR := experiments
PROFILE ?= no


.PHONY: all
all: | java-versions mvn-versions

.PHONY: clean-experiments
clean-experiments:

SUBJECTS := fastjson,alibaba/fastjson,5c6d6fd471ea1fab59f0df2dd31e0b936806780d,.,jdk8u462-b08,apache-maven-3.6.1 \
	jhipster-registry,jhipster/jhipster-registry,00db36611da5fc7aaf9d5372aa90f2465d80c0c4,.,jdk8u462-b08,apache-maven-3.6.1 \
	spring-ws-security,spring-projects/spring-ws,e8d89c9eb0929dda304174729c9c69fb29f448eb,spring-ws-security,jdk8u462-b08,apache-maven-3.6.1 \
	elastic-job-lite-core,elasticjob/elastic-job-lite,b022898ef1b8c984e17efb2a422ee45f6b13e46e,elastic-job-lite-core,jdk8u462-b08,apache-maven-3.6.1 \
	marine-api,ktuukkan/marine-api,af0003847db9ba822f67d4f1dceb8de3fe63250a,.,jdk8u462-b08,apache-maven-3.6.1 \
	http-request,kevinsawicki/http-request,2d62a3e9da726942a93cf16b6e91c0187e6c0136,lib,jdk8u462-b08,apache-maven-3.6.1 \
	aismessages,tbsalling/aismessages,7b0c4c708b6bb9a6da3d5737bcad1857ade8a931,.,jdk8u462-b08,apache-maven-3.6.1 \
	spring-ws-core,spring-projects/spring-ws,e8d89c9eb0929dda304174729c9c69fb29f448eb,spring-ws-core,jdk8u462-b08,apache-maven-3.6.1 \
	spring-data-ebean,hexagonframework/spring-data-ebean,dd11b97654982403b50dd1d5369cadad71fce410,.,jdk8u462-b08,apache-maven-3.6.1 \
	wdtk-dumpfiles,wikidata/wikidata-toolkit,20de6f7f12319f54eb962ff6e8357b3f5695d54d,wdtk-dumpfiles,jdk8u462-b08,apache-maven-3.6.1 \
	wdtk-util,wikidata/wikidata-toolkit,20de6f7f12319f54eb962ff6e8357b3f5695d54d,wdtk-util,jdk8u462-b08,apache-maven-3.6.1 \
	wildfly,wildfly/wildfly,b19048b72669fc0e96665b1b125dc1fda21f5993,naming,jdk8u462-b08,apache-maven-3.6.1 \
	guava,google/guava,8868c096cfdabbe38170b6e395369c315cfb72a1,guava-tests,jdk-24.0.2+12,apache-maven-3.9.9
comma := ,
experiment_id = $(word 1,$(subst $(comma), ,$(1)))
experiment_repo = $(word 2,$(subst $(comma), ,$(1)))
experiment_repodir = $(EXPERIMENTS_DIR)/$(word 2,$(subst /, ,$(call experiment_repo,$(1))))
experiment_commit = $(word 3,$(subst $(comma), ,$(1)))
experiment_subdir = $(if $(filter .,$(word 4,$(subst $(comma), ,$(1)))),,$(word 4,$(subst $(comma), ,$(1)))/)
experiment_java = $(EXPERIMENTS_DIR)/$(word 5,$(subst $(comma), ,$(1)))
experiment_mvn = $(EXPERIMENTS_DIR)/$(word 6,$(subst $(comma), ,$(1)))

define experiment =

ifndef $(subst /,-,$(call experiment_repodir,$(1)))_REPO
$(subst /,-,$(call experiment_repodir,$(1)))_REPO := 1

$(call experiment_repodir,$(1)):
	git clone --quiet https://github.com/$(word 2,$(subst $(comma), ,$(1))) $$@ && \
	cd $$@ && git -c advice.detachedHead=false checkout $(call experiment_commit,$(1))
endif

$(call experiment_repodir,$(1))/$(call experiment_subdir,$(1))Makefile: | $(call experiment_repodir,$(1))
	@printf "top_srcdir = $(PWD)\n" > $$@
	@printf "JAVA_HOME = $(PWD)/$(call experiment_java,$(1))\n" >> $$@
	@printf "MVN_HOME = $(PWD)/$(call experiment_mvn,$(1))\n" >> $$@
	@printf "include $(PWD)/experiment.mk\n" >> $$@

.PHONY: run-$(call experiment_id,$(1))
run-$(call experiment_id,$(1)): $(call experiment_repodir,$(1))/$(call experiment_subdir,$(1))Makefile \
	agent/build/libs/agent.jar \
	$(if $(filter yes,$(PROFILE)),$(EXPERIMENTS_DIR)/lightweight-java-profiler/build-64/liblagent.so,) | \
	$(call experiment_repodir,$(1)) \
	$(call experiment_java,$(1)) \
	$(call experiment_mvn,$(1)) \
	$(if $(filter yes,$(PROFILE)),$(EXPERIMENTS_DIR)/FlameGraph,)
	$(MAKE) -C $(call experiment_repodir,$(1))/$(call experiment_subdir,$(1)) PROFILE=$(PROFILE) all

all: run-$(word 1,$(subst $(comma), ,$(1)))

.PHONY: clean-$(word 1,$(subst $(comma), ,$(1)))
clean-$(word 1,$(subst $(comma), ,$(1))):
	$(MAKE) -C $(call experiment_repodir,$(1))/$(call experiment_subdir,$(1)) clean

clean-experiments: clean-$(word 1,$(subst $(comma), ,$(1)))
endef

$(foreach s,$(SUBJECTS),$(eval $(call experiment,$(s))))


$(EXPERIMENTS_DIR):
	@mkdir $(EXPERIMENTS_DIR)

agent/build/libs/agent.jar:
	./gradlew agent:build

$(EXPERIMENTS_DIR)/jdk8u462-b08: | $(EXPERIMENTS_DIR)
	@wget -q https://github.com/adoptium/temurin8-binaries/releases/download/jdk8u462-b08/OpenJDK8U-jdk_x64_linux_hotspot_8u462b08.tar.gz -P /tmp && \
	tar xf /tmp/OpenJDK8U-jdk_x64_linux_hotspot_8u462b08.tar.gz -C $(EXPERIMENTS_DIR)

$(EXPERIMENTS_DIR)/jdk-24.0.2+12: | $(EXPERIMENTS_DIR)
	@wget -q https://github.com/adoptium/temurin24-binaries/releases/download/jdk-24.0.2%2B12/OpenJDK24U-jdk_x64_linux_hotspot_24.0.2_12.tar.gz -P /tmp && \
	tar xf /tmp/OpenJDK24U-jdk_x64_linux_hotspot_24.0.2_12.tar.gz -C $(EXPERIMENTS_DIR)

.PHONY: java-versions
java-versions: | $(EXPERIMENTS_DIR)/jdk8u462-b08

define mvn_version =
$(EXPERIMENTS_DIR)/apache-maven-$(1): | $(EXPERIMENTS_DIR)
	@wget -q https://archive.apache.org/dist/maven/maven-$(word 1,$(subst ., ,$(1)))/$(1)/binaries/apache-maven-$(1)-bin.tar.gz -P /tmp && \
	tar xf /tmp/apache-maven-$(1)-bin.tar.gz -C $(EXPERIMENTS_DIR)

.PHONY: mvn-versions
mvn-versions: $(EXPERIMENTS_DIR)/apache-maven-$(1)
endef

.PHONY: mvn-versions
mvn-versions:

$(eval $(call mvn_version,3.6.1))
$(eval $(call mvn_version,3.9.9))


.PHONY: clean
clean:
	rm -rf $(EXPERIMENTS_DIR)

ifeq ($(PROFILE),yes)
$(EXPERIMENcompTS_DIR)/lightweight-java-profiler: | $(EXPERIMENTS_DIR)
	@git clone --quiet https://github.com/yinheli/lightweight-java-profiler.git $@

$(EXPERIMENTS_DIR)/lightweight-java-profiler/build-64/liblagent.so: | $(EXPERIMENTS_DIR)/lightweight-java-profiler $(EXPERIMENTS_DIR)/jdk8u462-b08
	cd $(EXPERIMENTS_DIR)/lightweight-java-profiler && $(MAKE) BITS=64 INCLUDES='-I$(PWD)/$(EXPERIMENTS_DIR)/jdk8u462-b08/include -I$(PWD)/$(EXPERIMENTS_DIR)/jdk8u462-b08/include/linux' all

$(EXPERIMENTS_DIR)/FlameGraph: | $(EXPERIMENTS_DIR)
	@git clone --quiet https://github.com/brendangregg/FlameGraph.git $@
endif
