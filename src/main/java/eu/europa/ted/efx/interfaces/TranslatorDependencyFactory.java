/*
 * Copyright 2022 European Union
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the European
 * Commission – subsequent versions of the EUPL (the "Licence"); You may not use this work except in
 * compliance with the Licence. You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence
 * is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the Licence for the specific language governing permissions and limitations under
 * the Lic
 */
package eu.europa.ted.efx.interfaces;

import org.antlr.v4.runtime.BaseErrorListener;

/**
 * Instantiates the dependencies needed by an EFX translator.
 * 
 * An EFX expression translator needs the following dependencies to be instantiated: - a
 * SymbolResolver to resolve symbols (references of fields etc.). - a ScriptGenerator to provide
 * target script language syntax for specific computations. - an error listener to handle any errors
 * encountered during translation An EFX template translator needs the all three dependencies listed
 * above, plus one more: - a MarkupGenerator to provide target markup language syntax for specific
 * output constructs.
 */
public interface TranslatorDependencyFactory {

  /**
   * Creates a SymbolResolver instance. to be used by an EFX translator .
   * 
   * This method is called by the EFX translator to instantiate the SymbolResolver it will use
   * during EFX expression translation to resolve symbols (i.e. references of fields, nodes,
   * codelists etc.).
   * 
   * @param sdkVersion The version of the SDK that contains the version of the EFX grammar that the
   *        EFX translator will attempt to translate. This is important as the symbols used in the
   *        EFX expression are defined in the specific version of the SDK.
   * @return An instance of ScriptGenerator to be used by the EFX translator.
   */
  public SymbolResolver createSymbolResolver(String sdkVersion);

  /**
   * Creates a ScriptGenerator instance.
   * 
   * This method is called by the EFX translator to instantiate the ScriptGenerator it will use to
   * translate EFX expressions to the target script language.
   * 
   * @param sdkVersion The version of the SDK that contains the version of the EFX grammar that the
   *        EFX translator will attempt to translate. This is important as it defines the EFX
   *        language features that ScriptGenerator instance should be able to handle.
   * @return An instance of ScriptGenerator to be used by the EFX translator.
   */
  public ScriptGenerator createScriptGenerator(String sdkVersion);

  /**
   * Creates a MarkupGenerator instance.
   * 
   * This method is called by the EFX translator to instantiate the MarkupGenerator it will use to
   * translate EFX templates to the target markup language.
   * 
   * @param sdkVersion The version of the SDK that contains the version of the EFX grammar that the
   *        EFX translator will attempt to translate. This is important as it defines the EFX
   *        language features that MarkupGenerator instance should be able to handle.
   * @return The instance of MarkupGenerator to be used by the EFX translator.
   */
  public MarkupGenerator createMarkupGenerator(String sdkVersion);

  /**
   * Creates an error listener instance.
   * 
   * This method is called by the EFX translator to instantiate the error listener it will use to to
   * report errors encountered during translation (i.e. syntax errors).
   * 
   * To customise error handling during EFX translator you may use different error listeners. For
   * example you may want an error listener that throws exceptions every time a syntax error is
   * encountered by the parser. In other scenarios you may need an error listener that outputs
   * parser errors to a console or a logfile.
   * 
   * @return The error listener to be used by the EFX translator.
   */
  public BaseErrorListener createErrorListener();
}
