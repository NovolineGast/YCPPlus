package com.yumegod.obfuscator.jobf.transformer;

import com.yumegod.obfuscator.jobf.protection.Protector;
import com.yumegod.obfuscator.jobf.transformer.impl.*;
import com.yumegod.obfuscator.jobf.transformer.impl.flow.ControlFlowObfuscator;
import com.yumegod.obfuscator.jobf.transformer.impl.flow.v2.FlowObfuscator;
import com.yumegod.obfuscator.jobf.transformer.impl.fun.DeadCodeRemover;
import com.yumegod.obfuscator.jobf.transformer.impl.fun.DirectoryClassFileTransformer;
import com.yumegod.obfuscator.jobf.transformer.impl.fun.WatermarkTransformer;
import com.yumegod.obfuscator.jobf.transformer.impl.reference.InvokeDynamicObfuscator;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;

import java.util.Map;

public abstract class Transformer {
    public abstract void process(Map<String, ClassNode> classes);

    public static void transform(Map<String, ClassNode> classes) {
        for (Transformer transformer : Companion.obfuscator) {
            transformer.process(classes);
//            try {
//                DuplicatedMemberChecker.check(value);
//            } catch (Throwable e) {
//                System.out.println("Duplicated member found in " + value.name + " after " + this.getClass().getSimpleName());
//                e.printStackTrace();
//            }
        }
    }


    static class Companion {
        private static final DuplicatedMemberChecker DUP_CHECK = new DuplicatedMemberChecker();
        public static final Transformer[] obfuscator = new Transformer[]{
                new InitializationTransformer(),
                new Protector.Companion(),
                new StringObfuscator(),
//                new FlowObfuscator(),
                new ControlFlowObfuscator(),
                new InvokeDynamicObfuscator(),
                DUP_CHECK,
                new NumberObfuscator(),
                DUP_CHECK,
                new DebugRemover(),
                DUP_CHECK,
                // TEST
//                new LineNumberRandomizer(), // This is causing some error with mixin
//                new FakeInstructionsTransformer(),
                new DeadCodeRemover(),
                DUP_CHECK,
                new WatermarkTransformer(),
                new DirectoryClassFileTransformer(),
                new AntiTamper()
        };
    }
}
