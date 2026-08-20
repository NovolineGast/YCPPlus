package com.yumegod.obfuscator.jobf.utils.namefactory.impl;

import com.yumegod.obfuscator.jobf.utils.namefactory.NameFactory;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public class CustomNameFactory implements NameFactory {
    private static final char COMMENT_CHARACTER = '#';

    private final List<String> names;
    private Set<String> nameSet;
    private int index = 0;

    /**
     * Creates a new <code>DictionaryNameFactory</code>.
     *
     * @param url The URL from which the names can be read.
     */
    public CustomNameFactory(URL url) throws IOException {
        this(url, true);
    }

    /**
     * Creates a new <code>DictionaryNameFactory</code>.
     *
     * @param url The URL from which the names can be read.
     * @param validJavaIdentifiers Specifies whether the produced names should be valid Java
     *     identifiers.
     */
    public CustomNameFactory(URL url, boolean validJavaIdentifiers)
            throws IOException {
        this(
                new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8)),
                validJavaIdentifiers);
    }

    /**
     * Creates a new <code>DictionaryNameFactory</code>.
     *
     * @param reader The reader from which the names can be read. The reader is closed at the end.
     * @param validJavaIdentifiers Specifies whether the produced names should be valid Java
     *     identifiers.
     */
    public CustomNameFactory(Reader reader, boolean validJavaIdentifiers)
            throws IOException {
        this.nameSet = readDictionary(reader, validJavaIdentifiers);
        this.names = new ArrayList<>(this.nameSet);
    }

    private static Set<String> readDictionary(Reader reader, boolean validJavaIdentifiers)
            throws IOException {
        try {
            Set<String> names = new LinkedHashSet<>();
            StringBuilder builder = new StringBuilder();

            while (true) {
                // Read the next character.
                int c = reader.read();

                // Is it a valid identifier character?
                if (c != -1
                        && (validJavaIdentifiers
                        ? (builder.length() == 0
                        ? Character.isJavaIdentifierStart((char) c)
                        : Character.isJavaIdentifierPart((char) c))
                        : (c != '\n' && c != '\r' && c != COMMENT_CHARACTER))) {
                    // Append it to the current identifier.
                    builder.append((char) c);
                } else {
                    // Did we collect a new identifier?
                    if (builder.length() > 0) {
                        // Add the completed name to the list of names, if it's
                        // not in it yet.
                        String name = builder.toString();
                        names.add(name);

                        // Clear the builder.
                        builder.setLength(0);
                    }

                    // Is this the beginning of a comment line?
                    if (c == COMMENT_CHARACTER) {
                        // Skip all characters till the end of the line.
                        do {
                            c = reader.read();
                        } while (c != -1 && c != '\n' && c != '\r');
                    }

                    // Is this the end of the file?
                    if (c == -1) {
                        // Just return.
                        return names;
                    }
                }
            }
        } finally {
            reader.close();
        }
    }

    // Implementations for NameFactory.

    public void reset() {
        index = 0;
    }

    public String nextName() {
        return name(index++);
    }

    public String name(int index) {
        int size = names.size();

        int x = index % size, y = index / size;
        return y == 0 ? strAt(x) : (name(y-1) + strAt(x));
    }

    public String strAt(int index) {
        return names.get(index);
    }
}