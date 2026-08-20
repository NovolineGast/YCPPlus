package com.yumegod.obfuscator.jobf.transformer.impl;

import com.yumegod.obfuscation.FlowObfuscate;
import com.yumegod.obfuscation.NoFlowObfuscate;
import com.yumegod.obfuscation.NoStringObfuscate;
import com.yumegod.obfuscation.StringObfuscate;
import com.yumegod.obfuscator.jobf.transformer.SingledClassTransformer;
import com.yumegod.obfuscator.jobf.transformer.impl.flow.ControlFlowObfuscator;
import com.yumegod.obfuscator.jobf.utils.ASMUtil;
import com.yumegod.obfuscator.jobf.utils.namefactory.NameFactory;
import com.yumegod.obfuscator.jobf.utils.namefactory.NameFactoryUtils;
import com.yumegod.obfuscator.utils.AnnotationUtils;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static org.objectweb.asm.Opcodes.*;

public class StringObfuscator extends SingledClassTransformer {
    private static final String fieldDesc = Type.getType(String[].class).getInternalName();
    private static final String methodDesc = Type.getMethodDescriptor(Type.getType(String.class), Type.INT_TYPE, Type.INT_TYPE);
    private static final NameFactory factory = NameFactoryUtils.getMemberNameFactory();
    private static final HashSet<String> conflictedName = new HashSet<>(1024);

    @Override
    public void process(ClassNode node) {
        if ((node.access & ACC_INTERFACE) != 0) return;
        if (AnnotationUtils.noneAnnotation(node.invisibleAnnotations, Type.getDescriptor(StringObfuscate.class)))
            return;
        if (AnnotationUtils.hasAnnotation(node.invisibleAnnotations, Type.getDescriptor(NoStringObfuscate.class)))
            return;

        String fieldName = nextFieldName(node);
        String methodName = nextMethodName(node);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int seed = random.nextInt() & 0xFFFF;
        LinkedList<String> strings = new LinkedList<>();

        for (MethodNode method : node.methods.toArray(new MethodNode[0])) {
            if (method.instructions != null && method.instructions.size() > 0) {
                while (!processMethod(node.name, method, methodName, methodDesc, strings, seed)) {
                    mkMethod(node, fieldName, methodName, strings, seed);
                    strings = new LinkedList<>();
                    fieldName = nextFieldName(node);
                    methodName = nextMethodName(node);
                    seed = random.nextInt() & 0xFFFF;
                }
                if (strings.size() > 256 || strings.stream().mapToInt(String::length).sum() > 8000) {
                    mkMethod(node, fieldName, methodName, strings, seed);
                    strings = new LinkedList<>();
                    fieldName = nextFieldName(node);
                    methodName = nextMethodName(node);
                    seed = random.nextInt() & 0xFFFF;
                }
            }
        }

        if (strings.isEmpty()) return;
        mkMethod(node, fieldName, methodName, strings, seed);
    }

    @Override
    public void process(Map<String, ClassNode> classes) {
        for (ClassNode classNode : classes.values()) {
            for (MethodNode node : classNode.methods) {
                if (methodDesc.equals(node.desc)) {
                    conflictedName.add(node.name);
                }
            }
            for (FieldNode node : classNode.fields) {
                if (fieldDesc.equals(node.desc)) {
                    conflictedName.add(node.name);
                }
            }
        }
        super.process(classes);
    }

    private static String nextFieldName(ClassNode node) {
        return nextName();
    }

    private static String nextMethodName(ClassNode node) {
        return nextName();
    }

    private static String nextName() {
        String name;
        do {
            name = factory.nextName();
        } while (conflictedName.contains(name));
        return name;
    }

    private static void mkMethod(ClassNode node, String fieldName, String methodName, LinkedList<String> strings, int seed) {
        boolean isInterface = (node.access & ACC_INTERFACE) != 0;
        node.fields.add(new FieldNode((isInterface ? ACC_PUBLIC : ACC_PRIVATE) | ACC_STATIC, fieldName, fieldDesc, null, null));
        MethodNode e = new MethodNode((isInterface ? ACC_PUBLIC : ACC_PRIVATE) | ACC_STATIC | ACC_SYNTHETIC | ACC_DEPRECATED | ACC_BRIDGE, methodName, StringObfuscator.methodDesc, null, null);
        mkMethod(e, node.name, fieldName, strings, seed);
        node.methods.add(e);
        if (AnnotationUtils.haveOrNotHave(node.invisibleAnnotations,
                Type.getDescriptor(NoFlowObfuscate.class),
                Type.getDescriptor(FlowObfuscate.class))) ControlFlowObfuscator.processMethod(e);
        MethodNode clInit = ASMUtil.findClInit(node);
        {
            InsnList nodes = new InsnList();
            nodes.add(generateIntPush(strings.size()));
            nodes.add(new TypeInsnNode(ANEWARRAY, Type.getType(String.class).getInternalName()));
            nodes.add(new FieldInsnNode(PUTSTATIC, node.name, fieldName, Type.getType(String[].class).getDescriptor()));
            clInit.instructions.insertBefore(clInit.instructions.getFirst(), nodes);
        }
        computeMax(e, node.name);
    }

