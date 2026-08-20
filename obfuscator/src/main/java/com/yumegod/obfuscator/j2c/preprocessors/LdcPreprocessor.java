package com.yumegod.obfuscator.j2c.preprocessors;

import com.yumegod.obfuscator.enums.Platform;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class LdcPreprocessor implements Preprocessor {
    @Override
    public void process(ClassNode classNode, MethodNode methodNode, Platform platform) {
        AbstractInsnNode insnNode = methodNode.instructions.getFirst();
        while (insnNode != null) {
            if (insnNode instanceof LdcInsnNode) {
                LdcInsnNode ldcInsnNode = (LdcInsnNode) insnNode;

                if (ldcInsnNode.cst instanceof Handle) {
                    methodNode.instructions.insertBefore(ldcInsnNode,
                            MethodHandleUtils.generateMethodHandleLdcInsn((Handle) ldcInsnNode.cst));
                    AbstractInsnNode nextInsnNode = insnNode.getNext();
                    methodNode.instructions.remove(insnNode);
                    insnNode = nextInsnNode;
                    continue;
                }

                if (ldcInsnNode.cst instanceof Type) {
                    Type type = (Type) ldcInsnNode.cst;

                    if (type.getSort() == Type.METHOD) {
                        methodNode.instructions.insertBefore(ldcInsnNode,
                                MethodHandleUtils.generateMethodTypeLdcInsn(type));
                        AbstractInsnNode nextInsnNode = insnNode.getNext();
                        methodNode.instructions.remove(insnNode);
                        insnNode = nextInsnNode;
                    }
                }
            }

            insnNode = insnNode.getNext();
        }
    }
}
