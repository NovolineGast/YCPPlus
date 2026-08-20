package com.yumegod.obfuscator.jobf.utils.namefactory.impl;

import com.yumegod.obfuscator.jobf.utils.namefactory.NameFactory;

import java.util.concurrent.ThreadLocalRandom;

public class Oo0NameFactory implements NameFactory {
    int capability = 0;

    public Oo0NameFactory(int capability) {
        this.capability = capability;
    }

    public Oo0NameFactory() {
        this(ThreadLocalRandom.current().nextInt(7, 20));
    }

    @Override
    public String nextName() {
        int cap = capability;
        char[] chars = new char[cap];
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < cap; i++) {
            char c;
            switch (random.nextInt(3)) {
                case 0: c = 'o';break;
                case 1: c = '0';break;
                default: c = 'O';
            }
            chars[i] = c;
        }
        chars[0] = 'O';
        return new String(chars);
    }

    @Override
    public void reset() {

    }
}
