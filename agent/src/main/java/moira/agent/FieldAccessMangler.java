package moira.agent;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class FieldAccessMangler extends MethodVisitor {
  private static String[] methodNames = {
    "writeStaticField",
    "writeArrayElement",
    "writeObjectField",
    "readStaticField",
    "readArrayElement",
    "readObjectField",
  };

  private static String[] methodDescriptions = {
    "(Ljava/lang/String;)V",
    "(Ljava/lang/Object;I)V",
    "(Ljava/lang/Object;Ljava/lang/String;)V",
    "(Ljava/lang/String;)V",
    "(Ljava/lang/Object;I)V",
    "(Ljava/lang/Object;Ljava/lang/String;)V",
  };

  private boolean isInitialized;
  private String superName;

  public FieldAccessMangler(
      final MethodVisitor mv, final String superName, final String methodName) {
    super(Opcodes.ASM9, mv);
    isInitialized = !methodName.equals("<init>");
    this.superName = superName;
  }

  @Override
  public void visitInsn(final int opcode) {
    switch (opcode) {
      case Opcodes.DASTORE:
      case Opcodes.LASTORE:
        mv.visitInsn(Opcodes.DUP2_X2);
        mv.visitInsn(Opcodes.POP2);
        mv.visitInsn(Opcodes.DUP2_X2);
        mv.visitMethodInsn(
            Opcodes.INVOKESTATIC, Agent.PROFILER, methodNames[1], methodDescriptions[1], false);
        break;

      case Opcodes.IASTORE:
      case Opcodes.FASTORE:
      case Opcodes.BASTORE:
      case Opcodes.CASTORE:
      case Opcodes.AASTORE:
      case Opcodes.SASTORE:
        mv.visitInsn(Opcodes.DUP_X2);
        mv.visitInsn(Opcodes.POP);
        mv.visitInsn(Opcodes.DUP2_X1);
        mv.visitMethodInsn(
            Opcodes.INVOKESTATIC, Agent.PROFILER, methodNames[1], methodDescriptions[1], false);
        break;

      case Opcodes.DALOAD:
      case Opcodes.LALOAD:
      case Opcodes.IALOAD:
      case Opcodes.FALOAD:
      case Opcodes.BALOAD:
      case Opcodes.CALOAD:
      case Opcodes.AALOAD:
      case Opcodes.SALOAD:
        mv.visitInsn(Opcodes.DUP2);
        mv.visitMethodInsn(
            Opcodes.INVOKESTATIC, Agent.PROFILER, methodNames[4], methodDescriptions[4], false);
        break;
      default:
    }

    mv.visitInsn(opcode);
  }

  @Override
  public void visitMethodInsn(
      final int opcode,
      final String owner,
      final String name,
      final String descriptor,
      final boolean isInterface) {
    if (opcode == Opcodes.INVOKESPECIAL
        && superName != null
        && owner.equals(superName)
        && name.equals("<init>")) isInitialized = true;
    mv.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
  }

  @Override
  public void visitFieldInsn(
      final int opcode, final String owner, final String name, final String description) {
    if (opcode == Opcodes.PUTFIELD && isInitialized) {
      switch (Type.getType(description).getSort()) {
        case Type.LONG:
        case Type.DOUBLE:
          mv.visitInsn(Opcodes.DUP2_X1);
          mv.visitInsn(Opcodes.POP2);
          mv.visitInsn(Opcodes.DUP_X2);
          break;

        default:
          mv.visitInsn(Opcodes.SWAP);
          mv.visitInsn(Opcodes.DUP_X1);
          break;
      }

      mv.visitLdcInsn(name);
      mv.visitMethodInsn(
          Opcodes.INVOKESTATIC, Agent.PROFILER, methodNames[2], methodDescriptions[2], false);

    } else if (opcode == Opcodes.GETFIELD && isInitialized) {
      mv.visitInsn(Opcodes.DUP);
      mv.visitLdcInsn(name);
      mv.visitMethodInsn(
          Opcodes.INVOKESTATIC, Agent.PROFILER, methodNames[5], methodDescriptions[5], false);
    } else if (opcode == Opcodes.PUTSTATIC) {
      mv.visitLdcInsn(owner + "#" + name);
      mv.visitMethodInsn(
          Opcodes.INVOKESTATIC, Agent.PROFILER, methodNames[0], methodDescriptions[0], false);
    } else if (opcode == Opcodes.GETSTATIC) {
      mv.visitLdcInsn(owner + "#" + name);
      mv.visitMethodInsn(
          Opcodes.INVOKESTATIC, Agent.PROFILER, methodNames[3], methodDescriptions[3], false);
    }

    mv.visitFieldInsn(opcode, owner, name, description);
  }
}
