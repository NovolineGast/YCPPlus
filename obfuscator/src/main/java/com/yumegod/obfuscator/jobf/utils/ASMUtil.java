package com.yumegod.obfuscator.jobf.utils;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.CodeSizeEvaluator;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Objects;

import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.RETURN;

public class ASMUtil {

    public static AbstractInsnNode pushInt(int value) {
        if (value >= -1 && value <= 5) {
            return new InsnNode(Opcodes.ICONST_0 + value);
        }
        if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
            return new IntInsnNode(Opcodes.BIPUSH, value);
        }
        if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
            return new IntInsnNode(Opcodes.SIPUSH, value);
        }
        return new LdcInsnNode(value);
    }


    public static AbstractInsnNode createNumberNode(int value) {
        int opcode = getNumberOpcode(value);
        switch (opcode) {
            case Opcodes.ICONST_M1:
            case Opcodes.ICONST_0:
            case Opcodes.ICONST_1:
            case Opcodes.ICONST_2:
            case Opcodes.ICONST_3:
            case Opcodes.ICONST_4:
            case Opcodes.ICONST_5:
                return new InsnNode(opcode);
            default:
                if (value >= -128 && value <= 127) {
                    return new IntInsnNode(Opcodes.BIPUSH, value);
                }
                if (value >= -32768 && value <= 32767) {
                    return new IntInsnNode(Opcodes.SIPUSH, value);
                }
                return new LdcInsnNode(value);
        }
    }

    public static int getNumberOpcode(int value) {
        switch (value) {
            case -1:
                return Opcodes.ICONST_M1;
            case 0:
                return Opcodes.ICONST_0;
            case 1:
                return Opcodes.ICONST_1;
            case 2:
                return Opcodes.ICONST_2;
            case 3:
                return Opcodes.ICONST_3;
            case 4:
                return Opcodes.ICONST_4;
            case 5:
                return Opcodes.ICONST_5;
            default:
                if (value >= -128 && value <= 127) {
                    return Opcodes.BIPUSH;
                }
                return (value >= -32768 && value <= 32767) ? Opcodes.SIPUSH : Opcodes.LDC;
        }
    }

    public static MethodNode findClInit(ClassNode node) {
        return node.methods.stream().filter(methodNode -> "<clinit>".equals(methodNode.name))
                .findFirst().orElseGet(() -> {
                    MethodNode methodNode = new MethodNode(ACC_STATIC, "<clinit>", "()V", null, null);
                    node.methods.add(methodNode);
                    methodNode.instructions.add(new InsnNode(RETURN));
                    return methodNode;
                });
    }

    public static String readableASM(MethodNode node) {
        Textifier printer = new Textifier();
        TraceMethodVisitor methodPrinter = new TraceMethodVisitor(printer);
        node.accept(methodPrinter);
        StringWriter sw = new StringWriter();
        printer.print(new PrintWriter(sw));
        printer.getText().clear();
        return sw.toString();
    }

    public static boolean hasInstructions(MethodNode methodNode) {
        return Objects.nonNull(methodNode.instructions) && methodNode.instructions.size() > 0;
    }

    public static int calculateSize(MethodNode methodNode) {
        CodeSizeEvaluator evaluator = new CodeSizeEvaluator(null);
        methodNode.accept(evaluator);
        return evaluator.getMaxSize();
    }
}
