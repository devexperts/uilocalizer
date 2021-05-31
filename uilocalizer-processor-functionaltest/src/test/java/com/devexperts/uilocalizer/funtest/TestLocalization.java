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

package com.devexperts.uilocalizer.funtest;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Locale;

import static org.junit.Assert.assertEquals;

public class TestLocalization {
    @Test
    public void testValidateFile() throws IOException {
        // test.properties is populated with the following default values in this particular order
        String expectedValue[] = {"CNBC", "Reuters", "Cancel", "OK", "Yes"};
        String line;
        int index = 0;
        String path = "build/localizationTool/test.properties";
        try (BufferedReader propsFile = new BufferedReader(new FileReader(path))) {
            // reading each line propsFile test.properties to catch actual values
            line = propsFile.readLine();
            while (line != null) {
                String value = line.split("=")[1];
                assertEquals(expectedValue[index], value);
                index++;
                line = propsFile.readLine();
            }
        }
        assertEquals(5, index);
    }

    @Test
    public void testEnum() {
        Lang.setLang(Locale.ITALIAN);
        assertEquals("ItalianReuters", EnumWithConstants.WORLD.getName());
    }


    @Test
    public void english() {
        Lang.setLang(Locale.ENGLISH);
        assertEquals("Cancel", Buttons.CANCEL);
    }

    @Test
    public void italian() {
        Lang.setLang(Locale.ITALIAN);
        assertEquals("Italy", ButtonsTwo.YES);
        assertEquals("Okeyo", ButtonConsumer.getOkText());
    }
}
