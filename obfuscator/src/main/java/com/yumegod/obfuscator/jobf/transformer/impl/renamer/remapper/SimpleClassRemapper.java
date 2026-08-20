package com.yumegod.obfuscator.jobf.transformer.impl.renamer.remapper;

import org.objectweb.asm.commons.Remapper;

import java.util.HashMap;
import java.util.Map;

public class SimpleClassRemapper extends Remapper {
    private final Map<String, String> map = new HashMap<>();

    public SimpleClassRemapper(Map<String, String> map) {
        this.map.putAll(map);
    }

    @Override
    public String map(String typeName) {
        String remappedName = map.get(typeName);
        return (remappedName != null) ? remappedName : typeName;
    }
}
