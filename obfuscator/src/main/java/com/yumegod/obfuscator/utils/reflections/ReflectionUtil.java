package com.yumegod.obfuscator.utils.reflections;

import org.apache.commons.compress.utils.Sets;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ReflectionUtil {
    public static HashMap<String, HashSet<String>> CLASSES_TO_ANNOTATION = new HashMap<>();
    public static HashMap<String, HashSet<String>> ANNOTATION_TO_CLASSES = new HashMap<>();

    private static ClassNode getClassNode(InputStream in) throws IOException {
        ClassReader reader = new ClassReader(in);
        ClassNode classNode = new ClassNode();
        reader.accept(classNode, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return classNode;
    }

    public static HashSet<String> getClassesByAnnotation(String annotation) {
        return ANNOTATION_TO_CLASSES.get(annotation);
    }
    public static HashSet<String> getAnnotationByClass(String className) {
        return CLASSES_TO_ANNOTATION.get(className);
    }

    private static String jName(ClassNode node) {
        return node.name.replace('/', '.');
    }

    private static HashSet<String> classAnnotations(ClassNode node) {
        HashSet<String> set = new HashSet<>();
        List<AnnotationNode> invisibleAnnotations = node.invisibleAnnotations;
        if (invisibleAnnotations != null) set.addAll(invisibleAnnotations.stream().map(a -> a.desc).collect(Collectors.toSet()));
        List<AnnotationNode> visibleAnnotations = node.visibleAnnotations;
        if (visibleAnnotations != null) set.addAll(visibleAnnotations.stream().map(a -> a.desc).collect(Collectors.toSet()));
        return set;
    }

    static {
        HashMap<String, HashSet<String>> classesAndAnnotations = new HashMap<>(), annotationToClasses = new HashMap<>();
        String[] split = ReflectionUtil.class.getName().split("\\.");
        String basePackage = split[0] + "." + split[1];
        try {
            PathMatcher CLASS_FILE = FileSystems.getDefault().getPathMatcher("glob:*.class");
            PathMatcher ARCHIVE = FileSystems.getDefault().getPathMatcher("glob:*.{jar}");
            URL location = ReflectionUtil.class.getProtectionDomain().getCodeSource().getLocation();
            Path path = Paths.get(location.toURI());
            if (Files.isDirectory(path)) {
                try {
                    Files.walkFileTree(path, Sets.newHashSet(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE,
                            new SimpleFileVisitor<Path>() {
                                @Override
                                public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                                    if (CLASS_FILE.matches(path.getFileName())) {
                                        try (InputStream in = Files.newInputStream(path)) {
                                            ClassNode node = getClassNode(in);
                                            String name = jName(node);
                                            if (name.startsWith(basePackage)) {
                                                HashSet<String> annotations = classAnnotations(node);
                                                if (annotations.isEmpty()) return FileVisitResult.CONTINUE;
                                                classesAndAnnotations.put(name, annotations);
                                                for (String annotation : annotations) {
                                                    annotationToClasses.computeIfAbsent(annotation, k -> new HashSet<>()).add(name);
                                                }
                                            }
                                        }
                                    }

                                    return FileVisitResult.CONTINUE;
                                }
                            });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (ARCHIVE.matches(path.getFileName())) {
                try (ZipFile zip = new ZipFile(path.toFile())) {
                    Enumeration<? extends ZipEntry> entries = zip.entries();
                    while (entries.hasMoreElements()) {
                        ZipEntry entry = entries.nextElement();
                        if (entry.isDirectory() || !entry.getName().endsWith(".class")) {
                            continue;
                        }

                        try (InputStream in = zip.getInputStream(entry)) {
                            ClassNode node = getClassNode(in);
                            String name = jName(node);
                            if (name.startsWith(basePackage)) {
                                HashSet<String> annotations = classAnnotations(node);
                                if (annotations.isEmpty()) continue;
                                classesAndAnnotations.put(name, annotations);
                                for (String annotation : annotations) {
                                    annotationToClasses.computeIfAbsent(annotation, k -> new HashSet<>()).add(name);
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        CLASSES_TO_ANNOTATION.putAll(classesAndAnnotations);
        ANNOTATION_TO_CLASSES.putAll(annotationToClasses);
    }
}
