package com.yumegod.obfuscator.j2c.specials;

import com.yumegod.obfuscator.j2c.MethodContext;

public interface SpecialMethodProcessor {
    String preProcess(MethodContext context);

    void postProcess(MethodContext context);
}
