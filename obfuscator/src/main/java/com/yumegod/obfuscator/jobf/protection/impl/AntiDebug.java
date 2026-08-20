package com.yumegod.obfuscator.jobf.protection.impl;

import com.yumegod.obfuscation.DebuggerCheck;
import com.yumegod.obfuscator.jobf.protection.Protector;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import static org.objectweb.asm.Opcodes.*;

public class AntiDebug extends Protector {
    @Override
    public void protect(ClassNode node) {
        if (node.invisibleAnnotations != null && node.invisibleAnnotations.stream().anyMatch(it -> it.desc.equals(Type.getDescriptor(DebuggerCheck.class)))) {
            String methodName = randomUniqueName();
            dump(node, methodName);
            MethodNode cl_init = node.methods.stream().filter(it -> it.name.equals("<clinit>"))
                    .findFirst().orElseGet(() -> {
                        MethodNode methodNode = new MethodNode(ACC_STATIC, "<clinit>", "()V", null, null);
                        node.methods.add(methodNode);
                        methodNode.instructions.add(new InsnNode(RETURN));
                        return methodNode;
                    });
            cl_init.instructions.insertBefore(cl_init.instructions.getFirst(), new MethodInsnNode(INVOKESTATIC, node.name, methodName, "()V", false));
        }
    }

    private static void dump(ClassNode node, String name) {
        MethodVisitor methodVisitor = node.visitMethod(ACC_PRIVATE | ACC_STATIC | ACC_BRIDGE | ACC_SYNTHETIC | ACC_SYNCHRONIZED, name, "()V", null, null);
        methodVisitor.visitCode();
        Label label0 = new Label();
        methodVisitor.visitLabel(label0);
        methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/management/ManagementFactory", "getRuntimeMXBean", "()Ljava/lang/management/RuntimeMXBean;", false);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/lang/management/RuntimeMXBean", "getInputArguments", "()Ljava/util/List;", true);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "iterator", "()Ljava/util/Iterator;", true);
        methodVisitor.visitVarInsn(ASTORE, 0);
        Label label1 = new Label();
        methodVisitor.visitLabel(label1);
        methodVisitor.visitFrame(Opcodes.F_APPEND, 1, new Object[]{"java/util/Iterator"}, 0, null);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "hasNext", "()Z", true);
        Label label2 = new Label();
        methodVisitor.visitJumpInsn(IFEQ, label2);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "next", "()Ljava/lang/Object;", true);
        methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/String");
        methodVisitor.visitVarInsn(ASTORE, 1);
        Label label3 = new Label();
        methodVisitor.visitLabel(label3);
        methodVisitor.visitVarInsn(ALOAD, 1);
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "toLowerCase", "()Ljava/lang/String;", false);
        methodVisitor.visitVarInsn(ASTORE, 1);
        Label label4 = new Label();
        methodVisitor.visitLabel(label4);
        methodVisitor.visitVarInsn(ALOAD, 1);
        methodVisitor.visitLdcInsn("debug");
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "contains", "(Ljava/lang/CharSequence;)Z", false);
        Label label5 = new Label();
        methodVisitor.visitJumpInsn(IFNE, label5);
        methodVisitor.visitVarInsn(ALOAD, 1);
        methodVisitor.visitLdcInsn("runjdwp");
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "contains", "(Ljava/lang/CharSequence;)Z", false);
        Label label6 = new Label();
        methodVisitor.visitJumpInsn(IFEQ, label6);
        methodVisitor.visitLabel(label5);
        methodVisitor.visitFrame(Opcodes.F_APPEND, 1, new Object[]{"java/lang/String"}, 0, null);
        methodVisitor.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        methodVisitor.visitLdcInsn("Debugger detected #1");
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
        Label label7 = new Label();
        methodVisitor.visitLabel(label7);
        methodVisitor.visitIntInsn(SIPUSH, 1145);
        methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/System", "exit", "(I)V", false);
        methodVisitor.visitLabel(label6);
        methodVisitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        methodVisitor.visitJumpInsn(GOTO, label1);
        methodVisitor.visitLabel(label2);
        methodVisitor.visitFrame(Opcodes.F_CHOP, 1, null, 0, null);
        methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/management/ManagementFactory", "getRuntimeMXBean", "()Ljava/lang/management/RuntimeMXBean;", false);
        methodVisitor.visitVarInsn(ASTORE, 0);
        Label label8 = new Label();
        methodVisitor.visitLabel(label8);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/lang/management/RuntimeMXBean", "getInputArguments", "()Ljava/util/List;", true);
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "toString", "()Ljava/lang/String;", false);
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "toLowerCase", "()Ljava/lang/String;", false);
        methodVisitor.visitVarInsn(ASTORE, 1);
        Label label9 = new Label();
        methodVisitor.visitLabel(label9);
        methodVisitor.visitVarInsn(ALOAD, 1);
        methodVisitor.visitLdcInsn("debug");
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "contains", "(Ljava/lang/CharSequence;)Z", false);
        Label label10 = new Label();
        methodVisitor.visitJumpInsn(IFNE, label10);
        methodVisitor.visitVarInsn(ALOAD, 1);
        methodVisitor.visitLdcInsn("runjdwp");
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "contains", "(Ljava/lang/CharSequence;)Z", false);
        Label label11 = new Label();
        methodVisitor.visitJumpInsn(IFEQ, label11);
        methodVisitor.visitLabel(label10);
        methodVisitor.visitFrame(Opcodes.F_FULL, 2, new Object[]{"java/lang/management/RuntimeMXBean", "java/lang/String"}, 0, new Object[]{});
        methodVisitor.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        methodVisitor.visitLdcInsn("Debugger detected #2");
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
        Label label12 = new Label();
        methodVisitor.visitLabel(label12);
        methodVisitor.visitIntInsn(SIPUSH, 1145);
        methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/System", "exit", "(I)V", false);
        methodVisitor.visitLabel(label11);
        methodVisitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        methodVisitor.visitInsn(RETURN);
        Label label13 = new Label();
        methodVisitor.visitLabel(label13);
        methodVisitor.visitLocalVariable("s", "Ljava/lang/String;", null, label3, label6, 1);
        methodVisitor.visitLocalVariable("runtimeMxBean", "Ljava/lang/management/RuntimeMXBean;", null, label8, label13, 0);
        methodVisitor.visitLocalVariable("jvmArgs", "Ljava/lang/String;", null, label9, label13, 1);
        methodVisitor.visitMaxs(2, 2);
        methodVisitor.visitEnd();
    }
}
