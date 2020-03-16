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

package testData.stringLocalization.constructorCallUnderLocalizableAnnotation.src;

class Test {
    @com.devexperts.uilocalizer.Localizable("some.property.key")
    SimpleTester simpleTester = new SimpleTester("Localizable", "NonNls", "Unknown");

    void foo() {
        @org.jetbrains.annotations.NonNls
        StringBuffer buffer = new StringBuffer("text");
        buffer = new StringBuffer("text");
    }

    private class SimpleTester{
        SimpleTester(String a, @org.jetbrains.annotations.NonNls String b, String c) {

        }
    }
}
