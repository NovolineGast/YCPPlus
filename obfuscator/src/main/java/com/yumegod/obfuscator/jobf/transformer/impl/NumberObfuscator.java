package com.yumegod.obfuscator.jobf.transformer.impl;

import com.yumegod.obfuscation.FlowObfuscate;
import com.yumegod.obfuscation.NoFlowObfuscate;
import com.yumegod.obfuscation.NoNumberObfuscate;
import com.yumegod.obfuscation.NumberObfuscate;
import com.yumegod.obfuscator.enums.NumberObfuscationMode;
import com.yumegod.obfuscator.jobf.transformer.SingledClassTransformer;
import com.yumegod.obfuscator.Main;
import com.yumegod.obfuscator.utils.AnnotationUtils;
import com.yumegod.obfuscator.utils.cfg.annotations.ConfigSection;
import com.yumegod.obfuscator.utils.cfg.annotations.StaticConfigReceiver;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.CodeSizeEvaluator;
import org.objectweb.asm.tree.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static org.objectweb.asm.Opcodes.*;

@StaticConfigReceiver
public class NumberObfuscator extends SingledClassTransformer {
    @ConfigSection("number_obfuscation.mode")
    public static NumberObfuscationMode MODE = NumberObfuscationMode.AGGRESSIVE;
    private static final Logger logger = LoggerFactory.getLogger(NumberObfuscator.class);

    @Override
    public void process(ClassNode node) {
//        if (Main.runnerInstance.safeMode) return;

        if (!shouldProcess(node)) return;

        node.methods.stream().filter(methodNode -> methodNode.instructions != null && methodNode.instructions.size() > 0).forEach(methodNode -> {
            simpleMathObf(methodNode);
            simpleObfuscateLong(methodNode);
            if (AnnotationUtils.haveAndNotHave(node.invisibleAnnotations, Type.getDescriptor(FlowObfuscate.class), Type.getDescriptor(NoFlowObfuscate.class))) return;
            CodeSizeEvaluator evaluator = new CodeSizeEvaluator(null);
            methodNode.accept(evaluator);
            int lwSize = evaluator.getMaxSize();
            boolean rndObf = false;
            while (lwSize < 0x3FFF) {
//                processMethod(methodNode, rndObf, false);
                processMethod0(methodNode, rndObf);
                rndObf = true;
                lwSize *= 8;
            }
        });
    }

    public static boolean shouldProcess(ClassNode node) {
        return AnnotationUtils.haveAndNotHave(node.invisibleAnnotations, Type.getDescriptor(NumberObfuscate.class), Type.getDescriptor(NoNumberObfuscate.class));
    }

