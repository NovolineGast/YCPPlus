package com.yumegod.obfuscator.jobf.transformer.impl.fun;

import com.yumegod.obfuscator.jobf.transformer.SingledClassTransformer;
import com.yumegod.obfuscator.jobf.utils.MathUtil;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodNode;

public class LineNumberRandomizer extends SingledClassTransformer {


    // Not sure if it will crash the native transformer
    // btw it bytecode transformers will work after native transformers' work
    // also we remove the line number in DebugRemover
    @Override
    public void process(ClassNode node) {
        for (MethodNode method : node.methods) {
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (instruction instanceof LineNumberNode) {
                    LineNumberNode lineNode = (LineNumberNode) instruction;
                    lineNode.line = MathUtil.randomInt();
                }
            }
        }
    }
}
