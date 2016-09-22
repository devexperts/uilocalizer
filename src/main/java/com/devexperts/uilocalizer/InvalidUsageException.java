package com.devexperts.uilocalizer;

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


import com.sun.tools.javac.tree.JCTree;

public class InvalidUsageException extends Exception {
    public InvalidUsageException(JCTree.JCVariableDecl localizedVariable, String message) {
        super(localizedVariable.toString() + " - " + message);
    }

    public InvalidUsageException(String message) {
        super(message);
    }
}
