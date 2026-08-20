package com.yumegod.obfuscator.j2c.tasks;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class SetupCompiler {
    private static final Logger logger = LoggerFactory.getLogger(SetupCompiler.class);
    private static String OS;

    public static void initializeCompiler() {
        OS = System.getProperty("os.name").toLowerCase();
        String platformTypeName = getPlatformTypeName();
        String fileName;
        String dirName;
        if (platformTypeName != null && !platformTypeName.isEmpty()) {
            if (isLinux()) {
                fileName = "zig-linux-" + platformTypeName + "-0.11.0.tar.xz";
                dirName = "zig-linux-" + platformTypeName + "-0.11.0";
            } else if (isMacOS()) {
                fileName = "zig-macos-" + platformTypeName + "-0.11.0.tar.xz";
                dirName = "zig-macos-" + platformTypeName + "-0.11.0";
            } else if (isWindows()) {
                fileName = "zig-windows-" + platformTypeName + "-0.11.0.zip";
                dirName = "zig-windows-" + platformTypeName + "-0.11.0";
            } else {
                throw new RuntimeException("Unsupported system");
            }
            loadCompiler(fileName, dirName);
        }
    }

    private static String getPlatformTypeName() {
        final String lowerCase;
        lowerCase = System.getProperty("os.arch").toLowerCase();
        String platformTypeName;
        switch (lowerCase) {
            case "x86_64":
            case "amd64": {
                platformTypeName = "x86_64";
                break;
            }
            case "aarch64": {
                platformTypeName = "aarch64";
                break;
            }
            case "x86": {
                platformTypeName = "i386";
                break;
            }
            default: {
                platformTypeName = "";
                break;
            }
        }
        return platformTypeName;
    }

    public static boolean isLinux() {
        return SetupCompiler.OS.contains("linux");
    }

    public static boolean isMacOS() {
        return SetupCompiler.OS.contains("mac") && SetupCompiler.OS.indexOf("os") > 0;
    }

    public static boolean isWindows() {
        return SetupCompiler.OS.contains("windows");
    }

    public static void loadCompiler(final String fileName, final String dirName) {
        Path dirPath = Paths.get(dirName);
        if (Files.exists(dirPath)) {
            logger.info("Existing compiler found, using " + dirPath.toAbsolutePath());
            return;
        }

        try {
            final String currentDir = System.getProperty("user.dir");
            logger.info("No existing compiler found, start downloading compiler...");
            logger.info("Downloading: https://ziglang.org/download/0.11.0/" + fileName);
            final InputStream in = new URL("https://ziglang.org/download/0.11.0/" + fileName).openStream();
            Files.copy(in, Paths.get(currentDir + File.separator + fileName), StandardCopyOption.REPLACE_EXISTING);
            logger.info("Download success, decompressing...");
            unzipFile(currentDir, fileName, currentDir);
            logger.info("Successfully installed compiler.");
            Files.delete(Paths.get(fileName));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void deleteFile(final String path, final String file) {
        new File(path + File.separator + file);
    }

    public static void unzipFile(final String path, final String file, final String destination) {
        try {
            extract(Paths.get(path + File.separator + file), Paths.get(destination));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void extract(final Path archive, final Path target) throws IOException {
        final String name = archive.toFile().getName();
        if (name.contains("zip")) {
            unzip(archive, target);
        } else if (name.contains("tar.xz")) {
            unTarXZ(archive, target);
        } else if (name.contains("tar")) {
            unTar(archive, target);
        } else {
            throw new RuntimeException("Unsupported file type");
        }
    }

    public static void unzip(final Path zip, final Path target) throws IOException {
        try (final ZipInputStream zin = new ZipInputStream(new BufferedInputStream(Files.newInputStream(zip)))) {
            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                extractEntry(target, zin, entry.getName(), entry.isDirectory());
            }
        }
    }

    public static void unTar(final Path tar, final Path target) throws IOException {
        try (final TarArchiveInputStream tin = new TarArchiveInputStream(new BufferedInputStream(Files.newInputStream(tar)))) {
            TarArchiveEntry entry;
            while ((entry = tin.getNextEntry()) != null) {
                extractEntry(target, tin, entry.getName(), entry.isDirectory());
            }
        }
    }

    public static void unTarXZ(final Path tar, final Path target) throws IOException {
        try (final XZCompressorInputStream xzcis = new XZCompressorInputStream(new BufferedInputStream(Files.newInputStream(tar)));
             final TarArchiveInputStream tin = new TarArchiveInputStream(xzcis, 1024)) {
            TarArchiveEntry entry;
            while ((entry = tin.getNextEntry()) != null) {
                extractEntry(target, tin, entry.getName(), entry.isDirectory());
            }
        }
    }

    private static void extractEntry(final Path target, final InputStream in, final String entryName, final boolean isDirectory) throws IOException {
        final Path entryPath = target.resolve(entryName);
        if (isDirectory) {
            Files.createDirectories(entryPath);
        } else {
            final Path dir = entryPath.getParent();
            Files.createDirectories(dir);
            Files.copy(in, entryPath);
        }
    }

    public static void writeToFile(ZipOutputStream outputStream, InputStream inputStream) throws Throwable {
        byte[] buffer = new byte[4096];
        try {
            while (inputStream.available() > 0) {
                int data = inputStream.read(buffer);
                outputStream.write(buffer, 0, data);
            }
        } finally {
            inputStream.close();
            outputStream.closeEntry();
        }
    }
}