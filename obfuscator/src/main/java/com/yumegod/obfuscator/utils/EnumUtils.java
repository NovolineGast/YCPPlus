package com.yumegod.obfuscator.utils;

import java.util.NoSuchElementException;

public class EnumUtils {
    public static <T extends Enum<T>> T getEnum(Class<T> enumClass, String name) {
        for (T t : enumClass.getEnumConstants()) {
            if (t.name().equalsIgnoreCase(name)) return t;
        }

        throw new NoSuchElementException(name);
    }

    public static Object getEnumObj(Class<?> enumClass, String name) {
        for (Object o : enumClass.getEnumConstants()) {
            if (o.toString().equalsIgnoreCase(name)) return o;
        }

        throw new NoSuchElementException(name);
    }
}
