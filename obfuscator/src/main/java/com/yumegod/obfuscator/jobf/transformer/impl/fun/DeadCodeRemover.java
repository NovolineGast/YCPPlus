package com.yumegod.obfuscator.jobf.transformer.impl.fun;

import com.yumegod.obfuscator.YumeCloudProtection;
import com.yumegod.obfuscator.jobf.transformer.Transformer;
import com.yumegod.obfuscator.utils.SafeClassWriter;
import com.yumegod.obfuscator.utils.cfg.annotations.ConfigSection;
import com.yumegod.obfuscator.utils.cfg.annotations.StaticConfigReceiver;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.*;

import java.util.Map;

import static org.objectweb.asm.Opcodes.*;

@StaticConfigReceiver
// remove such as {goto(l1) nop nop nop nop nop throw l1 ...} code block
public class DeadCodeRemover extends Transformer {
    @ConfigSection("shrink.enable")
    public static boolean enabled = false;

    @Override
    public void process(Map<String, ClassNode> classes) {
        if (!enabled) return;
        int removed = 0, nopTotal = 0;
        for (Map.Entry<String, ClassNode> entry : classes.entrySet()) {
            ClassNode node = entry.getValue();
            try {
                SafeClassWriter writer = new SafeClassWriter(YumeCloudProtection.instance.metadataReader, ClassWriter.COMPUTE_FRAMES);
                node.accept(writer);
                ClassNode newNode = new ClassNode();
                new ClassReader(writer.toByteArray()).accept(newNode, ClassReader.SKIP_FRAMES);
                for (MethodNode method : newNode.methods.toArray(new MethodNode[0])) {
                    boolean modified = false;
                    InsnList instructions = method.instructions;
                    AbstractInsnNode insnNode = instructions.getFirst(), lastLabelNode = null;

                    boolean isPureNOPBlock = true;
                    int nopCount = 0;
                    while (insnNode != null) {
                        if (insnNode.getOpcode() == NOP) {
                            nopCount++;
                        } else if (insnNode.getOpcode() == ATHROW) {
                            if (isPureNOPBlock && nopCount > 0 && lastLabelNode != null) {
                                AbstractInsnNode iterNode = lastLabelNode.getNext();
                                do {
                                    AbstractInsnNode tmp = iterNode.getNext();
                                    instructions.remove(iterNode);
                                    iterNode = tmp;
                                    nopTotal++;
                                } while (iterNode != null && insnNode != iterNode);
                                insnNode = insnNode.getPrevious();
                                removed++;
                                modified = true;
                            }
                            nopCount = 0;
                        } else if (insnNode.getOpcode() >= GOTO && insnNode.getOpcode() <= RETURN) {
                            isPureNOPBlock = true;
                            nopCount = 0;
                            lastLabelNode = insnNode.getNext();
                        } else if (insnNode instanceof LabelNode) {
//                            isPureNOPBlock = true;
                            lastLabelNode = insnNode;
//                            nopCount = 0;
                        } else if (!(insnNode instanceof FrameNode)) {
                            isPureNOPBlock = false;
                            nopCount = 0;
                        }
                        insnNode = insnNode.getNext();
                    }

                    if (modified) {
                        node.methods.replaceAll(m -> m.name.equals(method.name) && m.desc.equals(method.desc) && m.access == method.access ? method : m);
                    }
                }
            } catch (Throwable ignored) {}
        }
        System.out.println("Shrank " + (nopTotal / 1024) + " kb.");
    }
}
