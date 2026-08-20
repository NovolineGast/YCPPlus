package com.yumegod.obfuscator.utils.cfg.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigSection {
    /**
     * support type: String, int, long, double, boolean and Enum
     * @return The config section path
     */
    String value();
}
