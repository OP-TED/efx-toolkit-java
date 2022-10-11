# EFX Toolkit 1.0.3 Release Notes

_The EFX Toolkit for Java developers is a library that enables the transpilation of [EFX](https://docs.ted.europa.eu/eforms/latest/efx) expressions and templates to different target languages. It also includes an implementation of an EFX-to-XPath transpiler._

---
## In this release:

This patch fixes and issue in the translation of indirect label references (e.g. `#{BT-123-Field}`). The issue prevented applications from being able to render sequences of labels when the indirect reference returned sequences of values. 

You can see how this change can be utilised by applications by looking at the [changes](https://github.com/OP-TED/eforms-notice-viewer/compare/7e25fa2..9a1795a) released in version 0.4.0 of eforms-notice-viewer, which can now use a sequence of values returned by an indirect label reference to generate a comma separated list of labels.  

---

You can download the latest EFX Toolkit from Maven Central.  
[![Maven Central](https://img.shields.io/maven-central/v/eu.europa.ted.eforms/efx-toolkit-java?label=Download%20&style=flat-square)](https://search.maven.org/search?q=g:%22eu.europa.ted.eforms%22%20AND%20a:%22efx-toolkit-java%22)

Documentation for the EFX Toolkit is available at: https://docs.ted.europa.eu/eforms/latest/efx-toolkit

---

This version of the EFX Toolkit has a compile-time dependency on the following versions of eForms SDK versions and uses the EFX grammar that each version provides:
- eForms SDK 0.6.x
- eForms SDK 0.7.x
- eForms SDK 1.x.x