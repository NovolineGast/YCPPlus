package com.yumegod.obfuscator.jobf.transformer.impl.reference;

import com.yumegod.obfuscation.InvokeDynamic;
import com.yumegod.obfuscator.jobf.transformer.SingledClassTransformer;
import com.yumegod.obfuscator.jobf.transformer.impl.flow.ControlFlowObfuscator;
import com.yumegod.obfuscator.jobf.utils.builder.InstructionBuilder;
import com.yumegod.obfuscator.jobf.utils.namefactory.NameFactoryUtils;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.LinkedList;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import static org.objectweb.asm.Opcodes.*;

public class InvokeDynamicObfuscator extends SingledClassTransformer {
    private static final String lookupClzName = Type.getInternalName(MethodHandles.Lookup.class);

    static final String nameSplitter = randomString(4);
    static final int rndConstantKey = ThreadLocalRandom.current().nextInt(0xFFFF), maxLength = 128;

    @Override
    public void process(ClassNode node) {
        final String className = node.name;
        String descriptor = MethodType.methodType(CallSite.class,// MethodHandles.Lookup.class, String.class, MethodType
                Object.class, String.class, Object.class, String.class).toMethodDescriptorString();
        final String bootstrapMethodName = NameFactoryUtils.simpleNextMemberName(node, descriptor);

        Handle bootstrapHandle = new Handle(H_INVOKESTATIC, node.name, bootstrapMethodName,
                descriptor, false);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int[] keySets = generateStrKeys(random);

        boolean obfuscated = false;

        boolean noConfiguration = Optional.ofNullable(node.invisibleAnnotations)
                .map(l -> l.stream().anyMatch(ann -> ann.desc.equals(Type.getDescriptor(InvokeDynamic.class))))
                .orElse(false) || DebugConstant.NO_CONFIGURATION;
        for (MethodNode method : node.methods) {
            if (!noConfiguration && (method.invisibleAnnotations == null || method.invisibleAnnotations.stream().noneMatch(ann -> ann.desc.equals(Type.getDescriptor(InvokeDynamic.class)))))
                continue;
            if (method.instructions == null || method.instructions.size() == 0) continue; // nothing to do...

            AbstractInsnNode[] ainArr = method.instructions.toArray();
            for (int i = 0; i < ainArr.length; i++) {
                AbstractInsnNode insnNode = ainArr[i];
                if (insnNode instanceof MethodInsnNode) {
                    if (insnNode.getOpcode() == INVOKESPECIAL) continue;
                    MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;
                    if (methodInsnNode.name.startsWith("<")) continue;
                    indyMethod(method, methodInsnNode, keySets, bootstrapHandle, i);
                    obfuscated = true;
                } else if (insnNode instanceof FieldInsnNode) {
//                    if (method.name.startsWith("<")) continue;
//                    if (DEBUG) continue;
                    FieldInsnNode fieldInsnNode = (FieldInsnNode) insnNode;
                    // TODO support internal type
                    if (fieldInsnNode.desc.length() == 1) continue;
                    indyField(method, fieldInsnNode, keySets, bootstrapHandle, i);
                    obfuscated = true;
                }
            }

            if (DEBUG) computeMax(method, method.name);
        }

        if (obfuscated) {
            node.methods.add(generateHandle(className, bootstrapMethodName, keySets));
        }
    }

    private static void indyMethod(MethodNode method, MethodInsnNode methodInsnNode, int[] keySets, Handle bootstrapHandle, int i) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        String name = encryptName(randomAlphanumericString(random.nextInt(1, 10)) + nameSplitter + // 0
                        methodInsnNode.owner.replace('/', '.') + nameSplitter + // 1
                        methodInsnNode.name + nameSplitter + // 2
                        methodInsnNode.desc + nameSplitter + // 3
                        randomString(methodInsnNode.getOpcode() == INVOKESTATIC ? 3 : 2) + nameSplitter// 4
                , keySets
        );

        if (name.length() + 4 < maxLength) {
            name += encryptName(randomAlphanumericString(maxLength - name.length()), keySets);
        }

        String desc = methodInsnNode.desc;
//        if (DEBUG) System.out.println(i + " " + encryptName(name, keySets));

        if (methodInsnNode.getOpcode() != INVOKESTATIC)
            desc = (methodInsnNode.owner.startsWith("[") ? "(" : "(L") + methodInsnNode.owner + (methodInsnNode.owner.endsWith(";") ? "" : ";") + desc.substring(1);

