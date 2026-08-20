package com.yumegod.obfuscator.jobf.utils.namefactory.impl;

import com.yumegod.obfuscator.jobf.utils.namefactory.NameFactory;

public class PrefixNameFactory implements NameFactory {
    private final String prefix;
    private final NameFactory delegate;

    public PrefixNameFactory(String prefix, NameFactory delegate) {
        this.prefix = prefix;
        this.delegate = delegate;
    }

    @Override
    public String nextName() {
        return prefix + delegate.nextName();
    }

    @Override
    public void reset() {
        delegate.reset();
    }
}
