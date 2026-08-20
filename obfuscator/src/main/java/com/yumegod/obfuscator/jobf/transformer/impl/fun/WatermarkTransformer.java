package com.yumegod.obfuscator.jobf.transformer.impl.fun;

import com.yumegod.obfuscator.jobf.transformer.SingledClassTransformer;
import com.yumegod.obfuscator.Main;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static org.objectweb.asm.Opcodes.BIPUSH;
import static org.objectweb.asm.Opcodes.SIPUSH;

/**
 * @ Author: nuym
 * @ Date: 2024/5/4
 * @ Time: 13:13
 */
public class WatermarkTransformer extends SingledClassTransformer {

    String watermark = "";
    private int inserted = 0;

    @Override
    public void process(ClassNode node) {
        int hash = pjwHash(watermark);

        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (MethodNode method : node.methods) {
            if (random.nextBoolean()) {
                AbstractInsnNode[] nodes = method.instructions.toArray();
                if (nodes.length <= 5) continue;
                AbstractInsnNode ain = nodes[random.nextInt(nodes.length)];
                method.instructions.insertBefore(ain, random.nextBoolean() ? generateIntPush(hash) : new LdcInsnNode(watermark));
                method.instructions.insertBefore(ain, new InsnNode(Opcodes.POP));
                inserted++;
            }
        }

    }

    @Override
    public void process(Map<String, ClassNode> classes) {
        if (watermark.isEmpty()) return;
        super.process(classes);
        logger.info("Inserted {} watermark(s).\nRemember your watermark's hash is {}.", inserted, pjwHash(watermark));
    }

    // Peter Weinberger's PJW hashing algorithm
    private static int pjwHash(String s) {
        int h = 0;
        for (int i = 0; i < s.length(); i++) {
            int c = s.charAt(i);
            h = (h << 4) + c;
            int g = h & 0xf0000000;
            if (g != 0) {
                h ^= g >> 24;
                h &= ~g;
            }
        }
        return h;
    }

    private static AbstractInsnNode generateIntPush(int i) {
        if (i <= 5 && i >= -1) {
            return new InsnNode(i + 3); //iconst_i
        }
        if (i >= -128 && i <= 127) {
            return new IntInsnNode(BIPUSH, i);
        }

        if (i >= -32768 && i <= 32767) {
            return new IntInsnNode(SIPUSH, i);
        }
        return new LdcInsnNode(i);
    }
}
