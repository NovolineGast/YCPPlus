package com.yumegod.obfuscator.jobf.transformer.impl.fun;

import com.yumegod.obfuscator.jobf.transformer.SingledClassTransformer;
import com.yumegod.obfuscator.jobf.utils.ASMUtil;
import com.yumegod.obfuscator.jobf.utils.MathUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.concurrent.atomic.AtomicInteger;


public class FakeInstructionsTransformer extends SingledClassTransformer {

    @Override
    public void process(ClassNode node) {
        if (true) return;

        node.methods.stream()
                .filter(ASMUtil::hasInstructions)
                .forEach(method -> {
                    AtomicInteger index = new AtomicInteger();
                    InsnList instructions = method.instructions;

                    for (AbstractInsnNode insn : instructions.toArray()) {
                        if (insn instanceof LdcInsnNode) {
                            if (MathUtil.rndBool()) {
                                instructions.insertBefore(insn, new IntInsnNode(Opcodes.BIPUSH, MathUtil.randomInteger(-64, 64)));
                                instructions.insertBefore(insn, new InsnNode(Opcodes.POP));
                            }
                        } else if (index.getAndIncrement() % 6 == 0) {
                            if (MathUtil.randomFloat() > 0.6) {
                                instructions.insertBefore(insn, new IntInsnNode(Opcodes.BIPUSH, MathUtil.randomInteger(-27, 37)));
                                instructions.insertBefore(insn, new InsnNode(Opcodes.POP));
                            } else {
                                // FIXME wtf plz see what you do!
//                                instructions.insertBefore(insn, new InsnNode(Opcodes.NOP));
                            }
                        }
                    }
                });
    }
}