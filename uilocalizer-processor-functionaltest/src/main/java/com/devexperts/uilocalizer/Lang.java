package com.devexperts.uilocalizer;

import java.util.Locale;

public class Lang {
    static Locale lang = Locale.ENGLISH;

    public static void setLang(Locale lang) {
        Lang.lang = lang;
    }

    public static String getString_u(String key, String defaultString) {
        if (lang == Locale.ENGLISH)
            return defaultString;
        if (key.contains("OK"))
            return "Okeyo";
        if (key.contains("Cancel"))
            return "Noooooooo";
        if (key.contains("Reuters"))
            return "ItalianReuters";
        return "Italy";
    }
}
