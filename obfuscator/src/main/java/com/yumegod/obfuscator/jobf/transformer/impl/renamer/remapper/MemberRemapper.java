package com.yumegod.obfuscator.jobf.transformer.impl.renamer.remapper;

import com.yumegod.obfuscator.jobf.utils.namefactory.NameFactoryUtils;
import org.objectweb.asm.commons.SimpleRemapper;

import java.util.HashMap;
import java.util.Map;

public class MemberRemapper extends SimpleRemapper {
    private final Map<String, Map<String, Map<String, String>>> mappings;

    public MemberRemapper(final Map<String, Map<String, Map<String, String>>> mappings) {
        super(new HashMap<>(0));
        this.mappings = mappings;
    }

    @Override
    public String mapFieldName(String owner, String name, String desc) {
        Map<String, Map<String, String>> map = mappings.get(owner);
        if (map == null) return name;
        Map<String, String> stringMap = map.get(NameFactoryUtils.formattedDesc(desc));
        if (stringMap == null) return name;
        String remappedName = stringMap.get(name);
        return (remappedName != null) ? remappedName : name;
    }

    @Override
    public String mapMethodName(String owner, String name, String desc) {
        Map<String, Map<String, String>> map = mappings.get(owner);
        if (map == null) return name;
        Map<String, String> stringMap = map.get(NameFactoryUtils.formattedDesc(desc));
        if (stringMap == null) return name;
        String remappedName = stringMap.get(name);
        return (remappedName != null) ? remappedName : name;
    }
}
