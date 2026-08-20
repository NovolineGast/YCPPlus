package com.yumegod.obfuscator.utils.filter;

import com.yumegod.obfuscator.utils.apache.AntPathMatcher;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.*;
import java.util.stream.Collectors;

public class NodeMatcher {
    private static final AntPathMatcher matcher = new AntPathMatcher();

    public static boolean MATCH(String pattern, String path)
    {
        return matcher.match(pattern, path);
    }

    public static boolean ALL(Collection<String> pattern, Collection<String> paths) {
        return paths.stream().allMatch(path -> pattern.stream().anyMatch(p -> MATCH(p, path)));
    }
    public static boolean ALL_NOT_EMPTY(Collection<String> pattern, Collection<String> paths) {
        if (paths.isEmpty() || pattern.isEmpty()) return false;
        return paths.stream().allMatch(path -> pattern.stream().anyMatch(p -> MATCH(p, path)));
    }

    public static boolean ANY(Collection<String> pattern, Collection<String> paths) {
        return paths.stream().anyMatch(path -> pattern.stream().anyMatch(p -> MATCH(p, path)));
    }

    public static boolean ANY(Collection<MemberFilterExpr> expr, MethodNode node) {
        return expr.stream().anyMatch(e -> e.match(node));
    }

    public static boolean ANY(Collection<MemberFilterExpr> expr, FieldNode node) {
        return expr.stream().anyMatch(e -> e.match(node));
    }

    public static <T> List<T> collect(T[] arr) {
        if (arr == null) return new ArrayList<>(0);
        return Arrays.asList(arr);
    }

    public static List<String> ANNOTATIONS(List<AnnotationNode> annotations1, List<AnnotationNode> annotations2) {
        List<AnnotationNode> list = new LinkedList<>();
        if (annotations1 != null && !annotations1.isEmpty()) list.addAll(annotations1);
        if (annotations2 != null && !annotations2.isEmpty()) list.addAll(annotations2);
        return list.stream().map(a -> a.desc).collect(Collectors.toList());
    }

//    public static boolean match(ClassFilterExpr expr, ClassNode node) {
//        return match(expr.clazz, node.name)
//                && (expr.superClass == null || match(expr.superClass, node.superName))
//                && (expr.interfaces.isEmpty() || all(expr.interfaces, node.interfaces))
//                && (expr.annotations.isEmpty() || all(expr.annotations, node.visibleAnnotations.stream().map(a -> a.desc).collect(Collectors.toList())));
//    }
//
//    public static boolean match(ClassFilterExpr expr, ClassNode node, MethodNode method) {
//        return match(expr, node);
//    }
}
