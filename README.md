UI Localizer
============

Download
--------
<a href='https://bintray.com/devexperts/Maven/uilocalizer/_latestVersion'><img src='https://api.bintray.com/packages/devexperts/Maven/uilocalizer/images/download.svg'></a>

What is it?
-----------
UI Localizer — tool for simplifying localization of Java applications. It's just an annotation processor that captures classes annotated with `@Localizable` in the compile-time. It performs two actions:

1. Through bytecode manipulations, replaces initialization of `@Localizable` strings by reading value from .propreties file (using `ResourceBundle`)
2. Generates .properties template file with default values from source code

How to use tool in Java Project
-------------------------------
You should just add uilocalizer.jar to compiler classpath and mark all localizable strings with `@Localizable`. Example:

```java
@Localizable("order.dialog.title") private static final String TITLE = "Order Details";
@Localizable("order.dialog.comfirm") private static final String CONFIRM = "Confirm and Exit";
```

The only one annotation parameter should contain a name of property file (before the first dot) and property key (after the first dot).
Compilation of the example above will generate the following .properties template file:

```ini
dialog.title=Order Details
dialog.confirm=Confirm and Exit
```

To add support for any language in your application, you should:

1. Take the generated template file
2. Translate all properties values into the desired language
3. Rename file as originalname_LANGUAGETAG.properties
4. Add resulting .properties file to the application runtime classpath

Example of resulting French translation file:

```ini
dialog.title=Détails de la Commande
dialog.confirm=Confirmer et Quitter
```

You can choose the current language in your application using java property `ui.dialogs.locale`. Syntax for the java property is the same as syntax for argument of [java.util.Locale.forLanguageTag](https://docs.oracle.com/javase/8/docs/api/java/util/Locale.html#forLanguageTag-java.lang.String-) method:

```bash
-Dui.dialogs.locale=fr-fr
```

UI Localizer tool is fail-safe. That means if anything of mentioned above is missing (java property, key in a file for concrete language or a file for the language itself), default string value from initial source code will be used.

If you use UI Localizer for compilation multiple modules and you use the same property file name you should use the option `com.devexperts.uilocalizer.appendToPropertyFile` set to `true`, properties will be appended to the end of the file, e.g.: `-Acom.devexperts.uilocalizer.propertyFileAppend=true`. The default value is `false`.
Use this option with caution, don't forget to clean up all template files before compilation with UI Localizer.

Language consistency
--------------------
There is a way to store the language properties in one place. This method will guarantee language consistency of your application.
Also using such a controller you can change language "on the fly" by setting language. To use it you should:

1. Create a class with `public static Locale getLanguage()` method. For example:
 
    ```java
    public class SingletonLanguageController {
        private static final Object lock = new Object();
        private static volatile Locale lang;
        private static final String LANG_PROP = "ui.dialogs.locale";
        private static final String DEFAULT_LOCALE_TAG = "en-US";
    
        private SingletonLanguageController() {
        }
    
        public static Locale getLanguage() {
            if (lang == null) {
                synchronized (lock) {
                    if (lang == null) {
                        lang = Locale.forLanguageTag(System.getProperty(LANG_PROP, DEFAULT_LOCALE_TAG));
                    }
                }
            }
            return lang;
        }
    
        public static void updateLocale() {
            synchronized (lock) {
                lang = Locale.forLanguageTag(System.getProperty(LANG_PROP, DEFAULT_LOCALE_TAG));
            }
        }
    
        public static void setLanguage(String localeTag) {
            synchronized (lock) {
                System.setProperty(LANG_PROP, localeTag);
                updateLocale();
            }
        }
    }
    ```

2. Pass an argument to an annotation processor with option name `com.devexperts.uilocalizer.languageControllerPath`, value - fully qualified name:  
   
  `-Acom.devexperts.uilocalizer.languageControllerPath=com.example.SingletonLanguageController`

Bytecode changes
----------------

The tool just replaces each localizable string literal with a static method call, that attempts to read property with needed locale. If the attempt fails, the method returns initial value from the code. An example of annotated class and resulting bytecode:

**Initial code:**

```java
public class TestClass {
    @Localizable("scope.key")
    private static final String KUKU = "cucumber";
}
```

**Transformed code without language controller:**

```java
public class TestClass {
    
    @Localizable(value = "scope.key")
    private static final String KUKU = getString_u("scope.key", "cucumber");
    private static volatile java.util.Locale LOCALE_u;
    
    private static java.lang.String getString_u(String key, String defaultString) {
        try {
            if (LOCALE_u == null) 
                LOCALE_u = java.util.Locale.forLanguageTag(System.getProperty("ui.dialogs.locale", "en-US"));
            return new String(
                java.util.ResourceBundle.getBundle(key.substring(0, key.indexOf(46)), LOCALE_u)
                    .getString(key.substring(key.indexOf(46) + 1)));
        } catch (Exception e) {
            return defaultString;
        }
    }
}
```    

**Transformed code with language controller:**

```java
public class TestClass {
    
    @Localizable(value = "scope.key")
    private static final String KUKU = getString_u("scope.key", "cucumber");
    
    private static String getString_u(String key, String defaultString) {
        try {
            return new String(
                java.util.ResourceBundle.getBundle(key.substring(0, key.indexOf(46)), SingletonLanguageController.getLanguage())
                    .getString(key.substring(key.indexOf(46) + 1)));
        } catch (Exception e) {
            return defaultString;
        }
    }
}
```


Notes
-----

1. Smart build systems may omit part of classes during repeated build. To avoid missing properties in the template file, perform cleanup first.
2. Template files will be created in the current directory of javac process. If you use IDEA, it's likely ~/.IntellijideaXX/system/compile-server.
3. You can put not only files with translations into runtime classpath, but template files too. It's not necessary but allows you to change human readable strings dynamically, without project rebuild.
