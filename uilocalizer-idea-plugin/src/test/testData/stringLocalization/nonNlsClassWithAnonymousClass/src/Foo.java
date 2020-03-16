/*
 * #%L
 * UI Localizer
 * %%
 * Copyright (C) 2015 - 2020 Devexperts, LLC
 * %%
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * #L%
 */

package testData.stringLocalization.nonNlsClassWithAnonymousClass.src;

@org.jetbrains.annotations.NonNls
class Foo {
    public static final String hello = "Hello";
    public static final String world = "World";

    Runnable r1 = new Runnable() {
        public static final String hello = "Hello";
        @Override
        public void run() {
            String world = "World";
        }
    };


    static String foo(String s) {
        return "foo";
    }

    public static void main(String[] args) {
        foo("bar");
    }
}
