package com.devexperts.uilocalizer;

/*
 * #%L
 * UI Localizer
 * %%
 * Copyright (C) 2015 - 2017 Devexperts, LLC
 * %%
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * #L%
 */

import org.junit.AfterClass;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static org.junit.Assert.assertEquals;

public class OutputUtilTest {

    private static List<Map.Entry<String,String>> createTestList() {
        List<Map.Entry<String,String>> testList = new ArrayList<>();
        testList.add(new AbstractMap.SimpleEntry<>("testcom.specChar", "#!= \n!"));
        testList.add(new AbstractMap.SimpleEntry<>("testcom.usualString", "Test string."));
        testList.add(new AbstractMap.SimpleEntry<>("testcom.endLine", "\n"));
        testList.add(new AbstractMap.SimpleEntry<>("testcom.exclamPt", "!"));
        testList.add(new AbstractMap.SimpleEntry<>("testcom.equals", "="));
        testList.add(new AbstractMap.SimpleEntry<>("testcom.sharpString", "#"));
        testList.add(new AbstractMap.SimpleEntry<>("testcom.doubleBackSlash", "\\\\"));
        testList.add(new AbstractMap.SimpleEntry<>("testcom.backSlash", "\\"));
        return Collections.unmodifiableList(testList);
    }

    @Test
    public void generatePropertyFilesTest() throws Exception {
        List<Map.Entry<String,String>> testList = createTestList();
        OutputUtil.generatePropertyFiles(testList, false);
        Properties properties = loadProperties();
        testList.forEach(e -> checkPropertyExistence(properties, e.getKey(), e.getValue()));

        List<Map.Entry<String,String>> appendList = new ArrayList<>();
        appendList.add(new AbstractMap.SimpleEntry<>("testcom.appendProperty", "append"));
        OutputUtil.generatePropertyFiles(appendList, true);
        Properties appendProperties = loadProperties();

        List<Map.Entry<String,String>> resultList = new ArrayList<>();
        resultList.addAll(testList);
        resultList.addAll(appendList);
        resultList.forEach(e -> checkPropertyExistence(appendProperties, e.getKey(), e.getValue()));
    }

    private Properties loadProperties() throws IOException {
        InputStream propertiesInput = new FileInputStream("testcom.properties");
        Properties properties = new Properties();
        properties.load(propertiesInput);
        propertiesInput.close();
        return properties;
    }

    private static void checkPropertyExistence(Properties properties, String key, String value) {
        assertEquals(properties.getProperty(key.substring(key.indexOf('.') + 1, key.length())), value);
    }

    @AfterClass
    public static void clean() {
        File dir = new File("testcom.properties");
        dir.delete();
    }
}