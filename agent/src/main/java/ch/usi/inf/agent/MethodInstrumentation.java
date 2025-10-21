package ch.usi.inf.agent;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

class MethodInstrumentation extends MethodVisitor {
  private static String[] methodNames = {
    "writeStaticField",
    "writeArrayElement",
    "writeObjectField",
    "readStaticField",
    "readArrayElement",
    "readObjectField",
  };

  private static String[] methodDescs = {
    "(Ljava/lang/String;)V",
    "(Ljava/lang/Object;I)V",
    "(Ljava/lang/Object;Ljava/lang/String;)V",
    "(Ljava/lang/String;)V",
    "(Ljava/lang/Object;I)V",
    "(Ljava/lang/Object;Ljava/lang/String;)V",
  };

  private int extraStackBytes;

  public MethodInstrumentation(MethodVisitor mv) {
    super(Opcodes.ASM9, mv);
    this.extraStackBytes = 0;
  }

  private void useExtraStackWords(int w) {
    if (extraStackBytes < w * 4) extraStackBytes = w * 4;
  }

  @Override
  public void visitInsn(int op) {
    switch (op) {
      case Opcodes.DASTORE:
      case Opcodes.LASTORE:
        mv.visitInsn(Opcodes.DUP2_X2);
        mv.visitInsn(Opcodes.POP2);
        mv.visitInsn(Opcodes.DUP2_X2);
        mv.visitMethodInsn(
            Opcodes.INVOKESTATIC, Agent.PROFILER, methodNames[1], methodDescs[1], false);
        useExtraStackWords(2);
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
            Opcodes.INVOKESTATIC, Agent.PROFILER, methodNames[1], methodDescs[1], false);
        useExtraStackWords(2);
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
            Opcodes.INVOKESTATIC, Agent.PROFILER, methodNames[4], methodDescs[4], false);
        useExtraStackWords(2);
        break;
      default:
    }

    mv.visitInsn(op);
  }

  @Override
  public void visitFieldInsn(int op, String owner, String name, String desc) {
    if (name.startsWith("this$") || name.startsWith("val$")) {
      mv.visitFieldInsn(op, owner, name, desc);
      return;
    }

    if (op == Opcodes.PUTFIELD) {
      switch (Type.getType(desc).getSort()) {
        case Type.LONG:
        case Type.DOUBLE:
          mv.visitInsn(Opcodes.DUP2_X1);
          mv.visitInsn(Opcodes.POP2);
          mv.visitInsn(Opcodes.DUP_X2);
          mv.visitLdcInsn(name);
          mv.visitMethodInsn(
              Opcodes.INVOKESTATIC, Agent.PROFILER, methodNames[2], methodDescs[2], false);
          break;

        default:
          mv.visitInsn(Opcodes.SWAP);
          mv.visitInsn(Opcodes.DUP_X1);
          mv.visitLdcInsn(name);
          mv.visitMethodInsn(
              Opcodes.INVOKESTATIC, Agent.PROFILER, methodNames[2], methodDescs[2], false);
      }
      useExtraStackWords(2);

    } else if (op == Opcodes.GETFIELD) {
      mv.visitInsn(Opcodes.DUP);
      mv.visitLdcInsn(name);
      mv.visitMethodInsn(
          Opcodes.INVOKESTATIC, Agent.PROFILER, methodNames[5], methodDescs[5], false);
      useExtraStackWords(2);
    } else if (op == Opcodes.PUTSTATIC) {
      mv.visitLdcInsn(owner + "#" + name);
      mv.visitMethodInsn(
          Opcodes.INVOKESTATIC, Agent.PROFILER, methodNames[0], methodDescs[0], false);
      useExtraStackWords(2);
    } else if (op == Opcodes.GETSTATIC) {
      mv.visitLdcInsn(owner + "#" + name);
      mv.visitMethodInsn(
          Opcodes.INVOKESTATIC, Agent.PROFILER, methodNames[3], methodDescs[3], false);
      useExtraStackWords(2);
    }

    mv.visitFieldInsn(op, owner, name, desc);
  }

  public void visitMaxs(int maxStack, int maxLocals) {
    mv.visitMaxs(maxStack + extraStackBytes, maxLocals);
  }
}
