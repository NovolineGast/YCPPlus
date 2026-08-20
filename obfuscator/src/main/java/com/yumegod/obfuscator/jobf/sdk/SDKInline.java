package com.yumegod.obfuscator.jobf.sdk;

import com.yumegod.obfuscator.jobf.protection.Protector;
import com.yumegod.obfuscator.jobf.transformer.SingledClassTransformer;
import com.yumegod.obfuscator.jobf.utils.ASMUtil;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import static org.objectweb.asm.Opcodes.*;

public class SDKInline extends Protector {
    private static final Logger logger = LoggerFactory.getLogger(SingledClassTransformer.class);
    private static final String SDK_CLASS = "com/yumegod/obfuscation/sdk/YumeCloudProtectionSDK";
    private static final String STRONG_STRING_ENCRYPTION_METHOD = "StrongStringEncryption";

    @Override
    public void protect(ClassNode node) {
        for (MethodNode method : node.methods) {
            if (method.instructions.size() > 0) {
                inlineStrongStringObf(node, method);
            }
        }
    }

    public static void inlineStrongStringObf(ClassNode classNode, MethodNode method) {
        AtomicInteger modified = new AtomicInteger();
        for (AbstractInsnNode node : method.instructions.toArray()) {
            if (node instanceof MethodInsnNode) {
                MethodInsnNode min = (MethodInsnNode) node;
                if (min.owner.equals(SDK_CLASS) && min.name.equals(STRONG_STRING_ENCRYPTION_METHOD) &&
                        min.desc.equals(Type.getMethodDescriptor(Type.getType(String.class), Type.getType(String.class)))) {
                    if (min.getPrevious() == null || !(min.getPrevious() instanceof LdcInsnNode) || !(((LdcInsnNode) min.getPrevious()).cst instanceof String)) {
                        logger.warn("Invalid SDK method {} call in {}#{}{} | argument should be a constant string", STRONG_STRING_ENCRYPTION_METHOD, classNode.name, method.name, method.desc);
                        method.instructions.remove(min);
                        continue;
                    }

                    int xorKey = ThreadLocalRandom.current().nextInt(0xFFFF);
                    LdcInsnNode previous = (LdcInsnNode) min.getPrevious();
                    previous.cst = encrypt((String) previous.cst, xorKey);
                    InsnList instructions = dumpStringDecryptionMethod(Type.getMethodDescriptor(Type.getType(String.class), Type.getType(String.class)), xorKey).instructions;
                    offsetLocalVar(instructions, method.maxLocals);
                    method.instructions.insertBefore(min, instructions);
                    method.instructions.remove(min);
                    method.maxLocals += 4;
                    modified.incrementAndGet();
                }
            }
        }
        if (modified.get() > 0) {
            logger.info("inlined {} SDK method {} call(s) in {}", modified.get(), STRONG_STRING_ENCRYPTION_METHOD, method.name);
            computeMax(method, method.name);
        }
    }

    static String encrypt(String s, int key) {
        char[] array = s.toCharArray();
        for (int i = 0; i < array.length; i++) {
            array[i] = (char) (array[i] ^ key);
        }
        return new String(array);
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


    /* like this:
     * String decrypt(String $0) {
     *   char[] $1 = $0.toCharArray();
     *   for (char $2 = 0; i < $1.length; i++) {
     *     $1[$2] = (char) $1[$2] ^ xorKey;
     *   }
     *   return new String($1);
     * }
     */
    public static MethodNode dumpStringDecryptionMethod(String desc, int xorKey) {
        MethodNode visitor = new MethodNode(ACC_PRIVATE | ACC_STATIC | ACC_BRIDGE | ACC_SYNTHETIC, "LOL", desc, null, null);
        visitor.visitCode();
        visitor.visitVarInsn(ASTORE, 0);
        visitor.visitVarInsn(ALOAD, 0);
        visitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "toCharArray", "()[C", false);
        visitor.visitVarInsn(ASTORE, 1);
        visitor.visitInsn(ICONST_0);
        visitor.visitVarInsn(ISTORE, 2);
        visitor.visitVarInsn(ALOAD, 1);
        visitor.visitInsn(ARRAYLENGTH);
        visitor.visitVarInsn(ISTORE, 3);
        Label forEachHead = new Label(), forEachEnd = new Label();
        visitor.visitLabel(forEachHead);
        visitor.visitVarInsn(ILOAD, 2);
        visitor.visitVarInsn(ILOAD, 3);
        visitor.visitJumpInsn(IF_ICMPGE, forEachEnd);

        visitor.visitVarInsn(ALOAD, 1);
        visitor.visitVarInsn(ILOAD, 2);

        visitor.visitVarInsn(ALOAD, 1);
        visitor.visitVarInsn(ILOAD, 2);
        visitor.visitInsn(CALOAD);
        visitor.instructions.add(ASMUtil.createNumberNode(xorKey));
        visitor.visitInsn(IXOR);
        visitor.visitInsn(I2C);

        visitor.visitInsn(CASTORE);
        visitor.visitIincInsn(2, 1);
        visitor.visitJumpInsn(GOTO, forEachHead);
        visitor.visitLabel(forEachEnd);

        visitor.visitTypeInsn(NEW, "java/lang/String");
        visitor.visitInsn(DUP);
        visitor.visitVarInsn(ALOAD, 1);
        visitor.visitMethodInsn(INVOKESPECIAL, "java/lang/String", "<init>", "([C)V", false);
//        visitor.visitInsn(DUP);
//        visitor.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
//        visitor.visitInsn(SWAP);
//        visitor.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
        visitor.visitEnd();
        return visitor;
    }
}