    private static void mkMethod(MethodNode node, String ownerName, String fieldName, List<String> strings, int seed) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int encryptSeed = random.nextInt(65536) & 0xFFFF;
        InsnList instructions = node.instructions;
        instructions.add(new VarInsnNode(ILOAD, 0));
        instructions.add(generateIntPush(seed));
        instructions.add(new InsnNode(IXOR));
        int varStrIndex = 2, varEncryptedStr = 3, varCharArrOut = 4, varEncryptedStrStartIndex = 5, varStrLength = 6,
                varDecryptKey1 = 7, varEncryptedStrCArray = 8, varIterIndex = 9;
        int maxLocal = 10;
        instructions.add(new VarInsnNode(ISTORE, varStrIndex));
        String strArrDesc = Type.getType(String[].class).getDescriptor();
        instructions.add(new FieldInsnNode(GETSTATIC, ownerName, fieldName, strArrDesc));
        instructions.add(new VarInsnNode(ILOAD, varStrIndex));
        instructions.add(new InsnNode(AALOAD));

        LabelNode quit = new LabelNode(), start = new LabelNode();
        instructions.add(new JumpInsnNode(IFNONNULL, quit));

        {
            List<String> list = xorStrings(strings, encryptSeed);
            String encrypted = String.join("", list);
            instructions.add(new LdcInsnNode(encrypted));
            instructions.add(new VarInsnNode(ASTORE, varEncryptedStr));
//            instructions.add(new TypeInsnNode(NEW, Type.getType(StringBuilder.class).getInternalName()));
//            instructions.add(new InsnNode(DUP));
//            instructions.add(new MethodInsnNode(INVOKESPECIAL, Type.getType(StringBuilder.class).getInternalName(), "<init>", "()V"));
            // out char[]
            instructions.add(new InsnNode(ACONST_NULL));
            instructions.add(new VarInsnNode(ASTORE, varCharArrOut));
            instructions.add(start);
            {
                LabelNode beforeProcess = new LabelNode();

                LabelNode labelNode = new LabelNode();
                instructions.add(new InsnNode(ICONST_M1));
                instructions.add(new VarInsnNode(ISTORE, varEncryptedStrStartIndex));
                LinkedList<InsnList> insnLists = new LinkedList<>();
                // index 0
                {
                    InsnList nodes = new InsnList();
                    nodes.add(new VarInsnNode(ILOAD, varStrIndex));
                    nodes.add(new JumpInsnNode(IFNE, labelNode));
                    nodes.add(new InsnNode(ICONST_0));
                    nodes.add(new VarInsnNode(ISTORE, varEncryptedStrStartIndex));
                    nodes.add(new JumpInsnNode(GOTO, beforeProcess));
                    nodes.add(labelNode);
                    insnLists.add(nodes);
                }
                int totalLength = list.get(0).toCharArray().length;

                for (int i = 1; i < list.size(); i++) {
                    InsnList nodes = new InsnList();
                    LabelNode blockEnd = new LabelNode();
                    nodes.add(new VarInsnNode(ILOAD, varStrIndex));
                    nodes.add(generateIntPush(i));
                    nodes.add(new JumpInsnNode(IF_ICMPNE, blockEnd));
                    nodes.add(generateIntPush(totalLength));
                    nodes.add(new VarInsnNode(ISTORE, varEncryptedStrStartIndex));
                    if (random.nextBoolean()) nodes.add(new JumpInsnNode(GOTO, beforeProcess));
                    nodes.add(blockEnd);
                    totalLength += list.get(i).toCharArray().length;
                    insnLists.add(nodes);
                }

                Collections.shuffle(insnLists);
                for (InsnList insnList : insnLists) {
                    instructions.add(insnList);
                }

                instructions.add(beforeProcess);
            }
            {
                instructions.add(new VarInsnNode(ILOAD, 1));
                instructions.add(generateIntPush(seed));
                obfuscatedIXor(instructions);
                instructions.add(new VarInsnNode(ISTORE, varStrLength));
                instructions.add(new VarInsnNode(ILOAD, varStrLength));
                instructions.add(generateIntPush(0xFFFF));
                instructions.add(new InsnNode(IAND));
                instructions.add(new InsnNode(I2C));
                instructions.add(new VarInsnNode(ISTORE, varDecryptKey1));
            }
            {
                // encryptedStr.toCharArray() -> slot 8
                instructions.add(new VarInsnNode(ALOAD, varEncryptedStr));
                instructions.add(new MethodInsnNode(INVOKEVIRTUAL, Type.getType(String.class).getInternalName(), "toCharArray", "()[C"));
                instructions.add(new VarInsnNode(ASTORE, varEncryptedStrCArray));
            }
            instructions.add(new VarInsnNode(ILOAD, varStrLength));
            instructions.add(new IntInsnNode(NEWARRAY, 5)); // but I don't know why it should be 5...
            instructions.add(new VarInsnNode(ASTORE, varCharArrOut));
            {
                // simple obfuscation
                for (int size = instructions.size(); size < 12000; size += 1500) {
                    switch (random.nextInt(6)) {
                        case 0:
                            varStrIndex = obfPush(instructions, varStrIndex, maxLocal);
                            maxLocal++;
                            break;
                        case 1:
                            varStrLength = obfPush(instructions, varStrLength, maxLocal);
                            maxLocal++;
                            break;
                        case 2:
                            varEncryptedStrStartIndex = obfPush(instructions, varEncryptedStrStartIndex, maxLocal);
                            maxLocal++;
                            break;
                        case 3:
                            varDecryptKey1 = obfPush(instructions, varDecryptKey1, maxLocal);
                            maxLocal++;
                            break;
                        case 4:
                            instructions.add(generateIntPush(random.nextInt()));
                            instructions.add(random.nextBoolean() ? generateIntPush(random.nextInt()) : new InsnNode(DUP));
                            instructions.add(new InsnNode(POP2));
                            break;
                        case 5:
                            instructions.add(generateIntPush(random.nextInt()));
                            instructions.add(new InsnNode(POP));
                            break;
                        default:
                    }
                    if (random.nextBoolean()) {
                        int i = 0;
                        switch (random.nextInt(3)) {
                            case 0:
                                i = varStrIndex;
                                break;
                            case 1:
                                i = varStrLength;
                                break;
                            case 2:
                                i = varEncryptedStrStartIndex;
                                break;
                        }
                        instructions.add(new VarInsnNode(ILOAD, i));
                        instructions.add(new JumpInsnNode(IFLT, start));
                    }
                }
            }
            // decrypt loop
            {
                LabelNode loop = new LabelNode(), out = new LabelNode();
                instructions.add(new VarInsnNode(ILOAD, varEncryptedStrStartIndex));
                instructions.add(new VarInsnNode(ISTORE, varIterIndex));
                // for each
                instructions.add(loop);
                // i >= char_start_at + char_length -> goto out
                instructions.add(new VarInsnNode(ILOAD, varEncryptedStrStartIndex)); // 1 ++
                instructions.add(new VarInsnNode(ILOAD, varIterIndex)); // 2 ++
                instructions.add(new InsnNode(SWAP));
                instructions.add(new VarInsnNode(ILOAD, varStrLength)); // 3 ++
                instructions.add(new InsnNode(IADD)); // 2 : i (len + start)
                instructions.add(new JumpInsnNode(IF_ICMPGE, out)); // 0
                // out char[i - char_start_at]
                instructions.add(new VarInsnNode(ILOAD, varIterIndex)); // 1 ++
                instructions.add(new VarInsnNode(ALOAD, varCharArrOut)); // 2 ++
                instructions.add(new InsnNode(SWAP));
                instructions.add(new VarInsnNode(ILOAD, varEncryptedStrStartIndex)); // 3 ++
                instructions.add(new InsnNode(ISUB)); // 2 : char[]#out (i - char_start_at)

                instructions.add(new InsnNode(SWAP));
                instructions.add(new InsnNode(SWAP));
                // = ...
                instructions.add(new VarInsnNode(ILOAD, varIterIndex)); // 3 ++
                instructions.add(new VarInsnNode(ALOAD, varEncryptedStrCArray)); // 4 ++
                instructions.add(new InsnNode(SWAP));
                instructions.add(new InsnNode(CALOAD)); // 3 : char[]#out, int#index, - (char[], int#index) + char
                instructions.add(new VarInsnNode(ILOAD, varDecryptKey1)); // 4 ++
                instructions.add(new IincInsnNode(varIterIndex, 1)); // i++
                obfuscatedIXor(instructions); // 3 : char[]#out, int#index, (char, int#str_len)
                instructions.add(generateIntPush(encryptSeed)); // 4 ++
                obfuscatedIXor(instructions); // 3 : char[]#out, int#index, char
                int tmpCharSlot = maxLocal++;
                instructions.add(new VarInsnNode(ISTORE, tmpCharSlot));
                instructions.add(new VarInsnNode(ILOAD, tmpCharSlot));

                instructions.add(new InsnNode(I2C));
                instructions.add(new InsnNode(CASTORE)); // 0  - (char[], int#index, char)
                for (int iter = 0; iter < 4; iter++)
                    if (random.nextBoolean()) {
                        int i = 0;
                        switch (random.nextInt(3)) {
                            case 0:
                                i = varStrIndex;
                                break;
                            case 1:
                                i = varStrLength;
                                break;
                            case 2:
                                i = varEncryptedStrStartIndex;
                                break;
                        }
                        instructions.add(new VarInsnNode(ILOAD, i));
                        instructions.add(new JumpInsnNode(IFLT, random.nextBoolean() ? start : out));
                    }
                instructions.add(new JumpInsnNode(GOTO, loop));
                instructions.add(out);

                instructions.add(new FieldInsnNode(GETSTATIC, ownerName, fieldName, strArrDesc));
                instructions.add(new VarInsnNode(ILOAD, varStrIndex));
                instructions.add(new TypeInsnNode(NEW, Type.getType(String.class).getInternalName()));
                instructions.add(new InsnNode(DUP));
                instructions.add(new VarInsnNode(ALOAD, varCharArrOut));
                instructions.add(new MethodInsnNode(INVOKESPECIAL, Type.getType(String.class).getInternalName(), "<init>", "([C)V"));
                instructions.add(new MethodInsnNode(INVOKEVIRTUAL, Type.getType(String.class).getInternalName(), "intern", Type.getMethodDescriptor(Type.getType(String.class))));
                instructions.add(new InsnNode(AASTORE));
            }
        }
        node.maxLocals = maxLocal;
        instructions.add(quit);

