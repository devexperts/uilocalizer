/*
 * #%L
 * UI Localizer
 * %%
 * Copyright (C) 2015 - 2018 Devexperts, LLC
 * %%
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * #L%
 */

package com.devexperts.uilocalizer;

import com.sun.tools.javac.tree.JCTree;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OutputUtil {
    public static void printCompilationUnits(Collection<JCTree.JCCompilationUnit> compilationUnits,
                                             String outputFileName) throws FileNotFoundException {
        PrintWriter writer = new PrintWriter(outputFileName);
        compilationUnits.forEach(writer::println);
        writer.close();
    }

    public static void generatePropertyFiles(List<Map.Entry<String, String>> keysToDefaultValues, boolean append) throws FileNotFoundException {
        Map<String, PrintWriter> scopesToWriters = new HashMap<>();
        for (Map.Entry<String, String> e : keysToDefaultValues) {
            int dotIndex = e.getKey().indexOf('.');
            String scope = e.getKey().substring(0, dotIndex);
            String key = e.getKey().substring(dotIndex + 1);
            if (!scopesToWriters.containsKey(scope)) {
                scopesToWriters.put(scope, new PrintWriter(new FileOutputStream(new File(scope + ".properties"),
                    append)));
            }
            scopesToWriters.get(scope).println(key + "=" + screenSpecialSymbols(e.getValue()));
        }
        scopesToWriters.values().forEach(PrintWriter::close);
    }

    private static String screenSpecialSymbols(String value) {
        StringBuilder returnValue = new StringBuilder();
        for (char c : value.toCharArray()) {
            switch (c) {
                case ' ':
                    if (returnValue.length() == 0) {
                        returnValue.append("\\").append(c);
                    } else {
                        returnValue.append(c);
                    }
                    break;
                case '\\':
                case '!':
                case ':':
                case '#':
                case '=':
                    returnValue.append("\\").append(c);
                    break;
                case '\n':
                    returnValue.append("\\").append('n');
                    break;
                default:
                    if(c < 128) {
                        returnValue.append(c);
                    } else {
                        returnValue.append("\\u").append(String.format("%04X", (int) c));
                    }
            }
        }
        return returnValue.toString();
    }
}