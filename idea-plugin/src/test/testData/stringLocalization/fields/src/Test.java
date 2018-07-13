/*
 * #%L
 * UI Localizer
 * %%
 * Copyright (C) 2015 - 2018 Devexperts, LLC
 * %%
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * #L%
 */

class Foo {
    @org.jetbrains.annotations.NonNls
    String field;
    @org.jetbrains.annotations.NonNls
    String field2 = "text1";
    public static final String
    nonLocalized="fail";

    void foo() {
        field = "text2";
    }

    @com.devexperts.uilocalizer.Localizable("some.property.key")
    String field3 = "Localized string";
}