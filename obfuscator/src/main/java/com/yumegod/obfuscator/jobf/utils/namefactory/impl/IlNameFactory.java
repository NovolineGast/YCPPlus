package com.yumegod.obfuscator.jobf.utils.namefactory.impl;

import com.yumegod.obfuscator.jobf.utils.namefactory.NameFactory;

public class IlNameFactory implements NameFactory {

    @Override
    public String nextName() {
        int i;
        do {
            i = Math.abs((int) ((System.currentTimeMillis() ^ System.nanoTime()) % 0xFFFFFFFFL));
        } while (i == 0);
        StringBuilder builder = new StringBuilder();
        while (i > 0) {
            builder.append(i % 2 == 0 ? 'I' : 'l');
            i /= 2;
        }
        return builder.toString();
    }

    @Override
    public void reset() {}
}
