package moira.agent;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockitoAnnotations;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ClassManglerTest {
  @Mock private ClassVisitor classVisitorMock;
  @Mock private MethodVisitor methodVisitorMock;
  @Mock private ManglerConfig configMock;
  private ClassMangler mangler;

  private static final int VERSION = Opcodes.V1_8;
  private static final String OBJECT_SUPER = "java/lang/Object";
  private static final String CLASS_NAME = "com/example/Example";
  private static final String METHOD_NAME = "method";
  private static final String METHOD_DESC = "()V";
  private static final String PROFILER = "moira/profiler/SomeProfiler";

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
    mangler = new ClassMangler(classVisitorMock, configMock);
    when(configMock.getProfiler()).thenReturn(PROFILER);
  }

  @Test
  public void testSuspendedClass() {
    when(configMock.isSuspended(CLASS_NAME)).thenReturn(true);
    mangler.visit(VERSION, Opcodes.ACC_PUBLIC, CLASS_NAME, null, OBJECT_SUPER, null);
    verify(classVisitorMock)
        .visit(VERSION, Opcodes.ACC_PUBLIC, CLASS_NAME, null, OBJECT_SUPER, null);

    when(classVisitorMock.visitMethod(Opcodes.ACC_PUBLIC, METHOD_NAME, METHOD_DESC, null, null))
        .thenReturn(methodVisitorMock);

    try (final MockedConstruction<SuspendMangler> suspendMangler =
        mockConstruction(
            SuspendMangler.class,
            (mock, context) -> {
              assertThat(context.arguments().size(), is(5));
              assertThat(context.arguments().get(0), sameInstance(methodVisitorMock));
              assertThat(context.arguments().get(1), is(PROFILER));
              assertThat(context.arguments().get(2), is(Opcodes.ACC_PUBLIC));
              assertThat(context.arguments().get(3), is(METHOD_NAME));
              assertThat(context.arguments().get(4), is(METHOD_DESC));
            })) {
      final MethodVisitor result =
          mangler.visitMethod(Opcodes.ACC_PUBLIC, METHOD_NAME, METHOD_DESC, null, null);
      assertThat(result, notNullValue());
      final List<SuspendMangler> constructed = suspendMangler.constructed();
      assertThat(constructed.size(), is(1));
      assertThat(constructed.get(0), sameInstance(result));
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {Opcodes.ACC_ENUM, Opcodes.ACC_INTERFACE, Opcodes.ACC_SYNTHETIC})
  public void testFilteredClass(final int access) {
    when(configMock.isSuspended(CLASS_NAME)).thenReturn(false);
    when(configMock.shouldMangle(CLASS_NAME)).thenReturn(true);
    mangler.visit(VERSION, access, CLASS_NAME, null, OBJECT_SUPER, null);
    verify(classVisitorMock).visit(VERSION, access, CLASS_NAME, null, OBJECT_SUPER, null);

    when(classVisitorMock.visitMethod(Opcodes.ACC_PUBLIC, METHOD_NAME, METHOD_DESC, null, null))
        .thenReturn(methodVisitorMock);
    assertThat(
        mangler.visitMethod(Opcodes.ACC_PUBLIC, METHOD_NAME, METHOD_DESC, null, null),
        sameInstance(methodVisitorMock));
  }

  @Test
  public void testShouldNotMangleClass() {
    when(configMock.isSuspended(CLASS_NAME)).thenReturn(false);
    when(configMock.shouldMangle(CLASS_NAME)).thenReturn(false);
    mangler.visit(VERSION, Opcodes.ACC_PUBLIC, CLASS_NAME, null, OBJECT_SUPER, null);
    verify(classVisitorMock)
        .visit(VERSION, Opcodes.ACC_PUBLIC, CLASS_NAME, null, OBJECT_SUPER, null);

    when(classVisitorMock.visitMethod(Opcodes.ACC_PUBLIC, METHOD_NAME, METHOD_DESC, null, null))
        .thenReturn(methodVisitorMock);
    assertThat(
        mangler.visitMethod(Opcodes.ACC_PUBLIC, METHOD_NAME, METHOD_DESC, null, null),
        sameInstance(methodVisitorMock));
  }

  @Test
  public void testProxySuperClass() {
    when(configMock.isSuspended(CLASS_NAME)).thenReturn(false);
    when(configMock.shouldMangle(CLASS_NAME)).thenReturn(true);
    mangler.visit(VERSION, Opcodes.ACC_PUBLIC, CLASS_NAME, null, "java/lang/reflect/Proxy", null);
    verify(classVisitorMock)
        .visit(VERSION, Opcodes.ACC_PUBLIC, CLASS_NAME, null, "java/lang/reflect/Proxy", null);

    when(classVisitorMock.visitMethod(Opcodes.ACC_PUBLIC, METHOD_NAME, METHOD_DESC, null, null))
        .thenReturn(methodVisitorMock);
    assertThat(
        mangler.visitMethod(Opcodes.ACC_PUBLIC, METHOD_NAME, METHOD_DESC, null, null),
        sameInstance(methodVisitorMock));
  }

  @Test
  public void testMangledClass() {
    when(configMock.isSuspended(CLASS_NAME)).thenReturn(false);
    when(configMock.shouldMangle(CLASS_NAME)).thenReturn(true);
    mangler.visit(VERSION, Opcodes.ACC_PUBLIC, CLASS_NAME, null, OBJECT_SUPER, null);
    verify(classVisitorMock)
        .visit(VERSION, Opcodes.ACC_PUBLIC, CLASS_NAME, null, OBJECT_SUPER, null);

    when(classVisitorMock.visitMethod(Opcodes.ACC_PUBLIC, METHOD_NAME, METHOD_DESC, null, null))
        .thenReturn(methodVisitorMock);

    try (final MockedConstruction<FieldAccessMangler> fieldAccessMangler =
            mockConstruction(
                FieldAccessMangler.class,
                (mock, context) -> {
                  assertThat(context.arguments().size(), is(4));
                  assertThat(context.arguments().get(0), sameInstance(methodVisitorMock));
                  assertThat(context.arguments().get(1), is(PROFILER));
                  assertThat(context.arguments().get(2), is(OBJECT_SUPER));
                  assertThat(context.arguments().get(3), is(METHOD_NAME));
                });
        final MockedConstruction<TestCaseMangler> testCaseMangler =
            mockConstruction(
                TestCaseMangler.class,
                (mock, context) -> {
                  assertThat(context.arguments().size(), is(7));
                  assertThat(
                      context.arguments().get(0),
                      sameInstance(fieldAccessMangler.constructed().get(0)));
                  assertThat(context.arguments().get(1), is(PROFILER));
                  assertThat(context.arguments().get(2), is(OBJECT_SUPER));
                  assertThat(context.arguments().get(3), is(Opcodes.ACC_PUBLIC));
                  assertThat(context.arguments().get(4), is(CLASS_NAME));
                  assertThat(context.arguments().get(5), is(METHOD_NAME));
                  assertThat(context.arguments().get(6), is(METHOD_DESC));
                })) {
      MethodVisitor result =
          mangler.visitMethod(Opcodes.ACC_PUBLIC, METHOD_NAME, METHOD_DESC, null, null);
      assertThat(result, notNullValue());
      assertThat(fieldAccessMangler.constructed().size(), is(1));
      List<TestCaseMangler> testCases = testCaseMangler.constructed();
      assertThat(testCases.size(), is(1));
      assertThat(testCases.get(0), sameInstance(result));
    }
  }

  @Test
  public void testNullMethodVisitor() {
    when(configMock.isSuspended(CLASS_NAME)).thenReturn(false);
    when(configMock.shouldMangle(CLASS_NAME)).thenReturn(true);
    mangler.visit(VERSION, Opcodes.ACC_PUBLIC, CLASS_NAME, null, OBJECT_SUPER, null);
    verify(classVisitorMock)
        .visit(VERSION, Opcodes.ACC_PUBLIC, CLASS_NAME, null, OBJECT_SUPER, null);

    when(classVisitorMock.visitMethod(Opcodes.ACC_PUBLIC, METHOD_NAME, METHOD_DESC, null, null))
        .thenReturn(null);
    assertThat(
        mangler.visitMethod(Opcodes.ACC_PUBLIC, METHOD_NAME, METHOD_DESC, null, null), nullValue());
  }

  @ParameterizedTest
  @ValueSource(
      ints = {Opcodes.ACC_ABSTRACT, Opcodes.ACC_BRIDGE, Opcodes.ACC_SYNTHETIC, Opcodes.ACC_NATIVE})
  public void testMethodWithFilteredAccess(final int access) {
    when(configMock.isSuspended(CLASS_NAME)).thenReturn(false);
    when(configMock.shouldMangle(CLASS_NAME)).thenReturn(true);
    mangler.visit(VERSION, Opcodes.ACC_PUBLIC, CLASS_NAME, null, OBJECT_SUPER, null);
    verify(classVisitorMock)
        .visit(VERSION, Opcodes.ACC_PUBLIC, CLASS_NAME, null, OBJECT_SUPER, null);

    when(classVisitorMock.visitMethod(access, METHOD_NAME, METHOD_DESC, null, null))
        .thenReturn(methodVisitorMock);
    assertThat(
        mangler.visitMethod(access, METHOD_NAME, METHOD_DESC, null, null),
        sameInstance(methodVisitorMock));
  }
}
