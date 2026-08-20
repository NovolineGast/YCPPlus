package com.yumegod.obfuscator.jobf.utils.namefactory.impl;

import com.yumegod.obfuscator.jobf.utils.namefactory.NameFactory;

public class SuffixNameFactory implements NameFactory {
    private final String suffix;
    private final NameFactory delegate;

    public SuffixNameFactory(String suffix, NameFactory delegate) {
        this.suffix = suffix;
        this.delegate = delegate;
    }
    @Override
    public String nextName() {
        return delegate.nextName() + suffix;
    }

    @Override
    public void reset() {
        delegate.reset();
    }
}
