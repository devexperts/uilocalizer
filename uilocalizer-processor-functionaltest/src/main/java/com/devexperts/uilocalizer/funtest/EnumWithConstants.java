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

package com.devexperts.uilocalizer.funtest;

import com.devexperts.uilocalizer.Localizable;

public enum EnumWithConstants {
    HELLO(Constants.CNBC_NAME),
    WORLD(Constants.REUTERS_NAME);

    private String name;

    EnumWithConstants(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    private static class Constants {
        @Localizable("test.alert.dialog.news.NewsSourceForUi.Cnbc")
        private static final String CNBC_NAME = "CNBC";
        @Localizable("test.alert.dialog.news.NewsSourceForUi.Reuters")
        private static final String REUTERS_NAME = "Reuters";
    }
}
