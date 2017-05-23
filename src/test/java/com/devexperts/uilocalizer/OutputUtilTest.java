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
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class OutputUtilTest {

    private static Map<String, String> createTestMap() {
        Map<String, String> testMap = new HashMap<>();
        testMap.put("testcom.specChar", "#!= \n!");
        testMap.put("testcom.usualString", "Test string.");
        testMap.put("testcom.endLine", "\n");
        testMap.put("testcom.exclamPt", "!");
        testMap.put("testcom.equals", "=");
        testMap.put("testcom.sharpString", "#");
        return Collections.unmodifiableMap(testMap);
    }

    @Test
    public void generatePropertyFilesTest() throws Exception {
        Map<String, String> testMap = createTestMap();
        OutputUtil.generatePropertyFiles(testMap);
        InputStream propertiesInput = new FileInputStream("testcom.properties");
        Properties properties = new Properties();
        properties.load(propertiesInput);
        testMap.forEach((key, value) -> {
            assertEquals(properties.getProperty(key.substring(key.indexOf('.') + 1, key.length())), value);
        });
        propertiesInput.close();
    }

    @AfterClass
    public static void clean() {
        File dir = new File("testcom.properties");
        dir.delete();
    }
}
