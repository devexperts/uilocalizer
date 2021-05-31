/*
 * #%L
 * UI Localizer
 * %%
 * Copyright (C) 2015 - 2021 Devexperts, LLC
 * %%
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * #L%
 */

package com.devexperts.uilocalizer;

import com.sun.tools.javac.tree.JCTree;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.TreeMap;

public class OutputUtil {
    public static void printCompilationUnits(Collection<JCTree.JCCompilationUnit> compilationUnits, File outputFileName)
        throws FileNotFoundException
    {
        PrintWriter writer = new PrintWriter(outputFileName);
        compilationUnits.forEach(writer::println);
        writer.close();
    }

    /**
     * Generates/updates property files with collected properties.
     * This method is invoked once at the end of all processing.
     *
     * @param outputFolder directory where localization property files shall be placed
     * @param keysToDefaultValues List of Map.Entry((String)scope.suffix, (String)value)
     *
     * @throws IOException if some IO operation fails
     */
    public static void generatePropertyFiles(Path outputFolder, List<Map.Entry<String, String>> keysToDefaultValues)
        throws IOException
    {
        Map<String, TreeMap<String, String>> scopesWithKeysToDefaultValues = getTreeMapForScopes(keysToDefaultValues);

        for (String scope : scopesWithKeysToDefaultValues.keySet()) {
            File file = outputFolder.resolve(scope + ".properties").toFile();
            TreeMap<String, String> properties = scopesWithKeysToDefaultValues.get(scope);
            if (!file.exists()) {
                writeProperties(properties, file);
            } else {
                mergeProperties(properties, file);
            }
        }
    }

    private static void mergeProperties(TreeMap<String, String> properties, File file) throws IOException {
        TreeMap<String /* suffix */, String /* value */> readProperties = readProperties(file);
        boolean updateRequired = false;
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (!Objects.equals(value, readProperties.get(key))) {
                readProperties.put(key, value);
                updateRequired = true;
            }
        }
        if (updateRequired)
            writeProperties(readProperties, file);
    }

    private static Map<String, TreeMap<String, String>> getTreeMapForScopes(
        List<Map.Entry<String, String>> keysToDefaultValues)
    {
        Map<String /* scope */, TreeMap<String /* suffix */, String /* value */>> scopesWithKeysToDefaultValues =
            new HashMap<>();

        for (Map.Entry<String, String> e : keysToDefaultValues) {
            int dotIndex = e.getKey().indexOf('.');
            String scope = e.getKey().substring(0, dotIndex);
            String key = e.getKey().substring(dotIndex + 1);
            TreeMap<String, String> treeMap =
                scopesWithKeysToDefaultValues.computeIfAbsent(scope, s -> new TreeMap<>());
            treeMap.put(key, e.getValue());
        }
        return scopesWithKeysToDefaultValues;
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
                    if (c < 128) {
                        returnValue.append(c);
                    } else {
                        returnValue.append("\\u").append(String.format("%04X", (int) c));
                    }
            }
        }
        return returnValue.toString();
    }

    public static TreeMap<String /* suffix */, String /* value */> readProperties(File file) throws IOException {
        TreeMap<String, String> propertyMap;
        Properties properties;
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            propertyMap = new TreeMap<>();
            properties = new Properties();
            properties.load(fileInputStream);
        }
        for (String propertyName : properties.stringPropertyNames()) {
            String defaultValue = properties.getProperty(propertyName);
            propertyMap.put(propertyName, defaultValue);
        }
        return propertyMap;
    }

    private static void writeProperties(TreeMap<String /* suffix */, String /* value */> properties, File file)
        throws FileNotFoundException, UnsupportedEncodingException
    {
        try (PrintWriter printWriter = new PrintWriter(file, "UTF-8")) {
            for (String property : properties.keySet()) {
                printWriter.println(property + "=" + screenSpecialSymbols(properties.get(property)));
            }
        }
    }
}

