# Package contents
This package contains the core components of this library. 

* The `EfxTranslator` is the main class you will typically need from the EFX Toolkit (especially if all you want to do is translate EFX expressions to XPath). It provides static methods (for convenience) that can be invoked to translate EFX expressions and EFX templates.
* The `interfaces` package defines some interfaces that you can  implement if you want to customise the behaviour of the EFX translator.
* The `model` package contains classes used internally by the EFX translator.
* The `xpath` package contains classes used during translation of EFX expressions to XPath.
* The `sdk?` packages contain implementations of the EFX translators and eForms entities specific to different major versions of the eForms SDK. 

