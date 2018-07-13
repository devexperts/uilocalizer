
# CHANGELOG
## 2.0 - 2018-07-13
- NEW: Added new annotation `@LocalizationProperty`
- NEW: IDEA plugin sources delivered
- NEW: support for custom localization facility (see `com.devexperts.uilocalizer.localizationMethod` option)
- FIX: removed unnecessary `"new String()"` generation

*NOTE:* it's known issue that compilation with 'com.devexperts.uilocalizer.languageControllerPath' option fails 
with Java 9/10/11 compilers (but compiled with javac 1.8 classes works fine with 9+ environment)

## 1.5 - 2017-08-09
- NEW: Implemented ability to provide language from the outside.
- NEW: Supported append mode of creating default properties files.
- FIX: Enums corruption because of `LOCALE_U` initialization on the first line.
- FIX: Escape backslashes in a property file.

## 1.4 - 2017-05-23

- FIX: Append properties to a file in natural order
- FIX: Escape line breaks and other special symbols in a property file

## 1.3 - 2016-09-30

- FIX: Static initialization code now added to every class with annotations to preserve original class loading order.

## 1.2 - 2016-09-22

- Initial public version
