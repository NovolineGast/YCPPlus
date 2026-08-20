package com.yumegod.obfuscator.j2c.tasks;

import com.yumegod.obfuscator.Main;
import com.yumegod.obfuscator.YumeCloudProtection;
import com.yumegod.obfuscator.utils.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @Author: Yume
 * @Date: 2024/3/13 0:53
 */
public class Protect {
    private static final Logger logger = LoggerFactory.getLogger(Protect.class);

    public static void protect(Path cppDir, Path outputDir) {
        String project = YumeCloudProtection.auth ? "YumeCloud_NativeLibrary.vmp" : "YumeCloud_NativeLibrary_NoAuth.vmp";

        // VMProtect 为可选商业组件：vmp.exe 或对应 .vmp 工程缺失时跳过加壳，直接打包未加壳的原生库
        if (!Files.exists(cppDir.resolve("vmp.exe")) || !Files.exists(cppDir.resolve(project))) {
            logger.info("VMProtect not found (vmp.exe or {} missing). Skipping protection, shipping the unprotected native library.", project);
            try {
                Path library = cppDir.resolve("YumeCloud_NativeLibrary.dll");
                if (!Files.exists(library)) library = cppDir.resolve("YumeCloud_NativeLibrary");
                Util.addFileToZip("YumeCloudProtection/YCVM",
                        Files.readAllBytes(library), new File(outputDir.toAbsolutePath().toString()));
            } catch (IOException e) {
                logger.error("Failed to package the unprotected native library.", e);
            }
            return;
        }

        logger.info("Start protecting native library...");
        try {
            String protectCommand = "vmp.exe " + project;

            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("cmd", "/c", protectCommand);
            processBuilder.directory(new File(cppDir.toAbsolutePath().toString()));

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
                logger.info("Protection success.");
                Util.addFileToZip("YumeCloudProtection/YCVM",
                        Files.readAllBytes(Paths.get(cppDir.toAbsolutePath() + "/YumeCloud_NativeLibrary_vmp")), new File(outputDir.toAbsolutePath().toString()));
            } else {
                logger.error("Protection failed. Please contact admin!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}