    /**
     * <a href="https://itzsomebody.xyz/2020/03/29/math-obfuscation-of-java-bytecode.html">reference</a>
     *
     * @param methodNode method to obfuscate math
     */
    private void simpleMathObf(MethodNode methodNode) {
        InsnList nodes = methodNode.instructions;
        for (AbstractInsnNode node : nodes.toArray()) {
            if (node.getOpcode() == INEG) {
                nodes.insertBefore(node, new InsnNode(ICONST_M1));
                nodes.insertBefore(node, new InsnNode(IXOR));
                nodes.insertBefore(node, new InsnNode(ICONST_1));
                nodes.insertBefore(node, new InsnNode(IADD));
                nodes.remove(node);
            } else if (node.getOpcode() == ISUB) {
                nodes.insertBefore(node, new InsnNode(ICONST_M1));
                nodes.insertBefore(node, new InsnNode(IXOR));
                nodes.insertBefore(node, new InsnNode(IADD));
                nodes.insertBefore(node, new InsnNode(ICONST_1));
                nodes.insertBefore(node, new InsnNode(IADD));
                nodes.remove(node);
            }
        }
    }

//    public static void simpleObfuscateLong(MethodNode methodNode) {
//        ThreadLocalRandom random = ThreadLocalRandom.current();
//        int tempSlot = methodNode.maxLocals;
//        methodNode.maxLocals += 2;
//        for (AbstractInsnNode node : methodNode.instructions.toArray()) {
//            if (!isLongNumber(node)) continue;
//            InsnList nodes = new InsnList();
//            long value = getLongValue(node);
//            long rnd = random.nextLong() ^ random.nextLong();
//            if (random.nextBoolean()) {
//                nodes.add(generateLongPush(value ^ rnd));
//                rndLStoreLoad(nodes, tempSlot, random);
//                nodes.add(generateLongPush(rnd));
//            } else {
//                nodes.add(generateLongPush(rnd));
//                rndLStoreLoad(nodes, tempSlot, random);
//                nodes.add(generateLongPush(value ^ rnd));
//            }
//            rndLStoreLoad(nodes, tempSlot, random);
//            nodes.add(new InsnNode(LXOR));
//            rndLStoreLoad(nodes, tempSlot, random);
//            methodNode.instructions.insertBefore(node, nodes);
//            methodNode.instructions.remove(node);
//        }
//        for (AbstractInsnNode node : methodNode.instructions.toArray()) {
//            if (!isLongNumber(node)) continue;
//            InsnList nodes = new InsnList();
//            long value = getLongValue(node);
//            if (value == 1 || value == 0) {
//                continue;
//            }
//            boolean pushed = false;
//            for (int i = 63; i >= 0; i--) {
//                if ((value & (1L << i)) != 0) {
//                    nodes.add(new InsnNode(LCONST_1));
//                    for (int j = 0; j < i; j++) {
//                        nodes.add(new InsnNode(ICONST_1));
//                        nodes.add(new InsnNode(LSHL));
//                        if (random.nextInt(5) == 0) {
//                            nodes.add(new VarInsnNode(LSTORE, tempSlot));
//                            nodes.add(new VarInsnNode(LLOAD, tempSlot));
//                        }
//                    }
//
//                    if (pushed) nodes.add(new InsnNode(random.nextBoolean() ? LOR : LXOR));
//                    pushed = true;
//                }
//            }
//            methodNode.instructions.insertBefore(node, nodes);
//            methodNode.instructions.remove(node);
//        }
//    }
    public static void simpleObfuscateLong(MethodNode methodNode) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int varSlot = methodNode.maxLocals;
        methodNode.maxLocals += 2;
        for (AbstractInsnNode node : methodNode.instructions.toArray()) {
            if (!isLongNumber(node)) continue;
            long value = getLongValue(node);
            InsnList nodes = new InsnList();
            long seed = random.nextLong();
            if (random.nextBoolean()) {
                nodes.add(generateLongPush(value ^ seed));
                rndLStoreLoad(nodes, varSlot, random);
                nodes.add(generateLongPush(seed));
            } else {
                nodes.add(generateLongPush(seed));
                rndLStoreLoad(nodes, varSlot, random);
                nodes.add(generateLongPush(value ^ seed));
            }
            nodes.add(new InsnNode(LXOR));
            rndLStoreLoad(nodes, varSlot, random);
            methodNode.instructions.insertBefore(node, nodes);
            methodNode.instructions.remove(node);
        }
    }

    private static void rndLStoreLoad(InsnList nodes, int tempSlot, Random random) {
        if (random.nextInt(5) != 0) return;
        nodes.add(new VarInsnNode(LSTORE, tempSlot));
        nodes.add(new VarInsnNode(LLOAD, tempSlot));
    }

    private static long getLongValue(AbstractInsnNode node) {
        return (node instanceof LdcInsnNode && ((LdcInsnNode) node).cst instanceof Long) ? (Long) ((LdcInsnNode) node).cst : node.getOpcode() == LCONST_0 ? 0L : 1L;
    }

    private static boolean isLongNumber(AbstractInsnNode node) {
        return (node.getOpcode() == LDC && ((LdcInsnNode) node).cst instanceof Long) || node.getOpcode() == LCONST_0 || node.getOpcode() == LCONST_1;
    }

    private static AbstractInsnNode generateLongPush(long i) {
        if (i == 0) return new InsnNode(LCONST_0);
        if (i == 1) return new InsnNode(LCONST_1);
        return new LdcInsnNode(i);
    }

    public static void processMethod(MethodNode methodNode, boolean rndObf, boolean toStringLen) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int tempSlot = methodNode.maxLocals;
        methodNode.maxLocals++;
        for (AbstractInsnNode node : methodNode.instructions.toArray()) {
            if (!isIntegerNumber(node)) continue;
            InsnList nodes = new InsnList();
            int value = getIntValue(node);
            if (value == 1 || value == 0 || value == -1) {
                if (!toStringLen) continue;
                String toPush = "";
                switch (value) {
                    case 1:
                    case -1:
                        toPush = " ";
                        break;
                    case 0:
                        toPush = "";
                        break;
                }
                nodes.add(new LdcInsnNode(toPush));
                nodes.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "length", "()I", false));
                if (value == -1) nodes.add(new InsnNode(INEG));

                methodNode.instructions.insertBefore(node, nodes);
                methodNode.instructions.remove(node);
                continue;
            }
            if (rndObf && random.nextBoolean()) continue;
            boolean pushed = false;
            for (int i = 31; i >= 0; i--) {
                if ((value & (1 << i)) != 0) {
                    nodes.add(new InsnNode(ICONST_1));
                    if (random.nextBoolean() || rndObf) {
                        for (int j = 0; j < i; j++) {
                            nodes.add(new InsnNode(ICONST_1));
                            nodes.add(new InsnNode(ISHL));
                            if (!rndObf && random.nextInt(6) == 0) {
                                nodes.add(new VarInsnNode(ISTORE, tempSlot));
                                nodes.add(new VarInsnNode(ILOAD, tempSlot));
                            }
                        }
                    } else {
                        nodes.add(generateIntPush(i));
                        nodes.add(new InsnNode(ISHL));
                    }
                    if (pushed) nodes.add(new InsnNode(random.nextBoolean() ? IOR : IXOR));
                    pushed = true;
                }
            }
            methodNode.instructions.insertBefore(node, nodes);
            methodNode.instructions.remove(node);
        }
    }

    public static void processMethod0(MethodNode methodNode, boolean rndObf) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int globalIndex = methodNode.maxLocals;
        methodNode.maxLocals++;
        for (AbstractInsnNode node : methodNode.instructions.toArray()) {
            if (!isIntegerNumber(node)) continue;
            if (rndObf && random.nextBoolean()) continue;
            int value = getIntValue(node);
            InsnList nodes = new InsnList();
            switch (random.nextInt(3)) {
                case 0:
                    int seed = Math.abs(random.nextInt());
                    nodes.add(generateIntPush(seed));
                    nodes.add(generateIntPush(value ^ seed));
                    nodes.add(new InsnNode(IXOR));
                    break;
                case 1:
                case 2:
                    // from cesium
                    int shift = random.nextInt() & 255;
                    nodes.add(generateIntPush(getRotatedInt(value, shift)));
                    nodes.add(generateIntPush(shift));
                    nodes.add(new InsnNode(IUSHR));
                    nodes.add(generateIntPush(getRotatedInt(value, shift)));
                    nodes.add(generateIntPush(shift));
                    if (random.nextBoolean()) {
                        nodes.add(new InsnNode(ICONST_M1));
                        nodes.add(new InsnNode(IXOR));
                        nodes.add(new InsnNode(ICONST_1));
                        nodes.add(new InsnNode(IADD));
                    } else {
                        nodes.add(new InsnNode(INEG));
                    }

                    nodes.add(new InsnNode(ISHL));
                    nodes.add(new InsnNode(IOR));
                    break;
            }
            if (random.nextInt(5) == 0) {
                nodes.add(new LdcInsnNode(0xFFFFFFFF));
                nodes.add(new InsnNode(IAND));
            }
            if (random.nextInt(4) == 0) {
                if (random.nextInt(3) == 0) {
                    int i = methodNode.maxLocals;
                    methodNode.maxLocals++;
                    nodes.add(new VarInsnNode(ISTORE, i));
                    nodes.add(new VarInsnNode(ILOAD, i));
                } else {
                    nodes.add(new VarInsnNode(ISTORE, globalIndex));
                    nodes.add(new VarInsnNode(ILOAD, globalIndex));
                }
            }
            methodNode.instructions.insertBefore(node, nodes);
            methodNode.instructions.remove(node);
        }
    }

    // from cesium
    // rotates left
    private static int getRotatedInt(int value, int shiftDist) {
        return (value << shiftDist) | (value >>> -shiftDist);
    }

    private static boolean isIntegerNumber(AbstractInsnNode ain) {
        if (ain.getOpcode() == BIPUSH || ain.getOpcode() == SIPUSH) {
            return true;
        }
        if (ain.getOpcode() >= ICONST_M1 && ain.getOpcode() <= ICONST_5) {
            return true;
        }
        if (ain instanceof LdcInsnNode) {
            LdcInsnNode ldc = (LdcInsnNode) ain;
            return ldc.cst instanceof Integer;
        }
        return false;
    }

    private static AbstractInsnNode generateIntPush(int i) {
        if (i <= 5 && i >= -1) {
            return new InsnNode(i + 3); //iconst_i
        }
        if (i >= -128 && i <= 127) {
            return new IntInsnNode(BIPUSH, i);
        }

        if (i >= -32768 && i <= 32767) {
            return new IntInsnNode(SIPUSH, i);
        }
        return new LdcInsnNode(i);
    }

    private static int getIntValue(AbstractInsnNode node) {
        if (node.getOpcode() >= ICONST_M1 && node.getOpcode() <= ICONST_5) {
            return node.getOpcode() - 3;
        }
        if (node.getOpcode() == SIPUSH || node.getOpcode() == BIPUSH) {
            return ((IntInsnNode) node).operand;
        }
        if (node instanceof LdcInsnNode && ((LdcInsnNode) node).cst instanceof Integer) {
            return (int) ((LdcInsnNode) node).cst;
        }

        throw new IllegalArgumentException(node + " isn't an integer node");
    }
}
