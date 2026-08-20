package com.yumegod.obfuscator.utils.filter;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.yumegod.obfuscator.utils.filter.NodeMatcher.*;

public class ClassFilterExpr {
    private static final Pattern IMPLEMENTS_EXPR = Pattern.compile("impl(ements?)?");

    public String clazz = "**";
    public String superClass = "**";
    public Set<String> interfaces = new HashSet<>(),
            annotations = new HashSet<>();

    public int access = 0;
    public boolean strictAccess = false;
    private boolean classAsWell = false;
    public List<MemberFilterExpr> members = new ArrayList<>(5);


    public ClassFilterExpr(String expression) {
        resolve(expression);
    }

    public boolean match(ClassNode node) {
        return MATCH(clazz, node.name) && MATCH(superClass, node.superName)
                && (interfaces.isEmpty() || ALL_NOT_EMPTY(interfaces, node.interfaces))
                && (!strictAccess || access == (access & node.access))
                && (annotations.isEmpty() || ALL_NOT_EMPTY(annotations, ANNOTATIONS(node.visibleAnnotations, node.invisibleAnnotations)));
    }

    public boolean matchMethod(ClassNode node, MethodNode method) {
        return match(node) && directlyMatchMethod(method);
    }

    public boolean matchField(ClassNode node, FieldNode field) {
        return match(node) && directlyMatchField(field);
    }

    public boolean directlyMatchMethod(MethodNode method) {
        return members.isEmpty() || ANY(members, method);
    }

    public boolean directlyMatchField(FieldNode field) {
        return members.isEmpty() || ANY(members, field);
    }

    public List<MethodNode> getMethods(ClassNode node) {
        return node.methods.stream().filter(method -> matchMethod(node, method)).collect(Collectors.toList());
    }

    public List<FieldNode> getFields(ClassNode node) {
        return node.fields.stream().filter(field -> matchField(node, field)).collect(Collectors.toList());
    }

    public boolean matchClassAsWell() {
        return classAsWell;
    }

    private void resolve(String expr) {
        String[] parts = expr.trim().split(" ");
        IntContainer i = new IntContainer(0);

        String part;
        boolean named = false;
        // 0 - class, 1 - method, 2 - field

        try {
            for (int partsLength = parts.length; i.lt(partsLength); i.inc()) {
                part = parts[i.get()];
                if (part.isEmpty()) continue;
                if (part.startsWith("#")) {
                    parts[i.get()] = part.substring(1);
                    members.add(MemberFilterExpr.resolve(parts, i));
                    continue;
                } else if (part.startsWith("@")) {
                    String substring = part.substring(1);
                    annotations.add(toDescriptor(substring));
                    continue;
                }
                if (part.equals("extend")) {
                    part = parts[i.incAndGet()];
                    superClass = objectNameToInternal(part);
                } else if (IMPLEMENTS_EXPR.matcher(part).matches()) {
                    part = parts[i.incAndGet()];
                    String[] split = part.split(",");
                    for (String s : split) {
                        interfaces.add(objectNameToInternal(s));
                    }
                } else if (isAccess(part)) {
                    access |= getAccess(part);
                    strictAccess = true;
                } else if (!named) {
                    if (part.startsWith("^")) {
                        part = part.substring(1);
                        classAsWell = true;
                    }
                    clazz = objectNameToInternal(part);
                    named = true;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Invalid expression: " + expr + "(Error occurred while parsing token in part: '" + parts[i.get()] + "')", e);
        }
    }

    static boolean isAccess(String str) {
        return str.equals("public") || str.equals("protected") || str.equals("private") || str.equals("static") ||
                str.equals("final") || str.equals("abstract") || str.equals("native") || str.equals("synchronized") ||
                str.equals("transient") || str.equals("volatile");
    }

    static int getAccess(String str) {
        switch (str) {
            case "public":
                return Opcodes.ACC_PUBLIC;
            case "protected":
                return Opcodes.ACC_PROTECTED;
            case "private":
                return Opcodes.ACC_PRIVATE;
            case "static":
                return Opcodes.ACC_STATIC;
            case "final":
                return Opcodes.ACC_FINAL;
            case "abstract":
                return Opcodes.ACC_ABSTRACT;
            case "native":
                return Opcodes.ACC_NATIVE;
            case "synchronized":
                return Opcodes.ACC_SYNCHRONIZED;
            case "transient":
                return Opcodes.ACC_TRANSIENT;
            case "volatile":
                return Opcodes.ACC_VOLATILE;
            default:
                return 0;
        }
    }

    static String objectNameToInternal(String className) {
        return className.replace('.', '/');
    }

    static String toDescriptor(String className) {
        StringBuilder sb = new StringBuilder();
        while (className.endsWith("[]")) {
            className = className.substring(0, className.length() - 2);
            sb.append("[");
        }
        switch (className) {
            case "*":
            case "**":
                return "**";
            case "void":
                sb.append("V");
                break;
            case "boolean":
                sb.append("Z");
                break;
            case "byte":
                sb.append("B");
                break;
            case "char":
                sb.append("C");
                break;
            case "short":
                sb.append("S");
                break;
            case "int":
                sb.append("I");
                break;
            case "float":
                sb.append("F");
                break;
            case "long":
                sb.append("J");
                break;
            case "double":
                sb.append("D");
                break;
            default:
                sb.append("L").append(objectNameToInternal(className)).append(";");
                break;
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "Filter{" +
                "clazz='" + clazz + '\'' +
                ", superClass='" + superClass + '\'' +
                ", interfaces=" + interfaces +
                ", annotations=" + annotations +
                (strictAccess ? ", access=" + Integer.toBinaryString(access) : "") +
                ", members=" + members +
                '}';
    }
}
