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
package eu.europa.ted.efx.model;

/**
 * The references resolved, and the dictionaries looked up, while a context is the current one:
 * the reads of the context, and the reads of the root, which an absolute reference and a lookup
 * start from. Where no context is declared, a relative reference starts from the root, and its
 * read is a read of the root.
 *
 * <p>Each {@link Context} keeps its reads, and the {@link ContextStack} keeps those made where no
 * context is declared, so that an index can tell whether its position reads what the target
 * language would read differently for each element.</p>
 */
public final class Reads {

  private int ofContext;
  private int ofRoot;

  Reads() {}

  /**
   * Counts a read of the context: a reference resolved from it.
   */
  public void countReadOfContext() {
    this.ofContext++;
  }

  /**
   * Counts a read of the root: a reference resolved from it, or a lookup in a dictionary.
   */
  public void countReadOfRoot() {
    this.ofRoot++;
  }

  /**
   * @return how many references have been resolved from the context
   */
  public int ofContext() {
    return this.ofContext;
  }

  /**
   * @return how many reads of the context and of the root have been made
   */
  public int all() {
    return this.ofContext + this.ofRoot;
  }
}
