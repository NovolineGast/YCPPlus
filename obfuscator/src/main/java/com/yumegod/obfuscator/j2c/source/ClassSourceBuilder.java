package com.yumegod.obfuscator.j2c.source;

import com.yumegod.obfuscator.YumeCloudProtection;
import com.yumegod.obfuscator.j2c.HiddenCppMethod;
import com.yumegod.obfuscator.Main;
import com.yumegod.obfuscator.j2c.caches.NodeCache;
import com.yumegod.obfuscator.utils.Util;
import org.objectweb.asm.tree.ClassNode;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassSourceBuilder implements AutoCloseable {

    private final Path cppFile;
    private final Path hppFile;
    private final BufferedWriter cppWriter;
    private final BufferedWriter hppWriter;
    private final String className;
    private final String filename;
    private static int index = 0;

    public ClassSourceBuilder(Path cppOutputDir, String className, int classIndex) throws IOException {
        this.className = className;
        filename = String.format("%s_%d", Util.escapeCppNameString(className.replace('/', '_')), classIndex);

        cppFile = cppOutputDir.resolve(filename.concat(".cpp"));
        hppFile = cppOutputDir.resolve(filename.concat(".hpp"));
        cppWriter = Files.newBufferedWriter(cppFile, StandardCharsets.UTF_8);
        hppWriter = Files.newBufferedWriter(hppFile, StandardCharsets.UTF_8);
    }

    public void addHeader(int strings, int classes, int methods, int fields) throws IOException {
        cppWriter.append("#include \"../native_jvm.hpp\"\n");
        cppWriter.append("#include \"../VMProtectSDK.h\"\n");
        cppWriter.append("#include <string>\n");
        cppWriter.append("#include \"").append(getHppFilename()).append("\"\n");
        cppWriter.append("\n");
        cppWriter.append("// ").append(Util.escapeCommentString(className)).append("\n");
        cppWriter.append("namespace native_jvm::classes::ProtectedByYumeCloud_").append(filename).append(" {\n\n");

        if (strings > 0) {
            cppWriter.append(String.format("    jstring cstrings[%d];\n", strings));
        }
        if (classes > 0) {
            cppWriter.append(String.format("    std::mutex cclasses_mtx[%d];\n", classes));
            cppWriter.append(String.format("    jclass cclasses[%d];\n", classes));
        }
        if (methods > 0) {
            cppWriter.append(String.format("    jmethodID cmethods[%d];\n", methods));
        }
        if (fields > 0) {
            cppWriter.append(String.format("    jfieldID cfields[%d];\n", fields));
        }

        cppWriter.append("\n");
        cppWriter.append("    ");


        hppWriter.append("#include \"../native_jvm.hpp\"\n");
        hppWriter.append("\n");
        hppWriter.append("#ifndef ").append(filename.concat("_hpp").toUpperCase()).append("_GUARD\n");
        hppWriter.append("\n");
        hppWriter.append("#define ").append(filename.concat("_hpp").toUpperCase()).append("_GUARD\n");
        hppWriter.append("\n");
        hppWriter.append("// ").append(Util.escapeCommentString(className)).append("\n");
        hppWriter.append("namespace native_jvm::classes::ProtectedByYumeCloud_")
                .append(filename)
                .append(" {\n\n");
    }

    public void addInstructions(String instructions) throws IOException {
        cppWriter.append(instructions);
        cppWriter.append("\n");
    }

    public void registerMethods(NodeCache<String> strings, NodeCache<String> classes, String nativeMethods, List<HiddenCppMethod> hiddenMethods) throws IOException {
        index++;
        cppWriter.append("    void ProtectedByYumeCloud_register_methods(JNIEnv *env, jclass clazz) {\n");
        cppWriter.append("        VMProtectBeginUltra(\"").append(className).append("_NativeRegister_YumeCloudProtection_").append(String.valueOf(index)).append("\");").append("\n");
        cppWriter.append("        if (passCount == size) {").append("\n");
        for (Map.Entry<String, Integer> string : strings.getCache().entrySet()) {
            cppWriter.append("                if (jstring str = env->NewStringUTF(VMProtectDecryptStringA(\"").append(Util.escapeString(string.getKey())).append("\"))) { if (jstring int_str = utils::get_interned(env, str)) { ")
                    .append(String.format("cstrings[%d] = ", string.getValue()))
                    .append("(jstring) env->NewGlobalRef(int_str); env->DeleteLocalRef(str); env->DeleteLocalRef(int_str); } }\n");
        }

        if (!classes.isEmpty()) {
            cppWriter.append("\n");
        }

        if (!nativeMethods.isEmpty()) {
            cppWriter.append("                JNINativeMethod ProtectedByYumeCloud_methods[] = {\n");
            cppWriter.append(nativeMethods);
            cppWriter.append("                };\n\n");
            cppWriter.append("                if (clazz) env->RegisterNatives(clazz, ProtectedByYumeCloud_methods, sizeof(ProtectedByYumeCloud_methods) / sizeof(ProtectedByYumeCloud_methods[0]));\n");
            cppWriter.append("                if (env->ExceptionCheck()) {return; }");
            cppWriter.append("\n");
        }

        if (!hiddenMethods.isEmpty()) {
            HashMap<ClassNode, List<HiddenCppMethod>> sortedHiddenMethods = new HashMap<>();
            for (HiddenCppMethod method : hiddenMethods) {
                sortedHiddenMethods.computeIfAbsent(method.getHiddenMethod().getClassNode(), unused -> new ArrayList<>()).add(method);
            }

            for (ClassNode hiddenClazz : sortedHiddenMethods.keySet()) {
                cppWriter.append("                {\n");
                cppWriter.append("                    jclass hidden_class = env->FindClass(").append("VMProtectDecryptStringA(\"" + hiddenClazz.name + "\")").append(");\n");
                cppWriter.append("                    JNINativeMethod ProtectedByYumeCloud_hidden_methods[] = {\n");
                for (HiddenCppMethod method : sortedHiddenMethods.get(hiddenClazz)) {
                    cppWriter.append(String.format("                        { %s, %s, (void *)&%s },\n",
                            "const_cast<char*>(VMProtectDecryptStringA(\"" + method.getHiddenMethod().getMethodNode().name + "\"))",
                            "const_cast<char*>(VMProtectDecryptStringA(\"" + method.getHiddenMethod().getMethodNode().desc + "\"))",
                            method.getCppName()));
                }
                cppWriter.append("                    };\n");
                cppWriter.append("                    if (hidden_class) env->RegisterNatives(hidden_class, ProtectedByYumeCloud_hidden_methods, sizeof(ProtectedByYumeCloud_hidden_methods) / sizeof(ProtectedByYumeCloud_hidden_methods[0]));\n");
                cppWriter.append("                    if (env->ExceptionCheck()) {return; }");
                cppWriter.append(" env->DeleteLocalRef(hidden_class);\n");
                cppWriter.append("                }\n");

            }
        }
        if (YumeCloudProtection.auth) {
            cppWriter.append("            } else {").append("\n");
            cppWriter.append("                void (*func_ptr)() = (void (*)())0x1;func_ptr();").append("\n");
            cppWriter.append("            }").append("\n");
            cppWriter.append("        } else {").append("\n");
            cppWriter.append("            void (*func_ptr)() = (void (*)())0x1;func_ptr();").append("\n");
            cppWriter.append("        }").append("\n");
        }

        cppWriter.append("        } else {").append("\n");
        cppWriter.append("            void (*func_ptr)() = (void (*)())0x1;func_ptr();").append("\n");
        cppWriter.append("        }").append("\n");
        cppWriter.append("        VMProtectEnd();").append("\n");
        cppWriter.append("    }\n");
        cppWriter.append("}");

        hppWriter.append("    void ProtectedByYumeCloud_register_methods(JNIEnv *env, jclass clazz);\n");
        hppWriter.append("}\n\n#endif");
    }

    public String getFilename() {
        return filename;
    }

    public String getHppFilename() {
        return hppFile.getFileName().toString();
    }

    public String getCppFilename() {
        return cppFile.getFileName().toString();
    }

    @Override
    public void close() throws IOException {
        try {
            cppWriter.close();
        } finally {
            hppWriter.close();
        }
    }
}
