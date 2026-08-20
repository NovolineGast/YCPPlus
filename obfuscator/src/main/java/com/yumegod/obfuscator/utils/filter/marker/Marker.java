package com.yumegod.obfuscator.utils.filter.marker;

import com.yumegod.obfuscator.utils.filter.ClassFilterExpr;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Collection;


public class Marker {
    public static void mark(Collection<ClassNode> nodes) {
        for (ClassNode node : nodes) {
            mark(node);
        }
    }

    public static void mark(ClassNode node) {
        // TODO
        for (MarkerContext context : MarkerContext.CONTEXTS) {
            if (context.expressions.isEmpty()) continue;
            for (ClassFilterExpr expr : context.expressions) {
                if (expr.match(node)) {
                    boolean memberMatched = false;
                    for (MethodNode method : node.methods) {
                        if (expr.directlyMatchMethod(method)) {
                            context.onMethodMatched.accept(method);
                            memberMatched = true;
                            break;
                        }
                    }
                    for (FieldNode field : node.fields) {
                        if (expr.directlyMatchField(field)) {
                            context.onFieldMatched.accept(field);
                            memberMatched = true;
                            break;
                        }
                    }
                    if (memberMatched && expr.matchClassAsWell()) {
                        context.onClassMatched.accept(node);
                    }
                }
            }
        }
    }
}
