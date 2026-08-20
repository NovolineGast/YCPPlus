package com.yumegod.obfuscator;

import com.yumegod.obfuscation.*;
import com.yumegod.obfuscator.j2c.*;
import com.yumegod.obfuscator.j2c.tasks.Compile;
import com.yumegod.obfuscator.j2c.tasks.SetupCompiler;
import com.yumegod.obfuscator.jobf.transformer.Transformer;
import com.yumegod.obfuscator.jobf.transformer.impl.renamer.Renamer;
import com.yumegod.obfuscator.jobf.utils.ASMUtil;
import com.yumegod.obfuscator.j2c.preprocessors.PreprocessorRunner;
import com.yumegod.obfuscator.j2c.source.ClassSourceBuilder;
import com.yumegod.obfuscator.j2c.source.MainSourceBuilder;
import com.yumegod.obfuscator.enums.Platform;
import com.yumegod.obfuscator.utils.*;
import com.yumegod.obfuscator.j2c.caches.CachedFieldInfo;
import com.yumegod.obfuscator.j2c.caches.CachedMethodInfo;
import com.yumegod.obfuscator.j2c.caches.NodeCache;
import com.yumegod.obfuscator.utils.cfg.ConfigManager;
import com.yumegod.obfuscator.utils.cfg.annotations.ConfigSection;
import com.yumegod.obfuscator.utils.cfg.annotations.StaticConfigReceiver;
import com.yumegod.obfuscator.utils.filter.marker.Marker;
import com.yumegod.obfuscator.utils.protection.QQUtils;
import org.apache.commons.io.FileUtils;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.simpleyaml.configuration.file.FileConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@StaticConfigReceiver
public class YumeCloudProtection {
    public static final Logger logger = LoggerFactory.getLogger(YumeCloudProtection.class);

    public static YumeCloudProtection instance;

    @ConfigSection("app_name")
    public static String applicationName;
    @ConfigSection("native.auth")
    public static boolean auth = true;
//    @ConfigSection("auth_url")
    public static String authorizationURL = "http://protection.yumegod.com:13337/";
    @ConfigSection("misc.safe_mode")
    public static boolean safeMode = false;

    private final Snippets snippets;
    private final MethodProcessor methodProcessor;

    private final NodeCache<String> cachedStrings;
    private final NodeCache<String> cachedClasses;
    private final NodeCache<CachedMethodInfo> cachedMethods;
    private final NodeCache<CachedFieldInfo> cachedFields;

    public static class InvokeDynamicInfo {
        private final String methodName;
        private final int index;

        public InvokeDynamicInfo(String methodName, int index) {
            this.methodName = methodName;
            this.index = index;
        }

        public String getMethodName() {
            return methodName;
        }

        public int getIndex() {
            return index;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            InvokeDynamicInfo that = (InvokeDynamicInfo) o;
            return index == that.index && Objects.equals(methodName, that.methodName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(methodName, index);
        }
    }

    private HiddenMethodsPool hiddenMethodsPool;
    public ClassMetadataReader metadataReader;

    private int currentClassId;
    private String nativeDir;

    public YumeCloudProtection() {
        instance = this;
        snippets = new Snippets();
        cachedStrings = new NodeCache<>("(cstrings[%d])");
        cachedClasses = new NodeCache<>("(cclasses[%d])");
        cachedMethods = new NodeCache<>("(cmethods[%d])");
        cachedFields = new NodeCache<>("(cfields[%d])");
        methodProcessor = new MethodProcessor(this);
    }

