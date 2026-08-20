package com.yumegod.obfuscator.j2c.tasks;

import com.yumegod.obfuscator.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

/**
 * @Author: Yume
 * @Date: 2024/3/12 21:26
 */
public class Compile {
    public static ArrayList<String> sourceFiles = new ArrayList<String>();
    private static final Logger logger = LoggerFactory.getLogger(Compile.class);

    public static void compile(Path cppDir, Path outputDir, boolean hotSpot) {
        logger.info("Start compiling native library...");
        try {
            StringBuilder command = new StringBuilder();

            command.append("zig c++ -fno-sanitize=all -fno-sanitize-trap=all -Os -fno-optimize-sibling-calls -fno-slp-vectorize -target x86_64-windows -std=c++17 -fPIC -s -fvisibility=hidden").append(" ");
            if (hotSpot) {
                command.append("-DUSE_HOTSPOT=1").append(" ");
            }
            command.append("-I\"" + cppDir.toAbsolutePath()).append("\" ");
            command.append("-o \"" + cppDir.toAbsolutePath() + "/YumeCloud_NativeLibrary").append("\" ");

            File sources = new File(cppDir + "/sources.txt");
            StringBuilder source = new StringBuilder();
            for (String s : sourceFiles) {
                source.append(s).append("\n");
            }
            Files.write(sources.toPath(), source.toString().getBytes(StandardCharsets.UTF_8));

            command.append("@" + sources.getAbsolutePath()).append(" ");
            command.append("-L\"" + cppDir.toAbsolutePath()).append("\" ");
            command.append("-lAuthorization").append(" ");
            command.append("-lVMProtectSDK64").append(" ");
            command.append("-shared");

            // For debugging
//            System.out.println("\n\n" + command + "\n\n");

            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("cmd", "/c", command.toString());
            File compiler = new File(System.getProperty("user.dir") + "/zig-windows-x86_64-0.11.0");
            if (!compiler.exists()) {
                logger.error("Unable to locate compiler");
            }
            processBuilder.directory(compiler);

            Process process = processBuilder.start();

            Thread outputThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (Main.debug) {
                            System.out.println(line);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            Thread errorThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (Main.debug) {
                            System.err.println(line); // Print errors to standard error
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            outputThread.start();
            errorThread.start();

            int exitVal = process.waitFor();

            outputThread.join();
            errorThread.join();

            if (exitVal == 0) {
                logger.info("Compilation success.");
                Protect.protect(cppDir, outputDir);
            } else {
                logger.error("Compilation failed. Please contact admin!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}