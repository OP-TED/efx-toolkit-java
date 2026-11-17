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

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.atn.ATNState;
import org.antlr.v4.runtime.atn.Transition;
import org.antlr.v4.runtime.misc.IntervalSet;
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
  void testExpression_NeitherWithNorContextBlock() {
    assertSyntaxErrorMessage(
        "line 1:0 Syntax error: unexpected '1' in expression; expected '{' or 'WITH'.",
        () -> translateExpression("1"));
  }

  @Test
  void testExpression_BraceAfterContextBlock_IsNotAnExpressionBlock() {
    assertSyntaxErrorMessage("line 1:9 Syntax error: unexpected '{' in expression.",
        () -> translateExpression("{ND-Root}{1}"));
  }

  @Test
  void testExpression_ContextThatIsNotAnIdentifier() {
    final String expected = "line 1:5 Syntax error: unexpected '1' in expression;"
        + " expected a field identifier or a node identifier or an identifier.";
    assertSyntaxErrorMessage(expected, () -> translateExpression("WITH 1 COMPUTE 1"));
    assertSyntaxErrorMessage(expected, () -> translateExpression("WITH 1 SELECT ND-Root"));
  }

  @Test
  void testExpression_ContextBlockThatIsNotAnIdentifier() {
    assertSyntaxErrorMessage(
        "line 1:1 Syntax error: unexpected '1' in expression;"
            + " expected a field identifier or a node identifier or an identifier.",
        () -> translateExpression("{1} ${1}"));
  }

  @Test
  void testExpression_NeitherComputeNorSelect() {
    assertSyntaxErrorMessage(
        "line 1:13 Syntax error: unexpected '1' in expression; expected 'COMPUTE' or 'SELECT' or ','.",
        () -> translateExpression("WITH ND-Root 1"));
  }

  @Test
  void testExpression_ParameterWithoutItsVariable() {
    assertSyntaxErrorMessage(
        "line 1:20 Syntax error: unexpected 'COMPUTE' in parameter declaration; expected '$'.",
        () -> translateExpression("WITH ND-Root, text: COMPUTE 1"));
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

  @Test
  void testExpression_SpaceThatStartsNoToken() {
    assertSyntaxErrorMessage("line 1:22 Syntax error: unexpected character ' '.",
        () -> translateExpression("WITH ND-Root COMPUTE $ x"));
  }

  @Test
  void testExpression_CharacterThatCannotBeSeen_IsNamedByItsCodePoint() {
    assertSyntaxErrorMessage("line 1:23 Syntax error: unexpected character U+200B.",
        () -> translateExpression("WITH ND-Root COMPUTE 1 \u200B"));
  }

  @Test
  void testExpression_CombiningMark_IsNamedByItsCodePoint() {
    assertSyntaxErrorMessage("line 1:23 Syntax error: unexpected character U+FE0F.",
        () -> translateExpression("WITH ND-Root COMPUTE 1 \uFE0F"));
    assertSyntaxErrorMessage("line 1:23 Syntax error: unexpected character U+0903.",
        () -> translateExpression("WITH ND-Root COMPUTE 1 \u0903"));
    assertSyntaxErrorMessage("line 1:23 Syntax error: unexpected character U+20DD.",
        () -> translateExpression("WITH ND-Root COMPUTE 1 \u20DD"));
  }

  @Test
  void testExpression_SpaceOfAnotherKind_IsNamedByItsCodePoint() {
    assertSyntaxErrorMessage("line 1:22 Syntax error: unexpected character U+00A0.",
        () -> translateExpression("WITH ND-Root COMPUTE 1\u00A02"));
  }

  @Test
  void testExpression_CharacterDrawnBlank_IsNamedByItsCodePoint() {
    // A symbol and a letter by their category, both drawn as a blank
    assertSyntaxErrorMessage("line 1:23 Syntax error: unexpected character U+2800.",
        () -> translateExpression("WITH ND-Root COMPUTE 1 \u2800"));
    assertSyntaxErrorMessage("line 1:23 Syntax error: unexpected character U+3164.",
        () -> translateExpression("WITH ND-Root COMPUTE 1 \u3164"));
  }

  @Test
  void testExpression_CharacterThatCannotBeSeenInATextLiteral_IsWrittenAsAnEscape() {
    // Written as it is, the zero-width space would show the literal as empty: ''
    assertSyntaxErrorMessage(
        "line 1:23 Syntax error: unexpected '\\u200B' in expression; expected end of input.",
        () -> translateExpression("WITH ND-Root COMPUTE 1 '\u200B'"));
    assertSyntaxErrorMessage(
        "line 1:23 Syntax error: unexpected '\\u{E0001}' in expression; expected end of input.",
        () -> translateExpression("WITH ND-Root COMPUTE 1 '\uDB40\uDC01'"));
  }

  @Test
  void testExpression_CharacterThatCannotBeSeenInTextNotRead_IsWrittenAsAnEscape() {
    assertSyntaxErrorMessage(
        "line 1:23 Syntax error: unexpected 'is not '\\u200B'' in expression.",
        () -> translateExpression("WITH ND-Root COMPUTE 1 is not '\u200B'"));
  }

  @Test
  void testExpression_TextLiteralThatCanBeSeen_IsQuotedAsItIsWritten() {
    assertSyntaxErrorMessage(
        "line 1:23 Syntax error: unexpected 'Ημερομηνία' in expression; expected end of input.",
        () -> translateExpression("WITH ND-Root COMPUTE 1 'Ημερομηνία'"));
    // A combining mark after a letter shows as an accent on it
    assertSyntaxErrorMessage(
        "line 1:23 Syntax error: unexpected 'e\u0301' in expression; expected end of input.",
        () -> translateExpression("WITH ND-Root COMPUTE 1 'e\u0301'"));
  }

  @Test
  void testLexer_CharacterThatCannotBeSeenInTheTextRead_IsWrittenAsAnEscape() {
    final EfxErrorReportingLexer lexer =
        new EfxErrorReportingLexer(CharStreams.fromString("//\u200B"));
    lexer.removeErrorListeners();
    lexer.addErrorListener(ThrowingErrorListener.INSTANCE);
    assertSyntaxErrorMessage(
        "line 1:0 Syntax error: unexpected end of input after '//\\u200B'.",
        lexer::getAllTokens);
  }

  @Test
  void testLexer_CountsTheErrorsItReports() {
    final EfxErrorReportingLexer lexer =
        new EfxErrorReportingLexer(CharStreams.fromString("1 ¤ 2 ¤"));
    lexer.removeErrorListeners();
    lexer.pushMode(EfxLexer.EXPRESSION);
    lexer.getAllTokens();
    assertEquals(2, lexer.getNumberOfSyntaxErrors());
    lexer.setInputStream(CharStreams.fromString("1"));
    assertEquals(0, lexer.getNumberOfSyntaxErrors());
  }

  // #endregion Expressions ----------------------------------------------------

  // #region Parameter values --------------------------------------------------

  @Test
  void testParameterValue_NotALiteral() {
    assertSyntaxErrorMessage("line 1:0 Syntax error: unexpected 'big' in parameter value.",
        () -> translateExpression("WITH ND-Root, text:$p COMPUTE $p", "big"));
  }

  @Test
  void testParameterValue_ClosingBrace() {
    assertSyntaxErrorMessage(
        "line 1:3 Syntax error: unexpected '}' in parameter value; expected end of input.",
        () -> translateExpression("WITH ND-Root, text:$p COMPUTE $p", "'a'}"));
  }

  // #endregion Parameter values -----------------------------------------------

  // #region Templates ---------------------------------------------------------

  @Test
  void testTemplate_ExpressionBlockNotRead() {
    assertSyntaxErrorMessage(
        "line 1:10 Syntax error: unexpected '${BT-00-Text|}' in template content.",
        () -> translateTemplate("{ND-Root} ${BT-00-Text|}"));
  }

  @Test
  void testTemplate_ErrorInAnExpressionBlock() {
    assertSyntaxErrorMessage("line 1:15 Syntax error: unexpected '}' in expression.",
        () -> translateTemplate("{ND-Root} ${1 +}"));
    assertSyntaxErrorMessage(
        "line 1:14 Syntax error: unexpected '2' in template content; expected '|' or '}'.",
        () -> translateTemplate("{ND-Root} ${1 2"));
    assertSyntaxErrorMessage("line 1:26 Syntax error: unexpected '}' in expression.",
        () -> translateTemplate("WITH ND-Root DISPLAY ${1 +};"));
  }

  @Test
  void testTemplate_ErrorInALinkBlock() {
    assertSyntaxErrorMessage("line 1:19 Syntax error: unexpected '}' in expression.",
        () -> translateTemplate("{ND-Root} ${1}@{1 +}"));
    assertSyntaxErrorMessage("line 1:39 Syntax error: unexpected '}' in expression.",
        () -> translateTemplate("{ND-Root} #{field|name|BT-00-Text}@{1 +}"));
  }

  @Test
  void testTemplate_BraceAfterContextBlock_IsNotAnExpressionBlock() {
    assertSyntaxErrorMessage("line 1:9 Syntax error: unexpected '{' in template content.",
        () -> translateTemplate("{ND-Root}{1} a"));
  }

  @Test
  void testTemplate_BraceAfterDisplay_IsNotAnExpressionBlock() {
    assertSyntaxErrorMessage("line 1:20 Syntax error: unexpected '{' in template content.",
        () -> translateTemplate("WITH ND-Root DISPLAY{1} ;"));
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
  void testEndpoint_WithoutName() {
    assertSyntaxErrorMessage("line 1:8 Syntax error: unexpected end of input"
        + " in API endpoint declaration; expected a text literal.",
        () -> translateRules("ENDPOINT"));
  }

  @Test
  void testInclude_EmptyPath() {
    assertSyntaxErrorMessage("line 1:9 Syntax error: unexpected '\"' in #include directive.",
        () -> translateRules("#include \"\"\n"));
  }

  @Test
  void testInclude_PathNotClosedOnItsLine() {
    assertSyntaxErrorMessage(
        "line 1:9 Syntax error: unexpected end of line in #include directive.",
        () -> translateRules("#include \"a\n"));
  }

  @Test
  void testInclude_PathNotQuoted() {
    assertSyntaxErrorMessage(
        "line 1:9 Syntax error: unexpected character 'a' in #include directive.",
        () -> translateRules("#include a.efx\n"));
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

  /**
   * Every token that a rule of the parser reads has a name, so that a message can give it as
   * expected: the tokens are the labels of the transitions of the parser's ATN.
   */
  @Test
  void testEveryTokenThatTheParserReads_HasAName() {
    final IntervalSet read = new IntervalSet();
    for (final ATNState state : EfxParser._ATN.states) {
      if (state != null) {
        for (final Transition transition : state.getTransitions()) {
          if (transition.label() != null) {
            read.addAll(transition.label());
          }
        }
      }
    }
    final List<String> unnamed = read.toList().stream()
        .filter(type -> EfxErrorStrategy.nameOf(type) == null)
        .map(EfxParser.VOCABULARY::getSymbolicName)
        .collect(toList());
    assertEquals(List.of(), unnamed);
  }

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
