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

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;

import java.lang.reflect.Modifier;

public class AstNodeFactory {
    private static final String OBFUSCATION_SUFFIX = "u";
    public static final String GET_STRING_METHOD_NAME = "getString_" + OBFUSCATION_SUFFIX;
    private static final String LOCALE_FIELD_NAME = "LOCALE_" + OBFUSCATION_SUFFIX;
    private static final int DOT_ASCII = '.';
    private TreeMaker maker;
    private JavacElements utils;
    private final JCTree.JCVariableDecl localeField;
    private final String languageControllerPath;
    private final String customLocalizationMethod;

    public AstNodeFactory(TreeMaker maker, JavacElements utils, String languageControllerPath, String localizationMethod) {
        this.maker = maker;
        this.utils = utils;
        localeField = maker.VarDef(
            maker.Modifiers(Modifier.PRIVATE | Modifier.STATIC | Modifier.VOLATILE),
            utils.getName(LOCALE_FIELD_NAME),
            ident("java.util.Locale"),
            null);
        this.languageControllerPath = languageControllerPath;
        this.customLocalizationMethod = localizationMethod;
    }

    /**
     * generate localization method invocation to be used with localizable field initialization
     * @param key - localization key
     * @param defaultValue - default value
     * @return localization method invocation tree node
     */
    public JCTree.JCMethodInvocation getStringMethodInvocation(String key, String defaultValue) {
        return maker.Apply(
            List.<JCTree.JCExpression>nil(),
            ident(customLocalizationMethod != null ? customLocalizationMethod : GET_STRING_METHOD_NAME),
            List.of((JCTree.JCExpression) maker.Literal(key), maker.Literal(defaultValue))
        );
    }

    public JCTree.JCMethodDecl getStringMethodDeclaration() {
        return maker.MethodDef(
            maker.Modifiers(Modifier.PRIVATE | Modifier.STATIC),
            utils.getName(GET_STRING_METHOD_NAME),
            ident("java.lang.String"),
            List.<JCTree.JCTypeParameter>nil(),
            List.of(
                maker.VarDef(
                    maker.Modifiers(Flags.PARAMETER), utils.getName("key"),
                    ident("java.lang.String"), null),
                maker.VarDef(
                    maker.Modifiers(Flags.PARAMETER), utils.getName("defaultString"),
                    ident("java.lang.String"), null)
            ), List.<JCTree.JCExpression>nil(), getStringMethodBlock(), null
        );
    }

    public JCTree.JCVariableDecl getLocaleFieldDecl() {
        return localeField;
    }

    /**
     * Call this method before attaching nodes to the class tree.
     * Javac flow analyzer may fail if positions are not synchronized.
     * @param classDecl a classDecl
     */
    public void setPositionFor(JCTree.JCClassDecl classDecl) {
        maker.pos = classDecl.getStartPosition();
    }

    private JCTree.JCBlock getStringMethodBlock() {
        List<JCTree.JCStatement> statements = List.nil();
        statements = statements.append(getTry());
        return maker.Block(0, statements);
    }

    private JCTree.JCMethodInvocation getIndexOfInvocation() {
        return maker.Apply(List.<JCTree.JCExpression>nil(), ident("key.indexOf"),
            List.of((JCTree.JCExpression) maker.Literal(DOT_ASCII)));
    }

    private JCTree.JCMethodInvocation getPrefixSubstringInvocation() {
        return maker.Apply(List.<JCTree.JCExpression>nil(), ident("key.substring"),
            List.of(maker.Literal(0), getIndexOfInvocation()));
    }

    private JCTree.JCMethodInvocation getPostfixSubstringInvocation() {
        return maker.Apply(List.<JCTree.JCExpression>nil(), ident("key.substring"),
            List.of(maker.Binary(JCTree.Tag.PLUS, getIndexOfInvocation(), maker.Literal(1))));
    }

    private JCTree.JCMethodInvocation getBundleInvocation() {
        return maker.Apply(List.<JCTree.JCExpression>nil(), ident("java.util.ResourceBundle.getBundle"),
            List.of(getPrefixSubstringInvocation(),
                languageControllerPath == null ? ident(LOCALE_FIELD_NAME) :
                    maker.Apply(
                        List.nil(),
                        ident(languageControllerPath + ".getLanguage"),
                        List.nil()
                    )
            )
        );
    }

    private JCTree.JCExpression getBundleStringInvocation() {
        return maker.Apply(
            List.<JCTree.JCExpression>nil(),
            maker.Select(getBundleInvocation(), utils.getName("getString")),
            List.of(getPostfixSubstringInvocation())
        );
    }

    private JCTree.JCStatement getStringReturn() {
        return maker.Return((getBundleStringInvocation()));
    }

    private JCTree.JCCatch getCatcher() {
        return maker.Catch(
            maker.VarDef(maker.Modifiers(0), utils.getName("e"), ident("java.lang.Exception"), null),
            maker.Block(0, List.of((JCTree.JCStatement) maker.Return(ident("defaultString"))))
        );
    }


    private JCTree.JCTry getTry() {
        return maker
            .Try(maker.Block(0,
                languageControllerPath == null ?
                    List.of(getLocaleFieldLazyInit(), getStringReturn()):
                    List.of(getStringReturn())
                ),
                List.of(getCatcher()),
                null);
    }

    private JCTree.JCMethodInvocation getPropertyMethodInvocation() {
        return maker.Apply(List.<JCTree.JCExpression>nil(), ident("java.lang.System.getProperty"),
            List.of((JCTree.JCExpression) maker.Literal("ui.dialogs.locale"), maker.Literal("en-US")));
    }

    private JCTree.JCMethodInvocation getForLanguageTagMethodInvocation() {
        return maker.Apply(List.<JCTree.JCExpression>nil(), ident("java.util.Locale.forLanguageTag"),
            List.of((JCTree.JCExpression) getPropertyMethodInvocation()));
    }

    private JCTree.JCIf getLocaleFieldLazyInit() {
        return maker.If(maker.Binary(JCTree.Tag.EQ, maker.Ident(localeField.getName()), maker.Literal(TypeTag.BOT, null)),
            maker.Exec(maker.Assign(maker.Ident(localeField.getName()), getForLanguageTagMethodInvocation())), null);
    }

    private JCTree.JCExpression ident(String complexIdent) {
        String[] parts = complexIdent.split("\\.");
        JCTree.JCExpression expression = maker.Ident(utils.getName(parts[0]));
        for (int i = 1; i < parts.length; i++) {
            expression = maker.Select(expression, utils.getName(parts[i]));
        }
        return expression;
    }
}
