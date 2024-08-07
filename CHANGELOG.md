# EFX Toolkit 2.0.0-alpha.4 Release Notes

_The EFX Toolkit for Java developers is a library that enables the transpilation of [EFX](https://docs.ted.europa.eu/eforms/latest/efx) expressions and templates to different target languages. It also includes an implementation of an EFX-to-XPath transpiler._

---

## In this release

This release fixes an a bug that caused variables and parameters to be in the wrong order in the generated XSL.

A "qualifier" parameter was added in various methods, to allow the use of the corresponding new feature in the eForms Core Library 1.4.0.

The dependency on ANTLR was updated to version 4.13.1.


## EFX-1 Support

Although this is a pre-release version of the EFX Toolkit, it provides production-level support for EFX-1 transpilation. EFX-1 is the current version of EFX released with SDK 1.

NOTE: Transpilation of EFX-1 to XPath and XSL in this version of the EFX Toolkit is **better than** what is provided by **EFX Toolkit 1.3.0**.


## EFX-2 Support

The new version of EFX is still under development and will be released with SDK 2.0.0. For more information of EFX-2 see the release notes of the eForms SDK 2.0.0-alpha.1.

## Breaking changes

For users of the Toolkit that have implemented custom transpilers, this release contains a few breaking changes from 1.3.0.
More specifically:

- Some additional methods have been added to the SymbolResolver, ScriptGenerator and MarkupGenerator API. As a guide for your implementations please look a the implementations included in the EFX Toolkit for use by the EFX-to-XPath transpilation.
- Some deprecated methods were removed.
- An extensive refactoring in the type management system has rearranged the package structure. As a result some import statements in your code will need to be updated.

Users of the Toolkit that only use the included EFX-to-XPath transpiler will not be affected by the above changes.

## Future development

Further alpha and beta releases of SDK 2 and EFX Toolkit 2 will be issued. While in "alpha" development stage, further breaking changes may be introduced.

---

You can download the latest EFX Toolkit from Maven Central.  
[![Maven Central](https://img.shields.io/maven-central/v/eu.europa.ted.eforms/efx-toolkit-java?label=Download%20&style=flat-square)](https://central.sonatype.com/artifact/eu.europa.ted.eforms/efx-toolkit-java)

Documentation for the EFX Toolkit is available at: <https://docs.ted.europa.eu/eforms/latest/efx-toolkit>

---

This version of the EFX Toolkit has a compile-time dependency on the following versions of eForms SDK versions and uses the EFX grammar that each version provides:

- eForms SDK 1.x.x
- eForms SDK 2.0.0-alpha.1

It also depends on the [eForms Core Java library](https://github.com/OP-TED/eforms-core-java) version 1.4.0.
