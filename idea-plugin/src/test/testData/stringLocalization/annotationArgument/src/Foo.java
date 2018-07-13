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

package testData.stringLocalization.annotationArgument.src;

@interface Language {
  @org.jetbrains.annotations.NonNls
  java.lang.String value();
}

class Foo {
    void foo() {
        @Language("abcdefgh")
        String s;
    }
}