# EFX Toolkit 0.2.0 Release Notes

The EFX Toolkit for Java developers is a library that enables the transpilation of [EFX](https://docs.ted.europa.eu/eforms/latest/efx) expressions and templates to different target languages. It also provides an implementation that turns an EFX expression into XPath.

In this release we added support for the new EFX language features that were introduced with eForms SDK 0.7.0.

We also reorganized the code of the toolkit so that it can function with multiple versions of the SDK. You can use our approach as an example on how you can handle several incompatible versions of the SDK from one application that needs to read them.

Documentation for the EFX Toolkit is available at: https://docs.ted.europa.eu/eforms/latest/efx-toolkit

This version of the EFX Toolkit has a compile-time dependency on the following versions of eForms SDK versions and uses the EFX grammar that each version provides:
- eForms SDK 0.6.x
- eForms SDK 0.7.x
