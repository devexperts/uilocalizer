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

import com.sun.tools.javac.tree.JCTree;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Responsible for adding necessary static members to classes that have @Localizable constants inside.
 */
class AddLocalizationNodesVisitor extends JCTree.Visitor {
    private AstNodeFactory localizationNodesFactory;
    private Collection<JCTree.JCVariableDecl> localizableVariableDeclarations;
    private JCTree.JCClassDecl currentClass;
    private Set<JCTree.JCClassDecl> localizedClasses;
    private final String languageControllerPath;
    private final String customLocalizationMethod;

    AddLocalizationNodesVisitor(AstNodeFactory localizationNodesFactory,
                                Collection<JCTree.JCVariableDecl> localizableVariableDeclarations,
                                String languageControllerPath,
                                String customLocalizationMethod
        )
    {
        this.localizationNodesFactory = localizationNodesFactory;
        this.localizableVariableDeclarations = localizableVariableDeclarations;
        this.languageControllerPath = languageControllerPath;
        this.customLocalizationMethod = customLocalizationMethod;
        localizedClasses = new HashSet<>();
    }

    @Override
    public void visitTopLevel(JCTree.JCCompilationUnit jcCompilationUnit) {
        jcCompilationUnit.getTypeDecls().forEach(typeDecl -> typeDecl.accept(this));
    }

    @Override
    public void visitClassDef(JCTree.JCClassDecl jcClassDecl) {
        JCTree.JCClassDecl previous = currentClass;
        currentClass = jcClassDecl;
        jcClassDecl.defs.forEach(def -> def.accept(this));
        currentClass = previous;
    }

    @Override
    public void visitVarDef(JCTree.JCVariableDecl jcVariableDecl) {
        if (localizableVariableDeclarations.contains(jcVariableDecl)) {
            if (!localizedClasses.contains(currentClass)) {
                localizedClasses.add(currentClass);
                localizationNodesFactory.setPositionFor(currentClass);
                if (languageControllerPath == null && customLocalizationMethod == null)
                    currentClass.defs = currentClass.defs.append(localizationNodesFactory.getLocaleFieldDecl());
                if (customLocalizationMethod == null)
                    currentClass.defs = currentClass.defs.append(localizationNodesFactory.getStringMethodDeclaration());
            }
        }
    }

    @Override
    public void visitTree(JCTree jcTree) {
        // Do nothing
    }
}
