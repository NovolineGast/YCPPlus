package com.yumegod.obfuscator.jobf.utils.namefactory.impl;

import com.yumegod.obfuscator.jobf.utils.namefactory.NameFactory;

import java.util.Arrays;

// copied from proguard

/**
 * This <code>NameFactory</code> generates unique short names, using mixed-case
 * characters or lower-case characters only.
 *
 * @author Eric Lafortune
 */
public class SimpleNameFactory implements NameFactory
{
    private static final int CHARACTER_COUNT = 26;

    /**
     +     * Array of windows reserved names.
     +     * This array does not include COM{digit} or LPT{digit} as {@link SimpleNameFactory} does not generate digits.
     +     * This array must be sorted in ascending order as we're using {@link Arrays#binarySearch(Object[], Object)} on it.
     +     */
    private static final String[] reservedNames    = new String[] {"AUX", "CON", "NUL", "PRN"};

    private int index = 0;

    // Implementations for NameFactory.
    @Override
    public void reset()
    {
        index = 0;
    }

    @Override
    public String nextName()
    {
        return name(index++);
    }


    /**
     * Returns the name at the given index.
     */
    private String name(int index)
    {
        // Create a new name for this index
        return newName(index);
    }


    /**
     * Creates and returns the name at the given index.
     */
    private String newName(int index)
    {
        int totalCharacterCount = CHARACTER_COUNT;

        int baseIndex = index / totalCharacterCount;
        int offset    = index % totalCharacterCount;

        char newChar = charAt(offset);

        String newName = baseIndex == 0 ?
                String.valueOf(newChar) :
                (name(baseIndex-1) + newChar);

        if (Arrays.binarySearch(reservedNames, newName.toUpperCase()) >= 0)
        {
            newName += newChar;
        }
        return newName;
    }


    /**
     * Returns the character with the given index, between 0 and the number of
     * acceptable characters.
     */
    private char charAt(int index)
    {
        return (char)('a' + index);
    }
}
