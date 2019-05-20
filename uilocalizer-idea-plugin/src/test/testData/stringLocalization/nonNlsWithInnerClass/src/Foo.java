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

package testData.stringLocalization.nonNlsWithInnerClass.src;

@org.jetbrains.annotations.NonNls
class Foo {
    public static final String hello = "Hello";
    public static final String world = "World";

    static String foo(String s) {
        return "foo";
    }

    public static void main(String[] args) {
        foo("bar");
    }
    class InnerClass {
        public static final String inner = "Inner";
    }
}
