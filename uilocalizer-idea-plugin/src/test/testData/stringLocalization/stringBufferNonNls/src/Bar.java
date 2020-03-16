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

package testData.stringLocalization.stringBufferNonNls.src;

import java.util.Collection;

class SBTest {
    void foo(@org.jetbrains.annotations.NonNls Collection coll) {
        coll.add("aaa");
    }

    void foo(@org.jetbrains.annotations.NonNls StringBuffer buffer) {
        buffer.append("aaa");
        buffer.append("aaa").append("bbb").append("do not i18n this too");
    }
}
