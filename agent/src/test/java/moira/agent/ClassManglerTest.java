package moira.agent;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockitoAnnotations;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ClassManglerTest {
  private static final String PROFILER = "moira/agent/SomeProfiler";
  @Mock private ClassVisitor classVisitorMock;
  @Mock private MethodVisitor methodVisitorMock;
  private ClassMangler mangler;

  private static final int VERSION = Opcodes.V1_8;
  private static final String OBJECT_SUPER = "java/lang/Object";
  private static final String CLASS_NAME = "com/example/Example";
  private static final String METHOD_NAME = "method";
  private static final String METHOD_DESCRIPTION = "()V";

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
    mangler = new ClassMangler(classVisitorMock, PROFILER);
  }

  @ParameterizedTest
  @ValueSource(ints = {Opcodes.ACC_ENUM, Opcodes.ACC_INTERFACE})
  public void testVisitFilteredClass(final int access) {
    mangler.visit(VERSION, access, CLASS_NAME, null, OBJECT_SUPER, null);
    verify(classVisitorMock).visit(VERSION, access, CLASS_NAME, null, OBJECT_SUPER, null);
    when(classVisitorMock.visitMethod(
            Opcodes.ACC_PUBLIC, METHOD_NAME, METHOD_DESCRIPTION, null, null))
        .thenReturn(methodVisitorMock);
    final MethodVisitor methodVisitor =
        mangler.visitMethod(Opcodes.ACC_PUBLIC, METHOD_NAME, METHOD_DESCRIPTION, null, null);
    assertThat(methodVisitorMock, sameInstance(methodVisitor));
  }

  private static Stream<Arguments> testVisitMangledClassParams() {
    return Stream.of(
        Arguments.of(Opcodes.ACC_PUBLIC, "java/lang/Object"),
        Arguments.of(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, "java/lang/Object"),
        Arguments.of(Opcodes.ACC_PUBLIC, OBJECT_SUPER),
        Arguments.of(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, OBJECT_SUPER),
        Arguments.of(Opcodes.ACC_PUBLIC, "com/example/Super"),
        Arguments.of(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, "com/example/Super"));
  }

  @ParameterizedTest
  @MethodSource("testVisitMangledClassParams")
  public void testVisitMangledClass(final int access, final String superName) {
    mangler.visit(VERSION, access, CLASS_NAME, null, superName, null);
    verify(classVisitorMock).visit(VERSION, access, CLASS_NAME, null, superName, null);

    try (MockedConstruction<FieldAccessMangler> fieldAccessMock =
        mockConstruction(
            FieldAccessMangler.class,
            (mock, context) -> {
              assertThat(context.arguments().size(), is(4));
              assertThat(context.arguments().get(0), sameInstance(methodVisitorMock));
              assertThat(context.arguments().get(1), is(PROFILER));
              assertThat(context.arguments().get(2), is(superName));
              assertThat(context.arguments().get(3), is(METHOD_NAME));
            })) {

      final List<FieldAccessMangler> fieldAccessMangler = new ArrayList<>();

      try (MockedConstruction<TestCaseMangler> testCaseMock =
          mockConstruction(
              TestCaseMangler.class,
              (mock, context) -> {
                assertThat(context.arguments().size(), is(7));
                fieldAccessMangler.add((FieldAccessMangler) context.arguments().get(0));
                assertThat(context.arguments().get(1), is(PROFILER));
                assertThat(context.arguments().get(2), is(superName));
                assertThat(context.arguments().get(3), is(Opcodes.ACC_PUBLIC));
                assertThat(context.arguments().get(4), is(CLASS_NAME));
                assertThat(context.arguments().get(5), is(METHOD_NAME));
                assertThat(context.arguments().get(6), is(METHOD_DESCRIPTION));
              })) {

        when(classVisitorMock.visitMethod(
                Opcodes.ACC_PUBLIC, METHOD_NAME, METHOD_DESCRIPTION, null, null))
            .thenReturn(methodVisitorMock);

        final MethodVisitor result =
            mangler.visitMethod(Opcodes.ACC_PUBLIC, METHOD_NAME, METHOD_DESCRIPTION, null, null);
        assertThat(result, notNullValue());
        assertThat(fieldAccessMock.constructed().size(), is(1));
        assertThat(fieldAccessMock.constructed().get(0), is(fieldAccessMangler.get(0)));
        assertThat(testCaseMock.constructed().size(), is(1));
        assertThat(testCaseMock.constructed().get(0), sameInstance(result));
      }
    }
  }

  private static Stream<Arguments> testVisitMangledClassFilteredMethodParams() {
    return Stream.concat(
        Stream.of(
                Opcodes.ACC_ABSTRACT, Opcodes.ACC_BRIDGE, Opcodes.ACC_SYNTHETIC, Opcodes.ACC_NATIVE)
            .flatMap(
                access ->
                    Stream.of("java/lang/Object", "java/lang/reflect/Proxy")
                        .map(name -> Arguments.of(access, name))),
        Stream.of(Arguments.of(Opcodes.ACC_PUBLIC, "java/lang/reflect/Proxy")));
  }

  @ParameterizedTest
  @MethodSource("testVisitMangledClassFilteredMethodParams")
  public void testVisitMangledClassFilteredMethod(final int access, final String superName) {
    mangler.visit(
        VERSION, Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, CLASS_NAME, null, superName, null);
    verify(classVisitorMock)
        .visit(
            VERSION, Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, CLASS_NAME, null, superName, null);

    when(classVisitorMock.visitMethod(access, METHOD_NAME, METHOD_DESCRIPTION, null, null))
        .thenReturn(methodVisitorMock);
    final MethodVisitor methodVisitor =
        mangler.visitMethod(access, METHOD_NAME, METHOD_DESCRIPTION, null, null);
    assertThat(methodVisitor, sameInstance(methodVisitorMock));
  }

  @Test
  public void testVisitNullMethodVisitor() {
    mangler.visit(VERSION, Opcodes.ACC_PUBLIC, CLASS_NAME, null, OBJECT_SUPER, null);
    verify(classVisitorMock)
        .visit(VERSION, Opcodes.ACC_PUBLIC, CLASS_NAME, null, OBJECT_SUPER, null);
    when(classVisitorMock.visitMethod(
            Opcodes.ACC_PUBLIC, METHOD_NAME, METHOD_DESCRIPTION, null, null))
        .thenReturn(null);
    final MethodVisitor methodVisitor =
        mangler.visitMethod(Opcodes.ACC_PUBLIC, METHOD_NAME, METHOD_DESCRIPTION, null, null);
    assertThat(methodVisitor, nullValue());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "java/lang/ClassLoader",
        "java/net/URLClassLoader",
        "java/security/SecureClassLoader"
      })
  public void testVisitSuspendedClass(final String className) {
    mangler.visit(VERSION, Opcodes.ACC_PUBLIC, className, null, OBJECT_SUPER, null);
    verify(classVisitorMock)
        .visit(VERSION, Opcodes.ACC_PUBLIC, className, null, OBJECT_SUPER, null);

    try (MockedConstruction<SuspendMangler> mocked =
        mockConstruction(
            SuspendMangler.class,
            (mock, context) -> {
              assertThat(context.arguments().size(), is(5));
              assertThat(context.arguments().get(0), sameInstance(methodVisitorMock));
              assertThat(context.arguments().get(1), is(PROFILER));
              assertThat(context.arguments().get(2), is(Opcodes.ACC_PUBLIC));
              assertThat(context.arguments().get(3), is(METHOD_NAME));
              assertThat(context.arguments().get(4), is(METHOD_DESCRIPTION));
            })) {
      when(classVisitorMock.visitMethod(
              Opcodes.ACC_PUBLIC, METHOD_NAME, METHOD_DESCRIPTION, null, null))
          .thenReturn(methodVisitorMock);
      final MethodVisitor result =
          mangler.visitMethod(Opcodes.ACC_PUBLIC, METHOD_NAME, METHOD_DESCRIPTION, null, null);
      assertThat(result, notNullValue());

      final List<SuspendMangler> constructed = mocked.constructed();
      assertThat(constructed.size(), is(1));
      assertThat(constructed.get(0), sameInstance(result));
    }
  }
}
