package com.yumegod.obfuscator.jobf.transformer;

import com.yumegod.obfuscator.jobf.utils.ASMUtil;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicVerifier;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public abstract class SingledClassTransformer extends Transformer {
    public abstract void process(ClassNode node);

    @Override
    public void process(Map<String, ClassNode> classes) {
        classes.values().forEach(this::process);
    }

    protected static final boolean DEBUG = false;

    protected static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SingledClassTransformer.class);

    protected static boolean computeMax(MethodNode node, String name) {
        try {
            new Analyzer<>(DEBUG ? new BasicVerifier() : new BasicInterpreter()).analyzeAndComputeMaxs(name, node);
        } catch (Exception e) {
            if (!DEBUG) {
                logger.error("Failed to compute max local/stack in {} ({})", name, e.toString());
                return false;
            }
            logger.error("Error in {}", name, e);
            String string1 = readableASM(node);
            AtomicInteger lineNum = new AtomicInteger(0);
            String string = Arrays.stream(string1.trim().split("\n")).map(it -> lineNum.incrementAndGet() + " | " + it).collect(Collectors.joining("\n"));
            logger.warn(string);
            return false;
        }
        return true;
    }

    protected static String readableASM(MethodNode node) {
        return ASMUtil.readableASM(node);
    }


}
