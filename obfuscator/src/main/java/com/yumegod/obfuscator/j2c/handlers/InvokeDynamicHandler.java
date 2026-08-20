package com.yumegod.obfuscator.j2c.handlers;

import com.yumegod.obfuscator.j2c.MethodContext;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;

public class InvokeDynamicHandler extends GenericInstructionHandler<InvokeDynamicInsnNode> {

    @Override
    protected void process(MethodContext context, InvokeDynamicInsnNode node) {
        throw new RuntimeException("Indy should be handled at bytecode side");
    }

    @Override
    public String insnToString(MethodContext context, InvokeDynamicInsnNode node) {
        throw new RuntimeException("Indy should be handled at bytecode side");
    }

    @Override
    public int getNewStackPointer(InvokeDynamicInsnNode node, int currentStackPointer) {
        throw new RuntimeException("Indy should be handled at bytecode side");
    }
}
