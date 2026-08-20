package com.yumegod.obfuscator.jobf.transformer.impl;

import com.yumegod.obfuscator.YumeCloudProtection;
import com.yumegod.obfuscator.jobf.transformer.SingledClassTransformer;
import com.yumegod.obfuscator.utils.cfg.annotations.ConfigSection;
import com.yumegod.obfuscator.utils.cfg.annotations.StaticConfigReceiver;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static org.objectweb.asm.Opcodes.*;

@StaticConfigReceiver
public class DebugRemover extends SingledClassTransformer {
    @ConfigSection("misc.hide_members")
    public static boolean hideMembers = false;
    static String[] flags = {"~", "YCP", "YumeCloudProtection", "             "}, suffix = {"java", "kt"};
    static final String MIXIN_HEADER = "Lorg/spongepowered/asm/";

    @Override
    public void process(ClassNode node) {
        boolean mixin = node.invisibleAnnotations != null && node.invisibleAnnotations.stream().anyMatch(annotation -> annotation.desc.startsWith(MIXIN_HEADER));
        if (mixin) {
            return;
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        node.methods.forEach(DebugRemover::removeMethodDebug);
        shuffle(node, random);
        node.sourceFile = String.format("%s.%s", flags[random.nextInt(4)], suffix[random.nextInt(2)]);
        node.sourceDebug = "This Application Is Protected By YCP";
    }

    private static void removeMethodDebug(MethodNode node) {
        if (YumeCloudProtection.safeMode) return;
        sortLocalVariables(node);
        if (hideMembers) node.access |= ACC_SYNTHETIC;
        node.localVariables = new ArrayList<>();
        Optional.ofNullable(node.instructions).ifPresent(it -> Arrays.stream(it.toArray())
                        .filter(ain -> ain instanceof LineNumberNode).forEach(
                                it::remove
//                        ain -> ((LineNumberNode) ain).line = 0
                        )
        );
    }

    private static void shuffle(ClassNode node, Random random) {
        if ((node.access & Opcodes.ACC_ENUM) != 0) {
            return;
        }

        Collections.shuffle(node.methods, random);
        Collections.shuffle(node.fields, random);
        Collections.shuffle(node.innerClasses, random);
        Collections.shuffle(node.interfaces, random);

        if (node.invisibleAnnotations != null) Collections.shuffle(node.invisibleAnnotations, random);
        if (node.visibleAnnotations != null) Collections.shuffle(node.visibleAnnotations, random);
        if (node.invisibleTypeAnnotations != null) Collections.shuffle(node.invisibleTypeAnnotations, random);

        for (Object o : node.methods.toArray()) {
            if (o instanceof MethodNode) {
                MethodNode method = (MethodNode) o;
                if (method.invisibleAnnotations != null) Collections.shuffle(method.invisibleAnnotations, random);
                if (method.invisibleLocalVariableAnnotations != null) Collections.shuffle(method.invisibleLocalVariableAnnotations, random);
                if (method.invisibleTypeAnnotations != null) Collections.shuffle(method.invisibleTypeAnnotations, random);
                if (method.visibleAnnotations != null) Collections.shuffle(method.visibleAnnotations, random);
                if (method.visibleLocalVariableAnnotations != null) Collections.shuffle(method.visibleLocalVariableAnnotations, random);
                if (method.visibleTypeAnnotations != null) Collections.shuffle(method.visibleTypeAnnotations, random);

                if (method.localVariables != null) Collections.shuffle(method.localVariables, random);
                if (method.parameters != null) Collections.shuffle(method.parameters, random);

                Collections.shuffle(method.exceptions, random);
            }
        }
        for (Object o : node.fields.toArray()) {
            if (o instanceof FieldNode) {
                FieldNode field = (FieldNode) o;
                if (field.invisibleAnnotations != null) Collections.shuffle(field.invisibleAnnotations, random);
                if (field.invisibleTypeAnnotations != null) Collections.shuffle(field.invisibleTypeAnnotations, random);
                if (field.visibleAnnotations != null) Collections.shuffle(field.visibleAnnotations, random);
                if (field.visibleTypeAnnotations != null) Collections.shuffle(field.visibleTypeAnnotations, random);
            }
        }
    }

    public static void sortLocalVariables(MethodNode node) {
        HashMap<Integer, Integer> localsMapping = new HashMap<>();
        HashMap<Integer, Integer> sizeRequired = new HashMap<>();
        int currentSize = 0;
        if ((node.access & ACC_STATIC) == 0) {
            localsMapping.put(0, 0);
            currentSize++;
        }

        Type[] args = Type.getArgumentTypes(node.desc);
        for (Type arg : args) {
            localsMapping.put(currentSize, currentSize);
            currentSize += arg.getSize();
        }

        // generate mapping
        AbstractInsnNode[] array = node.instructions.toArray();
        LinkedList<Integer> order = new LinkedList<>();
        for (AbstractInsnNode ain : array) {
            if (!(ain instanceof VarInsnNode)) continue;
            int index = ((VarInsnNode) ain).var;
            switch (ain.getOpcode()) {
                case ISTORE:
                case FSTORE:
                case ASTORE:
                    if (!sizeRequired.containsKey(index)) sizeRequired.put(index, 1);
                    break;
                case LSTORE:
                case DSTORE:
                    sizeRequired.put(index, 2);
            }
            order.add(index);
        }

        for (Integer index : order) {
            if (!localsMapping.containsKey(index)) {
                localsMapping.put(index, currentSize);
                currentSize += sizeRequired.get(index);
            }
        }

//        for (AbstractInsnNode ain : array) {
//            switch (ain.getOpcode()) {
//                case ISTORE:
//                case FSTORE:
//                case LSTORE:
//                case DSTORE:
//                case ASTORE: {
//                    int index = ain.var;
//                    if (!localsMapping.containsKey(index)) {
//                        localsMapping.put(index, currentSize);
//
//                        currentSize++;
//                        if (ain.getOpcode() == DSTORE || ain.getOpcode() == LSTORE) currentSize++;
//                    }
//                }
//            }
//        }

        // apply mapping
        for (AbstractInsnNode ain : array) {
            switch (ain.getOpcode()) {
                case ILOAD:
                case FLOAD:
                case LLOAD:
                case DLOAD:
                case ALOAD:
                case ISTORE:
                case FSTORE:
                case LSTORE:
                case DSTORE:
                case ASTORE: {
                    int index = ((VarInsnNode) ain).var;
                    if (localsMapping.containsKey(index)) {
                        node.instructions.set(ain, new VarInsnNode(ain.getOpcode(), localsMapping.get(index)));
                    } else throw new AssertionError(String.format("SLV/ Shouldn't happen (%s: %s)", ain.getOpcode(), index));
                    break;
                }
                case IINC: {
                    int index = ((IincInsnNode) ain).var;
                    if (localsMapping.containsKey(index)) {
                        ((IincInsnNode) ain).var = localsMapping.get(index);
                    } else throw new AssertionError(String.format("SLV/ Shouldn't happen (%s: %s) !#2", ain.getOpcode(), index));
                    break;
                }
            }
        }

        node.maxLocals = currentSize;
    }
}