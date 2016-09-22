/*
 * #%L
 * UI Localizer
 * %%
 * Copyright (C) 2015 - 2016 Devexperts, LLC
 * %%
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * #L%
 */
import com.devexperts.uilocalizer.Localizable;

/**
 * @author Ivan Rakov
 */
public enum KindOfType {
    @Localizable("scope.key.ca") CANADIAN(7, "Canadian", "CA"),
    @Localizable("scope.key.no") USUAL("Usual");

    KindOfType(int id, String param, String code) {
        this.code = code;
        this.id = id;
        this.param = param;
    }

    KindOfType(String param) {
        this.param = param;
    }

    private String param;
    private String code;
    private int id;

}
