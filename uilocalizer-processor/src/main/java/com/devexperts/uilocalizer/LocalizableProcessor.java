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

import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Pair;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@SupportedAnnotationTypes({"com.devexperts.uilocalizer.Localizable"})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class LocalizableProcessor extends AbstractProcessor {
    private static final String LOCALIZABLE_ANNOTATION = "com.devexperts.uilocalizer.Localizable";
    private static final String LOCALIZATION_PROPERTY_ANNOTATION = "com.devexperts.uilocalizer.LocalizationProperty";
    public static final String LANGUAGE_CONTROLLER_PATH = "com.devexperts.uilocalizer.languageControllerPath";
    public static final String LOCALIZATION_METHOD_PATH = "com.devexperts.uilocalizer.localizationMethod";
    public static final String OUTPUT_FOLDER = "com.devexperts.uilocalizer.outputFolder";
    private static final String KEY_REGEX = "[a-zA-Z0-9_]+(\\.[a-zA-Z0-9_]+)+";
    private static final String OPTION_APPEND = "com.devexperts.uilocalizer.appendToPropertyFile";

    private JavacProcessingEnvironment javacProcessingEnv;
    private TreeMaker maker;
    private boolean propertyFileAppend;
    private String languageControllerPath;
    private String localizationMethod;
    private Path outputFolder;

    @Override
    public void init(ProcessingEnvironment procEnv) {
        super.init(procEnv);
        this.javacProcessingEnv = (JavacProcessingEnvironment) procEnv;
        this.maker = TreeMaker.instance(javacProcessingEnv.getContext());
        this.languageControllerPath = procEnv.getOptions().get(LANGUAGE_CONTROLLER_PATH);
        this.localizationMethod = procEnv.getOptions().get(LOCALIZATION_METHOD_PATH);
        this.propertyFileAppend = Boolean.parseBoolean(procEnv.getOptions().get(OPTION_APPEND));
        this.outputFolder = Paths.get(Optional.ofNullable(procEnv.getOptions().get(OUTPUT_FOLDER)).orElse("."))
            .toAbsolutePath().normalize();
    }

    @Override
    public Set<String> getSupportedOptions() {
        Set<String> supportedOptions = new HashSet<>();
        supportedOptions.add(LANGUAGE_CONTROLLER_PATH);
        supportedOptions.add(LOCALIZATION_METHOD_PATH);
        supportedOptions.add(OPTION_APPEND);
        supportedOptions.add(OUTPUT_FOLDER);
        return Collections.unmodifiableSet(supportedOptions);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (annotations == null || annotations.isEmpty()) {
            return false;
        }

        final TypeElement localizableAnnotation =
            javacProcessingEnv.getElementUtils().getTypeElement(LOCALIZABLE_ANNOTATION);
        final TypeElement localizationPropertyAnnotation =
            javacProcessingEnv.getElementUtils().getTypeElement(LOCALIZATION_PROPERTY_ANNOTATION);
        if (localizableAnnotation == null && localizationPropertyAnnotation == null) {
            return false;
        }

        boolean created = false;
        if (!Files.exists(outputFolder)) {
            try {
                Files.createDirectories(outputFolder);
                created = true;
            } catch (IOException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "UiLocalizer: " + e.getMessage());
            }
        }
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "UiLocalizer annotation processor triggered. " +
            "Output can be found at " + outputFolder + (created ? " and it was created" : ""));
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "UiLocalizer: Existing property files will be " +
            (propertyFileAppend ? "appended" : "rewritten"));

        if (this.localizationMethod != null) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                "UiLocalizer: localizationMethod is set to " + this.localizationMethod);
             if (this.languageControllerPath != null) {
                 // setting custom languageController with custom localization method is meaningless
                 processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                     "UiLocalizer: languageControllerPath is specified but cannot be used with localizationMethod option - ignored"
                     );
             }
        } else {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                "UiLocalizer: using internal (autogenerated) localization method");

            if (this.languageControllerPath != null) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                    "UiLocalizer: languageControllerPath is set to " + this.languageControllerPath);
            } else {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                    "UiLocalizer: languageControllerPath not set");
            }
        }

        JavacElements utils = javacProcessingEnv.getElementUtils();
        Set<? extends Element> localizableConstants = roundEnv.getElementsAnnotatedWith(localizableAnnotation);
        Set<? extends Element> localizationPropertyConstants =
            roundEnv.getElementsAnnotatedWith(localizationPropertyAnnotation);

        Set<JCTree.JCCompilationUnit> compilationUnits = Collections.newSetFromMap(new IdentityHashMap<>());
        Map<String, String> keysToDefaultValues = new LinkedHashMap<>();

        AstNodeFactory nodeFactory = new AstNodeFactory(maker, utils, languageControllerPath, localizationMethod);
        Collection<JCTree.JCVariableDecl> localizableVariableDeclarations = new ArrayList<>();
        try {
            for (final Element e : localizableConstants) {
                Pair<JCTree, JCTree.JCCompilationUnit> pair = utils.getTreeAndTopLevel(e, null, null);
                JCTree.JCVariableDecl currentField = (JCTree.JCVariableDecl) pair.fst;
                localizableVariableDeclarations.add(currentField);
                JCTree.JCAnnotation currentAnnotation = currentField.getModifiers().getAnnotations()
                    .stream().filter(a -> LOCALIZABLE_ANNOTATION.equals(a.type.toString())).findFirst().get();

                compilationUnits.add(pair.snd);
                String currentKey = getKey(currentAnnotation);
                String currentDefaultValue = getDefaultValue(currentField);
                keysToDefaultValues.put(currentKey, currentDefaultValue);
                injectLocalizationCall(currentField,
                    nodeFactory.getStringMethodInvocation(currentKey, currentDefaultValue));
            }
            for (final Element e : localizationPropertyConstants) {
                Pair<JCTree, JCTree.JCCompilationUnit> pair = utils.getTreeAndTopLevel(e, null, null);
                JCTree.JCVariableDecl currentField = (JCTree.JCVariableDecl) pair.fst;

                compilationUnits.add(pair.snd);
                checkFieldInitializer(currentField);

                JCTree.JCExpression[] arguments;
                if (currentField.getInitializer() instanceof JCTree.JCMethodInvocation) {
                    arguments = getArgumentsOfMethod(currentField);
                } else {
                    arguments = getArgumentsOfConstructor((JCTree.JCNewClass) currentField.getInitializer());
                }
                String currentKey = ((JCTree.JCLiteral) arguments[0]).value.toString().replace("\"", "");
                String currentDefaultValue = ((JCTree.JCLiteral) arguments[1]).value.toString().replace("\"", "");
                keysToDefaultValues.put(currentKey, currentDefaultValue);
            }
            AddLocalizationNodesVisitor visitor = new AddLocalizationNodesVisitor(
                    nodeFactory, localizableVariableDeclarations, languageControllerPath, localizationMethod);
            compilationUnits.forEach(unit -> unit.accept(visitor));
            java.util.List<Map.Entry<String, String>> sortedKeysToDefaultValues = keysToDefaultValues.entrySet()
                .stream()
                .sorted(Comparator.comparing(Map.Entry<String, String>::getKey)
                    .thenComparing(Map.Entry::getValue))
                .collect(Collectors.toList());

            OutputUtil.generatePropertyFiles(outputFolder, sortedKeysToDefaultValues, propertyFileAppend);
            File localizedClassesFile = outputFolder.resolve("localized_classes.txt").toFile();
            OutputUtil.printCompilationUnits(compilationUnits, localizedClassesFile);
        } catch (InvalidUsageException | FileNotFoundException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "UiLocalizer: " + e.getMessage());
        }
        return true;
    }

    private static void injectLocalizationCall(JCTree.JCVariableDecl targetField, JCTree.JCMethodInvocation call)
        throws InvalidUsageException
    {
        if (targetField.init instanceof JCTree.JCLiteral) {
            targetField.init = call;
        } else if (targetField.init instanceof JCTree.JCNewClass) {
            JCTree.JCExpression[] argsArray =
                getArgumentsOfConstructor((JCTree.JCNewClass) targetField.getInitializer());
            int index = findFirstStringLiteral(argsArray);
            argsArray[index] = call;
            ((JCTree.JCNewClass) targetField.init).args = List.from(argsArray);
        } else {
            throw new InvalidUsageException(targetField,
                "Only String constants and enums with String arguments can be localized");
        }
    }

    private static String getKey(JCTree.JCAnnotation annotation) throws InvalidUsageException {
        if (annotation.args.size() != 1) {
            throw new InvalidUsageException(annotation + " must have exactly one key argument");
        }
        String key = (String) ((JCTree.JCLiteral) (((JCTree.JCAssign) annotation.args.get(0)).rhs)).value;
        if (!key.matches(KEY_REGEX)) {
            throw new InvalidUsageException("Argument of " + annotation + " must match regex \"" + KEY_REGEX + "\"");
        }
        return key;
    }

    private static String getDefaultValue(JCTree.JCVariableDecl decl) throws InvalidUsageException {
        if (decl.getInitializer() == null) {
            throw new InvalidUsageException(decl, "Field must be initialized");
        }
        if (decl.getInitializer() instanceof JCTree.JCLiteral) {
            if (!"String".equals(decl.getType().toString()) && !"java.lang.String".equals(decl.getType().toString())) {
                throw new InvalidUsageException(decl, "Type of field must be String");
            }
            if (!decl.getModifiers().getFlags().contains(Modifier.STATIC) ||
                !decl.getModifiers().getFlags().contains(Modifier.FINAL))
            {
                throw new InvalidUsageException(decl, "Field must be static and final");
            }
            return ((JCTree.JCLiteral) decl.getInitializer()).value.toString();
        } else if (decl.getInitializer() instanceof JCTree.JCNewClass) {
            JCTree.JCExpression[] argsArray = getArgumentsOfConstructor((JCTree.JCNewClass) decl.getInitializer());
            int index = findFirstStringLiteral(argsArray);
            if (index != -1) {
                return (String) ((JCTree.JCLiteral) (argsArray[index])).value;
            } else {
                throw new InvalidUsageException(decl, "Calling constructor has no String arguments");
            }
        }
        throw new InvalidUsageException(decl, "Only String constants and enums with String arguments can be localized");
    }

    private void checkFieldInitializer(JCTree.JCVariableDecl decl) throws InvalidUsageException {
        if (decl.getInitializer() == null) {
            throw new InvalidUsageException(decl, "Field must be initialized");
        }
        if (decl.getInitializer() instanceof JCTree.JCMethodInvocation) {
            JCTree.JCMethodInvocation initializer = ((JCTree.JCMethodInvocation)decl.getInitializer());
            if (initializer.getArguments().size() != 2){
                throw new InvalidUsageException(decl, "Only supported: 2 arguments");
            }
            return;
        }

        if (decl.getInitializer() instanceof JCTree.JCNewClass){
            return;
        }
        throw new InvalidUsageException(decl, "Only method and constructor initializers are supported");
    }

    /**
     * @return index of the first element that represents String literal (or null if there's no any)
     */
    private static int findFirstStringLiteral(JCTree.JCExpression[] args) {
        for (int i = 0; i < args.length; i++) {
            JCTree.JCExpression expression = args[i];
            if (expression instanceof JCTree.JCLiteral) {
                JCTree.JCLiteral literal = (JCTree.JCLiteral) expression;
                if (literal.typetag == TypeTag.CLASS && literal.value.getClass() == String.class) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * @return arguments array for method call
     */
    private static JCTree.JCExpression[] getArgumentsOfMethod(JCTree.JCVariableDecl decl) {
        JCTree.JCMethodInvocation initializer = (JCTree.JCMethodInvocation) decl.getInitializer();
        com.sun.tools.javac.util.List<JCTree.JCExpression> args = initializer.args;
        JCTree.JCExpression[] argsArray = new JCTree.JCExpression[args.size()];
        return args.toArray(argsArray);
    }

    /**
     * @return arguments array for constructor call
     */
    private static JCTree.JCExpression[] getArgumentsOfConstructor(JCTree.JCNewClass cons) {
        com.sun.tools.javac.util.List<JCTree.JCExpression> args = cons.args;
        JCTree.JCExpression[] argsArray = new JCTree.JCExpression[args.size()];
        return args.toArray(argsArray);
    }
}
