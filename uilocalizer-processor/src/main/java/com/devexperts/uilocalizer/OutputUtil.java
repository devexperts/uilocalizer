/*
 * #%L
 * UI Localizer
 * %%
 * Copyright (C) 2015 - 2019 Devexperts, LLC
 * %%
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * #L%
 */

package com.devexperts.uilocalizer;

import com.sun.tools.javac.tree.JCTree;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

public class OutputUtil {
    public static void printCompilationUnits(Collection<JCTree.JCCompilationUnit> compilationUnits,
                                             File outputFileName) throws FileNotFoundException {
        PrintWriter writer = new PrintWriter(outputFileName);
        compilationUnits.forEach(writer::println);
        writer.close();
    }

    /**
     * this method is invoked once at the end of overall processing
     *
     * @param outputFolder
     * @param keysToDefaultValues List of Map.Entry((String)scope.suffix, (String)value)
     */
    public static void generatePropertyFiles(Path outputFolder,
                                             List<Map.Entry<String, String>> keysToDefaultValues) throws IOException {
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
        for (String key : properties.keySet()) {
            if (!readProperties.containsKey(key)) {
                readProperties.put(key, properties.get(key));
                updateRequired = true;
            }
            if (!properties.get(key).equals(readProperties.get(key))) {
                readProperties.replace(key, properties.get(key));
                updateRequired = true;
            }
        }
        if (updateRequired)
            writeProperties(readProperties, file);
    }

    private static Map<String, TreeMap<String, String>> getTreeMapForScopes(List<Map.Entry<String, String>> keysToDefaultValues) {
        Map<String /* scope */, TreeMap<String /* suffix */, String /* value */>> scopesWithKeysToDefaultValues = new HashMap<>();

        for (Map.Entry<String, String> e : keysToDefaultValues) {
            int dotIndex = e.getKey().indexOf('.');
            String scope = e.getKey().substring(0, dotIndex);
            String key = e.getKey().substring(dotIndex + 1);
            if (!scopesWithKeysToDefaultValues.containsKey(scope))
                scopesWithKeysToDefaultValues.put(scope, new TreeMap<>());
            scopesWithKeysToDefaultValues.get(scope).put(key, e.getValue());
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
        FileInputStream fileInputStream = new FileInputStream(file);
        TreeMap<String, String> propertyMap = new TreeMap<>();
        Properties properties = new Properties();
        properties.load(fileInputStream);
        fileInputStream.close();
        for (String propertyName : properties.stringPropertyNames()) {
            String defaultValue = properties.getProperty(propertyName);
            propertyMap.put(propertyName, defaultValue);
        }
        return propertyMap;
    }

    private static void writeProperties(TreeMap<String /* suffix */, String /* value */> properties, File file) throws FileNotFoundException {
        PrintWriter printWriter = new PrintWriter(new FileOutputStream(file, false));
        for (String property : properties.keySet())
            printWriter.println(property /* suffix */ + "=" + screenSpecialSymbols(properties.get(property)) /* value */);
        printWriter.close();
    }
}

