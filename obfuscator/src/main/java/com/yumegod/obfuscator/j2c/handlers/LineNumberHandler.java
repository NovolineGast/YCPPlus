package com.yumegod.obfuscator.j2c.handlers;

import com.yumegod.obfuscator.j2c.MethodContext;
import org.objectweb.asm.tree.LineNumberNode;

public class LineNumberHandler implements InstructionTypeHandler<LineNumberNode> {
    @Override
    public void accept(MethodContext context, LineNumberNode node) {
        context.line = node.line;
    }

    @Override
    public String insnToString(MethodContext context, LineNumberNode node) {
        return String.format("Line %d", node.line);
    }

    @Override
    public int getNewStackPointer(LineNumberNode node, int currentStackPointer) {
        return currentStackPointer;
    }

}
