package com.yumegod.obfuscator.jobf.transformer.impl.renamer;

import com.yumegod.obfuscation.NoRename;
import com.yumegod.obfuscation.NotNative;
import com.yumegod.obfuscator.YumeCloudProtection;
import com.yumegod.obfuscator.jobf.transformer.impl.renamer.remapper.InnerRemapper;
import com.yumegod.obfuscator.jobf.transformer.impl.renamer.remapper.MemberRemapper;
import com.yumegod.obfuscator.jobf.transformer.impl.renamer.remapper.SimpleClassRemapper;
import com.yumegod.obfuscator.jobf.utils.namefactory.NameFactory;
import com.yumegod.obfuscator.jobf.utils.namefactory.NameFactoryUtils;
import com.yumegod.obfuscator.utils.ClassMetadataReader;
import com.yumegod.obfuscator.utils.Util;
import com.yumegod.obfuscator.enums.RenameResourceAdaptMethod;
import com.yumegod.obfuscator.utils.apache.AntPathMatcher;
import com.yumegod.obfuscator.utils.cfg.ConfigManager;
import com.yumegod.obfuscator.utils.cfg.annotations.ConfigSection;
import com.yumegod.obfuscator.utils.cfg.annotations.StaticConfigReceiver;
import com.yumegod.obfuscator.utils.filter.ClassFilterExpr;
import com.yumegod.obfuscator.utils.filter.NodeMatcher;
import com.yumegod.obfuscator.utils.filter.marker.Marker;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.tree.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static com.yumegod.obfuscator.jobf.utils.ASMAccess.*;
import static com.yumegod.obfuscator.jobf.utils.namefactory.NameFactoryUtils.formattedDesc;

@StaticConfigReceiver
public class Renamer {
    @ConfigSection("rename.enable")
    public static boolean enable = false;
    @ConfigSection("rename.handle_inner_classes")
    public static boolean handleInnerClasses = false;
    @ConfigSection("rename.resource_adapt_method")
    public static RenameResourceAdaptMethod resourceAdaptMethod = RenameResourceAdaptMethod.EXCLUDE;
    @ConfigSection("rename.force_change_relative_resource")
    public static boolean forceChangeRelativeResource = false;

    // relocate
    @ConfigSection("rename.relocate.enable")
    public static boolean enableRelocate = true;
    @ConfigSection("rename.relocate.package")
    public static String relocatePackage = "YumeCloudProtection";

    // reflection
    @ConfigSection("rename.reflection_capability")
    public static boolean enableReflectionCapability = false;

    static final String MIXIN_HEADER = "Lorg/spongepowered/asm/";
    static final String _NO_RENAME_DESC = Type.getDescriptor(NoRename.class);
    static Logger logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    static String resourcesString = "";
    static List<String> configIncludes = new ArrayList<>();

