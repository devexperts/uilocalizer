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


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark static final string field with this annotation to make it localizable. Default resource bundles are generated in compile-time.<br>
 * Example:
 * <pre>
 *
 *    &#064;Localizable("scope.key") private static final String HELLO_MESSAGE = "Hello, sir!"; </pre>
 * after compilation will be equivalent to
 * <pre>{@code
 *    private static final String HELLO_MESSAGE = getString_u("scope.key", "Hello, sir!");
 *    private static final java.util.Locale LOCALE_u = java.util.Locale.forLanguageTag(java.lang.System.getProperty("ui.dialogs.locale", "en-US"));
 *    private static String getString_u(String key, String defaultString) {
 *        try {
 *            String val = ResourceBundle.getBundle(key.substring(0, key.indexOf('.')), LOCALE_u).getString(key.substring(key.indexOf('.') + 1));
 *            return new String(val.getBytes("ISO-8859-1"), "UTF-8");
 *        } catch (RuntimeException var3) {
 *            return defaultString;
 *        }
 *    }
 * }  </pre>
 * And new file scope.properties will be created with line:<pre>{@code
 *    key=Hello, sir!
 * }</pre>
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.FIELD})
public @interface Localizable {
    String value();
}
