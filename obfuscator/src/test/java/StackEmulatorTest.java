import com.yumegod.obfuscator.jobf.transformer.impl.flow.v2.stack.CodeFrame;
import com.yumegod.obfuscator.jobf.transformer.impl.flow.v2.stack.StackEmulator;
import com.yumegod.obfuscator.utils.Util;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class StackEmulatorTest {
    private static final AtomicInteger _LBL = new AtomicInteger(0);
    private static final HashMap<String, String> lbl = new HashMap<>();
    public static void main(String[] args) throws Exception {
//        ClassWriter dump = LoaderDump.dump();
//        byte[] bytes = dump.toByteArray();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Util.transfer(Files.newInputStream(Paths.get("IIIIiIiIIIiiIiiIIiiIIiiIIIiiiIiiIIIiIiIi.class")), out);
        byte[] bytes = out.toByteArray();
        ClassReader reader = new ClassReader(bytes);
        ClassNode classNode = new ClassNode();
        reader.accept(classNode, ClassReader.SKIP_FRAMES);
        StringBuilder sb = new StringBuilder();
        for (MethodNode method : classNode.methods) {
            try {
                System.out.println(method.name);
                Map<AbstractInsnNode, CodeFrame> map = StackEmulator.execute(method);
                AbstractInsnNode[] array = method.instructions.toArray();
                sb.append(method.name).append(":").append(method.desc);
                sb.append("\nASM:\n");
                for (AbstractInsnNode ain : array) {
                    parseNode(ain, sb, map);
                }
            } catch (Exception o) {
                o.printStackTrace();
            }
        }
        System.out.println(sb);
        BufferedWriter writer = new BufferedWriter(new FileWriter("out.txt", false));
        writer.write(sb.toString());
        writer.flush();
        writer.close();
    }

    private static void parseNode(AbstractInsnNode node, StringBuilder sb, Map<AbstractInsnNode, CodeFrame> map) {
        if (reflect.isEmpty()) init();
        if (node instanceof LabelNode) {
            sb.append(" ").append(labelStr(((LabelNode) node).getLabel())).append("\n");
            return;
        } else if (node instanceof LineNumberNode) {
            LineNumberNode lineNumberNode = (LineNumberNode) node;
            sb.append(" line:").append(lineNumberNode.line).append(" start:").append(labelStr(lineNumberNode.start.getLabel())).append("\n");
            return;
        } else {
            sb.append("  ").append(getName(node.getOpcode()));
            sb.append(" ");
        }
        switch (node.getType()) {
            case AbstractInsnNode.INSN: case AbstractInsnNode.LABEL: case AbstractInsnNode.LINE: break;
            case AbstractInsnNode.INT_INSN: {
                IntInsnNode insnNode = (IntInsnNode) node;
                sb.append("operand:").append(insnNode.operand);
                break;
            }
            case AbstractInsnNode.VAR_INSN: {
                VarInsnNode varInsnNode = (VarInsnNode) node;
                sb.append("var:").append(varInsnNode.var);
                break;
            }
            case AbstractInsnNode.TYPE_INSN: {
                TypeInsnNode typeInsnNode = (TypeInsnNode) node;
                sb.append("desc:").append(typeInsnNode.desc);
                break;
            }
            case AbstractInsnNode.FIELD_INSN: {
                FieldInsnNode fieldInsnNode = (FieldInsnNode) node;
                sb.append("owner:").append(fieldInsnNode.owner).append(" desc:").append(fieldInsnNode.desc).append(" name:").append(fieldInsnNode.name);
                break;
            }
            case AbstractInsnNode.METHOD_INSN: {
                MethodInsnNode methodInsnNode = (MethodInsnNode) node;
                sb.append("owner:").append(methodInsnNode.owner).append(" name:").append(methodInsnNode.name).append(" desc:").append(methodInsnNode.desc);
                break;
            }
            case AbstractInsnNode.INVOKE_DYNAMIC_INSN: {
                InvokeDynamicInsnNode invokeDynamicInsnNode = (InvokeDynamicInsnNode) node;
                sb.append("name:").append(invokeDynamicInsnNode.name).append(" desc:").append(invokeDynamicInsnNode.desc).append(" handle:").append(invokeDynamicInsnNode.bsm).append(" args:").append(Arrays.toString(invokeDynamicInsnNode.bsmArgs));
                break;
            }
            case AbstractInsnNode.JUMP_INSN: {
                JumpInsnNode jumpInsnNode = (JumpInsnNode) node;
                sb.append("to:").append(labelStr(jumpInsnNode.label.getLabel()));
                break;
            }
            case AbstractInsnNode.LDC_INSN: {
                LdcInsnNode ldcInsnNode = (LdcInsnNode) node;
                sb.append("put:");
                if (ldcInsnNode.cst instanceof String) {
                    sb.append("\"").append(ldcInsnNode.cst).append("\"");
                } else sb.append(ldcInsnNode.cst);
                break;
            }
            case AbstractInsnNode.IINC_INSN: {
                IincInsnNode iincInsnNode = (IincInsnNode) node;
                sb.append("var:").append(iincInsnNode.var).append(" increase:").append(iincInsnNode.incr);
                break;
            }
            case AbstractInsnNode.TABLESWITCH_INSN: {
                TableSwitchInsnNode tableSwitchInsnNode = (TableSwitchInsnNode) node;
                int min = tableSwitchInsnNode.min;
                sb.append("from:").append(min).append(" to:").append(tableSwitchInsnNode.max).append(" default:").append(tableSwitchInsnNode.dflt.getLabel()).append("\n");
                List<LabelNode> st = tableSwitchInsnNode.labels;
                for (int i = 0; i < st.size(); i++) {
                    LabelNode node1 = st.get(i);
                    sb.append(" ").append(min + i).append(": ").append(labelStr(node1.getLabel())).append("\n");
                }
                break;
            }
            case AbstractInsnNode.LOOKUPSWITCH_INSN: {
                LookupSwitchInsnNode lookupSwitchInsnNode = (LookupSwitchInsnNode) node;
                sb.append("default:").append(labelStr(lookupSwitchInsnNode.dflt.getLabel())).append("\n");
                List<Integer> keys = lookupSwitchInsnNode.keys;
                List<LabelNode> lbs = lookupSwitchInsnNode.labels;
                for (int i = 0; i < keys.size(); i++) {
                    sb.append(" ").append(keys.get(i)).append(": ").append(labelStr(lbs.get(i).getLabel())).append("\n");
                }
                break;
            }
            case AbstractInsnNode.MULTIANEWARRAY_INSN: {
                MultiANewArrayInsnNode multiANewArrayInsnNode = (MultiANewArrayInsnNode) node;
                sb.append("desc:").append(multiANewArrayInsnNode.desc).append(" dimensions:").append(multiANewArrayInsnNode.dims);
                break;
            }
//            case AbstractInsnNode.FRAME: {
//                FrameNode frameNode = (FrameNode) node;
//                switch (frameNode.type) {
//                    case Opcodes.F_NEW: sb.append("F_NEW");
//                    case Opcodes.F_FULL:  sb.append("F_FULL");
//                    case Opcodes.F_APPEND:  sb.append("F_APPEND");
//                    case Opcodes.F_CHOP:  sb.append("F_CHOP");
//                    case Opcodes.F_SAME:  sb.append("F_SAME");
//                    case Opcodes.F_SAME1:  sb.append("F_SAME1");
//                }
//                sb.append(" LOCAL:").append(frameNode.local).append(" stack:").append(frameNode.stack);
//                break;
//            }
        }
        if (map.containsKey(node)) {
            CodeFrame frame = map.get(node);
            StringBuilder s = new StringBuilder(" [");
            for (byte b : frame.getStackType()) {
                s.append(getType(b)).append(",");
            }
            sb.append(" stack: ").append(frame.getStackHeight()).append(s);
        }
        sb.append("\n");
    }

    private static final Map<Integer, String> reflect = new HashMap<>();

    private static String labelStr(Label label) {
        String key = label.toString();

        if (((Map<String, String>) StackEmulatorTest.lbl).containsKey(key)) {
            return ((Map<String, String>) StackEmulatorTest.lbl).get(key);
        }
//        int i = cd.incrementAndGet();
        String value = "L" + StackEmulatorTest._LBL.addAndGet(1);
        ((Map<String, String>) StackEmulatorTest.lbl).put(key, value);
        return value;
    }

    private static String getType(byte type) {
        switch (type) {
            case 0: return "null";
            case 1: return "bool";
            case 2: return "char";
            case 3: return "byte";
            case 4: return "short";
            case 5: return "int";
            case 6: return "float";
            case 7: return "long";
            case 8: return "double";
            case 9: return "array";
            case 10: return "object";
            case 11: return "uninitialized";
        }
        return "wtf";
    }

    private static void init() {
        for (Field field : Opcodes.class.getDeclaredFields()) {
            try {
                if (field.getName().toUpperCase(Locale.ENGLISH).equals(field.getName())) reflect.put((Integer) field.get(null), field.getName());
            } catch (Throwable ignored) {}
        }
    }

    private static String getName(int op) {
        return reflect.getOrDefault(op, "UNDEFINED_OPCODE");
    }
}
