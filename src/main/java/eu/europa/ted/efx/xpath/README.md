# Translating EFX to XPath

This package contains classes used for translating EFX expressions to XPath.

* `XPathScriptGenerator`: Implements the `ScriptGenerator` interface for EFX to XPath translation.
* `XPathContextualizer`: Used to convert a given absolute XPath expression to an XPath relative to another absolute XPath.

_Note: There is one more class that is specific to EFX-to-XPath translation which is not contained in this package: the [`SdkSymbolResolver`](../../eforms/sdk/SdkSymbolResolver.java) class. It is XPath specific because it returns XPaths taken from the eForms SDK._
