package moira.profiler;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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

  private List<String> makeDump() {
    final ByteArrayOutputStream output = new ByteArrayOutputStream();

    NaiveProfiler.dump(new PrintStream(output));

    return Stream.of(output.toString().split("\n"))
        .filter(line -> !line.isEmpty())
        .collect(Collectors.toList());
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

    assertThat(makeDump().size(), is(0));
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

    assertThat(makeDump().size(), is(0));
  }
}
