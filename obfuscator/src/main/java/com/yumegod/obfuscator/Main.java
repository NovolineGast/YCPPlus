package com.yumegod.obfuscator;

import com.yumegod.obfuscator.enums.NumberObfuscationMode;
import com.yumegod.obfuscator.enums.Platform;
import com.yumegod.obfuscator.enums.RenameResourceAdaptMethod;
import com.yumegod.obfuscator.utils.Util;
import com.yumegod.obfuscator.utils.cfg.ConfigManager;
import com.yumegod.obfuscator.utils.cfg.annotations.StaticConfigReceiver;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {
    public static final String VERSION = "2.5";

    public static NativeObfuscatorRunner runnerInstance;
    public static final Random random = new Random();

    // Development environment ONLY
    public static final boolean devMode = false, debug = false;

    @CommandLine.Command(name = "YumeCloudProtection", mixinStandardHelpOptions = true, version = "v" + VERSION,
            description = "All-in-one protection solution for Java applications")
    public static class NativeObfuscatorRunner implements Callable<Integer> {
        @CommandLine.Parameters(index = "0", description = "Input File")
        public File jarFile;

        @CommandLine.Parameters(index = "1", description = "Output File")
        public File outputFile;

        @CommandLine.Parameters(index = "2", description = "Application name")
        public String applicationName;

        @CommandLine.Option(names = {"-u", "--auth-url"}, description = "Authorization URL")
        public String authorizationURL;

        @CommandLine.Option(names = {"-d", "--debug"}, description = "Enable debug output")
        public boolean debug;

        @CommandLine.Option(names = {"-l", "--library"}, description = "Directory for dependent libraries")
        public File librariesDirectory;

        @CommandLine.Option(names = {"-b", "--black-list"}, description = "File with a list of blacklist classes/methods for native translation")
        public File blackListFile;

        @CommandLine.Option(names = {"-w", "--white-list"}, description = "File with a list of whitelist classes/methods for native translation")
        public File whiteListFile;

        @CommandLine.Option(names = {"-p", "--platform"}, defaultValue = "hotspot",
                description = "Target platform: hotspot - standard standalone HotSpot JRE, std_java - java standard")
        public Platform platform;

        @CommandLine.Option(names = {"-a", "--annotation"}, description = "Use annotations to ignore/include native obfuscation")
        public boolean useAnnotations;

        @CommandLine.Option(names = {"-c", "--call-encrypt"}, description = "Apply call encryption to all classes and methods")
        public boolean callEncryption;

        @CommandLine.Option(names = {"-r", "--rename-obfuscate"}, description = "Apply rename obfuscation to all classes and methods")
        public boolean renameObfuscation;

        @CommandLine.Option(
                names = {"--rra", "--rename-resource-adapt"}, defaultValue = "exclude", showDefaultValue = CommandLine.Help.Visibility.ALWAYS,
                description = "The method we should use to adapt resource.\naggressive -> also apply mapping into resource (plaintext only)\nexclude -> exclude all classes and methods appeared in resources (plaintext only)\nnone -> will not do any work"
        )
        public RenameResourceAdaptMethod resourceAdaptMethod;

        @CommandLine.Option(names = {"--force-change-relative-resource"}, description = "Force change relative resource path to absolute path")
        public boolean forceChangeRelativeResource;

        @CommandLine.Option(names = {"-n", "--number-obfuscate"}, description = "Apply number obfuscation to all classes and methods")
        public boolean numberObfuscation;

        @CommandLine.Option(names = {"-s", "--string-obfuscate"}, description = "Apply string obfuscation to all classes and methods")
        public boolean stringObfuscation;

        @CommandLine.Option(names = {"-f", "--flow-obfuscate"}, description = "Apply flow obfuscation to all classes and methods")
        public boolean flowObfuscation;

        @CommandLine.Option(
                names = {"--nom", "--number-obfuscate-mode"},
                description = "Determine the mode of operation of number obfuscation in conjunction with control flow obfuscation.\naggressive/normal/simple",
                defaultValue = "aggressive", showDefaultValue = CommandLine.Help.Visibility.ALWAYS
        )
        public NumberObfuscationMode numberObfuscationMode;

        @CommandLine.Option(names = {"--dir-classes"}, description = "Transfer of all classes to the directory")
        public boolean toDirClasses;

        @CommandLine.Option(names = {"--watermark"}, description = "Add a watermark to the jar file")
        public String watermark = "";

        @CommandLine.Option(names = {"--safe-mode"}, description = "Disable all experimental features")
        public boolean safeMode;

        @CommandLine.Option(names = {"--no-auth"}, description = "Disable the native YumeCloud Authorization")
        public boolean noAuth;

//        @CommandLine.Option(names = {"--no-shrink"}, description = "Disable simple shrinking")
//        public boolean noShrink;

        @Override
        public Integer call() throws Exception {
//            runnerInstance = this;
//            outputFile = outputFile.getCanonicalFile();
//            List<Path> libs = new ArrayList<>();
//            if (librariesDirectory != null) {
//                try (Stream<Path> stream = Files.walk(librariesDirectory.toPath(), FileVisitOption.FOLLOW_LINKS)) {
//                    stream.parallel().filter(Objects::nonNull).filter(getJarPathPredicate()).forEach(libs::add);
//                }
//            }
//            try (Stream<Path> pathStream = Files.walk(new File(System.getProperty("java.home")).toPath(), FileVisitOption.FOLLOW_LINKS)) {
//                pathStream.parallel().filter(Objects::nonNull).filter(getJarPathPredicate()).forEach(libs::add);
//            }

//            List<String> blackList = new ArrayList<>();
//            if (blackListFile != null) {
//                blackList = Files.readAllLines(blackListFile.toPath(), StandardCharsets.UTF_8).stream()
//                        .filter(s -> !s.isEmpty() && !s.startsWith("//")).map(String::trim).distinct()
//                        .collect(Collectors.toList());
//            }
//
//            List<String> whiteList = null;
//            if (whiteListFile != null) {
//                whiteList = Files.readAllLines(whiteListFile.toPath(), StandardCharsets.UTF_8).stream()
//                        .filter(s -> !s.isEmpty() && !s.startsWith("//")).map(String::trim).distinct()
//                        .collect(Collectors.toList());
//            }
//
//            if (authorizationURL == null || authorizationURL.isEmpty()) {
//                authorizationURL = "http://protection.yumegod.com:13337/";
//            }
//
//            Path outputDirectory;
//            if (devMode) {
//                outputDirectory = Files.createDirectory(Paths.get("devMode_" + Util.randomString(10) + File.separator));
//            } else {
//                outputDirectory = Files.createTempDirectory("YumeCloudProtection_");
//                new File(outputDirectory + File.separator).deleteOnExit();
//            }
//
//            Path workDir = Paths.get(outputDirectory + File.separator + UUID.randomUUID());
//            ConfigManager.loadConfig(null);
//            new YumeCloudProtection().process(jarFile, workDir,
//                    libs, platform);
            return 0;
        }

        private static Predicate<Path> getJarPathPredicate() {
            return f -> f.toString().endsWith(".jar") || f.toString().endsWith(".zip") || f.toString().endsWith(".jmod");
        }
    }


    public static void main(String[] args) throws Exception {
        System.out.println("=========================================================\n" +
                " __     __                    _____ _                 _ \n" +
                " \\ \\   / /                   / ____| |               | |\n" +
                "  \\ \\_/ /   _ _ __ ___   ___| |    | | ___  _   _  __| |\n" +
                "   \\   / | | | '_ ` _ \\ / _ \\ |    | |/ _ \\| | | |/ _` |\n" +
                "    | || |_| | | | | | |  __/ |____| | (_) | |_| | (_| |\n" +
                "    |_| \\__,_|_| |_| |_|\\___|\\_____|_|\\___/ \\__,_|\\__,_|\n" +
                "\nQQ Group: 868318886\n" +
                "=========================================================");
        System.out.println("YumeCloud Protection v" + VERSION + "\n");
        if (args.length == 0) {
            File file = new File("./default-config.yml");
            if (!file.exists()) {
                Files.copy(Objects.requireNonNull(YumeCloudProtection.class.getResourceAsStream("/default-config.yml")), file.toPath());
                System.out.println("Default config file generated, please edit it and run again.");
            }
        } else if (args.length == 1) {
            if (!args[0].startsWith("-")) {
                new YumeCloudProtection().processWithConfig(new File(args[0]));
                return;
            }
        }
        System.out.println("Usage: java -jar YumeCloudProtection.jar <config_file>");
//        System.exit(new CommandLine(new NativeObfuscatorRunner()).setCaseInsensitiveEnumValuesAllowed(true).execute(args));
    }
}
