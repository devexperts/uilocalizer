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

import com.intellij.codeInsight.intention.AddAnnotationFix;
import com.intellij.codeInsight.intention.HighPriorityAction;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.template.Expression;
import com.intellij.codeInsight.template.ExpressionContext;
import com.intellij.codeInsight.template.Result;
import com.intellij.codeInsight.template.Template;
import com.intellij.codeInsight.template.TemplateManager;
import com.intellij.codeInsight.template.TextResult;
import com.intellij.codeInspection.LocalQuickFixBase;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sugakandrey.
 */
class MarkLocalizableQuickFix extends LocalQuickFixBase implements HighPriorityAction {
  private static final Logger LOG = Logger.getInstance("#" + MarkLocalizableQuickFix.class);
  private final Class<?> annotationClass;
  private List<String> packageNameParts = new ArrayList<>();

  MarkLocalizableQuickFix(@NotNull Class<?> annotationClass, List<String> packageNameParts) {
    this(annotationClass, "Mark constant String field as @Localizable", packageNameParts);
  }

  MarkLocalizableQuickFix(@NotNull Class<?> annotationClass, String message, List<String> packageNameParts) {
    super(message);
    this.annotationClass = annotationClass;
    this.packageNameParts = packageNameParts;
  }

  void applyFix(@NotNull Project project, @NotNull PsiField field) {
    new AddAnnotationFix(Localizable.class.getCanonicalName(), field).applyFix();
    PsiClass enclosingClass = PsiTreeUtil.getParentOfType(field, PsiClass.class);
    final PsiFile containingFile = field.getContainingFile();
    final PsiDirectory directory = containingFile.getContainingDirectory();
    PsiPackage enclosingPackage = null;
    if (directory != null) {
      enclosingPackage = JavaDirectoryService.getInstance().getPackage(directory);
    }
    final PsiDocumentManager manager = PsiDocumentManager.getInstance(project);
    final Document document = manager.getDocument(containingFile);
    final TemplateManager templateManager = TemplateManager.getInstance(project);
    final Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
    if (editor != null) {
      if (document != null && document.isWritable()) {
        manager.doPostponedOperationsAndUnblockDocument(document);
        final String fieldName = field.getName();
        final Template template = buildTemplate(templateManager, (fieldName == null) ? "" : fieldName, enclosingClass, enclosingPackage);
        final PsiModifierList modifierList = field.getModifierList();
        if (modifierList != null) {
          final PsiAnnotation localizableAnnotation = modifierList.findAnnotation(annotationClass.getCanonicalName());
          if (localizableAnnotation != null) {
            final int endOffset = localizableAnnotation.getTextRange().getEndOffset();
            editor.getCaretModel().moveToOffset(endOffset);
            templateManager.startTemplate(editor, template);
          }
        } else {
          LOG.error("Failed to annotate field as @Localizable.");
        }
      } else {
        LOG.error("Document is not writable!");
      }
    } else {
      LOG.error("Editor is null!");
    }
  }

  @Override
  public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor problemDescriptor) {
    final PsiElement element = problemDescriptor.getPsiElement();
    PsiField parentField = PsiTreeUtil.getParentOfType(element, PsiField.class);
    if (parentField != null) {
      applyFix(project, parentField);
    }
  }

  @NotNull
  private String buildPropertyKey(@NotNull String fieldName, PsiClass enclosingClass, PsiPackage enclosingPackage) {
    int currentPosition = 0;
    String packageName;
    if (enclosingPackage == null || enclosingPackage.getName() == null) {
      packageName = "";
    } else packageName = enclosingPackage.getQualifiedName();
    String[] parts = packageName.split("\\.");
    while (currentPosition < packageNameParts.size() && currentPosition < parts.length &&
        parts[currentPosition].equals(packageNameParts.get(currentPosition))) {
      currentPosition++;
    }
    StringBuilder sb = new StringBuilder();
    if (currentPosition > 0) {
      for (int i = currentPosition; i < parts.length; i++) {
        sb.append(parts[i]);
        sb.append(".");
      }
      if (enclosingClass != null) {
        sb.append(enclosingClass.getName());
      }
    } else if (enclosingClass != null) {
      sb.append(enclosingClass.getQualifiedName());
    }
    sb.append(".");
    sb.append(toCamelCase(fieldName));
    return sb.toString();
  }

  private static String toCamelCase(String s) {
    StringBuilder sb = new StringBuilder();
    boolean first = true;
    for (String part : s.split("_")) {
      sb.append(first? part.charAt(0) : Character.toUpperCase(part.charAt(0)));
      first = false;
      sb.append(part.substring(1, part.length()).toLowerCase());
    }
    return sb.toString();
  }

  @NotNull
  private Template buildTemplate(TemplateManager templateManager, @NotNull String name,
                                 PsiClass enclosingClass, PsiPackage enclosingPackage) {
    final Template template = templateManager.createTemplate("", "");
    template.setToReformat(true);
    template.addTextSegment("(\"");
    template.addVariable(new AnnotationParameter(buildPropertyKey(name, enclosingClass, enclosingPackage)), true);
    template.addTextSegment("\")");
    template.addEndVariable();
    return template;
  }

  private static class AnnotationParameter extends Expression {
    private final TextResult result;

    private AnnotationParameter(@NotNull String result) {
      this.result = new TextResult(result);
    }

    @Nullable
    @Override
    public Result calculateResult(ExpressionContext expressionContext) {
      return result;
    }

    @Nullable
    @Override
    public Result calculateQuickResult(ExpressionContext expressionContext) {
      return result;
    }

    @Nullable
    @Override
    public LookupElement[] calculateLookupItems(ExpressionContext expressionContext) {
      return LookupElement.EMPTY_ARRAY;
    }
  }
}
