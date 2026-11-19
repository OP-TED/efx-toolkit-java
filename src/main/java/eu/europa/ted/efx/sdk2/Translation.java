/*
 * Copyright 2026 European Union
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the European
 * Commission – subsequent versions of the EUPL (the "Licence"); You may not use this work except in
 * compliance with the Licence. You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence
 * is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the Licence for the specific language governing permissions and limitations under
 * the Licence.
 */
package eu.europa.ted.efx.sdk2;

import eu.europa.ted.efx.interfaces.ScriptGenerator;
import eu.europa.ted.efx.interfaces.SymbolResolver;
import eu.europa.ted.efx.interfaces.TypeChecker;
import eu.europa.ted.efx.model.CallStack;
import eu.europa.ted.efx.model.ContextStack;

/**
 * The state that the translators of SDK 2 share during one translation: the symbols that
 * references resolve to, the script generator that composes the target language, the call stack
 * of declarations and template data, and the stack of the contexts that references are read in.
 *
 * <p>A translator creates one for each translation, so that the declarations and contexts of one
 * translation are not visible in the next.</p>
 */
final class Translation {

  /**
   * What the names the translator generates start with, such as the variable an index binds its
   * position to. The call stack rejects a declaration that gives such a name, so that no
   * declaration can hide a generated name or be hidden by one.
   */
  static final String RESERVED_PREFIX = "__";

  private final SymbolResolver symbols;
  private final ScriptGenerator script;
  private final CallStack stack = new CallStack(TypeChecker.V2, RESERVED_PREFIX);
  private final ContextStack efxContext;

  Translation(final SymbolResolver symbols, final ScriptGenerator script) {
    this.symbols = symbols;
    this.script = script;
    this.efxContext = new ContextStack(symbols);
  }

  SymbolResolver symbols() {
    return this.symbols;
  }

  ScriptGenerator script() {
    return this.script;
  }

  CallStack stack() {
    return this.stack;
  }

  ContextStack efxContext() {
    return this.efxContext;
  }

  /**
   * Gives the translation that follows this one: the same symbols and script generator, with an
   * empty call stack and no context, so that the declarations and contexts of this translation are
   * not visible in it.
   *
   * @return the next translation
   */
  Translation next() {
    return new Translation(this.symbols, this.script);
  }
}
