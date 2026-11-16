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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import eu.europa.ted.eforms.sdk.schematron.SchematronGenerator;
import eu.europa.ted.efx.EfxTestsBase;
import eu.europa.ted.efx.EfxTranslatorOptions;
import eu.europa.ted.efx.exceptions.ThrowingErrorListener;
import eu.europa.ted.efx.mock.DependencyFactoryMock;
import eu.europa.ted.efx.model.DecimalFormat;

/**
 * The syntax errors of EFX-2 expressions, templates and rules, as {@link EfxErrorStrategy} and
 * {@link EfxErrorReportingLexer} word them.
 */
class EfxErrorStrategyTest extends EfxTestsBase {

  private static final String SDK_VERSION = "eforms-sdk-2.0";

  @Override
  protected String getSdkVersion() {
    return SDK_VERSION;
  }

  // #region Expressions -------------------------------------------------------

  @Test
  void testExpression_EndsTooSoon() {
    assertSyntaxErrorMessage("line 1:24 Syntax error: unexpected end of input in expression.",
        () -> translateExpression("WITH ND-Root COMPUTE 1 +"));
  }

  @Test
  void testExpression_MissingComma() {
    assertSyntaxErrorMessage(
        "line 1:35 Syntax error: unexpected '1' in function call; expected ','.",
        () -> translateExpression("WITH ND-Root COMPUTE substring('a' 1)"));
  }

  @Test
  void testExpression_MissingClosingParenthesis() {
    assertSyntaxErrorMessage(
        "line 1:23 Syntax error: unexpected end of input in expression; expected ')'.",
        () -> translateExpression("WITH ND-Root COMPUTE (1"));
  }

  @Test
  void testExpression_ExtraNumber() {
    assertSyntaxErrorMessage(
        "line 1:23 Syntax error: unexpected '2' in expression; expected end of input.",
        () -> translateExpression("WITH ND-Root COMPUTE 1 2"));
  }

  @Test
  void testExpression_ExtraText_KeepsItsQuotes() {
    assertSyntaxErrorMessage(
        "line 1:25 Syntax error: unexpected 'b' in expression; expected end of input.",
        () -> translateExpression("WITH ND-Root COMPUTE 'a' 'b'"));
  }

  @Test
  void testExpression_ExtraOperator() {
    assertSyntaxErrorMessage("line 1:25 Syntax error: unexpected '*' in expression.",
        () -> translateExpression("WITH ND-Root COMPUTE 1 + * 2"));
  }

  @Test
  void testExpression_ForWithoutReturn() {
    assertSyntaxErrorMessage(
        "line 1:46 Syntax error: unexpected end of input in expression; expected 'return'.",
        () -> translateExpression("WITH ND-Root COMPUTE for text:$x in BT-00-Text"));
  }

  @Test
  void testExpression_IfWithoutElse() {
    assertSyntaxErrorMessage(
        "line 1:32 Syntax error: unexpected end of input in expression; expected 'else'.",
        () -> translateExpression("WITH ND-Root COMPUTE if 1 then 2"));
  }

  @Test
  void testExpression_FunctionCallNotClosed() {
    assertSyntaxErrorMessage(
        "line 1:37 Syntax error: unexpected end of input in function call; expected ')' or ','.",
        () -> translateExpression("WITH ND-Root COMPUTE substring('a', 1"));
  }

  @Test
  void testExpression_TextLiteralNotClosed() {
    assertSyntaxErrorMessage("line 1:21 Syntax error: unexpected end of input in text literal.",
        () -> translateExpression("WITH ND-Root COMPUTE 'abc"));
  }

  @Test
  void testExpression_CharacterThatStartsNoToken() {
    assertSyntaxErrorMessage("line 1:23 Syntax error: unexpected character '¤'.",
        () -> translateExpression("WITH ND-Root COMPUTE 1 ¤ 2"));
  }

  // #endregion Expressions ----------------------------------------------------

  // #region Templates ---------------------------------------------------------

