## Version 2.0 - 
- FIX: null pointer exception in new Idea versions.
- FIX: wrong highlighting in nested, anonymous classes and lambdas.
- FIX: inspection has not been highlighting literals in field constructor if field was marked as`@Localizable`, while 
it should mark all the literals excluding the first one.

## Version 1.4
- REMOVE: "Ignore literals which have value equal to existing property key" is removed
- FIX: "Ignore for classes from test packages" is added instead of "Ignore for JUnit assert arguments" option
- NEW: Added error-level report for `@Localizable` constants in non-static inner classes
- NEW: error-level report for `@Localizable` constants in interfaces
- NEW: hotfix for the special case when enum constant has the only one String literal argument
- FIX: "Trim beginning of package in property key suggestion" is added instead of the hardcoded package prefix