package com.yumegod.obfuscator.j2c.handlers;

import com.yumegod.obfuscator.j2c.MethodContext;
import org.objectweb.asm.tree.AbstractInsnNode;

public interface InstructionTypeHandler<T extends AbstractInsnNode> {
    void accept(MethodContext context, T node);

    String insnToString(MethodContext context, T node);

    int getNewStackPointer(T node, int currentStackPointer);
}
