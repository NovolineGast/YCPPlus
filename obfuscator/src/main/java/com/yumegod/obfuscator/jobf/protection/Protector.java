package com.yumegod.obfuscator.jobf.protection;

import com.yumegod.obfuscator.jobf.protection.impl.AntiDebug;
import com.yumegod.obfuscator.jobf.sdk.SDKInline;
import com.yumegod.obfuscator.jobf.transformer.SingledClassTransformer;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;

public abstract class Protector {
    public abstract void protect(ClassNode node);
//    public abstract void process(Map<String, ClassNode> classes);

    public static final class Companion extends SingledClassTransformer {
        final static Protector[] protectors = {
                new SDKInline(),
                new AntiDebug()
        };

        public void process(ClassNode node) {
            for (Protector protector : Companion.protectors) {
                protector.protect(node);
            }
        }
    }

    protected static String randomUniqueName() {
        int i;
        do {
            i = Math.abs((int) ((System.currentTimeMillis() ^ System.nanoTime()) % 0xFFFFFFFFL));
        } while (i == 0);
        StringBuilder builder = new StringBuilder();
        while (i > 0) {
            builder.append(i % 2 == 0 ? 'I' : 'l');
            i /= 2;
        }
        return builder.toString();
    }

    protected static void computeMax(MethodNode node, String name) {
        try {
            new Analyzer<>(new BasicInterpreter()).analyzeAndComputeMaxs(name, node);
        } catch (AnalyzerException e) {
            throw new RuntimeException(e);
        }
    }
}
