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

class Foo {
    void foo() {
        String v1 = "text";
        @org.jetbrains.annotations.NonNls String v2 = "text";
        String v3;
        @org.jetbrains.annotations.NonNls String v4;

        v3 = "text";
        v4 = "text";
    }
}
