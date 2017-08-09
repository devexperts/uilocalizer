package com.devexperts.uilocalizer;

/*
 * #%L
 * UI Localizer
 * %%
 * Copyright (C) 2015 - 2017 Devexperts, LLC
 * %%
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * #L%
 */


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
import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@SupportedAnnotationTypes({"com.devexperts.uilocalizer.Localizable"})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class LocalizableProcessor extends AbstractProcessor {
    private static final String ANNOTATION_TYPE = "com.devexperts.uilocalizer.Localizable";
    public static final String LANGUAGE_CONTROLLER_PATH = "com.devexperts.uilocalizer.languageControllerPath";
    private static final String KEY_REGEX = "[a-zA-Z0-9_]+(\\.[a-zA-Z0-9_]+)+";
    private static final String OPTION_APPEND = "com.devexperts.uilocalizer.appendToPropertyFile";

    private JavacProcessingEnvironment javacProcessingEnv;
    private TreeMaker maker;
    private boolean propertyFileAppend;
    private String languageControllerPath;

    @Override
    public void init(ProcessingEnvironment procEnv) {
        super.init(procEnv);
        this.javacProcessingEnv = (JavacProcessingEnvironment) procEnv;
        this.maker = TreeMaker.instance(javacProcessingEnv.getContext());
        this.languageControllerPath = procEnv.getOptions().get(LANGUAGE_CONTROLLER_PATH);
        this.propertyFileAppend = Boolean.parseBoolean(procEnv.getOptions().get(OPTION_APPEND));
    }

    @Override
    public Set<String> getSupportedOptions() {
        Set<String> supportedOptions = new HashSet<>();
        supportedOptions.add(LANGUAGE_CONTROLLER_PATH);
        supportedOptions.add(OPTION_APPEND);
        return Collections.unmodifiableSet(supportedOptions);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (annotations == null || annotations.isEmpty()) {
            return false;
        }

        final TypeElement annotation = javacProcessingEnv.getElementUtils().getTypeElement(ANNOTATION_TYPE);
        if (annotation == null) {
            return false;
        }

        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "UiLocalizer annotation processor triggered. " +
            "Output can be found at " + Paths.get(".").toAbsolutePath().normalize().toString());
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "UiLocalizer: Existing property files will be " +
            (propertyFileAppend ? "appended" : "rewritten"));
        if (this.languageControllerPath != null) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                "UiLocalizer: languageControllerPath is set to " + this.languageControllerPath);
        } else {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "UiLocalizer: languageControllerPath not set");
        }
        JavacElements utils = javacProcessingEnv.getElementUtils();
        Set<? extends Element> localizableConstants = roundEnv.getElementsAnnotatedWith(annotation);
        Set<JCTree.JCCompilationUnit> compilationUnits = Collections.newSetFromMap(new IdentityHashMap<>());
        Map<String, String> keysToDefaultValues = new LinkedHashMap<>();
        AstNodeFactory nodeFactory = new AstNodeFactory(maker, utils, languageControllerPath);
        Collection<JCTree.JCVariableDecl> localizableVariableDeclarations = new ArrayList<>();
        try {
            for (final Element e : localizableConstants) {
                Pair<JCTree, JCTree.JCCompilationUnit> pair = utils.getTreeAndTopLevel(e, null, null);
                JCTree.JCVariableDecl currentField = (JCTree.JCVariableDecl) pair.fst;
                localizableVariableDeclarations.add(currentField);
                JCTree.JCAnnotation currentAnnotation = currentField.getModifiers().getAnnotations()
                    .stream().filter(a -> ANNOTATION_TYPE.equals(a.type.toString())).findFirst().get();

                compilationUnits.add(pair.snd);
                String currentKey = getKey(currentAnnotation);
                String currentDefaultValue = getDefaultValue(currentField);
                keysToDefaultValues.put(currentKey, currentDefaultValue);
                injectLocalizationCall(currentField,
                    nodeFactory.getStringMethodInvocation(currentKey, currentDefaultValue));
            }
            AddLocalizationNodesVisitor visitor = new AddLocalizationNodesVisitor(
                    nodeFactory, localizableVariableDeclarations, languageControllerPath);
            compilationUnits.forEach(unit -> unit.accept(visitor));
            OutputUtil.generatePropertyFiles(keysToDefaultValues.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry<String, String>::getKey)
                    .thenComparing(Map.Entry::getValue))
                .collect(Collectors.toList()), propertyFileAppend);
            OutputUtil.printCompilationUnits(compilationUnits, "localized_classes.txt");
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
            JCTree.JCExpression[] argsArray = getArgumentsOf((JCTree.JCNewClass) targetField.getInitializer());
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
            JCTree.JCExpression[] argsArray = getArgumentsOf((JCTree.JCNewClass) decl.getInitializer());
            int index = findFirstStringLiteral(argsArray);
            if (index != -1) {
                return (String) ((JCTree.JCLiteral) (argsArray[index])).value;
            } else {
                throw new InvalidUsageException(decl, "Calling constructor has no String arguments");
            }
        }
        throw new InvalidUsageException(decl, "Only String constants and enums with String arguments can be localized");
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
     * @return arguments array for constructor call
     */
    private static JCTree.JCExpression[] getArgumentsOf(JCTree.JCNewClass cons) {
        com.sun.tools.javac.util.List<JCTree.JCExpression> args = cons.args;
        JCTree.JCExpression[] argsArray = new JCTree.JCExpression[args.size()];
        return args.toArray(argsArray);
    }
}
