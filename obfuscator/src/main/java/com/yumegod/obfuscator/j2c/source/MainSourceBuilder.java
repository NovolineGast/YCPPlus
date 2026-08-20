package com.yumegod.obfuscator.j2c.source;

import com.yumegod.obfuscator.YumeCloudProtection;
import com.yumegod.obfuscator.utils.Util;

import java.io.IOException;
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
        // 授权客户端已内置于 native_jvm.cpp（开放实现），验证失败时返回 JNI_ERR 终止加载
        String authorization = "";
        if (YumeCloudProtection.auth) {
            authorization = "    if (!native_jvm::utils::auth(VMProtectDecryptStringA(\"" + YumeCloudProtection.applicationName
                    + "\"), VMProtectDecryptStringA(\"" + YumeCloudProtection.authorizationURL + "login\"))) return JNI_ERR;\n";
        }
        return Util.dynamicFormat(template, Util.createMap(
                "watermark", new String(Base64.getDecoder().decode("ICAgICAgICBzdGQ6OmNvdXQgPDwKICAgICAgICAiXG49PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT1cbiIKICAgICAgICAiIF9fICAgICBfXyAgICAgICAgICAgICAgICAgICAgX19fX18gXyAgICAgICAgICAgICAgICAgXyBcbiIKICAgICAgICAiIFxcIFxcICAgLyAvICAgICAgICAgICAgICAgICAgIC8gX19fX3wgfCAgICAgICAgICAgICAgIHwgfFxuIgogICAgICAgICIgIFxcIFxcXy8gLyAgIF8gXyBfXyBfX8KgICAgX19fX3wgfCAgICB8IHwgX19fICBfICAgXyAgXyAgXyAgXyAgX3wgfFwuciIKICAgICAgICAiICAgXFwgICAvIHwgfCAnXyBgIF8gXFwgLyBfIFxcIHwgICAgfCB8LyBfIFxcfCB8IHwgfC8gX2AgfFxuIgogICAgICAgICIgICB8IHx8IHx3fCB8IHwgfCB8IHwgIF9fLyB8X19fX3wgfCAoXykgfCB8X3wgfCAoX3wgfFxuIgogICAgICAgICIgICB8X3wgXFxfXyxffCB8X3wgfF98XFxfX198XFxfX19fX3xffFxcX19fLyBcXF9fLF98XFxfXyxfXG4iCiAgICAgICAgIj09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PVxuIgogICAgICAgICJUaGlzIGFwcGxpY2F0aW9uIGlzIHByb3RlY3RlZCBieSBZdW1lQ2xvdWRcbiIKICAgICAgICA8PCBzdGQ6OmVuZGw7")),
                "register_code", registerMethods,
                "includes", includes,
                "native_dir", nativeDir,
                "class_count", classCount,
                "authorization", authorization
        ));
    }
}