  @Test
  void testTemplate_ExpressionBlockNotRead() {
    assertSyntaxErrorMessage("line 1:10 Syntax error: unexpected '${1 +}' in template content.",
        () -> translateTemplate("{ND-Root} ${1 +}"));
  }

  @Test
  void testTemplate_CharacterThatStartsNoToken() {
    assertSyntaxErrorMessage("line 1:10 Syntax error: unexpected character '{'.",
        () -> translateTemplate("{ND-Root ${1}"));
  }

  @Test
  void testTemplate_TextLiteralNotClosedOnItsLine() {
    assertSyntaxErrorMessage("line 1:12 Syntax error: unexpected end of line in text literal.",
        () -> translateTemplate("{ND-Root} ${'abc}"));
  }

  @Test
  void testTemplate_EscapeSequenceNotAllowedInText() {
    assertSyntaxErrorMessage("line 1:12 Syntax error: unexpected 'q' after '\\'.",
        () -> translateTemplate("{ND-Root} a \\q b"));
  }

  @Test
  void testTemplate_VariableWithoutValue() {
    assertSyntaxErrorMessage("line 1:14 Syntax error: unexpected ';' in expression.",
        () -> translateTemplate("let text:$x = ;"));
  }

  // #endregion Templates ------------------------------------------------------

  // #region Rules -------------------------------------------------------------

  private static final String RULE_START =
      String.join("\n", "---- STAGE 1a ----", "", "WITH ND-Root");

  @Test
  void testRule_AssertionEndsTooSoon() {
    assertSyntaxErrorMessage("line 5:4 Syntax error: unexpected 'AS' in expression.",
        () -> translateRules(lines(RULE_START, "    ASSERT 1 +", "    AS ERROR R-K7P-M2Q",
            "    FOR BT-00-Text IN 1")));
  }

  @Test
  void testRule_WithoutAs() {
    assertSyntaxErrorMessage(
        "line 5:4 Syntax error: unexpected 'FOR' in as clause; expected 'AS'.",
        () -> translateRules(lines(RULE_START, "    ASSERT TRUE", "    FOR BT-00-Text IN 1")));
  }

  @Test
  void testRule_NoticeSubtypeRangeOfThree() {
    assertSyntaxErrorMessage(
        "line 6:25 Syntax error: unexpected '-' in rules file;"
            + " expected end of input or '#include' or a stage header.",
        () -> translateRules(lines(RULE_START, "    ASSERT TRUE", "    AS ERROR R-K7P-M2Q",
            "    FOR BT-00-Text IN 1-2-3")));
  }

  @Test
  void testRule_IdentifierOfAnotherForm() {
    assertSyntaxErrorMessage(
        "line 5:13 Syntax error: unexpected 'R-000-0001' in rule identifier;"
            + " expected a rule identifier, such as 'R-K7P-M2Q'.",
        () -> translateRules(lines(RULE_START, "    ASSERT TRUE", "    AS ERROR R-000-0001",
            "    FOR BT-00-Text IN 1")));
  }

  // #endregion Rules ----------------------------------------------------------

  private static void assertSyntaxErrorMessage(final String expected,
      final Executable translation) {
    final ParseCancellationException exception =
        assertThrows(ParseCancellationException.class, translation);
    assertEquals(ParseCancellationException.class, exception.getClass(), exception::toString);
    assertEquals(expected, exception.getMessage());
  }

  private void translateRules(final String rules) throws InstantiationException {
    new EfxRulesTranslatorV2(new SchematronGenerator(),
        DependencyFactoryMock.INSTANCE.createSymbolResolver(SDK_VERSION, ""),
        DependencyFactoryMock.INSTANCE.createScriptGenerator(SDK_VERSION, "",
            new EfxTranslatorOptions("udf", DecimalFormat.EFX_DEFAULT)),
        ThrowingErrorListener.INSTANCE).translateRules(rules, DEFAULT_OPTIONS);
  }
}
