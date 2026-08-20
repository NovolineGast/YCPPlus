package com.yumegod.obfuscator.jobf.transformer.impl.flow;

import com.yumegod.obfuscation.FlowObfuscate;
import com.yumegod.obfuscation.NoFlowObfuscate;
import com.yumegod.obfuscator.YumeCloudProtection;
import com.yumegod.obfuscator.jobf.transformer.SingledClassTransformer;
import com.yumegod.obfuscator.jobf.transformer.impl.NumberObfuscator;
import com.yumegod.obfuscator.jobf.utils.ASMUtil;
import com.yumegod.obfuscator.Main;
import com.yumegod.obfuscator.enums.NumberObfuscationMode;
import com.yumegod.obfuscator.jobf.utils.namefactory.NameFactoryUtils;
import com.yumegod.obfuscator.utils.AnnotationUtils;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static org.objectweb.asm.Opcodes.*;

public class ControlFlowObfuscator extends SingledClassTransformer {
    static Map<String, FlowIntFlag> packageFlags = new HashMap<>();

    @Override
    public void process(ClassNode node) {
        if (AnnotationUtils.haveOrNotHave(node.invisibleAnnotations, Type.getDescriptor(NoFlowObfuscate.class),Type.getDescriptor(FlowObfuscate.class)))
            return;

        boolean shouldObfuscateNumber = NumberObfuscator.shouldProcess(node);
        NumberObfuscationMode numberObfuscationMode = NumberObfuscator.MODE;
//        ThreadLocalRandom random = ThreadLocalRandom.current();
        FlowIntFlag flag = null;
        if (numberObfuscationMode == NumberObfuscationMode.AGGRESSIVE)
            flag = packageFlags.get(node.name.contains("/") ? node.name.substring(0, node.name.lastIndexOf('/')) : "");
        else if (numberObfuscationMode == NumberObfuscationMode.NORMAL) {
            flag = new FlowIntFlag(node.name, NameFactoryUtils.simpleNextMemberName(node, "I"), /*random.nextInt()*/0, node);
        }

        for (MethodNode method : node.methods) {
            if (method.instructions != null && method.instructions.size() > 2) {
                processMethod(method);
//                BlockRandomSorter.doSort(method.instructions);
                if (shouldObfuscateNumber && Arrays.stream(method.instructions.toArray()).anyMatch(ControlFlowObfuscator::isIntegerNumber)) {
                    if (ASMUtil.calculateSize(method) < 0x7FFF) {
                        processIntObf(method, flag);
                    }
                }
            }
        }
        if (numberObfuscationMode == NumberObfuscationMode.NORMAL) {
            flag.releaseField();
        }
    }

    @Override
    public void process(Map<String, ClassNode> classes) {
        if (YumeCloudProtection.safeMode) return;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (NumberObfuscator.MODE == NumberObfuscationMode.AGGRESSIVE) for (Map.Entry<String, ClassNode> entry : classes.entrySet()) {
            ClassNode node = entry.getValue();
            if ((node.access & ACC_PUBLIC) != 0 && (node.access & ACC_ABSTRACT) == 0 && (node.access & ACC_INTERFACE) == 0 && (node.access & ACC_ENUM) == 0 && (node.access & ACC_FINAL) == 0) {
                String thePackage = node.name.contains("/") ? node.name.substring(0, node.name.lastIndexOf('/')) : "";
                FlowIntFlag flag = packageFlags.get(thePackage);
                if ((flag == null || random.nextInt(10) == 5) &&
                        Optional.ofNullable(node.invisibleAnnotations).map(it -> it.stream().anyMatch(anno -> !anno.desc.startsWith("Lorg/spongepowered/asm/"))).orElse(true) &&
                        (node.access & ACC_PUBLIC) != 0) {
                    flag = new FlowIntFlag(node.name, NameFactoryUtils.simpleNextMemberName(node, "I"), 0/*random.nextInt()*/, node);
                }
                packageFlags.put(thePackage, flag);
            }
        }
        packageFlags.entrySet().stream().filter(entry -> entry.getValue() == null).collect(Collectors.toSet()).forEach(entry -> {
            packageFlags.remove(entry.getKey());
//            ClassNode node = new ClassNode();
//            node.visit(ASM9, ACC_PUBLIC, entry.getKey() + "/" + randomUniqueName(), null, Type.getInternalName(Object.class), null);
//            String key = node.name + ".class";
//            classes.put(key, node);
//            NativeObfuscator.logger.info(key);
//            packageFlags.put(entry.getKey(), new FlowIntFlag(node.name, randomUniqueName(), random.nextInt(), node));
        });
        super.process(classes);
        for (FlowIntFlag flag : packageFlags.values()) {
            flag.releaseField();
        }
    }

