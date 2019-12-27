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

package com.devexperts.uilocalizer;

import com.intellij.codeInspection.i18n.I18nInspection;
import com.intellij.openapi.roots.LanguageLevelProjectExtension;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.testFramework.InspectionTestCase;
import com.intellij.testFramework.LightCodeInsightTestCase;

/**
 * Created by sugakandrey.
 */
public class StringLocalizationInspectionTest extends InspectionTestCase {
    private void doTest() throws Exception {
        doTest(new StringLocalizationInspection());
    }
    private void doTest(I18nInspection tool) throws Exception {
        doTest("stringLocalization/" + getTestName(true), tool);
    }

    public void testHardCodedStringLiteralAsParameter() throws Exception{ doTest(); }
    public void testReturnTypeInheritsNonNlsAnnotationFromParent() throws Exception{ doTest(); }
    public void testRecursiveInheritance() throws Exception { doTest(); }
    public void testParameterInheritsNonNlsAnnotationFromSuper() throws Exception { doTest(); }
    public void testLocalVariables() throws Exception { doTest(); }
    public void testFields() throws Exception{ doTest(); }
    public void testAnonymousClassConstructorParameter() throws Exception { doTest(); }
    public void testStringBufferNonNls() throws Exception { doTest(); }
    public void testNonNlsMethod() throws Exception { doTest(); }
    public void testNonNlsClass() throws Exception { doTest(); }
    public void testNonNlsInnerClass() throws Exception { doTest(); }
    public void testNonNlsWithInnerClass() throws Exception { doTest(); }
    public void testNonNlsClassWithLambda() throws Exception { doTest(); }
    public void testNonNlsPackage() throws Exception { doTest(); }
    public void testEnum() throws Exception {
        final JavaPsiFacade facade = LightCodeInsightTestCase.getJavaFacade();
        final LanguageLevel effectiveLanguageLevel = LanguageLevelProjectExtension.getInstance(facade.getProject()).getLanguageLevel();
        LanguageLevelProjectExtension.getInstance(facade.getProject()).setLanguageLevel(LanguageLevel.JDK_1_5);
        try {
            doTest();
        }
        finally {
            LanguageLevelProjectExtension.getInstance(facade.getProject()).setLanguageLevel(effectiveLanguageLevel);
        }
    }

    public void testVarargNonNlsParameter() throws Exception { doTest(); }
    public void testInitializerInAnonymousClass() throws Exception{ doTest(); }
    public void testNonNlsArray() throws Exception{ doTest(); }
    public void testParameterInNewAnonymousClass() throws Exception{ doTest(); }
    public void testConstructorCallOfNonNlsVariable() throws Exception{ doTest(); }
    public void testConstructorCallUnderLocalizableAnnotation() throws Exception{ doTest(); }
    public void testSwitchOnNonNlsString() throws Exception{ doTest(); }
    public void testNonNlsComment() throws Exception{
        StringLocalizationInspection inspection = new StringLocalizationInspection();
        inspection.setNonNlsPattern("MYNON-NLS");
        doTest(inspection);
    }
    public void testAnnotationArgument() throws Exception{ doTest(); }

    @Override
    protected String getTestDataPath() {
        return "src/test/testData";
    }
}
