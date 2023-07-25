package eu.europa.ted.efx.model.expressions;

import java.lang.reflect.Constructor;

import org.antlr.v4.runtime.misc.ParseCancellationException;

import eu.europa.ted.efx.model.ParsedEntity;

public interface Expression extends ParsedEntity {

  public String getScript();

  public Boolean isLiteral();

  static <T extends Expression> T instantiate(String script, Class<T> type) {
    return Expression.instantiate(script, false, type);
  }

  static <T extends Expression> T from(Expression source, Class<T> returnType) {
    return Expression.instantiate(source.getScript(), source.isLiteral(), returnType);
  }

  static <T extends Expression> T instantiate(String script, Boolean isLiteral, Class<T> type) {
    try {
      if (isLiteral) {
        Constructor<T> constructor = type.getConstructor(String.class, Boolean.class);
        return constructor.newInstance(script, isLiteral);
      } else {
        Constructor<T> constructor = type.getConstructor(String.class);
        return constructor.newInstance(script);
      }
    } catch (Exception e) {
      throw new ParseCancellationException(e);
    }
  }

  static <T extends Expression> T empty(Class<T> type) {
    return instantiate("", type);
  }

  /**
   * Base class for all {@link Expression} implementations.
   */
  public abstract class Impl implements Expression {

    private final String script;
    private final Boolean isLiteral;

    @Override
    public String getScript() {
      return this.script;
    }

    @Override
    public Boolean isLiteral() {
      return this.isLiteral;
    }

    protected Impl(final String script) {
      this(script, false);
    }

    protected Impl(final String script, final Boolean isLiteral) {
      this.script = script;
      this.isLiteral = isLiteral;
    }

    public final Boolean isEmpty() {
      return this.script.isEmpty();
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null) {
        return false;
      }

      if (Expression.class.isAssignableFrom(obj.getClass())) {
        return this.script.equals(((Expression) obj).getScript());
      }

      return false;
    }
  }
}