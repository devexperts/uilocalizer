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

class A extends C{
    String foo(String p){ return "text";}
}

class B extends A{
    String foo(String p){ return "text";}
}

class C extends A{
    String foo(String p){
      foo("text");
      return "text";
    }
}
