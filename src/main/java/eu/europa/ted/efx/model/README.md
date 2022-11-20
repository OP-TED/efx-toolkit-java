# Model

This package contains classes used internally by the EFX translator.

* The `Context` points to a location in the data source. For example, if the data source is XML, the context is pointing to a location in the data source by using XPath. If your data source was JSON the context would need to be JsonPath. The [`SymbolResolver`](../interfaces/SymbolResolver.java) is used to get the "path" that is stored in the `Context`.
* The `ContextStack` is used by the EFX expression translator to keep track of `Context` changes as it walks through the parse tree.  
* The `CallStack` is used by the EFX translator to transfer translated fragments from one step of the translation to another.
* `Expression` and its sub-classes represent expression fragments translated in the target script language. They are used by the EFX expression translator to gradually built the final translated output and to control type-safety.
* `Markup` represents markup fragments in the target markup language. It is used by the EFX template translator to gradually build the final translated output.  
* `CallStackObjectBase` is a base class for `Expression` and `Markup` (a base type for data that can be placed in the `CallStack`).
* `ContentBlock` and `ContentBlockStack` are used during EFX template translation to keep track of its hierarchical structure.