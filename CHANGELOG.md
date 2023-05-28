# EFX Toolkit 1.3.0 Release Notes

_The EFX Toolkit for Java developers is a library that enables the transpilation of [EFX](https://docs.ted.europa.eu/eforms/latest/efx) expressions and templates to different target languages. It also includes an implementation of an EFX-to-XPath transpiler._

---
## In this release:

- Updated the XPath 2.0 parse, XPathContextualizer and XPathScriptGenerator to correctly translate sequences. 
- Improved numeric formatting. The EfxTranslator API now includes overloaded methods that permit control of numeric formatting. The existing API has been preserved.
- Improved handling of multilingual text fields by adding automatic selection of the visualisation language .  
---

You can download the latest EFX Toolkit from Maven Central.  
[![Maven Central](https://img.shields.io/maven-central/v/eu.europa.ted.eforms/efx-toolkit-java?label=Download%20&style=flat-square)](https://search.maven.org/search?q=g:%22eu.europa.ted.eforms%22%20AND%20a:%22efx-toolkit-java%22)

Documentation for the EFX Toolkit is available at: https://docs.ted.europa.eu/eforms/latest/efx-toolkit

---

This version of the EFX Toolkit has a compile-time dependency on the following versions of eForms SDK versions and uses the EFX grammar that each version provides:
- eForms SDK 0.6.x
- eForms SDK 0.7.x
- eForms SDK 1.x.x

It also depends on the [eForms Core Java library](https://github.com/OP-TED/eforms-core-java) version 1.0.5.
