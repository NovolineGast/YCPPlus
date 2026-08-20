package com.yumegod.obfuscator.jobf.transformer.impl;

import com.yumegod.obfuscator.jobf.transformer.SingledClassTransformer;
import org.objectweb.asm.tree.ClassNode;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class DuplicatedMemberChecker extends SingledClassTransformer {
    @Override
    public void process(ClassNode node) {
        check(node);
    }

    public static void check(ClassNode node) {
        List<String> methodList = node.methods.stream().map(m -> m.name + m.desc).collect(Collectors.toList());
        HashSet<String> methodSet = new HashSet<>(methodList);
        if (methodList.size() != methodSet.size()) {
            throw new RuntimeException("Duplicated method found in " + node.name);
        }
        List<String> fieldList = node.fields.stream().map(f -> f.name + f.desc).collect(Collectors.toList());
        HashSet<String> fieldSet = new HashSet<>(fieldList);
        if (fieldList.size() != fieldSet.size()) {
            throw new RuntimeException("Duplicated field found in " + node.name);
        }
    }
}
