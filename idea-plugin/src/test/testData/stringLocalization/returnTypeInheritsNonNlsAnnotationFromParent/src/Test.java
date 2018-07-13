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

package testData.stringLocalization.returnTypeInheritsNonNlsAnnotationFromParent.src;

interface I {
    @org.jetbrains.annotations.NonNls String foo();
}

class B implements I{
    public String foo() {
        return "text";
    }
}

class A {
    B inner = new B() {
        public String foo() {
            return "text";
        }
    };
}