package com.yumegod.obfuscator.j2c;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NativeProtection {
    public static String checkNativeMethods(String cppSource, String fileName) {
        String fileClassName = fileName.substring(0, fileName.length() - 6).replace('_', '/');
        ArrayList<String>[] methodsToCheck = new ArrayList[]{new ArrayList<String>(), new ArrayList<String>(), new ArrayList<String>(), new ArrayList<String>()};
        for (int i = 0; i < MethodProcessor.methodsToCheck[0].size(); i++) {
            if (MethodProcessor.methodsToCheck[0].get(i).equals(fileClassName)) {
                methodsToCheck[0].add(MethodProcessor.methodsToCheck[0].get(i));
                methodsToCheck[1].add(MethodProcessor.methodsToCheck[1].get(i));
                methodsToCheck[2].add(MethodProcessor.methodsToCheck[2].get(i));
                methodsToCheck[3].add(MethodProcessor.methodsToCheck[3].get(i));
            }
        }
        int size = methodsToCheck[0].size();
        StringBuilder sb = new StringBuilder();
        sb.append("        int size = " + size + ";").append("\n");
        sb.append("        int passCount = 0;").append("\n");
        sb.append("        std::string methodList[" + size + "][4] = {").append("\n");
        for (int i = 0; i < size; i++) {
            String className = methodsToCheck[0].get(i);
            String methodName = methodsToCheck[1].get(i);
            String methodDesc = methodsToCheck[2].get(i);
            String isStatic = methodsToCheck[3].get(i);
            sb.append("            {VMProtectDecryptStringA(\"" + className + "\"), VMProtectDecryptStringA(\"" + methodName + "\"), VMProtectDecryptStringA(\"" + methodDesc + "\"), VMProtectDecryptStringA(\"" + isStatic + "\")},").append("\n");
        }
        sb.append("        };").append("\n");
        sb.append("        for (int i = 0; i < size; i++)\n" +
                "        {\n" +
                "            std::string method[] = {methodList[i][0], methodList[i][1], methodList[i][2], methodList[i][3]};\n" +
                "            jclass targetClass = env->FindClass(method[0].c_str());\n" +
                "            if (targetClass == nullptr)\n" +
                "            {\n" +
                "                volatile int *p = (int *)NULL;\n" +
                "                *p = 0;\n" +
                "            }\n" +
                "            jmethodID methodID;\n" +
                "            if (method[3] == VMProtectDecryptStringA(\"true\"))\n" +
                "            {\n" +
                "                methodID = env->GetStaticMethodID(targetClass, method[1].c_str(), method[2].c_str());\n" +
                "            }\n" +
                "            else\n" +
                "            {\n" +
                "                methodID = env->GetMethodID(targetClass, method[1].c_str(), method[2].c_str());\n" +
                "            }\n" +
                "            if (methodID == nullptr)\n" +
                "            {\n" +
                "                volatile int *p = (int *)NULL;\n" +
                "                *p = 0;\n" +
                "            }\n" +
                "            std::vector<jclass> paramTypes;\n" +
                "            int idx = 1;\n" +
                "            while (method[2][idx] != ')')\n" +
                "            {\n" +
                "                std::string paramTypeStr;\n" +
                "                if (method[2][idx] == '[')\n" +
                "                {\n" +
                "                    while (method[2][idx] == '[')\n" +
                "                    {\n" +
                "                        paramTypeStr += method[2][idx++];\n" +
                "                    }\n" +
                "                }\n" +
                "                if (method[2][idx] == 'L')\n" +
                "                {\n" +
                "                    while (method[2][idx] != ';')\n" +
                "                    {\n" +
                "                        paramTypeStr += method[2][idx++];\n" +
                "                    }\n" +
                "                    paramTypeStr += ';';\n" +
                "                    idx++;\n" +
                "                }\n" +
                "                else\n" +
                "                {\n" +
                "                    paramTypeStr += method[2][idx++];\n" +
                "                    jclass paramClass = nullptr;\n" +
                "                    switch (paramTypeStr.back())\n" +
                "                    {\n" +
                "                    case 'I':\n" +
                "                        paramClass = env->FindClass(VMProtectDecryptStringA(\"java/lang/Integer\"));\n" +
                "                        break;\n" +
                "                    case 'B':\n" +
                "                        paramClass = env->FindClass(VMProtectDecryptStringA(\"java/lang/Byte\"));\n" +
                "                        break;\n" +
                "                    case 'C':\n" +
                "                        paramClass = env->FindClass(VMProtectDecryptStringA(\"java/lang/Character\"));\n" +
                "                        break;\n" +
                "                    case 'D':\n" +
                "                        paramClass = env->FindClass(VMProtectDecryptStringA(\"java/lang/Double\"));\n" +
                "                        break;\n" +
                "                    case 'F':\n" +
                "                        paramClass = env->FindClass(VMProtectDecryptStringA(\"java/lang/Float\"));\n" +
                "                        break;\n" +
                "                    case 'J':\n" +
                "                        paramClass = env->FindClass(VMProtectDecryptStringA(\"java/lang/Long\"));\n" +
                "                        break;\n" +
                "                    case 'S':\n" +
                "                        paramClass = env->FindClass(VMProtectDecryptStringA(\"java/lang/Short\"));\n" +
                "                        break;\n" +
                "                    case 'Z':\n" +
                "                        paramClass = env->FindClass(VMProtectDecryptStringA(\"java/lang/Boolean\"));\n" +
                "                        break;\n" +
                "                    case 'V':\n" +
                "                        paramClass = env->FindClass(VMProtectDecryptStringA(\"java/lang/Void\"));\n" +
                "                        break;\n" +
                "                    default:\n" +
                "                        volatile int *p = (int *)NULL;\n" +
                "                        *p = 0;\n" +
                "                    }\n" +
                "                    if (paramClass == nullptr)\n" +
                "                    {\n" +
                "                        volatile int *p = (int *)NULL;\n" +
                "                        *p = 0;\n" +
                "                    }\n" +
                "                    paramTypes.push_back(paramClass);\n" +
                "                }\n" +
                "            }\n" +
                "            jclass *paramArray = new jclass[paramTypes.size()];\n" +
                "            std::copy(paramTypes.begin(), paramTypes.end(), paramArray);\n" +
                "            jclass methodClass = env->FindClass(VMProtectDecryptStringA(\"java/lang/reflect/Method\"));\n" +
                "            jmethodID getModifiersMethod = env->GetMethodID(methodClass, VMProtectDecryptStringA(\"getModifiers\"), VMProtectDecryptStringA(\"()I\"));\n" +
                "            jobject methodObject = env->ToReflectedMethod(targetClass, methodID, JNI_FALSE);\n" +
                "            jint modifiers = env->CallIntMethod(methodObject, getModifiersMethod);\n" +
                "            delete[] paramArray;\n" +
                "            if ((modifiers & 0x100) != 0)\n" +
                "            {\n" +
                "                passCount++;\n" +
                "            }\n" +
                "            else\n" +
                "            {\n" +
                "                volatile int *p = (int *)NULL;\n" +
                "                *p = 0;\n" +
                "            }\n" +
                "        }");

        String regex = "(void ProtectedByYumeCloud_register_methods\\(JNIEnv \\*env, jclass clazz\\) \\{\\s*VMProtectBeginUltra\\(\"[^\"]*\"\\);)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(cppSource);

        StringBuilder newSourceCode = new StringBuilder(cppSource);
        if (matcher.find()) {
            int insertionPoint = matcher.end();
            newSourceCode.insert(insertionPoint, "\n" + sb);
        }

        return newSourceCode.toString();
    }
}