package com.yumegod.obfuscator.j2c.preprocessors;

import com.yumegod.obfuscator.enums.Platform;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;

public class PreprocessorRunner {

    private final static List<Preprocessor> PREPROCESSORS = new ArrayList<>();

    static {
        PREPROCESSORS.add(new IndyPreprocessor());
        PREPROCESSORS.add(new LdcPreprocessor());
    }

    public static void preprocess(ClassNode classNode, MethodNode methodNode, Platform platform) {
        for (Preprocessor preprocessor : PREPROCESSORS) {
            preprocessor.process(classNode, methodNode, platform);
        }
    }
}
