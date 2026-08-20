package com.yumegod.obfuscator.loader;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

@SuppressWarnings("unused")
public class Loader {
    public static native void ProtectedByYumeCloud(int index, Class<?> clazz);

    static {
        try {
            String var1 = "YumeCloudProtection/YCVM";
            File var2 = File.createTempFile("YumeCloudProtection_", ".YumeCloudProtection");
            var2.deleteOnExit();
            InputStream var3 = Loader.class.getResourceAsStream("/" + var1);
            if (var3 == null) {
                throw new Exception("YumeCloudProtection dependency not found!");
            }
            Files.copy(var3, var2.toPath(), StandardCopyOption.REPLACE_EXISTING);
            var3.close(); // 好习惯
            System.load(var2.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}