package com.yumegod.obfuscator.j2c.source;

import com.yumegod.obfuscator.Main;
import com.yumegod.obfuscator.YumeCloudProtection;
import com.yumegod.obfuscator.utils.Util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

public class MainSourceBuilder {

    private final StringBuilder includes;
    private final StringBuilder registerMethods;

    public MainSourceBuilder() {
        includes = new StringBuilder();
        registerMethods = new StringBuilder();
    }

    public void addHeader(String hppFilename) {
        includes.append(String.format("#include \"output/%s\"\n", hppFilename));
    }

    public void registerClassMethods(int classId, String escapedClassName) {
        registerMethods.append(String.format(
                "        reg_methods[%d] = &(native_jvm::classes::ProtectedByYumeCloud_%s::ProtectedByYumeCloud_register_methods);\n",
                classId, escapedClassName));
    }

    public void registerDefine(String stringPooledClassName, String classFileName) {
        registerMethods.append(String.format(
                "        env->DeleteLocalRef(env->DefineClass(%s, nullptr, native_jvm::data::ProtectedByYumeCloud_%s::get_class_data(), native_jvm::data::ProtectedByYumeCloud_%s::get_class_data_length()));\n",
                stringPooledClassName,
                classFileName,
                classFileName
        ));
    }

    public String build(String nativeDir, int classCount) throws IOException {
        String template = Util.readResource("sources/native_jvm_output.cpp");
        if (YumeCloudProtection.auth) {
            includes.append("#include \"Authorization.h\"");
        }
        StringBuilder dllBytes = new StringBuilder();
        if (YumeCloudProtection.auth) {
            dllBytes.append("unsigned char dllBytes[] = {");
            InputStream dll = MainSourceBuilder.class.getResourceAsStream("/sources/Authorization.dll");
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = dll.read(buffer)) != -1) {
                for (int j = 0; j < bytesRead; j++) {
                    dllBytes.append((int) buffer[j] & 0xFF).append(",");
                }
            }
            dllBytes.append("};").append("\n");
            dllBytes.append("size_t dllSize = sizeof(dllBytes);").append("\n");
        }
        return Util.dynamicFormat(template, Util.createMap(
                "watermark", new String(Base64.getDecoder().decode("ICAgICAgICBzdGQ6OmNvdXQgPDwKICAgICAgICAiXG49PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT1cbiIKICAgICAgICAiIF9fICAgICBfXyAgICAgICAgICAgICAgICAgICAgX19fX18gXyAgICAgICAgICAgICAgICAgXyBcbiIKICAgICAgICAiIFxcIFxcICAgLyAvICAgICAgICAgICAgICAgICAgIC8gX19fX3wgfCAgICAgICAgICAgICAgIHwgfFxuIgogICAgICAgICIgIFxcIFxcXy8gLyAgIF8gXyBfXyBfX18gICBfX198IHwgICAgfCB8IF9fXyAgXyAgIF8gIF9ffCB8XG4iCiAgICAgICAgIiAgIFxcICAgLyB8IHwgfCAnXyBgIF8gXFwgLyBfIFxcIHwgICAgfCB8LyBfIFxcfCB8IHwgfC8gX2AgfFxuIgogICAgICAgICIgICAgfCB8fCB8X3wgfCB8IHwgfCB8IHwgIF9fLyB8X19fX3wgfCAoXykgfCB8X3wgfCAoX3wgfFxuIgogICAgICAgICIgICAgfF98IFxcX18sX3xffCB8X3wgfF98XFxfX198XFxfX19fX3xffFxcX19fLyBcXF9fLF98XFxfXyxfXG4iCiAgICAgICAgIj09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PVxuIgogICAgICAgICJUaGlzIGFwcGxpY2F0aW9uIGlzIHByb3RlY3RlZCBieSBZdW1lQ2xvdWRcbiIKICAgICAgICA8PCBzdGQ6OmVuZGw7")),
                "register_code", registerMethods,
                "auth_library", YumeCloudProtection.auth ? "    native_jvm::utils::init_auth(dllBytes, dllSize);\n" : "",
                "dllBytes", dllBytes.toString(),
                "includes", includes,
                "native_dir", nativeDir,
                "class_count", classCount,
                "authorization", YumeCloudProtection.auth ? "    native_jvm::utils::auth(VMProtectDecryptStringA(\"" + YumeCloudProtection.applicationName + "\"), VMProtectDecryptStringA(\"" + YumeCloudProtection.authorizationURL + "login\"));\n" : ""
        ));
    }
}