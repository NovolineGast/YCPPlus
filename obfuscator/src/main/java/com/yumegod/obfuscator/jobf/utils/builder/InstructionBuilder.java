package com.yumegod.obfuscator.jobf.utils.builder;

import com.yumegod.obfuscator.jobf.utils.ASMUtil;
import org.objectweb.asm.tree.*;

/**
 * Helper class for building instructions in an InsnList.
 */
public class InstructionBuilder {
    private final InsnList list = new InsnList();

    /**
     * Appends an instruction with the given opcode to the list.
     *
     * @param opcode The opcode of the instruction
     */
    public void insn(int opcode) {
        list.add(new InsnNode(opcode));
    }

    /**
     * Appends a var instruction with the given opcode and index to the list.
     *
     * @param opcode The opcode of the instruction
     * @param index  The index
     */
    public void varInsn(int opcode, int index) {
        list.add(new VarInsnNode(opcode, index));
    }

    /**
     * Appends a type instruction with the given opcode and type to the list.
     *
     * @param opcode The opcode of the instruction
     * @param type   The type
     */
    public void type(int opcode, String type) {
        list.add(new TypeInsnNode(opcode, type));
    }

    /**
     * Appends a field instruction with the given opcode, owner, name, and descriptor to the list.
     *
     * @param opcode The opcode of the instruction
     * @param owner  The owner
     * @param name   The name
     * @param desc   The descriptor
     */
    public void fieldInsn(int opcode, String owner, String name, String desc) {
        list.add(new FieldInsnNode(opcode, owner, name, desc));
    }

    /**
     * Appends a method instruction with the given opcode, owner, name, descriptor, and interface flag to the list.
     *
     * @param opcode      The opcode of the instruction
     * @param owner       The owner
     * @param name        The name
     * @param desc        The descriptor
     * @param isInterface Whether it is an interface method
     */
    public void methodInsn(int opcode, String owner, String name, String desc, boolean isInterface) {
        list.add(new MethodInsnNode(opcode, owner, name, desc, isInterface));
    }

    public void methodInsn(int opcode, String owner, String name, String desc) {
        list.add(new MethodInsnNode(opcode, owner, name, desc));
    }

    /**
     * Appends a jump instruction with the given opcode and label to the list.
     *
     * @param opcode The opcode of the instruction
     * @param label  The label
     */
    public void jump(int opcode, LabelNode label) {
        list.add(new JumpInsnNode(opcode, label));
    }

    /**
     * Appends an ldc instruction with the given value to the list.
     *
     * @param value The value
     */
    public void ldc(Object value) {
        list.add(new LdcInsnNode(value));
    }

    /**
     * Appends a number instruction with the given integer value to the list.
     *
     * @param i The integer value
     */
    public void number(int i) {
        list.add(ASMUtil.createNumberNode(i));
    }

    /**
     * Returns the current instruction list.
     *
     * @return The current instruction list
     */
    public InsnList getList() {
        return this.list;
    }

    /**
     * Adds an abstract instruction node to the list.
     *
     * @param node The abstract instruction node
     */
    public void add(AbstractInsnNode node) {
        list.add(node);
    }

    /**
     * Adds a label node to the list.
     *
     * @param l The label node
     */
    public void label(LabelNode l) {
        list.add(l);
    }
}