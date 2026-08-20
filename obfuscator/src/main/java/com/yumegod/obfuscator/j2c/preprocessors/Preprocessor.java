package com.yumegod.obfuscator.j2c.preprocessors;

import com.yumegod.obfuscator.enums.Platform;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public interface Preprocessor {

    void process(ClassNode classNode, MethodNode methodNode, Platform platform);
}
