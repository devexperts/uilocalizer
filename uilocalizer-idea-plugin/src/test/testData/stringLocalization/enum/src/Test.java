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

enum Test {
    CHECKIN("Text1"),
    ADD("Rext2");

    Test(final String id) {
        myId = id;
    }

    private final String myId;
}

enum Test2 {
    CHECKIN("Text1"),
    ADD("Rext2");

    Test2(@org.jetbrains.annotations.NonNls final String id) {
        myId = id;
    }

    private final String myId;
}
