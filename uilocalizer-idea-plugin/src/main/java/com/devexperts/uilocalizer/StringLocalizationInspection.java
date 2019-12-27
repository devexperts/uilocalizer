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

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInsight.daemon.GroupNames;
import com.intellij.codeInsight.intention.AddAnnotationFix;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.compiler.RemoveElementQuickFix;
import com.intellij.codeInspection.i18n.I18nInspection;
import com.intellij.ide.util.TreeClassChooser;
import com.intellij.ide.util.TreeClassChooserFactory;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.JavaRecursiveElementVisitor;
import com.intellij.psi.PsiAnonymousClass;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassInitializer;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiEnumConstant;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.impl.source.PsiFieldImpl;
import com.intellij.psi.impl.source.tree.java.PsiLocalVariableImpl;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.refactoring.introduceField.IntroduceConstantHandler;
import com.intellij.ui.AddDeleteListPanel;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.FieldPanel;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.util.containers.ContainerUtil;
import gnu.trove.THashSet;
import org.jdom.Element;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.DocumentEvent;


/**
 * Created by sugakandrey.
 */
public class StringLocalizationInspection extends I18nInspection {
  private final ResourceBundle bundle;
  // Fields are public on purpose: public fields are automatically serialized by IDEA into configuration xml file.
  @SuppressWarnings({"WeakerAccess"})
  public boolean ignoreExceptionConstructors = true;
  @SuppressWarnings({"WeakerAccess"})
  public boolean ignoreTestPackages = true;
  @SuppressWarnings({"WeakerAccess"})
  public boolean ignoreQualifiedClassReferences = true;
  @SuppressWarnings({"WeakerAccess"})
  public boolean ignoreAssertStatements = true;
  @SuppressWarnings({"WeakerAccess"})
  public boolean ignoreNumericalStrings = true;
  @SuppressWarnings({"WeakerAccess"})
  public boolean ignoreConstants = false;
  @SuppressWarnings({"WeakerAccess"})
  public boolean ignoreToStringContents = false;
  @SuppressWarnings({"WeakerAccess"})
  public boolean ignoreEnums = false;
  @SuppressWarnings({"WeakerAccess"})
  public boolean trimPackageBeginning = false;

  @NonNls
  @SuppressWarnings({"WeakerAccess"})
  public String packagePrefix = "";
  @NonNls
  @SuppressWarnings({"WeakerAccess"})
  public String specificIgnoredExceptionConstructors = "";

  public StringLocalizationInspection() {
    bundle = ResourceBundle.getBundle("LocalizationInspection");
  }

  void setNonNlsPattern(String pattern) {
    super.nonNlsCommentPattern = pattern;
    nonNlsCommentPattern = pattern;
  }

  @Override
  @NotNull
  public String getGroupDisplayName() {
    return GroupNames.INTERNATIONALIZATION_GROUP_NAME;
  }

  @NotNull
  @Override
  public String getDisplayName() {
    return bundle.getString("inspection.localization.display.name");
  }

  @NotNull
  @Override
  public String getShortName() {
    return "NonLocalizedString";
  }

  private static final String SKIP_FOR_ENUM = "ignoreForEnumConstant";

  @Override
  public void writeSettings(@NotNull Element node) throws WriteExternalException {
    super.writeSettings(node);
    if (ignoreEnums) {
      final Element e = new Element("option");
      e.setAttribute("name", SKIP_FOR_ENUM);
      e.setAttribute("value", Boolean.toString(ignoreEnums));
      node.addContent(e);
    }
  }

  @Override
  public void readSettings(@NotNull Element node) throws InvalidDataException {
    super.readSettings(node);
    for (Object o : node.getChildren()) {
      if (o instanceof Element && Comparing.strEqual(node.getAttributeValue("name"), SKIP_FOR_ENUM)) {
        final String ignoreForConstantsAttr = node.getAttributeValue("value");
        if (ignoreForConstantsAttr != null) {
          ignoreEnums = Boolean.parseBoolean(ignoreForConstantsAttr);
        }
        break;
      }
    }
  }


