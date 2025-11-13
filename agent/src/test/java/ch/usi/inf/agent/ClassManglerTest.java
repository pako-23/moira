package ch.usi.inf.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    mangler = new ClassMangler(classVisitorMock);
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
    assertSame(methodVisitorMock, methodVisitor);
  }

  private static Stream<Arguments> testVisitMangledClassParams() {
    return Stream.of(
        Arguments.of(Opcodes.ACC_PUBLIC, null),
        Arguments.of(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, null),
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
              assertEquals(3, context.arguments().size());
              assertSame(methodVisitorMock, context.arguments().get(0));
              assertEquals(superName, context.arguments().get(1));
              assertEquals(METHOD_NAME, context.arguments().get(2));
            })) {

      final List<FieldAccessMangler> fieldAccessMangler = new ArrayList<>();

      try (MockedConstruction<TestCaseMangler> testCaseMock =
          mockConstruction(
              TestCaseMangler.class,
              (mock, context) -> {
                assertEquals(6, context.arguments().size());
                fieldAccessMangler.add((FieldAccessMangler) context.arguments().get(0));
                assertEquals(superName, context.arguments().get(1));
                assertEquals(Opcodes.ACC_PUBLIC, context.arguments().get(2));
                assertEquals(CLASS_NAME, context.arguments().get(3));
                assertEquals(METHOD_NAME, context.arguments().get(4));
                assertEquals(METHOD_DESCRIPTION, context.arguments().get(5));
              })) {

        when(classVisitorMock.visitMethod(
                Opcodes.ACC_PUBLIC, METHOD_NAME, METHOD_DESCRIPTION, null, null))
            .thenReturn(methodVisitorMock);

        final MethodVisitor result =
            mangler.visitMethod(Opcodes.ACC_PUBLIC, METHOD_NAME, METHOD_DESCRIPTION, null, null);
        assertNotNull(result);
        assertEquals(1, fieldAccessMock.constructed().size());
        if (superName == null) {
          assertEquals(0, testCaseMock.constructed().size());
          assertSame(result, fieldAccessMock.constructed().get(0));
        } else {
          assertEquals(fieldAccessMangler.get(0), fieldAccessMock.constructed().get(0));
          assertEquals(1, testCaseMock.constructed().size());
          assertSame(result, testCaseMock.constructed().get(0));
        }
      }
    }
  }

  @ParameterizedTest
  @ValueSource(
      ints = {Opcodes.ACC_ABSTRACT, Opcodes.ACC_BRIDGE, Opcodes.ACC_SYNTHETIC, Opcodes.ACC_NATIVE})
  public void testVisitMangledClassFilteredMethod(final int access) {
    mangler.visit(
        VERSION, Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, CLASS_NAME, null, OBJECT_SUPER, null);
    verify(classVisitorMock)
        .visit(
            VERSION,
            Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT,
            CLASS_NAME,
            null,
            OBJECT_SUPER,
            null);

    when(classVisitorMock.visitMethod(access, METHOD_NAME, METHOD_DESCRIPTION, null, null))
        .thenReturn(methodVisitorMock);
    final MethodVisitor methodVisitor =
        mangler.visitMethod(access, METHOD_NAME, METHOD_DESCRIPTION, null, null);
    assertSame(methodVisitorMock, methodVisitor);
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
    assertNull(methodVisitor);
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
              assertEquals(4, context.arguments().size());
              assertSame(methodVisitorMock, context.arguments().get(0));
              assertEquals(Opcodes.ACC_PUBLIC, context.arguments().get(1));
              assertEquals(METHOD_NAME, context.arguments().get(2));
              assertEquals(METHOD_DESCRIPTION, context.arguments().get(3));
            })) {
      when(classVisitorMock.visitMethod(
              Opcodes.ACC_PUBLIC, METHOD_NAME, METHOD_DESCRIPTION, null, null))
          .thenReturn(methodVisitorMock);
      final MethodVisitor result =
          mangler.visitMethod(Opcodes.ACC_PUBLIC, METHOD_NAME, METHOD_DESCRIPTION, null, null);
      assertNotNull(result);

      final List<SuspendMangler> constructed = mocked.constructed();
      assertEquals(1, constructed.size());
      assertSame(constructed.get(0), result);
    }
  }
}
