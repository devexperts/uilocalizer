package com.devexperts.uilocalizer;

/*
 * #%L
 * UI Localizer
 * %%
 * Copyright (C) 2015 - 2016 Devexperts, LLC
 * %%
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * #L%
 */


import com.sun.tools.javac.tree.JCTree;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class OutputUtil {
    public static void printCompilationUnits(Collection<JCTree.JCCompilationUnit> compilationUnits,
        String outputFileName) throws FileNotFoundException
    {
        PrintWriter writer = new PrintWriter(outputFileName);
        compilationUnits.forEach(writer::println);
        writer.close();
    }

    public static void generatePropertyFiles(Map<String, String> keysToDefaultValues) throws FileNotFoundException {
        Map<String, PrintWriter> scopesToWriters = new HashMap<>();
        for (Map.Entry<String, String> e : keysToDefaultValues.entrySet()) {
            int dotIndex = e.getKey().indexOf('.');
            String scope = e.getKey().substring(0, dotIndex);
            String key = e.getKey().substring(dotIndex + 1);
            if (!scopesToWriters.containsKey(scope)) {
                scopesToWriters.put(scope, new PrintWriter(scope + ".properties"));
            }
            scopesToWriters.get(scope).println(key + "=" + e.getValue());
        }
        scopesToWriters.values().forEach(PrintWriter::close);
    }
}
