package com.yumegod.obfuscator.jobf.transformer.impl;

import com.yumegod.obfuscator.jobf.transformer.SingledClassTransformer;
import com.yumegod.obfuscator.jobf.utils.ASMUtil;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.Opcodes;

public class InitializationTransformer extends SingledClassTransformer {
    @Override
    public void process(ClassNode node) {
        InsnList toInsert = new InsnList();
        for (FieldNode field : node.fields) {
            if ((field.access & Opcodes.ACC_STATIC) == 0) continue;
            Object value = field.value;
            if (value == null) continue;
            if (value instanceof String) toInsert.add(new LdcInsnNode(value));
            else if (value instanceof Integer) toInsert.add(ASMUtil.createNumberNode((Integer) value));
            else if (value instanceof Float || value instanceof Double || value instanceof Long) toInsert.add(new LdcInsnNode(value));
            else continue;
            toInsert.add(new FieldInsnNode(Opcodes.PUTSTATIC, node.name, field.name, field.desc));
            field.value = null;
        }
        if (toInsert.size() == 0) return;
        InsnList instructions = ASMUtil.findClInit(node).instructions;
        instructions.insertBefore(instructions.getFirst(), toInsert);
    }
}
