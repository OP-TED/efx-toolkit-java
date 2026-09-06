# EFX Toolkit 2.0.0-alpha.8 Release Notes

_The EFX Toolkit for Java developers is a library that enables the transpilation of [EFX](https://docs.ted.europa.eu/eforms/latest/efx) expressions and templates to different target languages. It also includes an implementation of an EFX-to-XPath transpiler._

---

## In this release

This is an incremental update over 2.0.0-alpha.7.

### Changes since 2.0.0-alpha.7

- **Selectors**: an EFX expression can now yield the path of the XML elements a reference points to, instead of the values held in them. Write `&{reference}` in EFX-1, and `&{reference}` or `WITH context SELECT reference` in EFX-2. The first use of this new feature is in the `privacy.undisclosedFieldSelector` property in `fields.json` of SDK 1.16.0-beta.2 and SDK 2.0.0-alpha.3. The new property identifies the elements that must be masked and effectively withheld from publication.
- **Preferred language selection**: fixed an issue with the implicit invocation in EFX-1 of the `preferred-language-text` function when multilingual text fields are referenced. The function is now called correctly only when transpiling view templates, which is the only context where this functionality is applicable. EFX-2 requires explicit invocation of the function and is therefore unaffected by this fix.
- **Context overrides**: fixed multiple issues that caused the transpiler to produce valid but inaccurate XPaths when using a context override (`context::field`) to modify the path through which the value of a field is reached. Transpilation now preserves navigation steps and predicates accurately, and expressions using a context override will now produce a longer XPath, which however honours all predicates and enforces the presence of the context to select the designated value(s). This feature has not been used so far in any published SDK, so this change does not have an impact on the interpretation of existing rules and templates published in any SDK version.
- The eForms Core Java dependency is now 1.9.0.

The following sections describe the features of the 2.0.0 line, unchanged since 2.0.0-alpha.7.

### EFX-2 language support

- Implemented all new EFX-2 language features introduced in SDK 2.0.0-alpha.2. See the [SDK changelog](https://github.com/OP-TED/eForms-SDK/blob/release/2.0.0-alpha.2/CHANGELOG.md) for the full list.

### EFX Rules translator

- Added a new EFX Rules to Schematron translator, enabling business rules authored in EFX to be transpiled into Schematron validation schemas.
- Rules that apply to all notice subtypes are deduplicated into a shared Schematron pattern.
- New `ValidatorGenerator` interface for Schematron output generation.

### Dependency analysis

- Added dependency extractors for EFX rules, producing dependency graphs that track field, codelist, and variable references.

### Template translator

- Dictionary lookups are now translated using XSL keys for better performance.
- Profiling and timing support for template translation via new `TranslatorOptions` methods.

### EFX autocomplete API

- Added an autocomplete API for code editors, providing completion items for EFX keywords, built-in functions, and field properties.

### New interfaces and classes

- `ValidatorGenerator`: Interface for generating validation output (Schematron).
- `TranslatorContext`: Interface providing translation context to `MarkupGenerator` methods.
- `TypeChecker`: Interface for SDK-specific type validation.
- `IncludedFileResolver`: Interface for resolving `#include` directives in EFX files.
- New typed exception hierarchy: `EfxCompilationException`, `InvalidArgumentException`, `InvalidIdentifierException`, `InvalidIndentationException`, `InvalidUsageException`, `SdkInconsistencyException`, `SymbolResolutionException`, `TranslatorConfigurationException`, `TypeMismatchException`.

### Bug fixes

- Fixed invalid XPath in typed uniqueness conditions.
- Fixed quote escaping in string literals.

## EFX-1 Support

Although this is a pre-release version of the EFX Toolkit, it provides production-level support for EFX-1 transpilation. EFX-1 is the current version of EFX released with SDK 1.

NOTE: Transpilation of EFX-1 to XPath and XSL in this version of the EFX Toolkit is **better than** what is provided by **EFX Toolkit 1.3.0**.

## Breaking changes

`XPathContextualizer.join` now takes a third argument, which says how far the joined path may be shortened; the two-argument form has been removed. For the breaking changes introduced earlier in the 2.0.0 line, see the [2.0.0-alpha.6 release notes](https://github.com/OP-TED/efx-toolkit-java/releases/tag/2.0.0-alpha.6).

## Future development

Further alpha and beta releases of SDK 2 and EFX Toolkit 2 will be issued. While in "alpha" development stage, further breaking changes may be introduced.

---

You can download the latest EFX Toolkit from Maven Central.
[![Maven Central](https://img.shields.io/maven-central/v/eu.europa.ted.eforms/efx-toolkit-java?label=Download%20&style=flat-square)](https://central.sonatype.com/artifact/eu.europa.ted.eforms/efx-toolkit-java)

Documentation for the EFX Toolkit is available at: <https://docs.ted.europa.eu/eforms/latest/efx-toolkit>

---

This version of the EFX Toolkit has a compile-time dependency on the following eForms SDK versions and uses the EFX grammar that each version provides:

- eForms SDK 1.16.0-beta.2
- eForms SDK 2.0.0-alpha.3

It also depends on the [eForms Core Java library](https://github.com/OP-TED/eforms-core-java) version 1.9.0.
