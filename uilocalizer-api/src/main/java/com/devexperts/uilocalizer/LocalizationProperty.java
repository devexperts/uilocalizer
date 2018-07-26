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

package com.devexperts.uilocalizer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@link LocalizationProperty} annotation is used to collect locale-sensible properties without code transformation.
 * <p>
 * If marked field is initialized using a method or constructor invocation with two string parameters,
 * the first one will be considered a property key, and the second one - default value.
 * <p>
 * Following definitions will specify a property "dialog.someText" in bundle "order" with default value "Some text":
 * <pre>
 * &#064;LocalizationProperty
 * public final static SomeClass complexVar = new SomeClass("order.dialog.someText", "Some text");
 * &#064;LocalizationProperty
 * public final static SomeClass anotherComplexVar = SomeStaticClass.getSpecificValue("order.dialog.someText", "Some text");
 * </pre>
 *
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.FIELD})
public @interface LocalizationProperty {
}
