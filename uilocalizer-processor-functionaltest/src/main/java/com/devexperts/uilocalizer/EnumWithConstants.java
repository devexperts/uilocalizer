package com.devexperts.uilocalizer;

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
