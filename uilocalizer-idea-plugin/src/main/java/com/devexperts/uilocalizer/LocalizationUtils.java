/*
 * #%L
 * UI Localizer
 * %%
 * Copyright (C) 2015 - 2021 Devexperts, LLC
 * %%
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * #L%
 */

package com.devexperts.uilocalizer;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.CommonClassNames;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationParameterList;
import com.intellij.psi.PsiAnonymousClass;
import com.intellij.psi.PsiArrayAccessExpression;
import com.intellij.psi.PsiAssertStatement;
import com.intellij.psi.PsiAssignmentExpression;
import com.intellij.psi.PsiCall;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiConditionalExpression;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiEnumConstant;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiLocalVariable;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiNameValuePair;
import com.intellij.psi.PsiNewExpression;
import com.intellij.psi.PsiPackageStatement;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiReturnStatement;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.PsiSwitchLabelStatement;
import com.intellij.psi.PsiSwitchStatement;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeParameter;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.MethodSignature;
import com.intellij.psi.util.MethodSignatureUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.JavaPsiConstructorUtil;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Created by sugakandrey.
 */
class LocalizationUtils {
    @NonNls
    private static final String TO_STRING = "toString";

    private static PsiExpression getTopLevelExpression(@NotNull PsiExpression expression) {
        while (expression.getParent() instanceof PsiExpression) {
            final PsiExpression parent = (PsiExpression) expression.getParent();
            if (parent instanceof PsiConditionalExpression &&
                    ((PsiConditionalExpression) parent).getCondition() == expression) break;
            expression = parent;
            if (expression instanceof PsiAssignmentExpression) break;
        }
        return expression;
    }

