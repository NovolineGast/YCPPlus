package com.yumegod.obfuscator.jobf.transformer.impl.renamer;

import com.yumegod.obfuscation.NotNative;
import com.yumegod.obfuscator.Main;
import com.yumegod.obfuscator.utils.ClassMetadataReader;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

class ClassNodeTree {
    private static final int libParsingOptions = ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES;
    final Map<String, ClassNodeInfo> map = new HashMap<>();
    ClassNodeInfo root = require("java/lang/Object");

    @NotNative
    void parseLibraries(ClassMetadataReader metadata) {
        metadata.getCp().forEach(jar -> jar.stream().filter(entry -> entry.getName().endsWith(".class")).forEach(entry -> {
            try {
                ClassNode node = new ClassNode();
                new ClassReader(jar.getInputStream(entry)).accept(node, libParsingOptions);
                asLibraryClass(node);
                for (MethodNode method : node.methods) {
                    method.instructions.clear();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }));
        root = require("java/lang/Object");
        if (root.node != null) return;
        root.node = new ClassNode();
        try {
            new ClassReader("java.lang.Object").accept(root.node, libParsingOptions);
            asLibraryClass(root.node);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNative
    void asLibraryClass(ClassNode node) {
        ClassNodeInfo info = require(node.name).setNode(node);
        if (node.superName != null) require(node.superName).addChild(info);
        if (node.interfaces != null && !node.interfaces.isEmpty()) {
            for (String interfaceName : node.interfaces) {
                require(interfaceName).addChild(info);
            }
        }
    }

    @NotNative
    void parseClasses(Map<String, ClassNode> classes) {
        for (Map.Entry<String, ClassNode> entry : classes.entrySet()) {
            ClassNode node = entry.getValue();
            ClassNodeInfo info = require(node.name).ableToRename().setNode(node);
            if (node.superName != null) require(node.superName).addChild(info);
            if (node.interfaces != null && !node.interfaces.isEmpty()) {
                for (String interfaceName : node.interfaces) {
                    require(interfaceName).addChild(info);
                }
            }
        }
        dump();
    }

    void clear() {
//            // help gc...... 最少LinkedList里有这么一段话 可能会在j2c环境中出现性能问题
//            for (ClassNodeInfo info : map.values()) {
//                info.parents.clear();
//                info.children.clear();
//            }
        map.clear();
    }

    void dump() {
        if (!Main.devMode) return;
        StringBuilder builder = new StringBuilder();
        dump(builder, root, 0);
//            logger.info(builder.toString());
        try {
            Files.write(new File("dumped.txt").toPath(), builder.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void dump(StringBuilder sb, ClassNodeInfo info, int level) {
        for (int i = 0; i < level; i++) {
            sb.append("   |");
        }
        if (level != 0) sb.append("-");
        ClassNode node = info.node;
        sb.append(node == null ? info.hash : node.name);
        if (node != null) {
            sb.append(" ").append(node.methods.size());
        }

        sb.append("\n");
        for (ClassNodeInfo child : info.children) {
            dump(sb, child, level + 1);
        }
    }

    @NotNative
    ClassNodeInfo require(String name) {
        return map.computeIfAbsent(name, k -> new ClassNodeInfo().setName(name));
    }

    public void shrink() {
        shrink0(root);
    }

    @NotNative
    private boolean shrink0(ClassNodeInfo info) {
        if (info.node == null) return false;
        if (info.ableToRename) return true;
        if (info.children.isEmpty()) {
            return false;
        }
        Iterator<ClassNodeInfo> iterator = info.children.iterator();
        boolean result = false;
        while (iterator.hasNext()) {
            ClassNodeInfo child = iterator.next();
            if (!shrink0(child)) {
                child.parents.clear();
                map.remove(child.nodeName);
                iterator.remove();
                continue;
            }
            result = true;
        }
        return result;
    }
}