    public static void processMethod(MethodNode node) {
        ArrayList<AbstractInsnNode> whitelisted = new ArrayList<>();
        Optional.ofNullable(node.tryCatchBlocks).ifPresent(exs -> exs.forEach(ex -> {
            whitelisted.add(ex.start);
            whitelisted.add(ex.end);
            whitelisted.add(ex.handler);
        }));

//        JumpForwardAdapter.processRndJumpForward(node);
        processInsn(node, whitelisted, 10, 30, 0);
        if (ASMUtil.calculateSize(node) < 0x3FFF) processInsn(node, whitelisted, 3, 10, 2);
        computeMax(node, node.name);
    }

    public static void processSimple(MethodNode node) {
        ArrayList<AbstractInsnNode> whitelisted = new ArrayList<>();
        Optional.ofNullable(node.tryCatchBlocks).ifPresent(exs -> exs.forEach(ex -> {
            whitelisted.add(ex.start);
            whitelisted.add(ex.end);
            whitelisted.add(ex.handler);
        }));
        processInsn(node, whitelisted, 10, 30, 0);
        processInsn(node, whitelisted, 2, 6, 1);
        computeMax(node, node.name);
    }

    public static void processIntObf(MethodNode node, FlowIntFlag flag) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int currentFlag = random.nextInt();
        int flagSlot = node.maxLocals;
        node.maxLocals++;
        AbstractInsnNode[] array = node.instructions.toArray();
        boolean isClInit = node.name.equals("<clinit>");
        for (int i = 0; i < array.length; i++) {
            if (isClInit && i < 3) continue;
            AbstractInsnNode ain = array[i];
            if (random.nextInt(20) == 6) {
                InsnList nodes = new InsnList();
                int tmpFlag = random.nextInt();
                nodes.add(new VarInsnNode(ILOAD, flagSlot));
                nodes.add(generateIntPush(tmpFlag));
                nodes.add(new InsnNode(IXOR));
                tmpFlag ^= currentFlag;
                int delta = currentFlag - tmpFlag;
                nodes.add(generateIntPush(Math.abs(delta)));
                nodes.add(new InsnNode(delta >= 0 ? IADD : ISUB));
                nodes.add(new VarInsnNode(ISTORE, flagSlot));
                node.instructions.insertBefore(ain, nodes);
            }
            if (!isIntegerNumber(ain)) continue;
            int value = getIntValue(ain);
            InsnList nodes = new InsnList();
            if (random.nextInt(10) < 2) {
                int rnd2flag = random.nextInt();
                nodes.add(generateIntPush(rnd2flag));
                nodes.add(generateIntPush(rnd2flag ^ value ^ currentFlag));
                nodes.add(new InsnNode(IXOR));
            } else nodes.add(generateIntPush(value ^ currentFlag));
            nodes.add(new VarInsnNode(ILOAD, flagSlot));
            nodes.add(new InsnNode(IXOR));
            node.instructions.insertBefore(ain, nodes);
            node.instructions.remove(ain);
        }
        {
            InsnList initNodes = new InsnList();
            if (flag == null || isClInit) {
                initNodes.add(generateIntPush(currentFlag));
            } else {
                initNodes.add(new FieldInsnNode(GETSTATIC, flag.flagClass, flag.flagField, "I"));
                initNodes.add(generateIntPush(currentFlag ^ flag.flagValue));
                initNodes.add(new InsnNode(IXOR));
            }
            initNodes.add(new VarInsnNode(ISTORE, flagSlot));
            node.instructions.insertBefore(node.instructions.getFirst(), initNodes);
        }
    }

    private static boolean isIntegerNumber(AbstractInsnNode ain) {
        if (ain.getOpcode() == BIPUSH || ain.getOpcode() == SIPUSH) {
            return true;
        }
        if (ain.getOpcode() >= ICONST_M1 && ain.getOpcode() <= ICONST_5) {
            return true;
        }
        if (ain instanceof LdcInsnNode) {
            LdcInsnNode ldc = (LdcInsnNode) ain;
            return ldc.cst instanceof Integer;
        }
        return false;
    }

    private static int getIntValue(AbstractInsnNode node) {
        if (node.getOpcode() >= ICONST_M1 && node.getOpcode() <= ICONST_5) {
            return node.getOpcode() - 3;
        }
        if (node.getOpcode() == SIPUSH || node.getOpcode() == BIPUSH) {
            return ((IntInsnNode) node).operand;
        }
        if (node instanceof LdcInsnNode && ((LdcInsnNode) node).cst instanceof Integer) {
            return (int) ((LdcInsnNode) node).cst;
        }
        throw new IllegalArgumentException(node + " isn't an integer node");
    }

    public static void processInsn(MethodNode node, ArrayList<AbstractInsnNode> whitelisted, int minInstructions, int maxInstructions, int selectType) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        AbstractInsnNode[] nodeArray = node.instructions.toArray();
        InsnList nodes = node.instructions;
        Arrays.stream(nodeArray).forEach(nodes::remove);
        boolean splitting = false;
        InsnList block = new InsnList();
        for (AbstractInsnNode ain : nodeArray) {
            if (splitting) {
                int opcode = ain.getOpcode();
                if (block.size() > maxInstructions ||
                        (block.size() >= minInstructions && random.nextGaussian() > 0.8) ||
                        ain instanceof TableSwitchInsnNode || ain instanceof LookupSwitchInsnNode ||
//                        ain instanceof MethodInsnNode ||
                        opcode == GOTO || opcode == NEW || (opcode >= IRETURN && opcode <= RETURN) || opcode == ATHROW || opcode == CHECKCAST
                        || (ain instanceof MethodInsnNode && opcode == INVOKESPECIAL && ((MethodInsnNode) ain).name.equals("<init>")) || whitelisted.contains(ain)) {
                    splitting = false;

                    LabelNode realTarget = new LabelNode(), fakeTarget = new LabelNode(), end = new LabelNode();
                    if (selectType == 0 || (selectType == 2 && random.nextBoolean())) {
                        // if-else style
                        int i1 = random.nextInt(-1, 6), i2 = random.nextInt(-1, 6);
                        nodes.add(generateIntPush(i1));
                        nodes.add(generateIntPush(i2));
                        nodes.add(new JumpInsnNode(IF_ICMPLT, i1 < i2 ? realTarget : fakeTarget));
                        nodes.add(new JumpInsnNode(GOTO, i1 < i2 ? fakeTarget : realTarget));
                    } else {
                        // switch-case style (or DashO style)
                        boolean randomSwitch = random.nextBoolean();
                        nodes.add(randomSwitch ? generateIntPush(0) : generateIntPush(random.nextInt(1, 6)));
                        nodes.add(new TableSwitchInsnNode(0, 0,/* default = */ randomSwitch ? fakeTarget : realTarget, randomSwitch ? realTarget : fakeTarget));
                    }
                    {
                        boolean before = random.nextBoolean();
                        InsnList insnList = mirrorAndMutate(block);
                        InsnList fakeBlock = new InsnList();
                        fakeBlock.add(fakeTarget);
                        fakeBlock.add(insnList);

                        if (before) {
                            nodes.add(fakeBlock);
                            nodes.add(new JumpInsnNode(GOTO, end));
                        }
                        nodes.add(realTarget);
                        nodes.add(block);

                        if (!before) {
                            nodes.add(new JumpInsnNode(GOTO, end));
                            nodes.add(fakeBlock);
                        }
                        nodes.add(end);
                        block = new InsnList();
                    }
                    nodes.add(ain);
                } else if (!(ain instanceof LineNumberNode) && !(ain instanceof FrameNode)) block.add(ain);
            } else {
                nodes.add(ain);
                if (random.nextBoolean() && !whitelisted.contains(ain)) splitting = true;
            }
        }

        nodes.add(block);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    private static InsnList mirrorAndMutate(InsnList raw) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        InsnList mirror = new InsnList();
        HashMap<LabelNode, LabelNode> map = new HashMap<LabelNode, LabelNode>() {
            @Override
            public LabelNode get(Object key) {
                return key instanceof LabelNode ? (LabelNode) key : null;
            }
        };

        AbstractInsnNode[] array = raw.toArray();

        for (AbstractInsnNode node : array) {
            int opcode = node.getOpcode();
            if (node instanceof InsnNode) {
                if (opcode >= ICONST_M1 && opcode <= ICONST_5)
                    mirror.add(generateIntPush(random.nextInt()));
                else if (opcode >= LCONST_0 && opcode <= LCONST_1)
                    mirror.add(random.nextBoolean() ? new InsnNode(random.nextBoolean() ? LCONST_0 : LCONST_1) : new LdcInsnNode(random.nextLong()));
                else if (opcode >= FCONST_0 && opcode <= FCONST_2)
                    mirror.add(new InsnNode(random.nextInt(FCONST_0, FCONST_2)));
                else if (opcode >= DCONST_0 && opcode <= DCONST_1)
                    mirror.add(new InsnNode(random.nextBoolean() ? DCONST_0 : DCONST_1));
                else if (opcode == ARRAYLENGTH) {
                    mirror.add(new InsnNode(POP));
                    mirror.add(generateIntPush(random.nextInt()));
                } else if (opcode == IADD || opcode == FADD ||
                        opcode == ISUB || opcode == FSUB ||
                        opcode == IMUL || opcode == FMUL ||
                        opcode == IDIV || opcode == FDIV ||
                        opcode == IREM || opcode == FREM ||
                        opcode == ISHL || opcode == ISHR ||
                        opcode == IUSHR || opcode == LSHL ||
                        opcode == LSHR || opcode == LUSHR ||
                        opcode == IAND || opcode == IOR || opcode == IXOR || opcode == POP) {
                    mirror.add(new InsnNode(POP));
                } else if (opcode == POP2 ||
                        opcode == LADD || opcode == DADD ||
                        opcode == LSUB || opcode == DSUB ||
                        opcode == LMUL || opcode == DMUL ||
                        opcode == LDIV || opcode == DDIV ||
                        opcode == LREM || opcode == DREM ||
                        opcode == LAND || opcode == LOR || opcode == LXOR) {
                    mirror.add(new InsnNode(POP2));
                } else {
                    mirror.add(node.clone(map));

                }
            } else if (node instanceof IntInsnNode) {
                if (opcode == BIPUSH || opcode == SIPUSH)
                    mirror.add(new IntInsnNode(opcode, random.nextInt(Byte.MIN_VALUE, Byte.MAX_VALUE)));
                else if (opcode == NEWARRAY) mirror.add(new IntInsnNode(opcode, ((IntInsnNode) node).operand));
                else mirror.add(new IntInsnNode(opcode, random.nextInt(Short.MIN_VALUE, Short.MAX_VALUE)));
            } else if (node instanceof VarInsnNode) {
                switch (opcode) {
                    case ILOAD:
                        mirror.add(new InsnNode(random.nextInt(ICONST_M1, ICONST_5)));
                        break;
                    case LLOAD:
                        mirror.add(new InsnNode(random.nextBoolean() ? LCONST_0 : LCONST_1));
                        break;
                    case FLOAD:
                        mirror.add(new InsnNode(random.nextInt(FCONST_0, FCONST_2)));
                        break;
                    case DLOAD:
                        mirror.add(new InsnNode(random.nextBoolean() ? DCONST_0 : DCONST_1));
                        break;
//                    case ALOAD: mirror.add(new InsnNode(ACONST_NULL)); break; // 有待商榷
                    case NEWARRAY:
                        mirror.add(new InsnNode(POP));
                        mirror.add(new InsnNode(ACONST_NULL));
                        // :/
                        break;
                    default: // store类型就先不管了
                        mirror.add(node.clone(map));
                }
            } else if (node instanceof TypeInsnNode) {
                // ?
                mirror.add(node.clone(map));
            } else if (node instanceof FieldInsnNode) {
                // ?
                FieldInsnNode fi = (FieldInsnNode) node;
                Type type = Type.getType(fi.desc);
                if (fi.getOpcode() == GETFIELD) {
                    mirror.add(new InsnNode(POP));
                }
                if (fi.getOpcode() == GETFIELD || fi.getOpcode() == GETSTATIC) {
                    switch (type.getSort()) {
                        case Type.BOOLEAN:
                        case Type.BYTE:
                        case Type.CHAR:
                        case Type.SHORT:
                        case Type.INT:
                            mirror.add(new InsnNode(random.nextInt(ICONST_M1, ICONST_5)));
                            break;
                        case Type.FLOAT:
                            mirror.add(new InsnNode(random.nextInt(FCONST_0, FCONST_2)));
                            break;
                        case Type.LONG:
                            mirror.add(new InsnNode(random.nextBoolean() ? LCONST_0 : LCONST_1));
                            break;
                        case Type.DOUBLE:
                            mirror.add(new InsnNode(random.nextBoolean() ? DCONST_0 : DCONST_1));
                            break;
                        case Type.ARRAY:
                        case Type.OBJECT:
                            mirror.add(new InsnNode(ACONST_NULL));
                            break;
                        default:
                            mirror.add(node.clone(map));
                    }
                } else {
                    mirror.add(node.clone(map));
                }
            } else if (node instanceof MethodInsnNode) {
                // ?
                MethodInsnNode methodInsnNode = (MethodInsnNode) node;
                if (!(node.getOpcode() == INVOKESPECIAL && methodInsnNode.name.startsWith("<"))) {
                    Type[] argumentTypes = Type.getArgumentTypes(methodInsnNode.desc);
                    for (int i = argumentTypes.length - 1; i >= 0; i--) {
                        Type type = argumentTypes[i];
                        if (type.getSize() == 1) mirror.add(new InsnNode(POP));
                        else if (type.getSize() == 2) mirror.add(new InsnNode(POP2));
                    }
                    if (node.getOpcode() != INVOKESTATIC) {
                        mirror.add(new InsnNode(POP));
                    }
                    switch (Type.getReturnType(methodInsnNode.desc).getSort()) {
                        case Type.VOID:
                            break;
                        case Type.BOOLEAN:
                            mirror.add(new InsnNode(ICONST_0));
                            break;
                        case Type.BYTE:
                        case Type.CHAR:
                        case Type.SHORT:
                        case Type.INT:
                            mirror.add(new InsnNode(random.nextInt(ICONST_M1, ICONST_5)));
                            break;
                        case Type.FLOAT:
                            mirror.add(new InsnNode(random.nextInt(FCONST_0, FCONST_2)));
                            break;
                        case Type.LONG:
                            mirror.add(new InsnNode(random.nextBoolean() ? LCONST_0 : LCONST_1));
                            break;
                        case Type.DOUBLE:
                            mirror.add(new InsnNode(random.nextBoolean() ? DCONST_0 : DCONST_1));
                            break;
                        default:
                            mirror.add(new InsnNode(ACONST_NULL));
                    }
                } else mirror.add(node.clone(map));
            } else if (node instanceof JumpInsnNode) {
                int reversed;
                switch (opcode) {
                    case IF_ICMPEQ:
                    case IF_ICMPNE:
                    case IF_ICMPLT:
                    case IF_ICMPGE:
                    case IF_ICMPGT:
                    case IF_ICMPLE:
                        mirror.add(new InsnNode(POP));
                        mirror.add(new InsnNode(POP));
                        continue;
                    case IFEQ:
                        reversed = IFNE;
                        break;
                    case IFLT:
                        reversed = IFGE;
                        break;
                    case IFGE:
                        reversed = IFLT;
                        break;
                    case IFGT:
                        reversed = IFLE;
                        break;
                    case IFLE:
                        reversed = IFGT;
                        break;
                    case IF_ACMPEQ:
                        reversed = IF_ACMPNE;
                        break;
                    case IF_ACMPNE:
                        reversed = IF_ACMPEQ;
                        break;
                    case IFNULL:
                        reversed = IFNONNULL;
                        break;
                    case IFNONNULL:
                        reversed = IFNULL;
                        break;
                    case GOTO:
                        reversed = GOTO;
                        break; //
                    default:
                        reversed = IFEQ;
                        break;
                }
                mirror.add(new JumpInsnNode(reversed, ((JumpInsnNode) node).label));
            } else if (node instanceof LabelNode) {
                // nothing to do
            } else if (node instanceof LdcInsnNode) {
                if (((LdcInsnNode) node).cst instanceof String) mirror.add(new InsnNode(ACONST_NULL));
                else if (((LdcInsnNode) node).cst instanceof Type) mirror.add(new InsnNode(ACONST_NULL));
                else if (((LdcInsnNode) node).cst instanceof Handle) mirror.add(new InsnNode(ACONST_NULL));
                else if (((LdcInsnNode) node).cst instanceof Integer)
                    mirror.add(new InsnNode(random.nextInt(ICONST_M1, ICONST_5)));
                else if (((LdcInsnNode) node).cst instanceof Long)
                    mirror.add(new InsnNode(random.nextBoolean() ? LCONST_0 : LCONST_1));
                else if (((LdcInsnNode) node).cst instanceof Float)
                    mirror.add(new InsnNode(random.nextInt(FCONST_0, FCONST_2)));
                else if (((LdcInsnNode) node).cst instanceof Double)
                    mirror.add(new InsnNode(random.nextBoolean() ? DCONST_0 : DCONST_1));
                else mirror.add(node.clone(map));
            } else if (node instanceof IincInsnNode) {
                mirror.add(new IincInsnNode(((IincInsnNode) node).var, random.nextInt(Byte.MIN_VALUE, Byte.MAX_VALUE)));
            } else if (node instanceof TableSwitchInsnNode) {
                // has been ignored
            } else if (node instanceof LookupSwitchInsnNode) {
                // has been ignored
            } else if (node instanceof MultiANewArrayInsnNode) {
                mirror.add(node.clone(map));
            } else if (node instanceof InvokeDynamicInsnNode) {
                InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) node;
                mirror.add(indy.clone(map));
            }
        }

        return stackShrink(mirror);
    }

    @SuppressWarnings({"StatementWithEmptyBody"})
    private static InsnList stackShrink(InsnList list) {
        // asm这可恶的linked node list真让人又爱又恨
        InsnList mirror;
        HashMap<LabelNode, LabelNode> map = new HashMap<>();
        do {
            mirror = new InsnList();
            AbstractInsnNode node = list.getFirst();
            while (node != null) {
                int opcode = node.getOpcode();
                switch (opcode) {
                    case ICONST_M1:
                    case ICONST_0:
                    case ICONST_1:
                    case ICONST_2:
                    case ICONST_3:
                    case ICONST_4:
                    case ICONST_5:
                    case FCONST_0:
                    case FCONST_1:
                    case FCONST_2:
                    case BIPUSH:
                    case SIPUSH:
                    case ACONST_NULL:
                    case ILOAD:
                    case FLOAD:
                    case ALOAD:
                        if (node.getNext() != null && node.getNext().getOpcode() == POP) {
                            node = node.getNext().getNext();
                            continue;
                        }
                        break;
                    case LDC:
                        LdcInsnNode ldc = (LdcInsnNode) node;
                        if (node.getNext() != null &&
                                node.getNext().getOpcode() == ((ldc.cst instanceof Long || ldc.cst instanceof Double) ? POP2 : POP)) {
                            node = node.getNext().getNext();
                            continue;
                        }
                        break;
                    case LCONST_0:
                    case LCONST_1:
                    case DCONST_0:
                    case DCONST_1:
                    case LLOAD:
                    case DLOAD:
                    case DUP2:
                    case DUP2_X1:
                    case DUP2_X2:
                        if (node.getNext() != null && node.getNext().getOpcode() == POP2) {
                            node = node.getNext().getNext();
                            continue;
                        }
                        break;
                }

                if (node instanceof JumpInsnNode) {
                    mirror.add(new JumpInsnNode(node.getOpcode(), ((JumpInsnNode) node).label));
                } else if (node instanceof TableSwitchInsnNode) {
                    TableSwitchInsnNode sin = (TableSwitchInsnNode) node;
                    mirror.add(new TableSwitchInsnNode(sin.min, sin.max, sin.dflt, sin.labels.toArray(new LabelNode[0])));
                } else if (node instanceof LookupSwitchInsnNode) {
                    LookupSwitchInsnNode sin = (LookupSwitchInsnNode) node;
                    mirror.add(new LookupSwitchInsnNode(sin.dflt, sin.keys.stream().mapToInt(Integer::intValue).toArray(), sin.labels.toArray(new LabelNode[0])));
                } else if (node instanceof LabelNode) {
                    // ignored
                } else mirror.add(node.clone(map));
                node = node.getNext();
            }
            list = mirror;
            // make sure is always minimum size
        } while (list.size() != mirror.size());
        return mirror;
    }

    private static AbstractInsnNode generateIntPush(int i) {
        if (i <= 5 && i >= -1) {
            return new InsnNode(i + 3); //iconst_i
        }
        if (i >= -128 && i <= 127) {
            return new IntInsnNode(BIPUSH, i);
        }

        if (i >= -32768 && i <= 32767) {
            return new IntInsnNode(SIPUSH, i);
        }
        return new LdcInsnNode(i);
    }

    public static class FlowIntFlag {
        String flagClass;
        String flagField;
        int flagValue;
        ClassNode boundNode;

        FlowIntFlag(String flagClass, String flagField, int flagValue, ClassNode boundNode) {
            this.flagClass = flagClass;
            this.flagField = flagField;
            this.flagValue = flagValue;
            this.boundNode = boundNode;
        }

        public void releaseField() {
            ClassNode flaggedNode = boundNode;
            flaggedNode.fields.add(new FieldNode(ACC_PUBLIC | ACC_STATIC, flagField, "I", null, null));
            if (flagValue == 0) return;
            MethodNode methodNode = ASMUtil.findClInit(flaggedNode);
            AbstractInsnNode last = methodNode.instructions.getLast();
            int tmpRnd = ThreadLocalRandom.current().nextInt();
            methodNode.instructions.insertBefore(last, generateIntPush(tmpRnd));
            methodNode.instructions.insertBefore(last, generateIntPush(tmpRnd ^ flagValue));
            methodNode.instructions.insertBefore(last, new InsnNode(IXOR));
            methodNode.instructions.insertBefore(last, new FieldInsnNode(PUTSTATIC, flagClass, flagField, "I"));
        }
    }
}
