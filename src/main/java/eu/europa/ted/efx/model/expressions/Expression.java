/*
 * Copyright 2023 European Union
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
package eu.europa.ted.efx.model.expressions;

import java.lang.reflect.Constructor;
import java.util.Objects;

import org.antlr.v4.runtime.misc.ParseCancellationException;

import eu.europa.ted.efx.model.ParsedEntity;

/**
 * Root interface for all expression AST nodes in the EFX type system.
 *
 * An {@link Expression} wraps a target-language script fragment (e.g., XPath) produced during
 * EFX-to-target-language translation. The script is accessed via {@link #getScript()}.
 *
 * Expressions form a type hierarchy enabling compile-time type safety during translation.
 * Use {@link TypedExpression} subinterfaces for type-specific operations.
 *
 * @see TypedExpression for expressions with associated EFX data types
 * @see LiteralExpression for constant values known at translation time
 * @see PathExpression for references to document elements
 */
public interface Expression extends ParsedEntity {

  public String getScript();

  static <T extends Expression> T instantiate(String script, Class<T> type) {
    try {
      Constructor<T> constructor = type.getConstructor(String.class);
      return constructor.newInstance(script);
    } catch (Exception e) {
      throw new ParseCancellationException(e);
    }
  }

  static <T extends Expression> T from(Expression source, Class<T> returnType) {
    return Expression.instantiate(source.getScript(), returnType);
  }

  static <T extends Expression> T empty(Class<T> type) {
    return instantiate("", type);
  }

  /**
   * Base class for all {@link Expression} implementations.
   */
  public abstract class Impl implements Expression {

    private final String script;

    @Override
    public String getScript() {
      return this.script;
    }

    protected Impl(final String script) {
      this.script = script;
    }

    public final Boolean isEmpty() {
      return this.script.isEmpty();
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }

      if (!(obj instanceof Expression)) {
        return false;
      }

      Expression other = (Expression) obj;
      return Objects.equals(script, other.getScript())
          && (this instanceof LiteralExpression) == (other instanceof LiteralExpression);
    }

    @Override
    public int hashCode() {
      return Objects.hash(script, this instanceof LiteralExpression);
    }
  }
}
