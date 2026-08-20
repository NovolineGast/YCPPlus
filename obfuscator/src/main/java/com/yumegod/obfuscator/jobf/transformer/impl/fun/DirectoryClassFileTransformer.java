package com.yumegod.obfuscator.jobf.transformer.impl.fun;

import com.yumegod.obfuscator.jobf.transformer.Transformer;
import com.yumegod.obfuscator.Main;
import com.yumegod.obfuscator.utils.cfg.annotations.ConfigSection;
import com.yumegod.obfuscator.utils.cfg.annotations.StaticConfigReceiver;
import org.objectweb.asm.tree.ClassNode;

import java.util.HashSet;
import java.util.Map;

@StaticConfigReceiver
public class DirectoryClassFileTransformer extends Transformer {
    @ConfigSection("misc.transform_class_to_directory")
    public static boolean enabled = false;

    @Override
    public void process(Map<String, ClassNode> classes) {
        if (!enabled) return;
        HashSet<Map.Entry<String, ClassNode>> entries = new HashSet<>(classes.entrySet());
        classes.clear();
        for (Map.Entry<String, ClassNode> entry : entries) {
            classes.put(entry.getKey() + "/", entry.getValue());
        }
    }
}
