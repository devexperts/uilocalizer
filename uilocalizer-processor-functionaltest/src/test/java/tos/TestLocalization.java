package tos;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.io.*;
import java.util.Locale;

public class TestLocalization {
    @Test
    public void testValidateFile() throws IOException {
//      test.properties is populated with the following default values in this particular order
        String expected_value[] = {"CNBC", "Reuters", "Cancel", "OK", "Yes"};
        String actual_value[] = new String[5];
        String line;
        int index = 0;
        String path = "build/localizationTool/test.properties";
        BufferedReader read = new BufferedReader(new FileReader(path));

//      reading each line in test.properties to catch actual values
        line = read.readLine();
        while (line != null) {
            actual_value[index] = line.split("=")[1];
            assertEquals(expected_value[index], actual_value[index]);
            index++;
            line = read.readLine();
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
