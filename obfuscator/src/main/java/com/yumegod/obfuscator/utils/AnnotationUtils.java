package com.yumegod.obfuscator.utils;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class AnnotationUtils {
    public static List<AnnotationNode> getAnnotations(List<AnnotationNode> annotationNodes) {
        return annotationNodes == null ? Collections.emptyList() : annotationNodes;
    }

    public static boolean hasAnnotation(List<AnnotationNode> annotationNodes, String annotation) {
        return getAnnotations(annotationNodes).stream().anyMatch(a -> Objects.equals(a.desc, annotation));
    }

    public static boolean noneAnnotation(List<AnnotationNode> annotationNodes, String annotation) {
        return getAnnotations(annotationNodes).stream().noneMatch(a -> Objects.equals(a.desc, annotation));
    }

    public static void removeAnnotations(List<AnnotationNode> annotationNodes) {
        if (annotationNodes == null) return;
        annotationNodes.removeIf(a -> a.desc.startsWith("Lcom/yumegod/obfuscation/"));
    }

    public static void cleanAnnotations(ClassNode node) {
        removeAnnotations(node.invisibleAnnotations);
        node.methods.stream().map(it -> it.invisibleAnnotations).forEach(AnnotationUtils::removeAnnotations);
        node.fields.stream().map(it -> it.invisibleAnnotations).forEach(AnnotationUtils::removeAnnotations);
    }

    public static boolean haveAndNotHave(List<AnnotationNode> annotationNodes, String annotation1, String annotation2) {
        List<AnnotationNode> annotations = getAnnotations(annotationNodes);
        return annotations.stream().anyMatch(a -> Objects.equals(a.desc, annotation1)) &&
                annotations.stream().noneMatch(a -> Objects.equals(a.desc, annotation2));
    }

    public static boolean haveOrNotHave(List<AnnotationNode> annotationNodes, String annotation1, String annotation2) {
        List<AnnotationNode> annotations = getAnnotations(annotationNodes);
        return annotations.stream().anyMatch(a -> Objects.equals(a.desc, annotation1)) ||
                (annotations.stream().noneMatch(a -> Objects.equals(a.desc, annotation2)) && !annotations.isEmpty());
    }
}
