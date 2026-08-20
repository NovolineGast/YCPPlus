package com.yumegod.obfuscator.utils.filter;

// why not atomicInteger? idk.
class IntContainer {
    int value;
    IntContainer(int value) {
        this.value = value;
    }
    int get() {
        return value;
    }
    void set(int value) {
        this.value = value;
    }
    void inc() {
        value++;
    }
    void dec() {
        value--;
    }
    int getAndInc() {
        return value++;
    }
    int getAndDec() {
        return value--;
    }
    int incAndGet() {
        return ++value;
    }

    boolean lt(int other) {
        return value < other;
    }
    boolean gt(int other) {
        return value > other;
    }
    boolean le(int other) {
        return value <= other;
    }
    boolean ge(int other) {
        return value >= other;
    }
    boolean eq(int other) {
        return value == other;
    }
}
