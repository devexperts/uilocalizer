/*
 * #%L
 * UI Localizer
 * %%
 * Copyright (C) 2015 - 2018 Devexperts, LLC
 * %%
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * #L%
 */

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
public class Test {
  public static void main(String[] args){
    ActionListener listener = new ActionListener(){
      {
        final String test = "problem reported twice";
      }
      public void actionPerformed(final ActionEvent e) {

      }
    };
  }
}