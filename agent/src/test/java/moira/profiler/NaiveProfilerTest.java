package moira.profiler;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class NaiveProfilerTest {

  private static final String FIELD = "testField";
  private static final Object OBJECT = new Object();
  private static final Object[] ARRAY = new Object[10];
  private static final int INDEX = 0;
  private static final String[] TEST_NAME =
      new String[] {"TestSnapshotMyTest", "TestSnapshotMyTest2"};

  @BeforeEach
  public void setup() {
    NaiveProfiler.setup();
  }

  private List<String> makeDump(String fileName) {
    List<String> lines = null;
    fileName = "test-snapshot-prof-" + fileName;

    try {
      File file = new File(fileName);
      file.deleteOnExit();
      NaiveProfiler.dump(fileName);
      lines =
          Files.readAllLines(Paths.get(fileName)).stream().sorted().collect(Collectors.toList());
    } catch (IOException e) {
      fail(e.getMessage());
    }

    return lines;
  }

  @Test
  public void testObjectDependencyOtherField() {
    NaiveProfiler.enterTestMethod(TEST_NAME[0]);
    NaiveProfiler.enable();
    NaiveProfiler.writeObjectField(OBJECT, FIELD);
    NaiveProfiler.disable();
    NaiveProfiler.exitTestMethod();

    NaiveProfiler.enterTestMethod(TEST_NAME[1]);
    NaiveProfiler.enable();
    NaiveProfiler.readObjectField(OBJECT, FIELD + "o");
    NaiveProfiler.disable();
    NaiveProfiler.exitTestMethod();

    assertThat(makeDump("object-field-dependency").size(), is(0));
  }

  @Test
  public void testArrayDependencyOtherIndex() {
    NaiveProfiler.enterTestMethod(TEST_NAME[0]);
    NaiveProfiler.enable();
    NaiveProfiler.writeArrayElement(ARRAY, INDEX);
    NaiveProfiler.disable();
    NaiveProfiler.exitTestMethod();

    NaiveProfiler.enterTestMethod(TEST_NAME[1]);
    NaiveProfiler.enable();
    NaiveProfiler.readArrayElement(ARRAY, INDEX + 1);
    NaiveProfiler.disable();
    NaiveProfiler.exitTestMethod();

    assertThat(makeDump("array-field-dependency").size(), is(0));
  }
}