  @Nullable
  @Override
  public JComponent createOptionsPanel() {
    final GridBagLayout layout = new GridBagLayout();
    final JPanel panel = new JPanel(layout);
    final JCheckBox assertStatementsCheckbox = new JCheckBox(bundle.getString("inspection.localization.option.ignore.assert"), ignoreAssertStatements);
    assertStatementsCheckbox.addChangeListener(e -> ignoreAssertStatements = assertStatementsCheckbox.isSelected());

    final JCheckBox exceptionConstructorCheck =
        new JCheckBox(bundle.getString("inspection.localization.option.ignore.for.exception.constructor.arguments"),
            ignoreExceptionConstructors);
    exceptionConstructorCheck.addChangeListener(e -> ignoreExceptionConstructors = exceptionConstructorCheck.isSelected());

    final JTextField specifiedExceptions = new JTextField(specificIgnoredExceptionConstructors);
    specifiedExceptions.getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(DocumentEvent e) {
        specificIgnoredExceptionConstructors = specifiedExceptions.getText();
      }
    });

    final JCheckBox testPackagesCheckbox = new JCheckBox(
        bundle.getString("inspection.localization.option.ignore.for.junit.assert.arguments"), ignoreTestPackages);
    testPackagesCheckbox.addChangeListener(e -> ignoreTestPackages = testPackagesCheckbox.isSelected());

    final JCheckBox classRef = new JCheckBox(bundle.getString("inspection.localization.option.ignore.qualified.class.names"), ignoreQualifiedClassReferences);
    classRef.addChangeListener(e -> ignoreQualifiedClassReferences = classRef.isSelected());

    final JCheckBox nonAlpha = new JCheckBox(bundle.getString("inspection.localization.option.ignore.nonalphanumerics"), ignoreNumericalStrings);
    nonAlpha.addChangeListener(e -> ignoreNumericalStrings = nonAlpha.isSelected());

    final JCheckBox assignedToConstants = new JCheckBox(bundle.getString("inspection.localization.option.ignore.assigned.to.constants"), ignoreConstants);
    assignedToConstants.addChangeListener(e -> ignoreConstants = assignedToConstants.isSelected());

    final JCheckBox chkToString = new JCheckBox(bundle.getString("inspection.localization.option.ignore.tostring"), ignoreToStringContents);
    chkToString.addChangeListener(e -> ignoreToStringContents = chkToString.isSelected());

    final JCheckBox ignoreEnumConstants = new JCheckBox(bundle.getString("inspection.localization.option.ignore.enums"), ignoreEnums);
    ignoreEnumConstants.addChangeListener(e -> ignoreEnums = ignoreEnumConstants.isSelected());

    final JCheckBox trimPackageCheckbox = new JCheckBox(bundle.getString("inspection.localization.option.trim.packages"), trimPackageBeginning);
    trimPackageCheckbox.addChangeListener(e -> trimPackageBeginning = trimPackageCheckbox.isSelected());

    final GridBagConstraints gc = new GridBagConstraints();
    gc.fill = GridBagConstraints.HORIZONTAL;
    gc.insets.bottom = 2;

    gc.gridx = GridBagConstraints.REMAINDER;
    gc.gridy = 0;
    gc.weightx = 1;
    gc.weighty = 0;
    panel.add(assertStatementsCheckbox, gc);

    gc.gridy++;
    panel.add(testPackagesCheckbox, gc);

    gc.gridy++;
    panel.add(exceptionConstructorCheck, gc);

    gc.gridy++;
    final Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
    panel.add(new FieldPanel(specifiedExceptions,
        null,
        bundle.getString("inspection.localization.option.ignore.for.specified.exception.constructor.arguments"),
        openProjects.length == 0 ? null :
            (ActionListener) e -> createIgnoreExceptionsConfigurationDialog(openProjects[0], specifiedExceptions).show(),
        null), gc);

    final JTextField packagePrefixTextField = new JTextField(packagePrefix);
    gc.gridy++;
    panel.add(trimPackageCheckbox, gc);

    gc.gridy++;
    gc.anchor = GridBagConstraints.NORTHWEST;
    gc.weighty = 1;
    panel.add(new FieldPanel(packagePrefixTextField,
        bundle.getString("inspection.localization.option.trim.packages.label.text"),
        bundle.getString("inspection.localization.option.trim.packages.dialog.title"),
        null, () -> packagePrefix = packagePrefixTextField.getText()), gc);

    gc.gridy++;
    panel.add(classRef, gc);

    gc.gridy++;
    panel.add(assignedToConstants, gc);

    gc.gridy++;
    panel.add(chkToString, gc);

    gc.gridy++;
    panel.add(nonAlpha, gc);

    gc.gridy++;
    panel.add(ignoreEnumConstants, gc);

    gc.gridy++;
    final JTextField text = new JTextField(nonNlsCommentPattern);
    final FieldPanel nonNlsCommentPatternComponent =
        new FieldPanel(text, bundle.getString("inspection.localization.option.ignore.comment.pattern"),
            bundle.getString("inspection.localization.option.ignore.comment.title"), null, () -> {
          setNonNlsPattern(text.getText());
        });
    panel.add(nonNlsCommentPatternComponent, gc);

    final JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(panel);
    scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
    scrollPane.setBorder(null);
    scrollPane.setPreferredSize(new Dimension(panel.getPreferredSize().width + scrollPane.getVerticalScrollBar().getPreferredSize().width,
        panel.getPreferredSize().height +
            scrollPane.getHorizontalScrollBar().getPreferredSize().height));
    return scrollPane;
  }

  @SuppressWarnings("NonStaticInitializer")
  private DialogWrapper createIgnoreExceptionsConfigurationDialog(final Project project, final JTextField specifiedExceptions) {
    return new DialogWrapper(true) {
      private AddDeleteListPanel myPanel;

      {
        setTitle(bundle.getString(
            "inspection.localization.option.ignore.for.specified.exception.constructor.arguments"));
        init();
      }

      @Override
      protected JComponent createCenterPanel() {
        final String[] ignored = specificIgnoredExceptionConstructors.split(",");
        final List<String> initialList = new ArrayList<>();
        for (String e : ignored) {
          if (!e.isEmpty()) initialList.add(e);
        }
        myPanel = new AddDeleteListPanel<String>(null, initialList) {
          @Override
          protected String findItemToAdd() {
            final GlobalSearchScope scope = GlobalSearchScope.allScope(project);
            TreeClassChooser chooser = TreeClassChooserFactory.getInstance(project).
                createInheritanceClassChooser(
                    bundle.getString("inspection.localization.option.ignore.for.specified.exception.constructor.arguments"), scope,
                    JavaPsiFacade.getInstance(project).findClass("java.lang.Throwable", scope), true, true, null);
            chooser.showDialog();
            PsiClass selectedClass = chooser.getSelected();
            return selectedClass != null ? selectedClass.getQualifiedName() : null;
          }
        };
        return myPanel;
      }

      @Override
      protected void doOKAction() {
        StringBuilder buf = new StringBuilder();
        final Object[] exceptions = myPanel.getListItems();
        for (Object exception : exceptions) {
          buf.append(",").append(exception);
        }
        specifiedExceptions.setText(buf.length() > 0 ? buf.substring(1) : buf.toString());
        super.doOKAction();
      }
    };
  }

  @Nullable
  @Override
  public ProblemDescriptor[] checkMethod(@NotNull PsiMethod method, @NotNull InspectionManager manager, boolean isOnTheFly) {
    final PsiClass containingClass = method.getContainingClass();
    if (containingClass != null && (isNonNlsAnnotated(containingClass) || isNonNlsAnnotated(method))) {
      return ProblemDescriptor.EMPTY_ARRAY;
    }
    final PsiCodeBlock body = method.getBody();
    if (body != null) {
      return checkElement(body, manager, isOnTheFly);
    }
    return ProblemDescriptor.EMPTY_ARRAY;
  }

  @Nullable
  @Override
  public ProblemDescriptor[] checkClass(@NotNull PsiClass aClass, @NotNull InspectionManager manager, boolean isOnTheFly) {
    final PsiClass containingClass = aClass.getContainingClass();
    if (containingClass != null && isNonNlsAnnotated(containingClass)) {
      return ProblemDescriptor.EMPTY_ARRAY;
    }
    if (isNonNlsAnnotated(aClass)) {
      return ProblemDescriptor.EMPTY_ARRAY;
    }
    final PsiClassInitializer[] initializers = aClass.getInitializers();
    final ArrayList<ProblemDescriptor> problems = new ArrayList<>();
    Arrays.stream(initializers)
        .map(psiClassInitializer -> checkElement(psiClassInitializer.getBody(), manager, isOnTheFly))
        .filter(problemDescriptors -> problemDescriptors != null)
        .forEach(problem -> ContainerUtil.addAll(problems, problem));
    return problems.isEmpty() ? ProblemDescriptor.EMPTY_ARRAY : problems.toArray(new ProblemDescriptor[problems.size()]);
  }

  private static boolean isNonNlsAnnotated(@NotNull PsiMethod aMethod) {
      return AnnotationUtil.isAnnotated(aMethod, AnnotationUtil.NON_NLS, false, false);
  }

  private static boolean isNonNlsAnnotated(@NotNull PsiClass aClass) {
    PsiClass parentClass = PsiTreeUtil.getParentOfType(aClass, PsiClass.class);
    if (parentClass != null && isNonNlsAnnotated(parentClass)){
      return true;
    }
    final PsiDirectory directory = aClass.getContainingFile().getContainingDirectory();
    if (directory != null) {
      return AnnotationUtil.isAnnotated(aClass, AnnotationUtil.NON_NLS, false, false)
          || isNonNlsAnnotated(JavaDirectoryService.getInstance().getPackage(directory));
    } else {
      return AnnotationUtil.isAnnotated(aClass, AnnotationUtil.NON_NLS, false, false);
    }
  }

  private ProblemDescriptor[] checkFieldLegallyLocalized(PsiClass containingClass, PsiField field, InspectionManager manager, boolean isOnTheFly) {
    if (containingClass.isInterface()) {
      String errorMessage = bundle.getString("inspection.localization.message.interface");
      String fixMessage = String.format(bundle.getString("inspection.localization.hotfix.interface"), field.getName());
      return new ProblemDescriptor[]{manager.createProblemDescriptor(field, errorMessage, isOnTheFly,
              new LocalQuickFix[]{new RemoveElementQuickFix(fixMessage)}, ProblemHighlightType.GENERIC_ERROR)};
    }

    PsiClass enclosingClass = containingClass.getContainingClass();
    if (enclosingClass == null)
      return ProblemDescriptor.EMPTY_ARRAY;

    PsiModifierList innerModifierList = containingClass.getModifierList();
    if (innerModifierList == null)
      return ProblemDescriptor.EMPTY_ARRAY;

    if (innerModifierList.hasModifierProperty(PsiModifier.STATIC))
      return ProblemDescriptor.EMPTY_ARRAY;

    LocalQuickFix quickFix = new LocalQuickFix() {
      @Nls
      @NotNull
      @Override
      public String getName() {
        return String.format(bundle.getString("inspection.localization.hotfix.inner.class"), field.getName());
      }

      @Nls
      @NotNull
      @Override
      public String getFamilyName() {
        return getName();
      }

      @Override
      public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
        enclosingClass.add(field);
        field.delete();
      }
    };
    String message = bundle.getString("inspection.localization.message.inner.class");
    return new ProblemDescriptor[]{manager.createProblemDescriptor(field, message, isOnTheFly,
            new LocalQuickFix[]{quickFix}, ProblemHighlightType.GENERIC_ERROR)};
  }

  @Nullable
  @Override
  public ProblemDescriptor[] checkField(@NotNull PsiField field, @NotNull InspectionManager manager, boolean isOnTheFly) {
    final PsiClass containingClass = field.getContainingClass();
    if (containingClass != null && isNonNlsAnnotated(containingClass)) {
      return ProblemDescriptor.EMPTY_ARRAY;
    }
    if (AnnotationUtil.isAnnotated(field, Localizable.class.getCanonicalName(), false, false)) {
      ProblemDescriptor[] descriptors = checkFieldLegallyLocalized(containingClass, field, manager, isOnTheFly);
      if (descriptors.length != 0) {
        return descriptors;
      }
    }
    if (AnnotationUtil.isAnnotated(field, AnnotationUtil.NON_NLS, false, false)) {
      return ProblemDescriptor.EMPTY_ARRAY;
    }
    if (field instanceof PsiEnumConstant) {
      final PsiExpressionList argumentList = ((PsiEnumConstant) field).getArgumentList();
      if (argumentList == null) return ProblemDescriptor.EMPTY_ARRAY;
      return checkElement(argumentList, manager, isOnTheFly);
    }
    final PsiExpression initializer = field.getInitializer();
    if (initializer != null) {
      return checkElement(initializer, manager, isOnTheFly);
    }
    return ProblemDescriptor.EMPTY_ARRAY;
  }

  private static boolean isNonNlsAnnotated(final PsiPackage psiPackage) {
    return !(psiPackage == null || psiPackage.getName() == null) &&
        (AnnotationUtil.isAnnotated(psiPackage, AnnotationUtil.NON_NLS, false, false) ||
            isNonNlsAnnotated(psiPackage.getParentPackage()));
  }

  private static boolean isConstantFieldInitializer(@NotNull final PsiExpression expression) {
    final PsiElement parent = expression.getParent();
    if (!(parent instanceof PsiField)) {
      return false;
    }
    final PsiField parentField = (PsiField) parent;
    return parentField.hasModifierProperty(PsiModifier.STATIC) &&
        parentField.hasModifierProperty(PsiModifier.FINAL) &&
        expression == parentField.getInitializer();
  }

  @Nullable
  @Override
  public ProblemDescriptor[] checkFile(@NotNull PsiFile file, @NotNull InspectionManager manager, boolean isOnTheFly) {
    return ProblemDescriptor.EMPTY_ARRAY;
  }

  private ProblemDescriptor[] checkElement(@NotNull PsiElement element, @NotNull InspectionManager manager, boolean isOnTheFly) {
    StringLocalizationVisitor visitor = new StringLocalizationVisitor(manager, isOnTheFly);
    element.accept(visitor);
    List<ProblemDescriptor> problems = visitor.getProblems();
    return problems.isEmpty() ? ProblemDescriptor.EMPTY_ARRAY : problems.toArray(new ProblemDescriptor[problems.size()]);
  }

  private static boolean isAnnotationInProjectScope(@NotNull Class<?> aClass, @NotNull PsiModifierListOwner target, @NotNull Project project) {
    final JavaPsiFacade facade = JavaPsiFacade.getInstance(project);
    return !target.getManager().isInProject(target) || facade.findClass(aClass.getName(), target.getResolveScope()) != null;
  }

  private boolean shouldBeLocalized(@NotNull Project project, @NotNull PsiLiteralExpression expression,
                                    @NotNull String stringValue) {
    Set<PsiModifierListOwner> nonNlsTargets = new THashSet<>();
    if (LocalizationUtils.isFirstLiteralOfLocalizableField(expression)){
      return false;
    }

    if (ignoreNumericalStrings && !StringUtil.containsAlphaCharacters(stringValue)) {
      return false;
    }

    if (LocalizationUtils.isPassedToAnnotatedParam(expression, AnnotationUtil.NON_NLS, new HashMap<>(), nonNlsTargets)) {
      return false;
    }

    if (LocalizationUtils.isInNonNlsEquals(expression, nonNlsTargets)) {
      return false;
    }

    if (LocalizationUtils.isPassedToNonNlsVariable(expression, nonNlsTargets, ignoreConstants)) {
      return false;
    }

    if (LocalizationUtils.mustBePropertyKey(expression, new HashMap<>())) {
      return false;
    }

    if (LocalizationUtils.isInNonNlsMethodCall(expression, nonNlsTargets)) {
      return false;
    }

    if (LocalizationUtils.isReturnedFromNonNlsMethod(expression, nonNlsTargets)) {
      return false;
    }

    if (ignoreAssertStatements && LocalizationUtils.isArgOfAssertStatement(expression)) {
      return false;
    }

    if (ignoreExceptionConstructors && LocalizationUtils.isArgOfExceptionConstructor(expression)) {
      return false;
    }

    if (ignoreEnums && LocalizationUtils.isArgOfEnumConstant(expression)) {
      return false;
    }

    if (!ignoreExceptionConstructors &&
        LocalizationUtils.isArgOfSpecifiedExceptionConstructor(expression, specificIgnoredExceptionConstructors.split(","))) {
      return false;
    }

    if (ignoreTestPackages && LocalizationUtils.isFromTestPackage(expression)) {
      return false;
    }

    if (ignoreQualifiedClassReferences && LocalizationUtils.isQualifiedClassReference(expression, stringValue)) {
      return false;
    }

    if (ignoreToStringContents && LocalizationUtils.isToString(expression)) {
      return false;
    }

    Pattern pattern = nonNlsCommentPattern.trim().isEmpty() ? null : Pattern.compile(nonNlsCommentPattern);
    if (pattern != null) {
      PsiFile file = expression.getContainingFile();
      Document document = PsiDocumentManager.getInstance(project).getDocument(file);
      if (document != null) {
        int line = document.getLineNumber(expression.getTextRange().getStartOffset());
        int lineStartOffset = document.getLineStartOffset(line);
        CharSequence lineText = document.getCharsSequence().subSequence(lineStartOffset, document.getLineEndOffset(line));
        Matcher matcher = pattern.matcher(lineText);
        int start = 0;

        while (matcher.find(start)) {
          start = matcher.start();
          PsiElement element = file.findElementAt(lineStartOffset + start);
          if (PsiTreeUtil.getParentOfType(element, PsiComment.class, false) != null) return false;
          if (start == lineText.length() - 1) break;
          start++;
        }
      }
    }

    return true;
  }

  private class StringLocalizationVisitor extends JavaRecursiveElementVisitor {
    private final List<ProblemDescriptor> problems = new ArrayList<>();
    private final InspectionManager inspectionManager;
    private final boolean isOnTheFly;

    private StringLocalizationVisitor(@NotNull InspectionManager inspectionManager, boolean isOnTheFly) {
      this.inspectionManager = inspectionManager;
      this.isOnTheFly = isOnTheFly;
    }

    List<ProblemDescriptor> getProblems() {
      return problems;
    }

    @Override
    public void visitAnonymousClass(PsiAnonymousClass aClass) {
      visitElement(aClass);
    }

    @Override
    public void visitClass(PsiClass aClass) {
    }

    @Override
    public void visitField(PsiField field) {
    }

    @Override
    public void visitMethod(PsiMethod method) {
    }

    @Override
    public void visitClassInitializer(PsiClassInitializer initializer) {
    }

    @Override
    public void visitLiteralExpression(PsiLiteralExpression expression) {
      Object expressionValue = expression.getValue();
      if (!(expressionValue instanceof String)) return;

      String stringValue = (String) expressionValue;
      if (stringValue.trim().isEmpty()) {
        return;
      }

      if (shouldBeLocalized(inspectionManager.getProject(), expression, stringValue) &&
          !isNonNlsAnnotated(PsiTreeUtil.getParentOfType(expression, PsiClass.class))) {
        PsiField parentField = PsiTreeUtil.getParentOfType(expression, PsiField.class);

        final String description =
            MessageFormat.format(bundle.getString("inspection.localization.message.general.with.value"), "#ref");


        final List<LocalQuickFix> fixes = new ArrayList<>();

        final Project project = inspectionManager.getProject();
        final boolean constantFieldInitializer = isConstantFieldInitializer(expression);
        final boolean enumFirstLiteral = LocalizationUtils.isOnlyLiteralOfEnum(expression);
        if (!constantFieldInitializer) {
          if (enumFirstLiteral) {
            fixes.add(new MarkLocalizableQuickFix(Localizable.class, "Mark enum constant as @Localizable",
                buildPackageNameParts(packagePrefix)));
          } else {
            fixes.add(new IntroduceConstantFix());
            final JavaPsiFacade facade = JavaPsiFacade.getInstance(project);
            if (facade.findClass(Localizable.class.getName(), expression.getResolveScope()) != null) {
              fixes.add(new IntroduceConstantAndMarkLocalizableFix());
            }
          }
        }

        if (PsiUtil.isLanguageLevel5OrHigher(expression)) {
          if (parentField != null) {
            if (!AnnotationUtil.isAnnotated(parentField, AnnotationUtil.NON_NLS, true, false) &&
                isAnnotationInProjectScope(NonNls.class, parentField, project)) {
              fixes.add(new AddAnnotationFix(AnnotationUtil.NON_NLS, parentField));
            }
            if (constantFieldInitializer && isAnnotationInProjectScope(Localizable.class, parentField, project)) {
              fixes.add(new MarkLocalizableQuickFix(Localizable.class, buildPackageNameParts(packagePrefix)));
            }
          }
        }
        final LocalQuickFix[] fixesArray = fixes.toArray(new LocalQuickFix[fixes.size()]);
        final ProblemDescriptor problem = inspectionManager.createProblemDescriptor(
            expression,
            description,
            isOnTheFly,
            fixesArray,
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
        problems.add(problem);
      }
    }

  }

  private static List<String> buildPackageNameParts(String textualRepresentation) {
    if (!textualRepresentation.matches("[a-zA-Z_]+(\\.\\w+)*"))
      return Collections.emptyList();

    return Arrays.asList(textualRepresentation.split("\\."));
  }

  private static class IntroduceConstantFix implements LocalQuickFix {
    @Nls
    @NotNull
    @Override
    public String getName() {
      return IntroduceConstantHandler.REFACTORING_NAME;
    }

    @Nls
    @NotNull
    @Override
    public String getFamilyName() {
      return getName();
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor problemDescriptor) {
      ApplicationManager.getApplication().invokeLater(
          () -> {
            PsiElement element = problemDescriptor.getPsiElement();
            if (!(element instanceof PsiExpression)) return;

            PsiExpression[] expressions = {(PsiExpression) element};
            new IntroduceConstantHandler().invoke(project, expressions);
          }, project.getDisposed());
    }
  }

  private class IntroduceConstantAndMarkLocalizableFix implements LocalQuickFix {
    @Nls
    @NotNull
    @Override
    public String getName() {
      return "Extract constant and mark as @Localizable";
    }

    @Nls
    @NotNull
    @Override
    public String getFamilyName() {
      return getName();
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
      final PsiField[] resultField = new PsiField[1];
      PsiElement element = descriptor.getPsiElement();
      final PsiElement parent = element.getParent();
      ApplicationManager.getApplication().invokeLater(
          () -> {
            final int textOffset = element.getStartOffsetInParent();
            if (!(element instanceof PsiExpression)) return;

            PsiFile file = parent.getContainingFile();
            PsiExpression[] expressions = {(PsiExpression) element};
            final PsiDocumentManager manager = PsiDocumentManager.getInstance(project);
            final Document document = manager.getDocument(file);
            new IntroduceConstantHandler().invoke(project, expressions);
            assert document != null;
            manager.doPostponedOperationsAndUnblockDocument(document);
            int parentOffset;
            if (parent instanceof PsiFieldImpl) {
              final PsiFieldImpl fieldParent = (PsiFieldImpl) parent;
              parentOffset = parent.getTextOffset() - fieldParent.getType().toString().length() - fieldParent.getModifierList().getTextLength();
            } else if (parent instanceof PsiLocalVariableImpl){
              parentOffset = ((PsiLocalVariableImpl) parent).getStartOffset();
            } else {
              parentOffset = parent.getTextOffset();
            }
            final PsiElement extractedFieldElement = file.findElementAt(parentOffset + textOffset);
            final String extractedParent;
            assert extractedFieldElement != null;
            extractedParent = extractedFieldElement.getText();
            PsiClass psiClass = PsiTreeUtil.getParentOfType(parent, PsiClass.class);
            assert psiClass != null;
            final PsiField[] allFields = psiClass.getAllFields();
            for (PsiField field : allFields) {
              if (Objects.equals(field.getName(), extractedParent)) {
                resultField[0] = field;
              }
            }
            if (resultField[0] != null)
              new MarkLocalizableQuickFix(Localizable.class, buildPackageNameParts(packagePrefix)).applyFix(project, resultField[0]);
          }, ModalityState.NON_MODAL);
    }
  }
}
