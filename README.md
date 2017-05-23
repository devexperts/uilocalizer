UI Localizer
============
Download
--------
<a href='https://bintray.com/devexperts/Maven/uilocalizer/_latestVersion'><img src='https://api.bintray.com/packages/devexperts/Maven/uilocalizer/images/download.svg'></a>.

What is it?
-----------
UI Localizer — tool for simplifying localization of Java applications. It's just an annotation processor that captures classes annotated with @Localizable in the compile-time. It performs two actions:

1. Through bytecode manipulations, replaces initialization of @Localizable strings by reading value from .propreties file (using ResourceBundle)
2. Generates .properties template file with default values from source code

How to use tool in Java Project
-------------------------------
You should just add uilocalizer.jar to compiler classpath and mark all localizable strings with @Localizable. Example:

    @Localizable("order.dialog.title") private static final String TITLE = "Order Details";
    @Localizable("order.dialog.comfirm") private static final String CONFIRM = "Confirm and Exit";

The only one annotation parameter should contain name of property file (before first dot) and property key (after first dot).
Compilation of the example above will result in generation of the following .properties template file:

    dialog.title=Order Details
    dialog.confirm=Confirm and Exit

To add support of any language in your application, you should:

1. Take the generated template file
2. Translate all properties values into desired language
3. Rename file as originalname_LANGUAGETAG.properties
4. Add resulting .properties file to the application runtime classpath

Example of resulting French translation file:

    dialog.title=Détails de la Commande
    dialog.confirm=Confirmer et Quitter

You can choose current language in your appilcation using java property -Dui.dialogs.locale. Syntax for the java property is the same as syntax for argument of java.util.Locale#forLanguageTag method:

    -Dui.dialogs.locale=fr-fr

UI Localizer tool is fail-safe. That means if anything of mentioned above is missing (java property, key in file for concrete language or file for concrete language itself), default string value from initial source code will be used.

Bytecode changes
----------------
Tool just replaces localizable string literals by invocation of static method, that attempts to read property with needed locale. In attempt fails, method returns initial value from the code. Example of annotated class and resulting bytecode:

    public class TestClass {
        @Localizable("scope.key")
        private static final String KUKU = "cucumber";
    }

    public class TestClass {

        public TestClass() {
            super();
        }
        @Localizable(value = "scope.key")
        private static final String KUKU = getString_u("scope.key", "cucumber");
        private static final java.util.Locale LOCALE_u = java.util.Locale.forLanguageTag(java.lang.System.getProperty("ui.dialogs.locale", "en-US"));

        private static java.lang.String getString_u(java.lang.String key, java.lang.String defaultString) {
            try {
                java.lang.String val = java.util.ResourceBundle.getBundle(key.substring(0, key.indexOf(46)), LOCALE_u).getString(key.substring(key.indexOf(46) + 1));
                return new java.lang.String(val.getBytes("ISO-8859-1"), "UTF-8");
            } catch (java.lang.Exception e) {
                return defaultString;
            }
        }
    }

Notes
-----
1. Smart build systems may omit part of classes during repeated build. To avoid missing properties in the template file, perform cleanup first.
2. Template files will be created in current directory of javac process. If you use IDEA, it's likely ~/.IntellijideaXX/system/compile-server.
3. You can put not only files with translations into runtime classpath, but template files too. It's not necessary, but allows you to change human readable strings dynamically, without project rebuild.