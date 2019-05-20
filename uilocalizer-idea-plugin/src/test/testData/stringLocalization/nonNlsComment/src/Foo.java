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
    void foo(String... s) {
        foo("literal", "literal"); //   MYNON-NLS0
        String d = "xxxxx";  // NON-NLS
        String d0 = "xxxxx", d01="sssss";  //MYNON-NLS
        String d1 = "xxxxx";  // MYNON-NLS?

        String d2 = "xxxxx"; /* MYNON-NLS ??? */
        String wtf="MYNON-NLS";
        String dw2 = "xxxxx"; String wtw="MYNON-NLS"; /* MYNON-NLS ??? */
    }
}
