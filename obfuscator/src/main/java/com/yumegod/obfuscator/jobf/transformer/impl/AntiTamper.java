package com.yumegod.obfuscator.jobf.transformer.impl;

import com.yumegod.obfuscation.TamperPrevention;
import com.yumegod.obfuscator.jobf.transformer.SingledClassTransformer;
import com.yumegod.obfuscator.jobf.transformer.impl.flow.ControlFlowObfuscator;
import com.yumegod.obfuscator.jobf.utils.ASMUtil;
import com.yumegod.obfuscator.YumeCloudProtection;
import com.yumegod.obfuscator.utils.AnnotationUtils;
import com.yumegod.obfuscator.utils.SafeClassWriter;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import static com.yumegod.obfuscator.jobf.sdk.SDKInline.dumpStringDecryptionMethod;
import static org.objectweb.asm.Opcodes.*;

public class AntiTamper extends SingledClassTransformer {
    private static final String TAMPER_PREVENTION = Type.getDescriptor(TamperPrevention.class);
    private static final boolean TEST = false;

    @Override
    public void process(ClassNode node) {
        if (Optional.ofNullable(node.invisibleAnnotations).orElse(Collections.emptyList())
                .stream().anyMatch(anno -> TAMPER_PREVENTION.equals(anno.desc)) || TEST) {
            AnnotationUtils.cleanAnnotations(node);
            addAntiTamperLogic(node);
        }
    }

    private static void addAntiTamperLogic(ClassNode classNode) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        MethodNode clInit = ASMUtil.findClInit(classNode);
        int hashBase = random.nextInt();

        LabelNode labelBeforeReplacement = new LabelNode();

        InsnList actions = /*new InsnList()*/ generateCrash();
        {
//            actions.add(new InsnNode(ACONST_NULL));
//            actions.add(new InsnNode(ATHROW));
//            // System.out.println
//            actions.add(new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
//            actions.add(new LdcInsnNode("Failed to verify!"));
//            actions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false));
//            // Integer.toString call
//            actions.add(new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
//            actions.add(new VarInsnNode(ILOAD, 2));
//            actions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Integer", "toString", "(I)Ljava/lang/String;", false));
//            actions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false));

        }

        InsnList nodes = clInit.instructions;
        InsnList toInsert = dump(classNode.name, hashBase, actions, labelBeforeReplacement);
        offsetLocalVar(toInsert, 4);
        doStringObf(toInsert, 0);
        offsetLocalVar(toInsert, clInit.maxLocals);
        nodes.insert(toInsert);

        boolean succeed = false;

        clInit.maxLocals += 3;