    public static File processJar(File inputJar, Path outputDir, ClassMetadataReader metadataReader) throws IOException {
        if (YumeCloudProtection.safeMode) return inputJar;
        randomUniqueName();
        File outputJar = new File(outputDir.toFile(), inputJar.getName());
//        File outputJar = new File("_dump.jar");
        LinkedHashSet<String> whitelistedMemberNames = new LinkedHashSet<>();

        ClassNodeTree classNodeTree = new ClassNodeTree();

        {
            List<String> list = ConfigManager.getStringList("rename.include");
            if (list != null) for (String s : list) {
                configIncludes.add(s.replace(".", "/"));
            }
        }

        try (ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(outputJar.toPath())), StandardCharsets.UTF_8)) {
            HashMap<String, byte[]> resources = new HashMap<>();
            HashMap<String, ClassNode> classes = new HashMap<>();
            // one of the bad but easy method is...
            StringBuilder resourcesStringBuilder = new StringBuilder();
            try (ZipInputStream ins = new ZipInputStream(new BufferedInputStream(Files.newInputStream(inputJar.toPath())), StandardCharsets.UTF_8)) {
                while (true) {
                    ZipEntry entry = ins.getNextEntry();
                    if (entry == null) {
                        break;
                    }

                    byte[] bytes = getBytes(ins);
                    if (entry.getName().endsWith(".class")) {
                        if (!isClass(bytes)) {
                            out.putNextEntry(new ZipEntry(entry));
                            out.write(bytes);
                            resources.put(entry.getName(), bytes);
                            continue;
                        }

                        ClassReader reader = new ClassReader(bytes);
                        ClassNode node = new ClassNode();
                        reader.accept(node, 0);

                        if (enableReflectionCapability) node.methods.stream().map(method -> method.instructions).filter(instructions -> instructions.size() > 1).map(InsnList::toArray)
                                .forEach(array -> {
                                            Arrays.stream(array)
                                                    .filter(ain -> ain instanceof LdcInsnNode).map(ain -> (LdcInsnNode) ain)
                                                    .filter(ldc -> ldc.cst instanceof String).map(ldc -> (String) ldc.cst)
                                                    .filter(s -> !s.isEmpty())
                                                    .forEach(whitelistedMemberNames::add);
                                            Arrays.stream(array)
                                                    .filter(ain -> ain instanceof InvokeDynamicInsnNode).map(ain -> (InvokeDynamicInsnNode) ain)
                                                    .forEach(invokeDynamic -> {
                                                        whitelistedMemberNames.add(invokeDynamic.name);
                                                        Arrays.stream(invokeDynamic.bsmArgs).filter(it -> it instanceof String).map(it -> (String) it).forEach(whitelistedMemberNames::add);
                                                    });
                                        }
                                );

                        Marker.mark(node);

                        if (!enable) {
                            out.putNextEntry(new ZipEntry(entry.getName()));
                            ClassWriter writer = new ClassWriter(0);
                            node.accept(writer);
                            out.write(writer.toByteArray());
                            classNodeTree.asLibraryClass(node);
                            node.methods.forEach(it -> it.instructions.clear());
                            continue;
                        }
                        classes.put(node.name, node);
                    } else {
                        String string = new String(bytes, StandardCharsets.UTF_8);
                        if (isAsciiText(string)) {
                            resourcesStringBuilder.append(string);
                        }

                        resources.put(entry.getName(), bytes);
                    }
                }
            }

            if (classes.isEmpty()) {
                writeResources(resources, out, RenameResourceAdaptMethod.NONE, new HashMap<>(0));
                return outputJar;
            }

            WhitelistArg whitelistArg = new WhitelistArg();
            whitelistArg.whitelistedMemberNames = whitelistedMemberNames;

            HashMap<String, String> mapping = new HashMap<>();


            logger.info("Reading basic information about the libraries...");
            classNodeTree.parseLibraries(metadataReader);

            logger.info("Analyzing relationships between classes...");
            classNodeTree.parseClasses(classes);

            logger.info("Handling inheritance between classes...");
            checkMissingLibraries(classNodeTree, classes);

            logger.info("Cleaning trees...");
            classNodeTree.shrink();

            if (handleInnerClasses) {
                logger.info("Handling inner classes...");
                remapInner(classes);
            }

            logger.info("Generating members mapping...");
            Map<String, Map<String, Map<String, String>>> memberMapping = new HashMap<>();
            generateMembersMapping(classNodeTree, memberMapping, classes, whitelistArg);

            logger.info("Accepting members mapping...");
            MemberRemapper memberRemapper = new MemberRemapper(memberMapping);
            new HashSet<>(classes.entrySet()).forEach(entry -> {
                ClassNode value = entry.getValue();
                ClassNode node = new ClassNode();
                value.accept(new ClassRemapper(node, memberRemapper));
                classes.remove(entry.getKey());
                classes.put(node.name, node);
            });

            logger.info("Generating class mapping...");
            generateClassesMapping(mapping, classes);

            logger.info("Generated: {}.", mapping.size());

            RenameResourceAdaptMethod resourceAdaptMethod = Renamer.resourceAdaptMethod;
            resourcesString = resourcesStringBuilder.toString();
            resourcesStringBuilder.delete(0, resourcesStringBuilder.length());
            if (resourceAdaptMethod == RenameResourceAdaptMethod.EXCLUDE) {
                logger.info("Excluding classes from resources...");
                HashSet<String> strings = new HashSet<>(mapping.keySet());
                for (String raw : strings) {
                    if (resourcesString.contains(raw) || resourcesString.contains(raw.replaceAll("/", "."))) {
                        mapping.remove(raw);
                        logger.info("Excluded class {}", raw);
                    }
                }
            }

            logger.info("Accepting mapping to reflection call...");
            for (ClassNode node : classes.values()) {
                for (MethodNode method : node.methods) {
                    reflectionClassSupport(mapping, method);
                    fixResourceSupport(resources, node, method);
                }
            }
            memberMapping.clear();

            logger.info("Accepting classes mappings...");
            SimpleClassRemapper remapper = new SimpleClassRemapper(mapping);
            new HashMap<>(classes).forEach((key, value) -> {
                ClassNode node = new ClassNode();
                value.accept(new ClassRemapper(node, remapper));
                classes.remove(key);
                if (!key.equals(node.name) && !isPrivate(node.access)) {
                    node.access |= Opcodes.ACC_PUBLIC;
                    for (FieldNode field : node.fields) {
                        if (isPrivate(field.access) || isProtected(field.access)) continue;
                        field.access |= Opcodes.ACC_PUBLIC;
                    }
                    for (MethodNode method : node.methods) {
                        if (isPrivate(method.access) || isProtected(method.access)) continue;
                        method.access |= Opcodes.ACC_PUBLIC;
                    }
                }
                classes.put(node.name, node);
            });

//            for (ClassNode classNode : classes.values()) {
//                for (MethodNode method : classNode.methods) {
//                    reflectionMemberSupport(memberMapping, method);
//                }
//            }

            logger.info("Mapped total {} classes.", mapping.size());

            writeClasses(classes, out);
            classes.clear();

            logger.info("Handling resources...");
            writeResources(resources, out, resourceAdaptMethod, mapping);
            mapping.clear();
            resources.clear();
        }
        return outputJar;
    }

    private static void writeResources(HashMap<String, byte[]> resources, ZipOutputStream out, RenameResourceAdaptMethod resourceAdaptMethod, HashMap<String, String> mapping) {
        resources.forEach((name, bytes) -> {
            try {
                out.putNextEntry(new ZipEntry(name));
                if (resourceAdaptMethod == RenameResourceAdaptMethod.AGGRESSIVE) {
                    String s = new String(bytes, StandardCharsets.UTF_8);
                    if (isAsciiText(s)) {
                        for (Map.Entry<String, String> entry : mapping.entrySet()) {
                            String raw = s;
                            s = s.replaceAll(entry.getKey().replaceAll("/", "."), entry.getValue().replaceAll("/", "."))
                                    .replaceAll(entry.getKey(), entry.getValue());
                            if (s.hashCode() != raw.hashCode())
                                logger.info("Automatic handled in resource class name {}: {}", name, entry.getKey().replaceAll("/", "."));
                        }
                        out.write(s.getBytes(StandardCharsets.UTF_8));
                    } else {
                        out.write(bytes);
                    }
                } else {
                    out.write(bytes);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static void writeClasses(HashMap<String, ClassNode> classes, ZipOutputStream out) {
        classes.values().forEach(node -> {
            ClassWriter writer = new ClassWriter(0);
            node.accept(writer);

            try {
                ZipEntry e = new ZipEntry(node.name + ".class");
                out.putNextEntry(e);
                out.write(writer.toByteArray());
            } catch (IOException e) {
                e.printStackTrace(System.err);
            }
        });
    }

    private static boolean isAsciiText(String string) {
        for (char c : string.toCharArray()) {
            if (Character.getType(c) == Character.UNASSIGNED) return false;
        }
        return true;
    }

    private static boolean hasAnnotation(MethodNode node, Class<?> ann) {
        String name = Type.getInternalName(ann);
        return node.invisibleAnnotations != null && node.invisibleAnnotations.stream().anyMatch(it -> it.desc.equals(name));
    }

    private static void fixResourceSupport(Map<String, byte[]> resources, ClassNode clzNode, MethodNode node) {
        if (node.instructions == null || !forceChangeRelativeResource) {
            return;
        }
        String currentPathStart = clzNode.name.substring(0, clzNode.name.lastIndexOf("/") + 1);
        for (AbstractInsnNode insnNode : node.instructions.toArray()) {
            if (insnNode instanceof LdcInsnNode) {
                Object cst = ((LdcInsnNode) insnNode).cst;
                if (cst instanceof String) {
                    String string = (String) cst;
                    if (string.isEmpty() || string.startsWith("/")) {
                        continue;
                    }
                    String pathSep = currentPathStart + string;
                    for (String resourcePath : resources.keySet()) {
                        if (pathSep.equals(resourcePath)) {
                            String absolutePath = "/" + resourcePath;
                            ((LdcInsnNode) insnNode).cst = absolutePath;
                            logger.info("Replaced resource call from relative to absolute path in {}{}: {} -> {}", node.name, node.desc, string, absolutePath);
                        }
                    }
                }
            }
        }
    }

    private static void handleBlackWhiteListSetting(HashMap<String, String> classesMapping, HashMap<String, String> memberMapping, List<String> blacklist, List<String> whitelist) {
        if (blacklist == null || whitelist == null || (whitelist.isEmpty() && blacklist.isEmpty())) {
            return;
        }
        HashMap<String, String> transformedClassesMapping = new HashMap<>(), transformedMemberMapping = new HashMap<>();
        for (Map.Entry<String, String> entry : classesMapping.entrySet()) {
            transformedClassesMapping.put(Type.getObjectType(entry.getKey()).getClassName(), Type.getObjectType(entry.getValue()).getClassName());
        }
        for (Map.Entry<String, String> entry : memberMapping.entrySet()) {
            if (!entry.getKey().contains("(")) continue;
//            logger.info("ent: {}", entry.getKey());
            String[] splitRaw = entry.getKey().split("\\.");
            String desc = splitRaw[splitRaw.length - 1];
            String returnClass = Type.getReturnType(desc).getClassName();
            String params = Arrays.stream(Type.getArgumentTypes(desc)).map(Type::getClassName).collect(Collectors.joining(", "));
            String raw = "#" + returnClass + " " + splitRaw[splitRaw.length - 2] +
                    "(" + params + ")";

            transformedMemberMapping.put(raw, "#" + returnClass + " " + entry.getValue() + "(" + params + ")");
        }
        List<String> newWhitelist = new ArrayList<>(), newBlacklist = new ArrayList<>();

        processBWList0(whitelist, transformedMemberMapping, transformedClassesMapping, newWhitelist);
        processBWList0(blacklist, transformedMemberMapping, transformedClassesMapping, newBlacklist);
        whitelist.clear();
        blacklist.clear();
        whitelist.addAll(newWhitelist);
        blacklist.addAll(newBlacklist);
    }

    private static void processBWList0(List<String> whitelist, HashMap<String, String> transformedMemberMapping, HashMap<String, String> transformedClassesMapping, List<String> newWhitelist) {
        for (String s : whitelist) {
            for (Map.Entry<String, String> entry : transformedMemberMapping.entrySet()) {
                String key = entry.getKey();
                if (s.endsWith(key)) {
                    s = s.replace(key, entry.getValue());
                    break;
                }
            }
            for (Map.Entry<String, String> entry : transformedClassesMapping.entrySet()) {
                s = s.replaceAll(entry.getKey(), entry.getValue());
            }
            newWhitelist.add(s);
        }
    }

    @SuppressWarnings("LoggingSimilarMessage")
    private static void reflectionClassSupport(HashMap<String, String> mapping, MethodNode node) {
        if (node.instructions == null) {
            return;
        }
        for (AbstractInsnNode insnNode : node.instructions.toArray()) {
            if (insnNode instanceof LdcInsnNode) {
                LdcInsnNode ldc = (LdcInsnNode) insnNode;
                if (ldc.cst instanceof String) {
                    String string = ((String) ldc.cst).trim();
                    if (string.isEmpty()) continue;
                    if (string.contains(".")) for (String k : mapping.keySet()) {
                        if (string.replaceAll("\\.", "/").equals(k)) {
                            ldc.cst = mapping.get(k).replaceAll("/", ".");
                            logger.info("Replaced reflection class call in {}{}: {} -> {}", node.name, node.desc, string, ldc.cst);
                            break;
                        }
                    }
                    if (string.startsWith("L") && string.endsWith(";")) {
                        String className = string.substring(1, string.length() - 1);
                        if (mapping.containsKey(className)) {
                            String v = mapping.get(className);
                            ldc.cst = "L" + v + ";";
                            logger.info("Replaced reflection class call: {} -> {}", string, ldc.cst);
                        }
                    }
                    if (string.endsWith(".class")) {
                        String className = string.substring(0, string.length() - 6);
                        if (mapping.containsKey(className)) {
                            String v = mapping.get(className);
                            ldc.cst = v + ".class";
                            logger.info("Replaced reflection class call: {} -> {}", string, ldc.cst);
                        }
                    }
                }
            }
        }
    }

    private static void reflectionMemberSupport(HashMap<String, String> mapping, MethodNode node) {
        if (node.instructions == null) {
            return;
        }
        for (AbstractInsnNode insnNode : node.instructions.toArray()) {
            if (insnNode instanceof LdcInsnNode) {
                LdcInsnNode ldc = (LdcInsnNode) insnNode;
                if (ldc.cst instanceof String) {
                    String string = (String) ldc.cst;
                    if (string.isEmpty()) continue;
                    if (mapping.containsKey(string)) {
                        ldc.cst = mapping.get(string);
                        logger.info("Replaced reflection member call in {}{}: {} -> {}", node.name, node.desc, string, ldc.cst);
                    }
                }
            }
        }
    }

    private static void remapInner(HashMap<String, ClassNode> classes) {
        InnerRemapper remapper = new InnerRemapper();
        ArrayList<ClassNode> classNodes = new ArrayList<>(classes.values());
        Pattern innerClasses = Pattern.compile(".*[A-Za-z0-9]+\\$[0-9]+");
        for (ClassNode classNode : classNodes) {
            if (innerClasses.matcher(classNode.name).matches() && Optional.ofNullable(classNode.invisibleAnnotations).map(it -> it.stream().noneMatch(ann -> ann.desc.startsWith(MIXIN_HEADER))).orElse(true)) {
                String newName;

                if (classNode.name.contains("/")) {
                    String packageName = classNode.name.substring(0, classNode.name.lastIndexOf('/') + 1);
                    newName = packageName + randomUniqueName();
                } else newName = "";

                String mappedName;

                do {
                    mappedName = newName + randomUniqueName();
                } while (!remapper.map(classNode.name, mappedName));
            }
        }

        for (final ClassNode classNode : classNodes) {
            if (!enable || (classNode.invisibleAnnotations != null && classNode.invisibleAnnotations.stream().anyMatch(it -> it.desc.equals(_NO_RENAME_DESC) || it.desc.startsWith(MIXIN_HEADER)))
            ) {
                continue;
            }
            classes.remove(classNode.name);

            ClassNode newNode = new ClassNode();
            ClassRemapper classRemapper = new ClassRemapper(newNode, remapper);
            classNode.accept(classRemapper);

            newNode.outerClass = null;
            newNode.innerClasses.clear();

            newNode.outerMethod = null;
            newNode.outerMethodDesc = null;
            newNode.access &= ~Opcodes.ACC_PRIVATE;
            newNode.access &= ~Opcodes.ACC_PROTECTED;
//            newNode.access |= Opcodes.ACC_PUBLIC;
            newNode.methods.forEach(method -> {
                method.access &= ~Opcodes.ACC_PRIVATE;
                method.access &= ~Opcodes.ACC_PROTECTED;
//                method.access |= Opcodes.ACC_PUBLIC;
            });

            newNode.fields.forEach(field -> {
                field.access &= ~Opcodes.ACC_PRIVATE;
                field.access &= ~Opcodes.ACC_PROTECTED;
//                field.access |= Opcodes.ACC_PUBLIC;
            });

            // don't relocate classes with native method
            if (classNode.methods.stream().anyMatch(methodNode -> isNative(methodNode.access))) {
                classes.put(classNode.name, classNode);
                continue;
            }
            // non supported yet!
            if ((classNode.access & Opcodes.ACC_ANNOTATION) != 0) {
                classes.put(classNode.name, classNode);
                continue;
            }
            classes.put(newNode.name, newNode);

        }
    }

    private static byte[] getBytes(InputStream ins) throws IOException {
        byte[] data = new byte[4096];
        ByteArrayOutputStream entryBuffer = new ByteArrayOutputStream();

        int len;
        do {
            len = ins.read(data);
            if (len > 0) {
                entryBuffer.write(data, 0, len);
            }
        } while (len != -1);

        return entryBuffer.toByteArray();
    }

    private static void checkMissingLibraries(ClassNodeTree classNodeTree, HashMap<String, ClassNode> classes) {
        Map<String, ClassNodeInfo> map = classNodeTree.map;
        if (map.values().stream().noneMatch(info -> info.node == null)) return;
        for (String name : classes.keySet()) {
            ClassNodeInfo info = map.get(name);
            if (info == null) continue;
            if (info.node == null) continue;
            if (!info.ableToRename) continue;
            LinkedList<ClassNodeInfo> traces = new LinkedList<>();
            traces.add(info);
            checkMissingParent0(info, traces);
        }
    }

    private static void checkMissingParent0(ClassNodeInfo info, LinkedList<ClassNodeInfo> traces) {
        if (info.parents.isEmpty()) return;
        if (!info.children.isEmpty()) return;
        for (ClassNodeInfo parent : info.parents) {
            LinkedList<ClassNodeInfo> tracesNext = new LinkedList<>(traces);
            tracesNext.add(parent);
            if (parent.node == null) {
                StringBuilder sb = new StringBuilder("inheritance trace:");
                for (ClassNodeInfo trace : tracesNext) {
                    sb.append("\n   -> ").append(trace.nodeName.replace("/",  "."));
                    if (trace.node == null) sb.append(" (!)");
                }
                logger.warn("Class file {} is not found in classpath, please check your libraries setting.\n{}", parent.nodeName.replace("/",  "."), sb);
            }
            checkMissingParent0(parent, tracesNext);
        }
        traces.clear();
    }

    private static void generateClassesMapping(HashMap<String, String> mapping, HashMap<String, ClassNode> classes) {
//        String flattenPackage = "";
//        if (main.size() == 1) {
//            String[] split = main.get(0).split("/");
//            flattenPackage = split.length > 0 ? String.join("/", Arrays.copyOfRange(split, 0, split.length - 1)) : "";
//            if (!flattenPackage.isEmpty()) flattenPackage += "/";
//        }
//        flattenPackage += randomUniqueName() + "/" + randomUniqueName() + "/";
//
//        final String pkg = flattenPackage;
        ArrayList<Map.Entry<String, ClassNode>> classEntries = new ArrayList<>(classes.entrySet());
        Collections.shuffle(classEntries);
        String defaultRelocatePackage = relocatePackage.isEmpty() ? "" : (relocatePackage.endsWith("/") ? relocatePackage : (relocatePackage + "/"));
        classEntries.forEach(e -> {
            String name = e.getKey();
            ClassNode node = e.getValue();
            // don't relocate classes with native method
            if (!configIncluded(name) || node.methods.stream().anyMatch(methodNode -> isNative(methodNode.access))) return;
            if (!enable ||
                    (node.invisibleAnnotations != null && node.invisibleAnnotations.stream().anyMatch(it -> it.desc.equals(_NO_RENAME_DESC) || it.desc.startsWith(MIXIN_HEADER)))
            ) {
                return;
            }
            if (name.startsWith("com/yumegod/obfuscation")) return; // don't obfuscate our api
            String newName;
            String pkg = defaultRelocatePackage;
            if (!enableRelocate) {
                pkg = node.name.substring(0, node.name.lastIndexOf('/') + 1);
            }
            do {
                newName = pkg + NameFactoryUtils.getNameFactoryByPackage(pkg).nextName();
            } while (mapping.containsValue(newName));
            mapping.put(name, newName);
        });
    }

    private static void generateMembersMapping(ClassNodeTree tree, Map<String, Map<String, Map<String, String>>> mapping, Map<String, ClassNode> classes,
                                               WhitelistArg whitelistArg) {
        membersAnalysisDepth(tree.root, mapping, classes, new HashMap<>(), NameFactoryUtils.getMemberNameFactory(), whitelistArg);
    }

    private static final Function<String, Map<String, Map<String, String>>> CLASS_CREATE_MAP_FUNC = k -> new HashMap<>();

    @NotNative
    private static void membersAnalysisDepth(
            ClassNodeInfo info,
            Map<String, Map<String, Map<String, String>>> mapping, Map<String, ClassNode> classes, Map<String, Map<String, String>> descMemberMap,
            NameFactory nameFactory,
            WhitelistArg whitelistArg
    ) {
        ClassNode classNode = info.node;
        // capability
        boolean shouldSpreadBackward = info.children.isEmpty();
        String className = classNode.name;
        LinkedHashSet<String> whitelistedMemberNames = whitelistArg.whitelistedMemberNames;
        Map<String, Map<String, String>> descriptorMap = mapping.computeIfAbsent(className, CLASS_CREATE_MAP_FUNC);
        if (info.ableToRename && configIncluded(className)) {
            for (MethodNode method : classNode.methods) {
                String methodName = method.name;
                if (!methodCanBeRenamed(method) || ((classNode.access & Opcodes.ACC_ANNOTATION) != 0)|| methodName.contains("$")) continue;
                String desc = formattedDesc(method.desc);

                if (shouldNotRename(method.invisibleAnnotations) ||
                        (resourceAdaptMethod == RenameResourceAdaptMethod.EXCLUDE && resourcesString.contains(methodName)) ||
                        (whitelistedMemberNames.contains(methodName))
                ) {
                    // this method got ignored, and we should make its change spread backward if needed
                    if (!isPrivate(method.access) && descMemberMap.containsKey(desc)) {
                        if (methodName.equals(descMemberMap.get(desc).get(methodName))) continue;
                        shouldSpreadBackward = true;
                    }
                    continue;
                }
                Map<String, String> membersNameMap = retrieveNameMap(descMemberMap, desc);
                String newName = membersNameMap.get(methodName);
                if (newName == null) {
                    // 尝试兼容实现多个接口+继承的情况
                    newName = retrieveNameMap(descriptorMap, desc).get(methodName);
                    if (newName == null) {
                        nameFactory.reset();
                        do {
                            newName = nameFactory.nextName();
                        } while (membersNameMap.containsValue(newName));
                    }
                }
//                if (!isPrivate(method.access))
                {
                    membersNameMap.put(methodName, newName);
                }
                retrieveNameMap(descriptorMap, desc).put(methodName, newName);
            }
            for (FieldNode field : classNode.fields) {
                String fieldName = field.name;
                String desc = formattedDesc(field.desc);

                if (shouldNotRename(field.invisibleAnnotations)) continue;
                if(resourceAdaptMethod == RenameResourceAdaptMethod.EXCLUDE && resourcesString.contains(fieldName) || whitelistedMemberNames.contains(fieldName)) {
                    if (!isPrivate(field.access) && descMemberMap.containsKey(desc) && descMemberMap.get(desc).containsKey(fieldName)) {
                        shouldSpreadBackward = true;
                        descMemberMap.get(desc).put(fieldName, fieldName);
                    }
                    continue;
                }
                Map<String, String> membersNameMap = retrieveNameMap(descMemberMap, desc);
                String newName = membersNameMap.get(fieldName);
                if (newName == null) {
                    newName = retrieveNameMap(descriptorMap, desc).get(fieldName);
                    if (newName == null) {
                        nameFactory.reset();
                        do {
                            newName = nameFactory.nextName();
                        } while (membersNameMap.containsValue(newName));
                    }
//                    if (!isPrivate(field.access))
                    {
                        membersNameMap.put(fieldName, newName);
                    }
                }
                retrieveNameMap(descriptorMap, desc).put(fieldName, newName);
            }
        } else {
            for (MethodNode method : classNode.methods) {
//                if (!isPrivate(method.access))
                    retrieveNameMap(descMemberMap, formattedDesc(method.desc)).put(method.name, method.name);
            }
            for (FieldNode field : classNode.fields) {
//                if (!isPrivate(field.access))
                    retrieveNameMap(descMemberMap, formattedDesc(field.desc)).put(field.name, field.name);
            }
        }
        if (shouldSpreadBackward) {
            for (ClassNodeInfo parent : info.parents) {
                membersSpreadBackward(parent, mapping, classes, new HashMap<>(descMemberMap));
            }
        }

        for (Map.Entry<String, Map<String, String>> entry : descMemberMap.entrySet()) {
            Map<String, String> map = descriptorMap.get(entry.getKey());
            if (map == null) {
                map = new HashMap<>();
            }
            map.putAll(entry.getValue());
        }
        for (ClassNodeInfo it : info.children) {
            HashMap<String, Map<String, String>> map = new HashMap<>();
            // deep clone
            for (Map.Entry<String, Map<String, String>> entry : descMemberMap.entrySet()) {
                map.put(entry.getKey(), new HashMap<>(entry.getValue()));
            }
            membersAnalysisDepth(it, mapping, classes, map, nameFactory, whitelistArg);
        }
    }

    @NotNative
    private static void membersSpreadBackward(
            ClassNodeInfo info,
            Map<String, Map<String, Map<String, String>>> mapping, Map<String, ClassNode> classes, Map<String, Map<String, String>> membersMap
    ) {
        if (info.node == null) return;
        String className = info.node.name;
        if (info.ableToRename) {
            if (mapping.containsKey(className)) {
                mapping.get(className).putAll(membersMap);
            }
        }
        for (ClassNodeInfo it : info.parents) {
            membersSpreadBackward(it, mapping, classes, membersMap);
        }
    }

    /**
     * Gets the name map, based on the given map and a given descriptor.
     * A new empty map is created if necessary.
     * @param descriptorMap the map of descriptors to [old name - new name] maps.
     * @param descriptor    the member's descriptor.
     * @return the corresponding name map.
     */
    @NotNative
    static Map<String, String> retrieveNameMap(Map<String, Map<String, String>> descriptorMap, String descriptor) {
        return descriptorMap.computeIfAbsent(descriptor, k -> new HashMap<>());
    }

    private static boolean shouldNotRename(List<AnnotationNode> annotations) {
        return annotations != null && annotations.stream().anyMatch(it -> it.desc.equals(_NO_RENAME_DESC) || it.desc.startsWith(MIXIN_HEADER));
    }

    private static boolean isClass(byte[] bytes) {
        return Util.byteArrayToInt(Arrays.copyOfRange(bytes, 0, 4)) == 0xCAFEBABE;
    }

    private static String randomUniqueName() {
        int i;
        do {
            i = Math.abs((int) ((System.currentTimeMillis() ^ System.nanoTime()) % 0xFFFFFFFFL));
        } while (i == 0);
        StringBuilder builder = new StringBuilder();
        while (i > 0) {
            builder.append(i % 2 == 0 ? 'I' : 'l');
            i /= 2;
        }
        String string = builder.toString();
        // 真受不了
        return string.isEmpty() ? randomUniqueName() : string;
    }

    static boolean configIncluded(String name) {
        for (String expr : configIncludes) {
            if (NodeMatcher.MATCH(expr, name)) return true;
        }
        return false;
    }

    private static String getMainClass(String s) {
        return getMainClass("Main-Class: ", s);
    }

    private static String getMainClass(String key, String s) {
        String mainClass = null;

        for (String s1 : s.split("\n")) {
            if (s1.startsWith(key)) {
                mainClass = s1.substring(key.length()).trim().replace("\r", "");
            }
        }

        return mainClass;
    }

    private static boolean methodCanBeRenamed(MethodNode method) {
        return !isNative(method.access) && !"main".equals(method.name)
                && !"premain".equals(method.name) && !method.name.startsWith("<");
    }

    public static boolean isAnonymousInnerClass(ClassNode classNode) {
        // Check based on naming convention: Look for $ followed by a number
        if (!classNode.name.matches(".*\\$\\d+$")) {
            return false;
        }

        // Check for InnerClass attribute where innerName is null
        if (classNode.innerClasses != null) {
            for (InnerClassNode icn : classNode.innerClasses) {
                if (icn.name.equals(classNode.name) && icn.innerName == null) {
                    return true;
                }
            }
        }

        return false;
    }


    /* struct */ static class WhitelistArg {
        LinkedHashSet<String> whitelistedMemberNames = new LinkedHashSet<>(0);
    }
}