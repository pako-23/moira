package ch.usi.inf.agent;

import ch.usi.inf.collect.Map;
import ch.usi.inf.collect.MapBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;

public class TestCaseMangler extends AdviceAdapter {
  private static String[] methodNames = {
    "enterTestMethod", "exitTestMethod",
  };

  private static String[] methodDescriptions = {
    "(Ljava/lang/String;)V", "()V",
  };

  private static Map<String, Void> testsFilter = null;
  private static TestDetector detector = new TestDetector();

  private boolean instrument;
  private Label tryBegin;
  private final String testCase;

  public TestCaseMangler(
      final MethodVisitor mv,
      final String superName,
      final int access,
      final String className,
      final String methodName,
      final String description) {
    super(Opcodes.ASM9, mv, access, methodName, description);
    this.testCase = className + "#" + methodName;

    instrument =
        isJUint3TestMethod(superName, access, className, methodName, description)
            || detector.isJUint4TestMethod(className, methodName, description);
    if (testsFilter != null && testsFilter.contains(testCase)) instrument = false;
  }

  private boolean isJUint3TestMethod(
      final String superName,
      final int access,
      final String className,
      final String methodName,
      final String description) {
    return detector.isJUnit3TestClass(superName, className)
        && ((methodName.startsWith("test") && (access & Opcodes.ACC_PUBLIC) != 0)
            || (methodName.equals("runTest") && description.equals("()V")));
  }

  @Override
  public void visitCode() {
    super.visitCode();
    if (instrument) {
      tryBegin = new Label();
      mv.visitLabel(tryBegin);
    }
  }

  @Override
  public void onMethodEnter() {
    if (instrument) {
      mv.visitLdcInsn(testCase);
      mv.visitMethodInsn(
          Opcodes.INVOKESTATIC, Agent.PROFILER, methodNames[0], methodDescriptions[0], false);
    }
  }

  @Override
  public void onMethodExit(int opcode) {
    if (instrument && opcode != Opcodes.ATHROW) {
      mv.visitMethodInsn(
          Opcodes.INVOKESTATIC, Agent.PROFILER, methodNames[1], methodDescriptions[1], false);
    }
  }

  @Override
  public void visitMaxs(int maxStack, int maxLocals) {
    if (instrument) {
      final Label tryEnd = new Label();
      mv.visitTryCatchBlock(tryBegin, tryEnd, tryEnd, null);
      mv.visitLabel(tryEnd);
      mv.visitFrame(F_NEW, 0, null, 1, new Object[] {"java/lang/Throwable"});
      mv.visitMethodInsn(
          Opcodes.INVOKESTATIC, Agent.PROFILER, methodNames[1], methodDescriptions[1], false);
      mv.visitInsn(Opcodes.ATHROW);
    }

    mv.visitMaxs(maxStack, maxLocals);
  }

  public static void registerTestsFilter(final String fileName) {
    testsFilter = MapBuilder.<String, Void>builder().build();

    try (Stream<String> lines = Files.lines(Paths.get(fileName))) {
      lines.forEach(TestCaseMangler::registerTest);
    } catch (IOException e) {
      System.err.println("Warning: failed to read tests filter: " + e.getMessage());
      testsFilter = null;
    }
  }

  private static void registerTest(final String line) {
    final String[] tests = line.split(" ");
    for (final String test : tests) if (!test.isEmpty()) testsFilter.getOrPut(test, () -> null);
  }

  public static void clearTestsFilter() {
    testsFilter = null;
  }
}
