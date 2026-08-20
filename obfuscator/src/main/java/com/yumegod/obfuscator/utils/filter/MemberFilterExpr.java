package com.yumegod.obfuscator.utils.filter;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static com.yumegod.obfuscator.utils.filter.ClassFilterExpr.*;
import static com.yumegod.obfuscator.utils.filter.NodeMatcher.*;

public class MemberFilterExpr {

    public String name = "**", desc = "**";
    public Set<String> annotations = new HashSet<>();
    public int access = 0;
    public boolean strictAccess = false;
    public boolean isMethod = false;

    public boolean match(MethodNode node) {
        return isMethod && MATCH(name, node.name) && MATCH(desc, node.desc)
                && (!strictAccess || access == (node.access & access))
                && (annotations.isEmpty() || ALL_NOT_EMPTY(annotations, ANNOTATIONS(node.visibleAnnotations, node.invisibleAnnotations)));
    }

    public boolean match(FieldNode node) {
        return !isMethod && MATCH(name, node.name) && MATCH(desc, node.desc)
                && (!strictAccess || access == (node.access & access))
                && (annotations.isEmpty() || ALL_NOT_EMPTY(annotations, ANNOTATIONS(node.visibleAnnotations, node.invisibleAnnotations)));
    }


    static MemberFilterExpr resolve(String[] parts, IntContainer i) {
        MemberFilterExpr expr = new MemberFilterExpr();
        String returnValue = null;
        boolean inDesc = false;
        boolean named = false;
        StringBuilder descBuilder = new StringBuilder();
        Build: {
            for (int length = parts.length; i.lt(length); i.inc()) {
                String part = parts[i.get()].trim();
                if (part.isEmpty()) continue;
                if (part.startsWith("#")) {
                    i.dec();
                    break Build;
                }

                if (inDesc) {
                    descBuilder.append(part);
                    if (part.contains(")")) {
                        inDesc = false;
                        String substring = part.substring(0, part.indexOf(')'));
                        if (substring.trim().isEmpty()) continue;
                        parts[i.getAndDec()] = substring;
                        continue;
                    }
                }

                if (part.startsWith("@")) {
                    expr.annotations.add(toDescriptor(part.substring(1)));
                }
                if (isAccess(part)) {
                    expr.access |= getAccess(part);
                    expr.strictAccess = true;
                }
//              else if (part.startsWith("!")) {}
                else if (part.contains("(")) {
                    int at = part.indexOf('(');
                    if (!named && at > 0) {
                        expr.name = part.substring(0, at);
                        named = true;
                    }
                    inDesc = true;
                    parts[i.get()] = part.substring(at + 1);
                    i.dec();
                    expr.isMethod = true;
                } else if (returnValue == null) {
                    returnValue = toDescriptor(part);
                } else if (!named) {
                    expr.name = part;
                    named = true;
                }
            }
        }
        if (descBuilder.length() > 0) {
            String rawDesc = descBuilder.toString();
            int leftBracketIndex = rawDesc.indexOf('(');
            if (leftBracketIndex != -1) {
                rawDesc = rawDesc.substring(leftBracketIndex + 1);
            }
            int rightBracketIndex = rawDesc.indexOf(')');
            if (rightBracketIndex != -1) {
                rawDesc = rawDesc.substring(0, rightBracketIndex);
            }
            String[] params = rawDesc.split(",");
            for (int i1 = 0; i1 < params.length; i1++) {
                String param = params[i1].trim();
                if (param.isEmpty()) continue;
                params[i1] = toDescriptor(param);
            }
            expr.desc = "(" + String.join("", params) + ")" + returnValue;
        } else {
            expr.desc = returnValue;
        }
        return expr;
    }

    @Override
    public String toString() {
        return "Member{" +
                "name='" + name + '\'' +
                ", desc='" + desc + '\'' +
                ", annotations=" + annotations +
                (strictAccess ? ", access=" + Integer.toBinaryString(access) : "") +
                '}';
    }
}
