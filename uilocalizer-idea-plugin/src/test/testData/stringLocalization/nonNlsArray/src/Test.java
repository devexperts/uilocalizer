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

package testData.stringLocalization.nonNlsArray.src;

class Foo {
    @org.jetbrains.annotations.NonNls String[] myArray = new String[] {"text1", "text2"};
    @org.jetbrains.annotations.NonNls Stirng[] foo() {
        myArray = new String[] {"text3", "text4"};
        myArray[0] = "text5";

        return new String[] {"text6"};
    }
}
