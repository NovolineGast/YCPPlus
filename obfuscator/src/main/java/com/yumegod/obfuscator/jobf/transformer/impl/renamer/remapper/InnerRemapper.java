package com.yumegod.obfuscator.jobf.transformer.impl.renamer.remapper;

import org.objectweb.asm.commons.Remapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class InnerRemapper extends Remapper {
    private final Map<String, String> map = new HashMap<>();
    private final Map<String, String> mapReversed = new HashMap<>();
    private final Map<String, String> packageMap = new HashMap<>();
    private final Map<String, Map<String, String>> mapField = new HashMap<>(); //name + desc
    private final Map<String, Map<String, String>> mapMethod = new HashMap<>(); //name + desc

    public String mapMethodName(String owner, String name, String desc) {
        Map<String, String> map = mapMethod.get(map(owner));
        if (map != null) {
            String data = map.get(name + mapDesc(desc));

            if (data != null) {
                return data;
            }
        }
        return name;
    }

    /**
     * Map invokedynamic method name to the new name. Subclasses can override.
     *
     * @param name name of the invokedynamic.
     * @param desc descriptor of the invokedynamic.
     * @return new invokdynamic name.
     */
    public String mapInvokeDynamicMethodName(String name, String desc) {
        return name;
    }

    /**
     * Map field name to the new name. Subclasses can override.
     *
     * @param owner owner of the field.
     * @param name  name of the field
     * @param desc  descriptor of the field
     * @return new name of the field.
     */
    public String mapFieldName(String owner, String name, String desc) {
        Map<String, String> map = mapField.get(map(owner));
        if (map != null) {
            String data = map.get(name + mapDesc(desc));
            if (data != null) {
                return data;
            }
        }
        return name;
    }

    public String map(String in) {
        int lin = in.lastIndexOf('/');
        String className = lin == -1 ? in : in.substring(lin + 1);
        if (lin == -1) {
            return map.getOrDefault(in, in);
        } else {
            String newClassName = map.getOrDefault(in, className);
            int nlin = newClassName.lastIndexOf('/');
            newClassName = nlin == -1 ? newClassName : newClassName.substring(nlin + 1);
            return mapPackage(in.substring(0, lin)) + "/" + newClassName;
        }
    }

    public String mapPackage(String in) {
        int lin = in.lastIndexOf('/');
        if (lin != -1) {
            String originalName = in.substring(lin + 1);
            String parentPackage = in.substring(0, lin);
            String newPackageName = packageMap.getOrDefault(in, originalName);
//            String newPackageName = "obfuscator";
            int nlin = newPackageName.lastIndexOf('/');
            newPackageName = nlin == -1 ? newPackageName : newPackageName.substring(nlin + 1);
            return mapPackage(parentPackage) + "/" + newPackageName;
        } else {
            return packageMap.getOrDefault(in, in);
//            return "obfuscator";
        }
//        return "classes";
    }

    public boolean map(String old, String newName) {
        Objects.requireNonNull(newName);

        if (mapReversed.containsKey(newName)) {
            return false;
        }

        map.put(old, newName);
        mapReversed.put(newName, old);
        return true;
    }
}
