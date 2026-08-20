package com.yumegod.obfuscator.jobf.transformer.impl.fun;

import com.yumegod.obfuscator.jobf.transformer.SingledClassTransformer;
import org.objectweb.asm.tree.ClassNode;

/**
 * @ Author: nuym
 * @ Date: 2024/5/4
 * @ Time: 13:25
 */
public class InnerClassRemover extends SingledClassTransformer {
    @Override
    public void process(ClassNode node) {
        node.innerClasses.clear();
        node.outerClass = null;
        node.outerMethod = null;
        node.outerMethodDesc = null;
    }
}