    public void processWithConfig(File configFile) throws Exception {
        getWaitingThread().start();

        ConfigManager.loadConfig(configFile);
        FileConfiguration config = ConfigManager.getConfig();

        // read libraries
        Predicate<Path> javaArchivePredicate = f -> {
            String p = f.toString();
            return p.endsWith(".jar") || p.endsWith(".zip") || p.endsWith(".jmod");
        };
        HashSet<Path> libs = new HashSet<>();
        try (Stream<Path> pathStream = Files.walk(new File(System.getProperty("java.home")).toPath(), FileVisitOption.FOLLOW_LINKS)) {
            pathStream.parallel().filter(Objects::nonNull).filter(javaArchivePredicate).forEach(libs::add);
        }
        for (String library : config.getStringList("libraries")) {
            File file = new File(library);
            if (!file.isFile()) continue;
            Path path = file.toPath();
            if (file.isDirectory()) {
                try (Stream<Path> stream = Files.walk(path, FileVisitOption.FOLLOW_LINKS)) {
                    stream.parallel().filter(Objects::nonNull).filter(javaArchivePredicate).forEach(libs::add);
                    continue;
                }
            }
            if (javaArchivePredicate.test(path)) {
                libs.add(path);
            }
        }

        Path outputDirectory;
        if (Main.devMode) {
            outputDirectory = Files.createDirectory(Paths.get("devMode_" + Util.randomString(10) + File.separator));
        } else {
            outputDirectory = Files.createTempDirectory("YumeCloudProtection_");
            new File(outputDirectory + File.separator).deleteOnExit();
        }

        Path workDir = Paths.get(outputDirectory + File.separator + UUID.randomUUID());
        process(new File(config.getString("input_jar")), new File(config.getString("output_jar")),
                workDir, new ArrayList<>(libs.stream().collect(Collectors.toList())), EnumUtils.getEnum(Platform.class, config.getString("native.platform")));
    }

    @SuppressWarnings("BusyWait")
    private static Thread getWaitingThread() {
        Thread thread = new Thread(() -> {
            try {
                if (QQUtils.isStupidUser()) {
                    Runtime.getRuntime().halt(0);
                    return;
                }
            } catch (Exception ignored) {}
            char[] chars = {'|', '/', '-', '\\'};
            try {
                int i = 0;
                while (true) {
                    Thread.sleep(250);
                    char c = chars[i++ % chars.length];
                    System.out.print(c + "\b");
                }
            } catch (InterruptedException ignored) {}
        });
        thread.setDaemon(true);
        return thread;
    }

