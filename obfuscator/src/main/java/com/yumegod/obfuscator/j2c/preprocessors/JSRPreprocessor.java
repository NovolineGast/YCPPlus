package com.yumegod.obfuscator.j2c.preprocessors;

import com.yumegod.obfuscator.enums.Platform;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class JSRPreprocessor implements Preprocessor{
    @Override
    public void process(ClassNode classNode, MethodNode methodNode, Platform platform) {
        JSRInlinerAdapter adapter = new JSRInlinerAdapter(methodNode, methodNode.access, methodNode.name, methodNode.desc, methodNode.signature, methodNode.exceptions.toArray(new String[0]));
        methodNode.accept(adapter);

    }
}
