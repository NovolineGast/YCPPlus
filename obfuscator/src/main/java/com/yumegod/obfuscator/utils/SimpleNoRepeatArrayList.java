package com.yumegod.obfuscator.utils;

import java.util.ArrayList;
import java.util.Collection;

public class SimpleNoRepeatArrayList<T> extends ArrayList<T> {
    @Override
    public boolean add(T t) {
        if (contains(t)) return false;
        return super.add(t);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        return super.addAll(c);
    }

    public SimpleNoRepeatArrayList(Collection<? extends T> c) {
        super(c == null ? new ArrayList<>(0) : c);
    }
}
