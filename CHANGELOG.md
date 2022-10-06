# EFX Toolkit 1.0.2 Release Notes

_The EFX Toolkit for Java developers is a library that enables the transpilation of [EFX](https://docs.ted.europa.eu/eforms/latest/efx) expressions and templates to different target languages. It also includes an implementation of an EFX-to-XPath transpiler._

---
## In this release:

This patch fixes a bug in the translation of shorthand indirect label references (e.g. `#{BT-123-Field}`). The bug affected only fields pointing to XML attributes.

---

You can download the latest EFX Toolkit from Maven Central.  
[![Maven Central](https://img.shields.io/maven-central/v/eu.europa.ted.eforms/efx-toolkit-java?label=Download%20&style=flat-square)](https://search.maven.org/search?q=g:%22eu.europa.ted.eforms%22%20AND%20a:%22efx-toolkit-java%22)

Documentation for the EFX Toolkit is available at: https://docs.ted.europa.eu/eforms/latest/efx-toolkit

---

This version of the EFX Toolkit has a compile-time dependency on the following versions of eForms SDK versions and uses the EFX grammar that each version provides:
- eForms SDK 0.6.x
- eForms SDK 0.7.x
- eForms SDK 1.x.x