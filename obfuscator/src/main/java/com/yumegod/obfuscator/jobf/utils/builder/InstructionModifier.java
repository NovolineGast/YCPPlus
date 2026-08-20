package com.yumegod.obfuscator.jobf.utils.builder;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashMap;

public final class InstructionModifier {

    private final HashMap<AbstractInsnNode, InsnList> replacements = new HashMap<>();
    private final HashMap<AbstractInsnNode, InsnList> appends = new HashMap<>();
    private final HashMap<AbstractInsnNode, InsnList> prepends = new HashMap<>();

    /**
     * Appends the given instruction list after the specified original instruction.
     *
     * @param original The original instruction
     * @param append   The instruction list to append
     */
    public void append(AbstractInsnNode original, InsnList append) {
        appends.put(original, append);
    }

    /**
     * Replaces the specified original instruction with the given replacement instruction list.
     *
     * @param original     The original instruction to replace
     * @param replacements The replacement instruction list
     */
    public void replace(AbstractInsnNode original, InsnList replacements) {
        this.replacements.put(original, replacements);
    }

    /**
     * Applies the instruction modifications to the specified method node.
     *
     * @param methodNode The method node to modify
     */
    public void apply(MethodNode methodNode) {
        replacements.forEach((insn, list) -> {
            methodNode.instructions.insertBefore(insn, list);
            methodNode.instructions.remove(insn);
        });
        prepends.forEach((insn, list) -> {
            methodNode.instructions.insertBefore(insn, list);
        });
        appends.forEach((insn, list) -> {
            methodNode.instructions.insert(insn, list);
        });
    }
}