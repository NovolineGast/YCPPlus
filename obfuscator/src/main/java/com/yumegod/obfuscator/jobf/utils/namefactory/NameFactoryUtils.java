package com.yumegod.obfuscator.jobf.utils.namefactory;

import com.yumegod.obfuscator.jobf.utils.namefactory.impl.*;
import com.yumegod.obfuscator.utils.cfg.annotations.ConfigSection;
import com.yumegod.obfuscator.utils.cfg.annotations.StaticConfigReceiver;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

@StaticConfigReceiver
public class NameFactoryUtils {
    public static final HashMap<String, NameFactory> packageNFs = new HashMap<>();
    @ConfigSection("rename.prefix")
    public static String prefix = "";
    @ConfigSection("rename.suffix")
    public static String suffix = "";
    @ConfigSection("rename.dictionary.package")
    public static String packageDictionary = "%mixed_case";
    @ConfigSection("rename.dictionary.class")
    public static String classDictionary = "%mixed_case";
    @ConfigSection("rename.dictionary.member")
    public static String memberDictionary = "%mixed_case";

    @ConfigSection("rename.aggressive_overload")
    public static boolean aggressiveOverload = true;
    private static NameFactory memberNameFactoryCache;


    public static NameFactory generate(String dictionary) {
        NameFactory factory;
        if (dictionary.startsWith("%")) {
            switch (dictionary.toLowerCase()) {
                case "%mixed_case":
                    factory = new MixedCaseNameFactory();
                    break;
                case "%il":
                    factory = new IlNameFactory();
                    break;
                case "%o0":
                    factory = new Oo0NameFactory();
                    break;
                default:
                    factory = new SimpleNameFactory();
            }
        } else {
            try {
                factory = new CustomNameFactory(new URL(dictionary));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (prefix != null && !prefix.isEmpty()) {
            factory = new PrefixNameFactory(prefix, factory);
        }
        if (suffix != null && !suffix.isEmpty()) {
            factory = new SuffixNameFactory(suffix, factory);
        }
        return factory;
    }

    public static NameFactory getMemberNameFactory() {
        if (memberNameFactoryCache == null) memberNameFactoryCache = generate(memberDictionary);
        return memberNameFactoryCache;
    }

    public static String simpleNextMemberName(ClassNode node, String desc) {
        String formattedDesc = formattedDesc(desc);
        Set<String> names = node.methods.stream().filter(m -> formattedDesc(m.desc).equals(formattedDesc)).map(m -> m.name).collect(Collectors.toSet());
        names.addAll(node.fields.stream().filter(f -> formattedDesc(f.desc).equals(formattedDesc)).map(f -> f.name).collect(Collectors.toSet()));
        NameFactory nf = getMemberNameFactory();
        nf.reset();
        String result;
        do {
            result = nf.nextName();
        } while (names.contains(result));
        return result;
    }

    public static String formattedDesc(String desc) {
        if (aggressiveOverload) {
            return desc;
        } else {
            return desc.substring(0, desc.lastIndexOf(')') + 1);
        }
    }

    public static NameFactory getNameFactoryByPackage(String pkg) {
        return packageNFs.computeIfAbsent(pkg, k -> generate(classDictionary));
    }
}
