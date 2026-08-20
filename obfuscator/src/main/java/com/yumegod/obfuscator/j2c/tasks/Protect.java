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
        logger.info("Start protecting native library...");
        try {
            String protectCommand = "vmp.exe ";

            if (YumeCloudProtection.auth) {
                protectCommand += "YumeCloud_NativeLibrary.vmp";
            } else {
                protectCommand += "YumeCloud_NativeLibrary_NoAuth.vmp";
            }

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