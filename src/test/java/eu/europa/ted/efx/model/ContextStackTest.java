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

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import eu.europa.ted.efx.model.Context.NodeContext;
import eu.europa.ted.efx.model.expressions.scalar.NodePath;

class ContextStackTest {

  // The stack resolves no identifier here: the contexts are pushed as they are.
  private final ContextStack stack = new ContextStack(null);

  @Test
  void testReadsInCurrentContext_DeclaredContext_GivesItsReads() {
    final Reads withoutContext = this.stack.readsInCurrentContext();
    final NodeContext context = new NodeContext("ND-Root", new NodePath("/*"));
    this.stack.push(context);

    assertSame(context.reads(), this.stack.readsInCurrentContext());
    assertNotSame(withoutContext, this.stack.readsInCurrentContext());
  }

  @Test
  void testReadsInCurrentContext_NullOnTop_GivesTheReadsWithoutContext() {
    final Reads withoutContext = this.stack.readsInCurrentContext();
    this.stack.push(new NodeContext("ND-Root", new NodePath("/*")));
    this.stack.push(null);

    assertSame(withoutContext, this.stack.readsInCurrentContext());
  }
}
