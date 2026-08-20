package com.yumegod.obfuscation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that disable StringObfuscation
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface NoStringObfuscate {
}