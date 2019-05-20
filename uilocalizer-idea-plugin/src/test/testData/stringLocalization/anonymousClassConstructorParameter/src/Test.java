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

class Test {
  private class InnerTest {
    public InnerTest(String s) { }
    public abstract void run();
  }

  public void foo(String s) {
    bar(new InnerTest("Literal") { public void run() { } });
  }

  public void bar(InnerTest t) {

  }
}
