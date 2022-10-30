# EFX Toolkit 1.2.0 Release Notes

_The EFX Toolkit for Java developers is a library that enables the transpilation of [EFX](https://docs.ted.europa.eu/eforms/latest/efx) expressions and templates to different target languages. It also includes an implementation of an EFX-to-XPath transpiler._

---
## In this release:

- We fixed a bug in the `XPathScriptGenerator` that was causing references to fields of type `measure` (duration) to throw an exception when multiple values where matched by the reference. 
- We fixed an issue in the `SdkSymbolResolver` that was causing some code labels to be resolved incorrectly. The `SdkSymbolResolver` now correctly looks for the root codelist associated with a field in the codelist metadata provided in the `codelists` folder, instead of relying on the codelist constraint metadata provided in `fields.json`. _**CAUTION:** If you have implemented your own `SymbolResolver` make sure that your implementation of `getRootCodelistOfField` retrieves the parent codelist information from `codelists/codelists.json` or directly from the `.gc` files in the `codelists` folder of the eForms SDK._
- We refactored the code to move some common classes to the [eForms Core Java library](https://github.com/OP-TED/eforms-core-java). In the context of this overall refactoring the [eForms Notice Viewer](https://github.com/OP-TED/eforms-notice-viewer) sample application was also affected.
---

You can download the latest EFX Toolkit from Maven Central.  
[![Maven Central](https://img.shields.io/maven-central/v/eu.europa.ted.eforms/efx-toolkit-java?label=Download%20&style=flat-square)](https://search.maven.org/search?q=g:%22eu.europa.ted.eforms%22%20AND%20a:%22efx-toolkit-java%22)

Documentation for the EFX Toolkit is available at: https://docs.ted.europa.eu/eforms/latest/efx-toolkit

---

This version of the EFX Toolkit has a compile-time dependency on the following versions of eForms SDK versions and uses the EFX grammar that each version provides:
- eForms SDK 0.6.x
- eForms SDK 0.7.x
- eForms SDK 1.x.x

It also depends on the [eForms Core Java library](https://github.com/OP-TED/eforms-core-java) version 1.0.0.
