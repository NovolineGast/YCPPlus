package com.yumegod.obfuscator.jobf.utils;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class MathUtil {
    public static final Random RANDOM = new Random();

    public static int randomInt() {
        return RANDOM.nextInt();
    }

    public static int randomInt(final int min, final int max) {
        return RANDOM.nextInt(max - min) + min;
    }

    private static final ThreadLocalRandom random = ThreadLocalRandom.current();

    /**
     * Randomizes a number.
     *
     * @param max as max int range.
     * @return randomized int value.
     */
    public static int randomInteger(int max) {
        return random.nextInt(max);
    }

    /**
     * Randomizes a number.
     *
     * @return randomized int value.
     */
    public static int randomInteger() {
        return random.nextInt();
    }

    /**
     * Randomizes a number.
     *
     * @return randomized double value.
     */
    public static double randomDouble() {
        return random.nextDouble();
    }

    /**
     * Randomizes a number.
     *
     * @return randomized float value.
     */
    public static float randomFloat() {
        return random.nextFloat();
    }

    /**
     * Randomizes a number.
     *
     * @param min as min value to randomize.
     * @param max as max value to randomize.
     * @return randomized int value.
     */
    public static int randomInteger(int min, int max) {
        return random.nextInt(min, max);
    }

    /**
     * Randomizes a number.
     *
     * @param min as min value to randomize.
     * @param max as max value to randomize.
     * @return randomized double value.
     */
    public static double randomDouble(double min, double max) {
        return random.nextDouble(min, max);
    }

    /**
     * Randomizes a number.
     *
     * @param min as min value to randomize.
     * @param max as max value to randomize.
     * @return randomized float value.
     */
    public static float randomFloat(double min, double max) {
        return (float) randomDouble(min, max);
    }

    /**
     * Randomizes the chance.
     *
     * @param chance as chance to check.
     * @return if `true` chance is right.
     */
    public static boolean chance(double chance) {
        return Math.random() * 100.0 <= chance;
    }

    /**
     * Randomizes a boolean.
     *
     * @return randomized boolean value.
     */
    public static boolean rndBool() {
        return random.nextBoolean();
    }
}
