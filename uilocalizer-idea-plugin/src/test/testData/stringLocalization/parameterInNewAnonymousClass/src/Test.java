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

package testData.stringLocalization.parameterInNewAnonymousClass.src;

class Test {
    public static final Test TEST = new Test("text") {
        public void foo() {}
    };

    public Test(@org.jetbrains.annotations.NonNls String p) {

    }

    public abstract void foo();
}