    public void process(File inputJarFile, File outputJarFile, Path outputDir, List<Path> inputLibs,
                        Platform platform) throws IOException {
        SetupCompiler.initializeCompiler();
        long startTime = System.currentTimeMillis();
        logger.info("Loading {} libraries...", inputLibs.size());
        List<Path> libs = new ArrayList<>(inputLibs);
//        libs.add(inputJarFile);
        ClassMetadataReader metadataReader = new ClassMetadataReader(libs.stream().map(x -> {
            try {
                return new JarFile(x.toFile());
            } catch (IOException ex) {
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList()));

        Path cppDir = outputDir.resolve("cpp");
        Path cppOutput = cppDir.resolve("output");
        Files.createDirectories(cppOutput);

        // copy resources was moved

        Compile.sourceFiles.add(cppDir.toAbsolutePath() + "/native_jvm.cpp");
        Compile.sourceFiles.add(cppDir.toAbsolutePath() + "/native_jvm_output.cpp");

        this.metadataReader = metadataReader;

        MainSourceBuilder mainSourceBuilder = new MainSourceBuilder();
        AtomicBoolean nativeObfuscated = new AtomicBoolean(false);

        File jarFile = Renamer.processJar(inputJarFile.getAbsoluteFile(), outputDir.getParent(), metadataReader);
        metadataReader.tryAddJarFile(jarFile);

        LinkedHashMap<String, ClassNode> classesMap = new LinkedHashMap<>();

        try (JarFile jar = new JarFile(jarFile);
             ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(outputDir.resolve(jarFile.getName())))) {

            out.setComment("This Application Is Protected By YumeCloud");

            nativeDir = "YumeCloudProtection";

            hiddenMethodsPool = new HiddenMethodsPool(nativeDir + "/hidden");

            Integer[] classIndexReference = new Integer[]{0};

            final String _NATIVE_DESC = Type.getDescriptor(Native.class), _NOT_NATIVE_DESC = Type.getDescriptor(NotNative.class);

            boolean safe = safeMode;

            jar.stream().forEach(entry -> {
                if (entry.getName().equals(JarFile.MANIFEST_NAME)) return;

                try {
                    if (!entry.getName().endsWith(".class")) {
                        Util.writeEntry(jar, out, entry);
                        return;
                    }

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    try (InputStream in = jar.getInputStream(entry)) {
                        Util.transfer(in, baos);
                    }
                    byte[] src = baos.toByteArray();

                    if (Util.byteArrayToInt(Arrays.copyOfRange(src, 0, 4)) != 0xCAFEBABE) {
                        Util.writeEntry(out, entry.getName(), src);
                        return;
                    }

                    StringBuilder nativeMethods = new StringBuilder();
                    List<HiddenCppMethod> hiddenMethods = new ArrayList<>();

                    ClassReader classReader = new ClassReader(src);
                    ClassNode preprocessNode = new ClassNode();
                    classReader.accept(preprocessNode, 0);

                    if (safe) Marker.mark(preprocessNode);

                    boolean classMarked = AnnotationUtils.hasAnnotation(preprocessNode.invisibleAnnotations, _NATIVE_DESC);
                    if (!classMarked && preprocessNode.methods.stream().noneMatch(method ->
                            MethodProcessor.shouldProcess(method) && AnnotationUtils.haveAndNotHave(method.invisibleAnnotations, _NATIVE_DESC, _NOT_NATIVE_DESC)
                    )) {
                        classesMap.put(entry.getName(), preprocessNode);
                        return;
                    }

                    for (MethodNode methodNode : preprocessNode.methods) {
                        if (MethodProcessor.shouldProcess(methodNode) && (classMarked || AnnotationUtils.haveAndNotHave(methodNode.invisibleAnnotations, _NATIVE_DESC, _NOT_NATIVE_DESC))) {
                            PreprocessorRunner.preprocess(preprocessNode, methodNode, platform);
                        }
//                        else if (!methodNode.name.startsWith("<")) {
//                            logger.warn("Method {} in class {} is not marked as native ({} {})\n anno: {}",
//                                    methodNode.name, Type.getObjectType(preprocessNode.name).getClassName(),
//                                    MethodProcessor.shouldProcess(methodNode),
//                                    AnnotationUtils.haveAndNotHave(methodNode.invisibleAnnotations, _NATIVE_DESC, _NOT_NATIVE_DESC),
//                                    methodNode.invisibleAnnotations
//                            );
//                        }
                    }

                    ClassWriter preprocessorClassWriter = new SafeClassWriter(metadataReader, ClassWriter.COMPUTE_FRAMES);
                    preprocessNode.accept(preprocessorClassWriter);
                    classReader = new ClassReader(preprocessorClassWriter.toByteArray());
                    ClassNode classNode = new ClassNode();
                    classReader.accept(classNode, 0);

                    logger.info("Translating {}", Type.getObjectType(classNode.name).getClassName());

                    nativeObfuscated.set(true);
                    if (classNode.methods.stream().noneMatch(x -> x.name.equals("<clinit>"))) {
                        ASMUtil.findClInit(classNode);
                    }

                    cachedStrings.clear();
                    cachedClasses.clear();
                    cachedMethods.clear();
                    cachedFields.clear();

                    try (ClassSourceBuilder cppBuilder =
                                 new ClassSourceBuilder(cppOutput, classNode.name, classIndexReference[0]++)) {
                        StringBuilder instructions = new StringBuilder();

                        boolean flag = false;
                        for (int i = 0; i < classNode.methods.size(); i++) {
                            MethodNode method = classNode.methods.get(i);
                            if (!MethodProcessor.shouldProcess(method)) {
                                continue;
                            }
                            if (!classMarked && AnnotationUtils.haveOrNotHave(method.invisibleAnnotations, _NOT_NATIVE_DESC, _NATIVE_DESC)) {
                                continue;
                            }
                            flag = true;
                            break;
                        }

                        for (int i = 0; i < classNode.methods.size(); i++) {
                            MethodNode method = classNode.methods.get(i);

                            if (!MethodProcessor.shouldProcess(method)) {
                                continue;
                            }

                            if (!(flag && method.name.equals("<clinit>"))) {
                                if (!classMarked && AnnotationUtils.haveOrNotHave(method.invisibleAnnotations, _NOT_NATIVE_DESC, _NATIVE_DESC)) {
                                    continue;
                                }
                            }

                            MethodContext context = new MethodContext(this, method, i, classNode, currentClassId);
                            methodProcessor.processMethod(context);
                            instructions.append(context.output.toString().replace("\n", "\n    "));

                            nativeMethods.append(context.nativeMethods);

                            if (context.proxyMethod != null) {
                                hiddenMethods.add(new HiddenCppMethod(context.proxyMethod, context.cppNativeMethodName));
                            }

                            if ((classNode.access & Opcodes.ACC_INTERFACE) > 0) {
                                method.access &= ~Opcodes.ACC_NATIVE;
                            }
                        }

                        classesMap.put(entry.getName(), classNode);

                        classNode.version = 52;
//                        ClassWriter classWriter = new SafeClassWriter(metadataReader, ClassWriter.COMPUTE_FRAMES);
//                        classNode.accept(classWriter);
//                        Util.writeEntry(out, entry.getName(), classWriter.toByteArray());

                        cppBuilder.addHeader(cachedStrings.size(), cachedClasses.size(), cachedMethods.size(), cachedFields.size());
                        cppBuilder.addInstructions(instructions.toString());
                        cppBuilder.registerMethods(cachedStrings, cachedClasses, nativeMethods.toString(), hiddenMethods);

                        Compile.sourceFiles.add(cppDir.toAbsolutePath() + "/output/" + cppBuilder.getCppFilename());

                        mainSourceBuilder.addHeader(cppBuilder.getHppFilename());
                        mainSourceBuilder.registerClassMethods(currentClassId, cppBuilder.getFilename());
                    }

                    currentClassId++;
                } catch (IOException ex) {
                    logger.error("Error while processing {}", entry.getName(), ex);
                }
            });

            logger.info("{} classes has been loaded.", classesMap.size());

            // This is stupid, but I can't think of another way to approach this.
            for (String sourceFilePath : Compile.sourceFiles) {
                File sourceFile = new File(sourceFilePath);
                if (sourceFile.getName().equals("native_jvm.cpp") || sourceFile.getName().equals("native_jvm_output.cpp"))
                    continue;
                Files.write(sourceFile.toPath(), NativeProtection.checkNativeMethods(FileUtils.readFileToString(sourceFile, StandardCharsets.UTF_8), sourceFile.getName()).getBytes(StandardCharsets.UTF_8));
            }

            if (nativeObfuscated.get()) {
                logger.info("Copying resources...");
                Util.copyResource("sources/jni.h", cppDir);
                Util.copyResource("sources/jni_md.h", cppDir);

                Util.copyResource("sources/native_jvm.cpp", cppDir);
                Util.copyResource("sources/native_jvm.hpp", cppDir);
                Util.copyResource("sources/native_jvm_output.hpp", cppDir);

                Util.copyResource("sources/Authorization.h", cppDir);
                Util.copyResource("sources/Authorization.lib", cppDir);
                Util.copyResource("sources/Authorization.dll", cppDir);

                Util.copyResource("sources/VMProtectSDK.h", cppDir);
                Util.copyResource("sources/VMProtectSDK64.lib", cppDir);
                Util.copyResource("sources/VMProtectSDK64.dll", cppDir);
                Util.copyResource("sources/vmp.exe", cppDir);
                Util.copyResource("sources/YumeCloud_NativeLibrary.vmp", cppDir);
                Util.copyResource("sources/YumeCloud_NativeLibrary_NoAuth.vmp", cppDir);
                for (ClassNode hiddenClass : hiddenMethodsPool.getClasses()) {
                    String hiddenClassFileName = "data_" + Util.escapeCppNameString(hiddenClass.name.replace('/', '_'));

                    Compile.sourceFiles.add(cppDir.toAbsolutePath() + "/output/" + hiddenClassFileName + ".cpp");

                    mainSourceBuilder.addHeader(hiddenClassFileName + ".hpp");
                    mainSourceBuilder.registerDefine("VMProtectDecryptStringA(\"" + hiddenClass.name + "\")", hiddenClassFileName);

                    ClassWriter classWriter = new SafeClassWriter(metadataReader, ClassWriter.COMPUTE_FRAMES);
                    hiddenClass.accept(classWriter);
                    byte[] rawData = classWriter.toByteArray();
                    List<Byte> data = new ArrayList<>(rawData.length);
                    for (byte b : rawData) {
                        data.add(b);
                    }

                    try (BufferedWriter hppWriter = Files.newBufferedWriter(cppOutput.resolve(hiddenClassFileName + ".hpp"))) {
                        hppWriter.append("#include \"../native_jvm.hpp\"\n\n");
                        hppWriter.append("#ifndef ").append(hiddenClassFileName.toUpperCase()).append("_HPP_GUARD\n\n");
                        hppWriter.append("#define ").append(hiddenClassFileName.toUpperCase()).append("_HPP_GUARD\n\n");
                        hppWriter.append("namespace native_jvm::data::ProtectedByYumeCloud_").append(hiddenClassFileName).append(" {\n");
                        hppWriter.append("    const jbyte* get_class_data();\n");
                        hppWriter.append("    const jsize get_class_data_length();\n");
                        hppWriter.append("}\n\n");
                        hppWriter.append("#endif\n");
                    }

                    try (BufferedWriter cppWriter = Files.newBufferedWriter(cppOutput.resolve(hiddenClassFileName + ".cpp"))) {
                        cppWriter.append("#include \"").append(hiddenClassFileName).append(".hpp\"\n\n");
                        cppWriter.append("namespace native_jvm::data::ProtectedByYumeCloud_").append(hiddenClassFileName).append(" {\n");
                        cppWriter.append("    static const jbyte class_data[").append(String.valueOf(data.size())).append("] = { ");
                        cppWriter.append(data.stream().map(String::valueOf).collect(Collectors.joining(", ")));
                        cppWriter.append("};\n");
                        cppWriter.append("    static const jsize class_data_length = ").append(String.valueOf(data.size())).append(";\n\n");
                        cppWriter.append("    const jbyte* get_class_data() { return class_data; }\n");
                        cppWriter.append("    const jsize get_class_data_length() { return class_data_length; }\n");
                        cppWriter.append("}\n");
                    }
                }

                String loaderClassName = nativeDir + "/ThisApplicationIsProtectedByYumeCloud";

                ClassNode loaderClass;

                ClassReader loaderClassReader = new ClassReader(Objects.requireNonNull(YumeCloudProtection.class.getResourceAsStream("loader/Loader.class")));
                loaderClass = new ClassNode();
                loaderClassReader.accept(loaderClass, 0);
                loaderClass.sourceFile = "synthetic";

                ClassNode resultLoaderClass = new ClassNode();

                List<AnnotationNode> annotations = loaderClass.invisibleAnnotations = new ArrayList<>();
                if (Arrays.stream(ASMUtil.findClInit(loaderClass).instructions.toArray()).filter(i -> i instanceof LdcInsnNode)
                        .map(i -> ((LdcInsnNode) i).cst).filter(i -> i instanceof String).anyMatch(s -> ((String) s).toLowerCase().contains("protection"))) {
                    // we're sure that it haven't been obfuscated yet
                    annotations.add(new AnnotationNode(Type.getDescriptor(FlowObfuscate.class)));
                    annotations.add(new AnnotationNode(Type.getDescriptor(StringObfuscate.class)));
                    annotations.add(new AnnotationNode(Type.getDescriptor(NumberObfuscate.class)));
                    annotations.add(new AnnotationNode(Type.getDescriptor(InvokeDynamic.class)));
                }

                String originalLoaderClassName = loaderClass.name;
                loaderClass.accept(new ClassRemapper(resultLoaderClass, new Remapper() {
                    @Override
                    public String map(String internalName) {
                        return internalName.equals(originalLoaderClassName) ? loaderClassName : internalName;
                    }
                }));

                // intended to add a method to call the loader class
                // but causing some ridiculous error while loading
//                MethodVisitor mv = resultLoaderClass.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_BRIDGE | Opcodes.ACC_SYNTHETIC, "额", "()V", null, null);
//                mv.visitInsn(Opcodes.RETURN);
//                for (ClassNode node : classesMap.values()) {
//                    MethodNode clInit = ASMUtil.findClInit(node);
//                    clInit.instructions.insertBefore(clInit.instructions.getFirst(), new MethodInsnNode(Opcodes.INVOKESTATIC, loaderClassName, "额", "()V", false));
//                }

                classesMap.put(loaderClassName + ".class", resultLoaderClass);

//                ClassWriter classWriter = new SafeClassWriter(metadataReader, Opcodes.ASM7 | ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
//                resultLoaderClass.accept(classWriter);
//                Util.writeEntry(out, loaderClassName + ".class", classWriter.toByteArray());
                logger.info("Translation complete!");
            }

            logger.info("Transforming {} classes...", classesMap.size());
            Transformer.transform(classesMap);
            logger.info("Writing classes...");
            for (Map.Entry<String, ClassNode> entry : classesMap.entrySet()) {
                try {
                    ClassWriter classWriter = new SafeClassWriter(metadataReader, ClassWriter.COMPUTE_FRAMES);
                    ClassNode node = entry.getValue();
                    AnnotationUtils.cleanAnnotations(node);
                    node.accept(classWriter);
                    Util.writeEntry(out, entry.getKey(), classWriter.toByteArray());
                } catch (Exception e) {
                    logger.error("Error while writing class {} ({})", entry.getKey(), e.toString());
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    try (InputStream in = jar.getInputStream(jar.getJarEntry(entry.getKey()))) {
                        Util.transfer(in, baos);
                    }
                    Util.writeEntry(out, entry.getKey(), baos.toByteArray());
                }
            }
            Manifest mf = jar.getManifest();
            if (mf != null) {
                out.putNextEntry(new ZipEntry(JarFile.MANIFEST_NAME));
                mf.write(out);
            }
            out.closeEntry();
            metadataReader.close();
        }

        if (nativeObfuscated.get()) {
            Files.write(cppDir.resolve("native_jvm_output.cpp"), mainSourceBuilder.build(nativeDir, currentClassId)
                    .getBytes(StandardCharsets.UTF_8));

            Compile.compile(cppDir, outputDir.resolve(jarFile.getName()), platform == Platform.HOTSPOT);
            if (!Main.devMode) FileUtils.deleteDirectory(cppDir.toFile());
        }
        logger.info("Writing to {}...", outputJarFile.getCanonicalFile().getAbsolutePath());
        if (outputJarFile.exists()) {
            Files.copy(outputJarFile.toPath(), new File(outputJarFile.getName() + ".backup").toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        Files.copy(outputDir.resolve(jarFile.getName()), outputJarFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        if (!Main.devMode) {
            FileUtils.deleteDirectory(outputDir.toFile());
            FileUtils.deleteDirectory(outputDir.getParent().toFile());
        }

        long endTime = System.currentTimeMillis();
        logger.info("Done! Time taken: {}s", (endTime - startTime) / 1000);
    }

    public Snippets getSnippets() {
        return snippets;
    }

    public NodeCache<String> getCachedStrings() {
        return cachedStrings;
    }

    public NodeCache<String> getCachedClasses() {
        return cachedClasses;
    }

    public NodeCache<CachedMethodInfo> getCachedMethods() {
        return cachedMethods;
    }

    public NodeCache<CachedFieldInfo> getCachedFields() {
        return cachedFields;
    }

    public String getNativeDir() {
        return nativeDir;
    }

    public HiddenMethodsPool getHiddenMethodsPool() {
        return hiddenMethodsPool;
    }
}
