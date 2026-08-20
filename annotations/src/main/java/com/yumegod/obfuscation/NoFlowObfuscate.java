package com.yumegod.obfuscation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that disable FlowObfuscation
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface NoFlowObfuscate {
}