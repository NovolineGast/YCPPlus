package com.yumegod.obfuscator.jobf.transformer.impl.renamer;

import com.yumegod.obfuscation.NotNative;
import org.objectweb.asm.tree.ClassNode;

import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;

class ClassNodeInfo {
    static final AtomicInteger index = new AtomicInteger();
    final int hash = index.incrementAndGet();
    ClassNode node;
    final HashSet<ClassNodeInfo> parents = new HashSet<>();
    final HashSet<ClassNodeInfo> children = new HashSet<>();
    boolean ableToRename = false;
    String nodeName;

    @NotNative
    ClassNodeInfo setNode(ClassNode node) {
        this.node = node;
        nodeName = node.name;
        return this;
    }

    @NotNative
    ClassNodeInfo setName(String name) {
        nodeName = name;
        return this;
    }

    @NotNative
    void addParent(ClassNodeInfo parent) {
        if (!parents.add(parent)) return;
        parent.addChild(this);
    }

    @NotNative
    void addChild(ClassNodeInfo child) {
        if (!children.add(child)) return;
        child.addParent(this);
    }

    @NotNative
    ClassNodeInfo ableToRename() {
        ableToRename = true;
        return this;
    }

    @NotNative
    @Override
    public int hashCode() {
        return this.hash;
    }

    @NotNative
    @Override
    public boolean equals(Object obj) {
        return obj instanceof ClassNodeInfo && this.hash == obj.hashCode();
    }
}
