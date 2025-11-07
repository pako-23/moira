SUBJECTS := jhipster-registry

EXPERIMENTS_DIR := experiments

$(EXPERIMENTS_DIR)/jhipster-registry: REPO=jhipster/jhipster-registry
$(EXPERIMENTS_DIR)/jhipster-registry: REPODIR=jhipster-registry
$(EXPERIMENTS_DIR)/jhipster-registry: COMMIT=00db36611da5fc7aaf9d5372aa90f2465d80c0c4
$(EXPERIMENTS_DIR)/jhipster-registry: SUBDIR=.
$(EXPERIMENTS_DIR)/jhipster-registry: JAVA_HOME=jdk8u462-b08
$(EXPERIMENTS_DIR)/jhipster-registry: MVN_HOME=apache-maven-3.6.1

.PHONY: all
all: | java-versions mvn-versions

.PHONY: clean-experiments
clean-experiments:

$(EXPERIMENTS_DIR):
	@mkdir $(EXPERIMENTS_DIR)

agent/build/libs/agent.jar: $(shell find agent/ -name *.java) $(shell find profiler/ -name *.java)
	./gradlew agent:build

define experiment =
$(EXPERIMENTS_DIR)/$(1):
	@git clone --quiet https://github.com/$$(REPO) $$@ && \
	printf "top_srcdir = $(PWD)\nJAVA_HOME = $(PWD)/$(EXPERIMENTS_DIR)/$$(JAVA_HOME)\nMVN_HOME = $(PWD)/$(EXPERIMENTS_DIR)/$$(MVN_HOME)\ninclude ../../experiment.mk\n" > $(EXPERIMENTS_DIR)/$(1)/Makefile && \
	cd $$@ && git -c advice.detachedHead=false checkout $$(COMMIT)

.PHONY: experiment-$(1)
experiment-$(1): agent/build/libs/agent.jar $(EXPERIMENTS_DIR)/lightweight-java-profiler/build-64/liblagent.so | $(EXPERIMENTS_DIR)/$(1) $(EXPERIMENTS_DIR)/FlameGraph
	$(MAKE) -C $(EXPERIMENTS_DIR)/$(1) all

all: experiment-$(1)

.PHONY: clean-$(1)
clean-$(1):
	$(MAKE) -C $(EXPERIMENTS_DIR)/$(1) clean

clean-experiments: clean-$(1)
endef

$(foreach s,$(SUBJECTS),$(eval $(call experiment,$(s))))

$(EXPERIMENTS_DIR)/jdk8u462-b08: | $(EXPERIMENTS_DIR)
	@wget -q https://github.com/adoptium/temurin8-binaries/releases/download/jdk8u462-b08/OpenJDK8U-jdk_x64_linux_hotspot_8u462b08.tar.gz -P /tmp && \
	tar xf /tmp/OpenJDK8U-jdk_x64_linux_hotspot_8u462b08.tar.gz -C $(EXPERIMENTS_DIR)

.PHONY: java-versions
java-versions: | $(EXPERIMENTS_DIR)/jdk8u462-b08

$(EXPERIMENTS_DIR)/apache-maven-3.6.1: | $(EXPERIMENTS_DIR)
	@wget -q https://archive.apache.org/dist/maven/maven-3/3.6.1/binaries/apache-maven-3.6.1-bin.tar.gz -P /tmp && \
	tar xf /tmp/apache-maven-3.6.1-bin.tar.gz -C $(EXPERIMENTS_DIR)

.PHONY: mvn-versions
mvn-versions: $(EXPERIMENTS_DIR)/apache-maven-3.6.1

.PHONY: clean
clean:
	rm -rf $(EXPERIMENTS_DIR)

$(EXPERIMENTS_DIR)/lightweight-java-profiler: | $(EXPERIMENTS_DIR)
	@git clone --quiet https://github.com/yinheli/lightweight-java-profiler.git $@

$(EXPERIMENTS_DIR)/lightweight-java-profiler/build-64/liblagent.so: | $(EXPERIMENTS_DIR)/lightweight-java-profiler $(EXPERIMENTS_DIR)/jdk8u462-b08
	cd $(EXPERIMENTS_DIR)/lightweight-java-profiler && $(MAKE) BITS=64 INCLUDES='-I$(PWD)/$(EXPERIMENTS_DIR)/jdk8u462-b08/include -I$(PWD)/$(EXPERIMENTS_DIR)/jdk8u462-b08/include/linux' all

$(EXPERIMENTS_DIR)/FlameGraph: | $(EXPERIMENTS_DIR)
	@git clone --quiet https://github.com/brendangregg/FlameGraph.git $@
