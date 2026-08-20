package com.yumegod.obfuscator.jobf.utils;

import static org.objectweb.asm.Opcodes.*;

@SuppressWarnings("unused")
public final class ASMAccess {
    public static boolean isPublic(int access) {
        return (access & ACC_PUBLIC) != 0;
    }

    public static boolean isPrivate(int access) {
        return (access & ACC_PRIVATE) != 0;
    }

    public static boolean isProtected(int access) {
        return (access & ACC_PROTECTED) != 0;
    }

    public static boolean isStatic(int access) {
        return (access & ACC_STATIC) != 0;
    }

    public static boolean isFinal(int access) {
        return (access & ACC_FINAL) != 0;
    }

    public static boolean isSuper(int access) {
        return (access & ACC_SUPER) != 0;
    }

    public static boolean isVolatile(int access) {
        return (access & ACC_VOLATILE) != 0;
    }

    public static boolean isTransient(int access) {
        return (access & ACC_TRANSIENT) != 0;
    }

    public static boolean isStrict(int access) {
        return (access & ACC_STRICT) != 0;
    }

    public static boolean isSynchronized(int access) {
        return (access & ACC_SYNCHRONIZED) != 0;
    }

    public static boolean isAbstract(int access) {
        return (access & ACC_ABSTRACT) != 0;
    }

    public static boolean isInterface(int access) {
        return (access & ACC_INTERFACE) != 0;
    }

    public static boolean isSynthetic(int access) {
        return (access & ACC_SYNTHETIC) != 0;
    }

    public static boolean isEnum(int access) {
        return (access & ACC_ENUM) != 0;
    }

    public static boolean isAnnotation(int access) {
        return (access & ACC_ANNOTATION) != 0;
    }

    public static boolean isBridge(int access) {
        return (access & ACC_BRIDGE) != 0;
    }

    public static boolean isVarargs(int access) {
        return (access & ACC_VARARGS) != 0;
    }

    public static boolean isNative(int access) {
        return (access & ACC_NATIVE) != 0;
    }
}
