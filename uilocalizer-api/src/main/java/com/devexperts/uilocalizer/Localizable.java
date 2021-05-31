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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark static final string field with this annotation to make it localizable. Default resource bundles are generated in compile-time.<br>
 *
 * Example:
 * <pre>
 *
 *    &#064;Localizable("scope.key") private static final String HELLO_MESSAGE = "Hello, sir!"; </pre>
 *
 * after compilation will look something like this (exact implementation depends of compile-time annotation processor options):
 * <pre>{@code
 *     private static final String HELLO_MESSAGE = getString_u("scope.key", "Hello, sir!");
 *     private static volatile java.util.Locale LOCALE_u;
 *
 *     private static java.lang.String getString_u(java.lang.String key, java.lang.String defaultString) {
 *         try {
 *             if (LOCALE_u == null) LOCALE_u =
 *                 java.util.Locale.forLanguageTag(java.lang.System.getProperty("ui.dialogs.locale", "en-US"));
 *             return java.util.ResourceBundle.getBundle(key.substring(0, key.indexOf(46)), LOCALE_u)
 *                 .getString(key.substring(key.indexOf(46) + 1));
 *         } catch (java.lang.Exception e) {
 *             return defaultString;
 *         }
 *     }
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