        instructions.add(new FieldInsnNode(GETSTATIC, ownerName, fieldName, strArrDesc));
        instructions.add(new VarInsnNode(ILOAD, 2));
        instructions.add(new InsnNode(AALOAD));
        instructions.add(new InsnNode(ARETURN));
    }

    private static int obfPush(InsnList instructions, int varStrIndex, int to) {
        instructions.add(new VarInsnNode(ILOAD, varStrIndex));
        instructions.add(new VarInsnNode(ISTORE, to));
        return to;
    }

    private static void obfuscatedIXor(InsnList instructions) {
        for (int i = 0; i < ThreadLocalRandom.current().nextInt(10); i++) {
            instructions.add(new InsnNode(SWAP));
        }
        instructions.add(new InsnNode(IXOR));
    }

    private static List<String> xorStrings(List<String> strings, int seed) {
        LinkedList<String> list = new LinkedList<>();
        for (String s : strings) {
            char[] array = s.toCharArray();
            for (int i = 0; i < array.length; i++) {
                array[i] = (char) (array[i] ^ (s.length() & 0xFFFF) ^ seed);
            }
            list.add(new String(array));
        }
        return list;
    }

    // Rotates the given char left by the specified number of bits
    private static char rotateCharLeft(char value, int shiftDist) {
        // Convert char to int (Java chars are implicitly promoted to ints in bitwise operations)
        int intValue = value;

        // Perform the rotation using the same logic as in the provided function
        intValue = (intValue << shiftDist) | (intValue >>> -shiftDist);

        // Cast the rotated integer back to a char
        return (char) intValue;
    }

    private boolean processMethod(String parent, MethodNode method, String name, String desc, LinkedList<String> strings, int seed) {
        for (AbstractInsnNode node : method.instructions.toArray()) {
            if (node instanceof LdcInsnNode) {
                LdcInsnNode ldc = (LdcInsnNode) node;
                if (ldc.cst instanceof String) {
                    String s = (String) ldc.cst;
                    int length = s.length();
                    int index;
                    if (strings.contains(s)) {
                        index = strings.indexOf(s);
                    } else {
                        index = strings.size();
                        if (length + strings.stream().mapToInt(String::length).sum() > 25565 && !strings.isEmpty())
                            return false;
                        strings.add(s);
                    }
                    method.instructions.insertBefore(node, getInsnList(parent, name, desc, index ^ seed, length ^ seed));
                    method.instructions.remove(node);
                    int sum = strings.stream().mapToInt(String::length).sum();
                    if (strings.size() > 300 || sum > 8000) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private InsnList getInsnList(String className, String name, String desc, int index, int length) {
        InsnList nodes = new InsnList();
        nodes.add(generateIntPush(index));
        nodes.add(generateIntPush(length));
        nodes.add(new MethodInsnNode(INVOKESTATIC, className, name, desc, false));
        return nodes;
    }

    private static AbstractInsnNode generateIntPush(int i) {
        if (i <= 5 && i >= -1) {
            return new InsnNode(i + 3); //iConst_i
        }
        if (i >= -128 && i <= 127) {
            return new IntInsnNode(BIPUSH, i);
        }

        if (i >= -32768 && i <= 32767) {
            return new IntInsnNode(SIPUSH, i);
        }
        return new LdcInsnNode(i);
    }
}
