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

package testData.stringLocalization.constructorCallOfNonNlsVariable.src;

class Test {
    void foo() {
        @org.jetbrains.annotations.NonNls StringBuffer buffer = new StringBuffer("text");
        buffer = new StringBuffer("text");
    }
}