        InvokeDynamicInsnNode indyNode = new InvokeDynamicInsnNode(Integer.toString(random.nextInt(100)), desc, bootstrapHandle);
        indyNode.bsmArgs = new Object[]{name};
        method.instructions.insert(methodInsnNode, indyNode);
        method.instructions.remove(methodInsnNode);
    }

    private static void indyField(MethodNode method, FieldInsnNode fieldInsnNode, int[] keySets, Handle bootstrapHandle, int i) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int opcode = fieldInsnNode.getOpcode();
        int signLength = 0;
        switch (opcode) {
            case GETSTATIC:
                signLength = 4;
                break;
            case PUTSTATIC:
                signLength = 5;
                break;
            case GETFIELD:
                signLength = 6;
                break;
            case PUTFIELD:
                signLength = 7;
                break;
        }
        String name = encryptName(randomAlphanumericString(random.nextInt(1, 10)) + nameSplitter + // 0
                        fieldInsnNode.owner.replace('/', '.') + nameSplitter + // 1
                        fieldInsnNode.name + nameSplitter + // 2
                        Type.getType(fieldInsnNode.desc).getInternalName().replace('/', '.') + nameSplitter + // 3
                        randomString(signLength) + nameSplitter // 4
                , keySets
        );

        if (name.length() + 4 < maxLength) {
            name += encryptName(randomAlphanumericString(maxLength - name.length()), keySets);
        }
        String desc;
        switch (opcode) {
            case GETSTATIC:
                desc = "()" + fieldInsnNode.desc;
                break;
            case GETFIELD:
                desc = "(L" + fieldInsnNode.owner + ";)" + fieldInsnNode.desc;
                break;
            case PUTSTATIC:
                desc = "(" + fieldInsnNode.desc + ")V";
                break;
            case PUTFIELD:
                desc = "(L" + fieldInsnNode.owner + ";" + fieldInsnNode.desc + ")V";
                break;
            default:
                desc = "()V";
        }

        if (DEBUG) System.out.println(i + " " + encryptName(name, keySets) + " ----> " + desc);
        InvokeDynamicInsnNode indyNode = new InvokeDynamicInsnNode(Integer.toString(random.nextInt(100)), desc, bootstrapHandle);
        indyNode.bsmArgs = new Object[]{name};
        method.instructions.set(fieldInsnNode, indyNode);
    }

    private static String encryptName(String string, int[] keySets) {
        char[] array = string.toCharArray();
        for (int i = 0; i < array.length; i++) {
            array[i] = (char) (array[i] ^ keySets[i % keySets.length] ^ rndConstantKey);
        }
        return new String(array);
    }

    private static int[] generateStrKeys(ThreadLocalRandom random) {
        int[] keySets = new int[random.nextInt(5, 170)];
        for (int i = 0; i < keySets.length; i++) {
            keySets[i] = random.nextInt(-512, 512);
        }
        return keySets;
    }

    private static MethodNode generateHandle(String className, String name, int[] strKeys) {
        MethodNode methodNode = new MethodNode(ACC_PRIVATE | ACC_STATIC | ACC_DEPRECATED | ACC_SYNTHETIC | ACC_BRIDGE, name, "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/invoke/CallSite;", null, null);
        InsnList nodes = methodNode.instructions;
        LabelNode start = new LabelNode(), end = new LabelNode(), handler = new LabelNode();

        nodes.add(start);

        // local var map:
        //  0 -> lookup
        //  1 -> name
        //  2 -> MethodType (nothing to use)
        //  3 -> String (should be decoded)

        // name decode block
        int charArrLocation = 12;
        {
            LabelNode decodeIn = new LabelNode(), decodeOut = new LabelNode();

            InstructionBuilder builder = new InstructionBuilder();
            // string.toCharArray() -> charArrLocation
            builder.varInsn(ALOAD, 3);
            builder.methodInsn(INVOKEVIRTUAL, Type.getInternalName(String.class), "toCharArray", "()[C");
            builder.varInsn(ASTORE, charArrLocation);
            // "index"
            builder.number(0);
            builder.varInsn(ISTORE, 4);
            // prevent local mismatch
            builder.number(0);
            builder.varInsn(ISTORE, 5);

            builder.label(decodeIn);
            builder.varInsn(ILOAD, 4);
            builder.varInsn(ALOAD, charArrLocation);
            builder.insn(ARRAYLENGTH);
            builder.jump(IF_ICMPGE, decodeOut);
            builder.varInsn(ILOAD, 4);
            builder.number(strKeys.length);
            builder.insn(IREM);
            nodes.add(builder.getList());
            builder = new InstructionBuilder();
            LabelNode keyGetterEnd = new LabelNode();
            LinkedList<LabelNode> switchTable = new LinkedList<>();
            // generate switch map
            // iterate over array
            // store key to 5
            for (int key : strKeys) {
                LabelNode e = new LabelNode();
                switchTable.add(e);
                builder.label(e);
                builder.number(key);
                builder.varInsn(ISTORE, 5);
                builder.jump(GOTO, keyGetterEnd);
            }
            builder.label(keyGetterEnd);
            nodes.add(new TableSwitchInsnNode(0, strKeys.length - 1, keyGetterEnd, switchTable.toArray(new LabelNode[0])));
            nodes.add(builder.getList());

            builder = new InstructionBuilder();
            builder.varInsn(ALOAD, charArrLocation);
            builder.varInsn(ILOAD, 4);
            builder.insn(CALOAD);
            builder.varInsn(ILOAD, 5);
            builder.insn(IXOR);
            builder.number(rndConstantKey);
            builder.insn(IXOR);
            // c(arr)
            builder.varInsn(ALOAD, charArrLocation);
            builder.insn(SWAP);
            // (arr)ci
            builder.varInsn(ILOAD, 4);
            builder.insn(SWAP);
            // (arr)ic
            builder.insn(I2C);
            builder.insn(CASTORE);
            builder.add(new IincInsnNode(4, 1));
            builder.jump(GOTO, decodeIn);
            builder.label(decodeOut);
            nodes.add(builder.getList());
            nodes.add(new TypeInsnNode(NEW, Type.getType(String.class).getInternalName()));
            nodes.add(new InsnNode(DUP));
            nodes.add(new VarInsnNode(ALOAD, charArrLocation));
            nodes.add(new MethodInsnNode(INVOKESPECIAL, Type.getType(String.class).getInternalName(), "<init>", "([C)V"));
            nodes.add(new VarInsnNode(ASTORE, 1));
        }
        // 0 -> lookup
        // 1 -> name (decoded)
        // 2 -> MethodType (nothing to use)
        // used // unneeded:
        //  3: char[]
        //  4: int
        //  5: int

        InstructionBuilder builder = new InstructionBuilder();
        // 6: String[]
        {
            builder.varInsn(ALOAD, 1);
            builder.ldc(nameSplitter);
            builder.methodInsn(INVOKEVIRTUAL, Type.getInternalName(String.class), "split", "(Ljava/lang/String;)[Ljava/lang/String;");
            builder.varInsn(ASTORE, 6);
        }
        // 8: int // flag
        {
            builder.varInsn(ALOAD, 6);
            builder.number(4);
            builder.insn(AALOAD);
            builder.methodInsn(INVOKEVIRTUAL, Type.getInternalName(String.class), "length", "()I");
            builder.varInsn(ISTORE, 8);
        }
        LabelNode fieldInsnStart = new LabelNode();
        builder.varInsn(ILOAD, 8);
        builder.number(4);
        builder.jump(IF_ICMPGE, fieldInsnStart);
        // java/lang/invoke/MethodType -> 7
        {
            builder.varInsn(ALOAD, 6);
            builder.number(3);
            builder.insn(AALOAD);
            builder.ldc(Type.getType("L" + className + ";"));
            builder.methodInsn(INVOKEVIRTUAL, Type.getInternalName(Class.class), "getClassLoader", "()Ljava/lang/ClassLoader;");
            builder.methodInsn(INVOKESTATIC, Type.getInternalName(MethodType.class), "fromMethodDescriptorString", "(Ljava/lang/String;Ljava/lang/ClassLoader;)Ljava/lang/invoke/MethodType;");
            builder.varInsn(ASTORE, 7);
        }
        //prepare = | Class.forName($6[1]), $6[2], $7
        {
            builder.varInsn(ALOAD, 0);
            builder.type(CHECKCAST, lookupClzName);

            builder.varInsn(ALOAD, 6);
            builder.number(1);
            builder.insn(AALOAD);
            builder.methodInsn(INVOKESTATIC, Type.getInternalName(Class.class), "forName", "(Ljava/lang/String;)Ljava/lang/Class;");

            builder.varInsn(ALOAD, 6);
            builder.number(2);
            builder.insn(AALOAD);

            builder.varInsn(ALOAD, 7);
        }
        {
            builder.varInsn(ILOAD, 8);
            builder.number(3);
            LabelNode jmpToInvokeStatic = new LabelNode();
            builder.jump(IF_ICMPEQ, jmpToInvokeStatic);
            builder.methodInsn(INVOKEVIRTUAL, lookupClzName, "findVirtual", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;");
            builder.varInsn(ASTORE, 9);
            builder.jump(GOTO, end);
            builder.label(jmpToInvokeStatic);
        }
        {
            builder.methodInsn(INVOKEVIRTUAL, lookupClzName, "findStatic", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;");
            builder.varInsn(ASTORE, 9);
            builder.jump(GOTO, end);
        }
        builder.label(fieldInsnStart);
        // prepare = Class.forName($6[1]), $6[2], Class.forName($6[3])
        {
            builder.varInsn(ALOAD, 0);
            builder.type(CHECKCAST, lookupClzName);

            builder.varInsn(ALOAD, 6);
            builder.number(1);
            builder.insn(AALOAD);
            builder.methodInsn(INVOKESTATIC, Type.getInternalName(Class.class), "forName", "(Ljava/lang/String;)Ljava/lang/Class;");

            builder.varInsn(ALOAD, 6);
            builder.number(2);
            builder.insn(AALOAD);

            builder.varInsn(ALOAD, 6);
            builder.number(3);
            builder.insn(AALOAD);
            builder.varInsn(ASTORE, 11);
            {
                builder.varInsn(ALOAD, 11);
                builder.methodInsn(INVOKESTATIC, Type.getInternalName(Class.class), "forName", "(Ljava/lang/String;)Ljava/lang/Class;");
            }
        }
        // field
//            GETSTATIC: signLength = 4;
//            PUTSTATIC: signLength = 5;
//            GETFIELD : signLength = 6;
//            PUTFIELD : signLength = 7;
        {
            builder.varInsn(ILOAD, 8);
            builder.number(4);
            LabelNode jmpToPutStatic = new LabelNode();
            builder.jump(IF_ICMPNE, jmpToPutStatic);
            builder.methodInsn(INVOKEVIRTUAL, lookupClzName, "findStaticGetter", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;");
            builder.varInsn(ASTORE, 9);
            builder.jump(GOTO, end);
            builder.label(jmpToPutStatic);
        }
        {
            builder.varInsn(ILOAD, 8);
            builder.number(5);
            LabelNode jmpToGet = new LabelNode();
            builder.jump(IF_ICMPNE, jmpToGet);
            builder.methodInsn(INVOKEVIRTUAL, lookupClzName, "findStaticSetter", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;");
            builder.varInsn(ASTORE, 9);
            builder.jump(GOTO, end);
            builder.label(jmpToGet);
        }
        {
            builder.varInsn(ILOAD, 8);
            builder.number(6);
            LabelNode jmpToPut = new LabelNode();
            builder.jump(IF_ICMPNE, jmpToPut);
            builder.methodInsn(INVOKEVIRTUAL, lookupClzName, "findGetter", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;");
            builder.varInsn(ASTORE, 9);
            builder.jump(GOTO, end);
            builder.label(jmpToPut);
        }
        {
            builder.methodInsn(INVOKEVIRTUAL, lookupClzName, "findSetter", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;");
            builder.varInsn(ASTORE, 9);
            builder.jump(GOTO, end);
        }
        // catch
        {
            builder.label(handler);
            // debug only
//            builder.methodInsn(INVOKEVIRTUAL, Type.getInternalName(Throwable.class), "printStackTrace", "()V");
            builder.insn(ACONST_NULL);
            builder.insn(ARETURN);
        }
        // return new ConstantCallSite(methodHandle);
        {
            builder.label(end);
            builder.add(new TypeInsnNode(NEW, Type.getInternalName(ConstantCallSite.class)));
            builder.insn(DUP);
            builder.varInsn(ALOAD, 9);
            builder.methodInsn(INVOKESPECIAL, Type.getInternalName(ConstantCallSite.class), "<init>", "(Ljava/lang/invoke/MethodHandle;)V");
            builder.varInsn(ASTORE, 10);
            builder.varInsn(ALOAD, 10);
            builder.insn(ARETURN);
        }
        nodes.add(builder.getList());
        methodNode.tryCatchBlocks.add(new TryCatchBlockNode(start, end, handler, "java/lang/Throwable"));
        AbstractInsnNode[] arr = nodes.toArray();
        if (!computeMax(methodNode, name) && DEBUG) for (int i = 0; i < arr.length; i++) {
            logger.error(i + " | " + arr[i].getOpcode());
        }

        methodNode.maxLocals = 20;
        ControlFlowObfuscator.processSimple(methodNode);
        ControlFlowObfuscator.processIntObf(methodNode, null);
//        ControlFlowObfuscator.processMethod(methodNode);
        return methodNode;
    }

    private static String randomString(int length) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        char[] chars = new char[length];
        for (int i = 0; i < length; i++) {
            chars[i] = ((char) (random.nextInt(0x5000) + 0x0020));
        }
        return new String(chars);
    }

    private static String randomAlphanumericString(int length) {
        char[] string = new char[length];
        ThreadLocalRandom random = ThreadLocalRandom.current();
        char[] allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();

        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(allowedChars.length);
            string[i] = allowedChars[randomIndex];
        }

        return new String(string);
    }

    @SuppressWarnings("unused")
    private static void test() {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
    }

    private static final class DebugConstant {
        // obfuscate all methods
        final static boolean NO_CONFIGURATION = false;
    }
}
