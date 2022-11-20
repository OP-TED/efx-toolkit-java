# Interfaces

This package defines a number of interfaces that you need to implement in order to customise the transpiler:

* `ScriptGenerator`: Implement this interface if you want to translate **EFX expressions** to a new target script language. For translating to XPath you can reuse the [`XPathScriptGenerator`](../xpath/XPathScriptGenerator.java).
* `MarkupGenerator`: Implement this interface if you want to translate **EFX templates** to a new target markup language. 
* `SymbolResolver`: Used to resolve symbols (i.e. field names) and get their type or location in the data source. Implement this interface if you want to use a data source different than eForms XML or a metadata store different than the eForms SDK. For eForms XML you can reuse the [`SdkSymbolResolver`](../../eforms/sdk/SdkSymbolResolver.java) class.
* `TranslatorDependencyFactory`: The dependencies of a translator are nothing more than a `SymbolResolver`, a `ScriptGenerator` and a `MarkupGenerator`. Implement this interface to allow the translator to instantiate the dependencies it needs for a specific translation scenario (i.e. translate EFX templates to XSLT that you will then apply to XML notices).
