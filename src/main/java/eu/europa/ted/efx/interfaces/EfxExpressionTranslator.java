package eu.europa.ted.efx.interfaces;

import org.antlr.v4.runtime.BaseErrorListener;

public interface EfxExpressionTranslator {
  EfxExpressionTranslator init(SymbolResolver symbolResolver, ScriptGenerator scriptGenerator,
      BaseErrorListener errorListener);

  String translateExpression(final String context, final String expression);
}
