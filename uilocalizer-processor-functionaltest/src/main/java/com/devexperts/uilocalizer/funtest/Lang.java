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