        for (int i = 0; i < 0xFFFFFF; i++) {
            nodes.set(labelBeforeReplacement.getNext(), generateIntPush(i));
            try {
                int hash = hash(build(classNode), hashBase);
                if ((hash & 0xFF) == (i % 256)) {
                    YumeCloudProtection.logger.info("Inserted AntiTamper logic into {} with identity {}", classNode.name, hash);
                    succeed = true;
                    break;
                }
            } catch (Throwable ignored) {
            }
        }
        if (!succeed) {
            YumeCloudProtection.logger.info("Failed to insert AntiTamper logic into {}", classNode.name);
        }
    }

    static byte[] build(ClassNode node) {
        SafeClassWriter writer = new SafeClassWriter(YumeCloudProtection.instance.metadataReader, ClassWriter.COMPUTE_FRAMES);
        node.accept(writer);
        return writer.toByteArray();
    }

    static int hash(InputStream stream, int hashBase) throws Throwable {
        int hash = /*0x17AF29CB*/ hashBase;
        int i;
        while ((i = stream.read()) != -1) {
            hash = ((hash ^ i) << 5) ^ (hash >>> 17) + i;
        }
        return hash;
    }

    static int hash(byte[] bytes, int hashBase) throws Throwable {
        try (ByteArrayInputStream stream = new ByteArrayInputStream(bytes)) {
            return hash(stream, hashBase);
        }
    }

    static void offsetLocalVar(InsnList list, int offset) {
        for (AbstractInsnNode node : list) {
            if (node instanceof VarInsnNode) {
                ((VarInsnNode) node).var += offset;
            } else if (node instanceof IincInsnNode) {
                ((IincInsnNode) node).var += offset;
            }
        }
    }

    private static InsnList dump(String classInternalName, int hashBase, InsnList actions, LabelNode labelBeforeReplacement) {
        MethodNode mv = new MethodNode(0, "dump", "()V", null, null);
        InsnList nodes = mv.instructions;
        InsnNode toBeReplacedNode = new InsnNode(ICONST_1);
        {
            Label label0 = new Label();
            mv.visitLabel(label0);
            mv.visitLdcInsn(Type.getType("L" + classInternalName + ";"));
            mv.visitLdcInsn("/" + classInternalName + ".class");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getResourceAsStream", "(Ljava/lang/String;)Ljava/io/InputStream;", false);
            mv.visitVarInsn(ASTORE, 1);
            Label label1 = new Label();
            mv.visitLabel(label1);
            nodes.add(ASMUtil.createNumberNode(hashBase));
            mv.visitVarInsn(ISTORE, 2);
            Label label2 = new Label();
            mv.visitLabel(label2);
//            methodVisitor.visitFrame(Opcodes.F_APPEND, 2, new Object[]{"java/io/InputStream", Opcodes.INTEGER}, 0, null);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/InputStream", "read", "()I", false);
            mv.visitInsn(DUP);
            mv.visitVarInsn(ISTORE, 3);
            Label label3 = new Label();
            mv.visitLabel(label3);
            mv.visitInsn(ICONST_M1);
            Label label4 = new Label();
            mv.visitJumpInsn(IF_ICMPEQ, label4);
            Label label5 = new Label();
            mv.visitLabel(label5);
            mv.visitVarInsn(ILOAD, 2);
            mv.visitVarInsn(ILOAD, 3);
            mv.visitInsn(IXOR);
            mv.visitInsn(ICONST_5);
            mv.visitInsn(ISHL);
            mv.visitVarInsn(ILOAD, 2);
            mv.visitIntInsn(BIPUSH, 17);
            mv.visitInsn(IUSHR);
            mv.visitVarInsn(ILOAD, 3);
            mv.visitInsn(IADD);
            mv.visitInsn(IXOR);
            mv.visitVarInsn(ISTORE, 2);
            mv.visitJumpInsn(GOTO, label2);
            mv.visitLabel(label4);
//            methodVisitor.visitFrame(Opcodes.F_APPEND, 1, new Object[]{Opcodes.INTEGER}, 0, null);
            mv.visitVarInsn(ILOAD, 2);
            mv.visitIntInsn(SIPUSH, 255);
            mv.visitInsn(IAND);
            nodes.add(toBeReplacedNode);
            mv.visitIntInsn(SIPUSH, 256);
            mv.visitInsn(IREM);
            Label label6 = new Label();
            mv.visitJumpInsn(IF_ICMPEQ, label6);
            Label label7 = new Label();
            mv.visitLabel(label7);
            nodes.add(actions);
            mv.visitLabel(label6);
            mv.visitInsn(RETURN);
//            methodVisitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        }
        ControlFlowObfuscator.processSimple(mv);
        Arrays.stream(nodes.toArray()).filter(node -> node.getOpcode() == RETURN)
                .forEach(nodes::remove);
        nodes.insertBefore(toBeReplacedNode, labelBeforeReplacement);
        return nodes;
    }

    static void doStringObf(InsnList instructions, int maxLocals) {
        for (AbstractInsnNode node : instructions.toArray()) {
            if (node instanceof LdcInsnNode) {
                LdcInsnNode previous = (LdcInsnNode) node;
                if (previous.cst instanceof String) {
                    int xorKey = ThreadLocalRandom.current().nextInt(0xFFFF);
                    previous.cst = encrypt((String) previous.cst, xorKey);
                    InsnList nodes = dumpStringDecryptionMethod(Type.getMethodDescriptor(Type.getType(String.class), Type.getType(String.class)), xorKey).instructions;
                    offsetLocalVar(nodes, maxLocals);
                    instructions.insert(previous, nodes);
                }
            }

        }
    }

    static String encrypt(String s, int key) {
        char[] array = s.toCharArray();
        for (int i = 0; i < array.length; i++) {
            array[i] = (char) (array[i] ^ key);
        }
        return new String(array);
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

    private static InsnList generateCrash() {
        MethodNode mv = new MethodNode(0, "dump", "()V", null, null);
        mv.visitCode();
        mv.visitLdcInsn(Type.getType("Lsun/misc/Unsafe;"));
        mv.visitLdcInsn("theUnsafe");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getDeclaredField", "(Ljava/lang/String;)Ljava/lang/reflect/Field;", false);
        mv.visitVarInsn(ASTORE, 0);
        Label label3 = new Label();
        mv.visitLabel(label3);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitInsn(ICONST_1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Field", "setAccessible", "(Z)V", false);
        Label label4 = new Label();
        mv.visitLabel(label4);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitInsn(ACONST_NULL);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Field", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
        mv.visitTypeInsn(CHECKCAST, "sun/misc/Unsafe");
        mv.visitVarInsn(ASTORE, 1);
        Label label5 = new Label();
        mv.visitLabel(label5);
        mv.visitLdcInsn("java.lang.Integer$IntegerCache");
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;", false);
        mv.visitVarInsn(ASTORE, 2);
        Label label6 = new Label();
        mv.visitLabel(label6);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitLdcInsn("cache");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getDeclaredField", "(Ljava/lang/String;)Ljava/lang/reflect/Field;", false);
        mv.visitVarInsn(ASTORE, 3);
        Label label7 = new Label();
        mv.visitLabel(label7);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitVarInsn(ALOAD, 3);
        mv.visitMethodInsn(INVOKEVIRTUAL, "sun/misc/Unsafe", "staticFieldOffset", "(Ljava/lang/reflect/Field;)J", false);
        mv.visitVarInsn(LSTORE, 4);
        Label label8 = new Label();
        mv.visitLabel(label8);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitLdcInsn(randomString());
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "getInteger", "(Ljava/lang/String;)Ljava/lang/Integer;", false);
        mv.visitVarInsn(LLOAD, 4);
        mv.visitInsn(ACONST_NULL);
        mv.visitMethodInsn(INVOKEVIRTUAL, "sun/misc/Unsafe", "putObject", "(Ljava/lang/Object;JLjava/lang/Object;)V", false);
        return mv.instructions;
    }

    private static String randomString() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int length = random.nextInt(5, 0xFF);
        char[] chars = new char[length];
        for (int i = 0; i < length; i++) {
            chars[i] = ((char) (random.nextInt(0xFFFF)));
        }
        return new String(chars);
    }
}