    static boolean isPassedToNonNlsVariable(PsiLiteralExpression expression, Set<PsiModifierListOwner> nonNlsTargets, boolean ignoreConstants) {
        PsiExpression toplevel = getTopLevelExpression(expression);
        PsiVariable var = null;
        if (toplevel instanceof PsiAssignmentExpression) {
            PsiExpression lExpression = ((PsiAssignmentExpression) toplevel).getLExpression();
            while (lExpression instanceof PsiArrayAccessExpression) {
                lExpression = ((PsiArrayAccessExpression) lExpression).getArrayExpression();
            }
            if (lExpression instanceof PsiReferenceExpression) {
                final PsiElement resolved = ((PsiReferenceExpression) lExpression).resolve();
                if (resolved instanceof PsiVariable) var = (PsiVariable) resolved;
            }
        }

        if (var == null) {
            PsiElement parent = toplevel.getParent();
            if (parent instanceof PsiVariable && toplevel.equals(((PsiVariable) parent).getInitializer())) {
                var = (PsiVariable) parent;
            } else if (parent instanceof PsiExpressionList) {
                parent = parent.getParent();
                if (parent instanceof PsiSwitchLabelStatement) {
                    final PsiSwitchStatement switchStatement = ((PsiSwitchLabelStatement) parent).getEnclosingSwitchStatement();
                    if (switchStatement != null) {
                        final PsiExpression switchStatementExpression = switchStatement.getExpression();
                        if (switchStatementExpression instanceof PsiReferenceExpression) {
                            final PsiElement resolved = ((PsiReferenceExpression) switchStatementExpression).resolve();
                            if (resolved instanceof PsiVariable) var = (PsiVariable) resolved;
                        }
                    }
                }
            }
        }

        if (var != null) {
            if (annotatedAsNonNls(var)) {
                return true;
            }
            if (ignoreConstants &&
                    var.hasModifierProperty(PsiModifier.STATIC) &&
                    var.hasModifierProperty(PsiModifier.FINAL)) {
                return true;
            }
            nonNlsTargets.add(var);
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    static boolean isArgOfAssertStatement(PsiLiteralExpression expression) {
        return PsiTreeUtil.getParentOfType(expression, PsiAssertStatement.class, PsiClass.class) instanceof PsiAssertStatement;
    }

    @SuppressWarnings("unchecked")
    static boolean isArgOfEnumConstant(PsiLiteralExpression expression) {
        final PsiElement parent = PsiTreeUtil.getParentOfType(expression, PsiExpressionList.class, PsiClass.class);
        return parent instanceof PsiExpressionList &&
                parent.getParent() instanceof PsiEnumConstant;
    }

    static boolean isFromTestPackage(PsiLiteralExpression expression) {
        PsiFile enclosingFile = PsiTreeUtil.getParentOfType(expression, PsiFile.class);
        PsiPackageStatement packageStatement = PsiTreeUtil.getChildOfType(enclosingFile, PsiPackageStatement.class);
        return packageStatement != null && packageStatement.getPackageName().matches(".*\\.test($|\\..*)");
    }

    static boolean isQualifiedClassReference(PsiLiteralExpression expression, String value) {
        if (StringUtil.startsWithChar(value, '#')) {
            value = value.substring(1); // A favor for JetBrains team to catch common Logger usage practice.
        }
        return JavaPsiFacade.getInstance(expression.getProject()).findClass(value, GlobalSearchScope.allScope(expression.getProject())) != null;
    }

    static boolean isToString(PsiLiteralExpression expression) {
        final PsiMethod method = PsiTreeUtil.getParentOfType(expression, PsiMethod.class);
        if (method == null) return false;
        final PsiType returnType = method.getReturnType();
        return TO_STRING.equals(method.getName())
                && method.getParameterList().getParametersCount() == 0
                && returnType != null
                && "java.lang.String".equals(returnType.getCanonicalText());
    }

    @SuppressWarnings("unchecked")
    static boolean isReturnedFromNonNlsMethod(PsiLiteralExpression expression, Set<PsiModifierListOwner> nonNlsTargets) {
        PsiElement parent = expression.getParent();
        PsiMethod method;
        if (parent instanceof PsiNameValuePair) {
            method = AnnotationUtil.getAnnotationMethod((PsiNameValuePair) parent);
        } else {
            final PsiElement returnStatement = PsiTreeUtil.getParentOfType(expression, PsiReturnStatement.class, PsiMethodCallExpression.class);
            if (!(returnStatement instanceof PsiReturnStatement)) {
                return false;
            }
            method = PsiTreeUtil.getParentOfType(expression, PsiMethod.class);
        }
        if (method == null) return false;

        if (AnnotationUtil.isAnnotated(method, AnnotationUtil.NON_NLS, true, false)) {
            return true;
        }
        nonNlsTargets.add(method);
        return false;
    }

    static boolean mustBePropertyKey(@NotNull PsiExpression expression,
        @NotNull Map<String, Object> annotationAttributeValues) {
        final PsiElement parent = expression.getParent();
        if (parent instanceof PsiVariable) {
            final PsiAnnotation annotation = AnnotationUtil.findAnnotation((PsiVariable)parent, AnnotationUtil.PROPERTY_KEY);
            if (annotation != null) {
                return processAnnotationAttributes(annotationAttributeValues, annotation);
            }
        }
        return isPassedToAnnotatedParam(expression, AnnotationUtil.PROPERTY_KEY, annotationAttributeValues, null);
    }

    private static boolean annotatedAsNonNls(final PsiModifierListOwner parent) {
        if (parent instanceof PsiParameter) {
            final PsiParameter parameter = (PsiParameter) parent;
            final PsiElement declarationScope = parameter.getDeclarationScope();
            if (declarationScope instanceof PsiMethod) {
                final PsiMethod method = (PsiMethod) declarationScope;
                final int index = method.getParameterList().getParameterIndex(parameter);
                return isMethodParameterAnnotatedWith(method, index, null, AnnotationUtil.NON_NLS, null, null);
            }
        }
        return AnnotationUtil.isAnnotated(parent, AnnotationUtil.NON_NLS, false, false);
    }

    private static boolean isMethodParameterAnnotatedWith(final PsiMethod method,
                                                          final int idx,
                                                          @Nullable Collection<PsiMethod> processed,
                                                          final String annFqn,
                                                          @Nullable Map<String, Object> annotationAttributeValues,
                                                          @Nullable final Set<PsiModifierListOwner> nonNlsTargets) {
        if (processed != null) {
            if (processed.contains(method)) return false;
        } else {
            processed = new THashSet<>();
        }
        processed.add(method);

        final PsiParameter[] params = method.getParameterList().getParameters();
        PsiParameter param;
        if (idx >= params.length) {
            if (params.length == 0) {
                return false;
            }
            PsiParameter lastParam = params[params.length - 1];
            if (lastParam.isVarArgs()) {
                param = lastParam;
            } else {
                return false;
            }
        } else {
            param = params[idx];
        }
        final PsiAnnotation annotation = AnnotationUtil.findAnnotation(param, annFqn);
        if (annotation != null) {
            return processAnnotationAttributes(annotationAttributeValues, annotation);
        }
        if (nonNlsTargets != null) {
            nonNlsTargets.add(param);
        }

        final PsiMethod[] superMethods = method.findSuperMethods();
        for (PsiMethod superMethod : superMethods) {
            if (isMethodParameterAnnotatedWith(superMethod, idx, processed, annFqn, annotationAttributeValues, null))
                return true;
        }

        return false;
    }

    private static boolean processAnnotationAttributes(@Nullable Map<String, Object> annotationAttributeValues, @NotNull PsiAnnotation annotation) {
        if (annotationAttributeValues != null) {
            final PsiAnnotationParameterList parameterList = annotation.getParameterList();
            final PsiNameValuePair[] attributes = parameterList.getAttributes();
            for (PsiNameValuePair attribute : attributes) {
                final String name = attribute.getName();
                if (annotationAttributeValues.containsKey(name)) {
                    annotationAttributeValues.put(name, attribute.getValue());
                }
            }
        }
        return true;
    }

    static boolean isInNonNlsEquals(PsiExpression expression, final Set<PsiModifierListOwner> nonNlsTargets) {
        if (!(expression.getParent().getParent() instanceof PsiMethodCallExpression)) {
            return false;
        }
        final PsiMethodCallExpression call = (PsiMethodCallExpression) expression.getParent().getParent();
        final PsiReferenceExpression methodExpression = call.getMethodExpression();
        final PsiExpression qualifier = methodExpression.getQualifierExpression();
        if (qualifier != expression) {
            return false;
        }
        if (!"equals".equals(methodExpression.getReferenceName())) {
            return false;
        }
        final PsiElement resolved = methodExpression.resolve();
        if (!(resolved instanceof PsiMethod)) {
            return false;
        }
        PsiType objectType = PsiType.getJavaLangObject(resolved.getManager(), resolved.getResolveScope());
        MethodSignature equalsSignature = MethodSignatureUtil.createMethodSignature("equals",
                new PsiType[]{objectType},
                PsiTypeParameter.EMPTY_ARRAY,
                PsiSubstitutor.EMPTY);
        if (!equalsSignature.equals(((PsiMethod) resolved).getSignature(PsiSubstitutor.EMPTY))) {
            return false;
        }
        final PsiExpression[] expressions = call.getArgumentList().getExpressions();
        if (expressions.length != 1) {
            return false;
        }
        final PsiExpression arg = expressions[0];
        PsiReferenceExpression ref = null;
        if (arg instanceof PsiReferenceExpression) {
            ref = (PsiReferenceExpression) arg;
        } else if (arg instanceof PsiMethodCallExpression) ref = ((PsiMethodCallExpression) arg).getMethodExpression();
        if (ref != null) {
            final PsiElement resolvedEntity = ref.resolve();
            if (resolvedEntity instanceof PsiModifierListOwner) {
                PsiModifierListOwner modifierListOwner = (PsiModifierListOwner) resolvedEntity;
                if (annotatedAsNonNls(modifierListOwner)) {
                    return true;
                }
                nonNlsTargets.add(modifierListOwner);
            }
        }
        return false;
    }

    static boolean isInNonNlsMethodCall(@NotNull PsiExpression expression,
                                        final Set<PsiModifierListOwner> nonNlsTargets) {
        expression = getTopLevelExpression(expression);
        final PsiElement parent = expression.getParent();
        if (parent instanceof PsiExpressionList) {
            final PsiElement grParent = parent.getParent();
            if (grParent instanceof PsiMethodCallExpression) {
                return isNonNlsMethodCall((PsiMethodCallExpression) grParent, nonNlsTargets);
            } else if (grParent instanceof PsiNewExpression) {
                final PsiElement parentOfNew = grParent.getParent();
                if (parentOfNew instanceof PsiLocalVariable) {
                    final PsiLocalVariable newVariable = (PsiLocalVariable) parentOfNew;
                    if (annotatedAsNonNls(newVariable)) {
                        return true;
                    }
                    nonNlsTargets.add(newVariable);
                    return false;
                } else if (parentOfNew instanceof PsiAssignmentExpression) {
                    final PsiExpression lExpression = ((PsiAssignmentExpression) parentOfNew).getLExpression();
                    if (lExpression instanceof PsiReferenceExpression) {
                        final PsiElement resolved = ((PsiReferenceExpression) lExpression).resolve();
                        if (resolved instanceof PsiModifierListOwner) {
                            final PsiModifierListOwner modifierListOwner = (PsiModifierListOwner) resolved;
                            if (annotatedAsNonNls(modifierListOwner)) {
                                return true;
                            }
                            nonNlsTargets.add(modifierListOwner);
                            return false;
                        }
                    }
                }
            }
        }
        return false;
    }

    private static boolean isNonNlsMethodCall(PsiMethodCallExpression grParent, Set<PsiModifierListOwner> nonNlsTargets) {
        final PsiReferenceExpression methodExpression = grParent.getMethodExpression();
        final PsiExpression qualifier = methodExpression.getQualifierExpression();
        if (qualifier instanceof PsiReferenceExpression) {
            final PsiElement resolved = ((PsiReferenceExpression) qualifier).resolve();
            if (resolved instanceof PsiModifierListOwner) {
                final PsiModifierListOwner modifierListOwner = (PsiModifierListOwner) resolved;
                if (annotatedAsNonNls(modifierListOwner)) {
                    return true;
                }
                nonNlsTargets.add(modifierListOwner);
                return false;
            }
        } else if (qualifier instanceof PsiMethodCallExpression) {
            final PsiType type = qualifier.getType();
            final PsiType methodExpressionType = methodExpression.getType();
            if (type != null? type.equals(methodExpressionType) : methodExpressionType == null) {
                return isNonNlsMethodCall((PsiMethodCallExpression) qualifier, nonNlsTargets);
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    static boolean isArgOfExceptionConstructor(PsiLiteralExpression expression) {
        final PsiElement parent = PsiTreeUtil.getParentOfType(expression, PsiExpressionList.class, PsiClass.class);
        if (!(parent instanceof PsiExpressionList)) {
            return false;
        }
        final PsiElement grandparent = parent.getParent();
        final PsiClass aClass;
        if (JavaPsiConstructorUtil.isSuperConstructorCall(grandparent)) {
            final PsiMethod method = ((PsiMethodCallExpression) grandparent).resolveMethod();
            if (method != null) {
                aClass = method.getContainingClass();
            } else {
                return false;
            }
        } else {
            if (!(grandparent instanceof PsiNewExpression)) {
                return false;
            }
            final PsiJavaCodeReferenceElement reference = ((PsiNewExpression) grandparent).getClassReference();
            if (reference == null) {
                return false;
            }
            final PsiElement referent = reference.resolve();
            if (!(referent instanceof PsiClass)) {
                return false;
            }

            aClass = (PsiClass) referent;
        }
        final Project project = expression.getProject();
        final GlobalSearchScope scope = GlobalSearchScope.allScope(project);
        final PsiClass throwable = JavaPsiFacade.getInstance(project).findClass(CommonClassNames.JAVA_LANG_THROWABLE, scope);
        assert aClass != null;
        return throwable != null && aClass.isInheritor(throwable, true);
    }

    @SuppressWarnings("unchecked")
    static boolean isArgOfSpecifiedExceptionConstructor(PsiLiteralExpression expression, String[] specifiedExceptions) {
        if (specifiedExceptions.length == 0) return false;

        final PsiElement parent = PsiTreeUtil.getParentOfType(expression, PsiExpressionList.class, PsiClass.class);
        if (!(parent instanceof PsiExpressionList)) {
            return false;
        }
        final PsiElement grandparent = parent.getParent();
        if (!(grandparent instanceof PsiNewExpression)) {
            return false;
        }
        final PsiJavaCodeReferenceElement reference =
                ((PsiNewExpression) grandparent).getClassReference();
        if (reference == null) {
            return false;
        }
        final PsiElement referent = reference.resolve();
        if (!(referent instanceof PsiClass)) {
            return false;
        }
        final PsiClass aClass = (PsiClass) referent;

        for (String specifiedException : specifiedExceptions) {
            if (specifiedException.equals(aClass.getQualifiedName())) return true;

        }

        return false;
    }

    static boolean isPassedToAnnotatedParam(@NotNull PsiExpression expression,
                                            final String annFqn,
                                            @Nullable Map<String, Object> annotationAttributeValues,
                                            @Nullable final Set<PsiModifierListOwner> nonNlsTargets) {
        expression = getTopLevelExpression(expression);
        final PsiElement parent = expression.getParent();

        if (!(parent instanceof PsiExpressionList)) return false;
        int idx = -1;
        final PsiExpression[] args = ((PsiExpressionList) parent).getExpressions();
        for (int i = 0; i < args.length; i++) {
            PsiExpression arg = args[i];
            if (PsiTreeUtil.isAncestor(arg, expression, false)) {
                idx = i;
                break;
            }
        }
        if (idx == -1) return false;
        PsiElement grParent = parent.getParent();

        if (grParent instanceof PsiAnonymousClass) {
            grParent = grParent.getParent();
        }

        if (grParent instanceof PsiCall) {
            PsiMethod method = ((PsiCall) grParent).resolveMethod();
            if (method != null && isMethodParameterAnnotatedWith(method, idx, null, annFqn, annotationAttributeValues, nonNlsTargets)) {
                return true;
            }
        }
        return false;
    }

    static boolean isOnlyLiteralOfEnum(PsiLiteralExpression literal) {
        if (!(literal.getValue() instanceof String))
            return false;

        PsiElement parent = literal.getParent();
        if (!(parent instanceof PsiExpressionList))
            return false;

        PsiExpressionList expressionList = (PsiExpressionList)parent;
        parent = expressionList.getParent();
        if (!(parent instanceof PsiEnumConstant))
            return false;

        int stringLiteralCount = 0;
        for (PsiExpression expression : expressionList.getExpressions()) {
            if (expression instanceof PsiLiteralExpression) {
                PsiLiteralExpression nextLiteral = (PsiLiteralExpression) expression;
                if (nextLiteral.getValue() instanceof String)
                    stringLiteralCount++;
            }
        }
        return stringLiteralCount == 1;
    }

    static boolean isFirstLiteralOfLocalizableField(PsiLiteralExpression literal) {
        if (!(literal.getValue() instanceof String))
            return false;
        PsiField field = PsiTreeUtil.getParentOfType(literal, PsiField.class);
        if (field == null || !AnnotationUtil.isAnnotated(field, Localizable.class.getCanonicalName(), false)) {
            return false;
        }

        PsiElement parent = literal.getParent();
        if (!(parent instanceof PsiExpressionList))
            return true;

        PsiExpressionList expressionList = (PsiExpressionList)parent;

        for (PsiExpression expression : expressionList.getExpressions()) {
            if (expression instanceof PsiLiteralExpression) {
                PsiLiteralExpression nextLiteral = (PsiLiteralExpression) expression;
                if (nextLiteral.getValue() instanceof String) {
                    return nextLiteral.equals(literal);
                }
            }
        }
        return false;
    }
}
