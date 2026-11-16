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

import static eu.europa.ted.efx.sdk2.EfxParser.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.antlr.v4.runtime.InputMismatchException;
import org.antlr.v4.runtime.IntStream;
import org.antlr.v4.runtime.NoViableAltException;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.Vocabulary;
import org.antlr.v4.runtime.misc.IntervalSet;

/**
 * Words every syntax error of EFX-2 in one style, as in "Syntax error: unexpected '1' in function
 * call; expected ','." The message names what was found, as written, or the end of the input; the
 * place it was found in, which is the nearest enclosing rule that has a name in
 * {@link #RULE_NAMES}; and, when the rule being read needs one of at most {@value #MOST_EXPECTED}
 * tokens next, those tokens. {@link EfxErrorReportingLexer} words the error of the lexer in the
 * same style.
 */
public class EfxErrorStrategy extends DefaultErrorStrategy {

    /** The most tokens a message names as expected. */
    private static final int MOST_EXPECTED = 3;

    private static final String END_OF_INPUT = "end of input";

    private static final Map<Integer, String> RULE_NAMES = Map.ofEntries(
            // Expression entry points
            Map.entry(RULE_singleExpression, "expression"),
            Map.entry(RULE_expression, "expression"),
            Map.entry(RULE_standardExpressionBlock, "expression block"),
            Map.entry(RULE_expressionBlock, "expression block"),
            Map.entry(RULE_shorthandFieldValueReferenceFromContextField, "field value shorthand"),

            // Literals
            Map.entry(RULE_literal, "literal"),
            Map.entry(RULE_stringLiteral, "text literal"),
            Map.entry(RULE_numericLiteral, "number"),
            Map.entry(RULE_booleanLiteral, "boolean value"),
            Map.entry(RULE_dateLiteral, "date"),
            Map.entry(RULE_timeLiteral, "time"),
            Map.entry(RULE_durationLiteral, "duration"),

            // Functions and arguments
            Map.entry(RULE_builtInFunction, "function call"),
            Map.entry(RULE_functionInvocation, "function call"),
            Map.entry(RULE_argumentList, "argument list"),
            Map.entry(RULE_argument, "argument"),
            Map.entry(RULE_parameterList, "parameter list"),
            Map.entry(RULE_parameterDeclaration, "parameter declaration"),
            Map.entry(RULE_parameterValue, "parameter value"),
            Map.entry(RULE_typeDeclaration, "type"),
            Map.entry(RULE_primitiveType, "type"),

            // Iterators
            Map.entry(RULE_predicate, "condition"),
            Map.entry(RULE_iteratorList, "iterator list"),
            Map.entry(RULE_iteratorExpression, "iterator"),

            // References
            Map.entry(RULE_reference, "reference"),
            Map.entry(RULE_fieldReference, "field reference"),
            Map.entry(RULE_simpleFieldReference, "field reference"),
            Map.entry(RULE_absoluteFieldReference, "field reference"),
            Map.entry(RULE_linkedFieldReference, "field reference"),
            Map.entry(RULE_fieldContext, "field reference"),
            Map.entry(RULE_nodeReference, "node reference"),
            Map.entry(RULE_simpleNodeReference, "node reference"),
            Map.entry(RULE_absoluteNodeReference, "node reference"),
            Map.entry(RULE_nodeContext, "node reference"),
            Map.entry(RULE_codelistReference, "codelist reference"),
            Map.entry(RULE_variableReference, "variable reference"),
            Map.entry(RULE_fieldProperty, "field property"),
            Map.entry(RULE_linkedFieldProperty, "field property"),

            // Variables
            Map.entry(RULE_variableDeclaration, "variable declaration"),
            Map.entry(RULE_contextDeclarationBlock, "context declaration"),
            Map.entry(RULE_contextDeclaration, "context declaration"),
            Map.entry(RULE_variableList, "variable list"),

            // Template structure
            Map.entry(RULE_templateFile, "template"),
            Map.entry(RULE_templateLine, "template line"),
            Map.entry(RULE_templateDeclaration, "template declaration"),
            Map.entry(RULE_templateDefinition, "template definition"),
            Map.entry(RULE_template, "template content"),
            Map.entry(RULE_templateFragment, "template content"),
            Map.entry(RULE_indentation, "indentation"),
            Map.entry(RULE_lineBreak, "line break"),
            Map.entry(RULE_summarySection, "summary section"),
            Map.entry(RULE_navigationSection, "navigation section"),

            // Template blocks
            Map.entry(RULE_labelBlock, "label block"),
            Map.entry(RULE_textBlock, "text"),
            Map.entry(RULE_linkBlock, "link block"),
            Map.entry(RULE_chooseTemplate, "choose block"),
            Map.entry(RULE_displayTemplate, "display clause"),
            Map.entry(RULE_invokeTemplate, "invoke clause"),
            Map.entry(RULE_whenBlock, "when clause"),
            Map.entry(RULE_otherwiseBlock, "otherwise clause"),

            // Global declarations
            Map.entry(RULE_globalDeclaration, "global declaration"),
            Map.entry(RULE_functionDeclaration, "function declaration"),
            Map.entry(RULE_dictionaryDeclaration, "dictionary declaration"),

            // Rules (validation)
            Map.entry(RULE_rulesFile, "rules file"),
            Map.entry(RULE_ruleSet, "rule set"),
            Map.entry(RULE_simpleRule, "rule"),
            Map.entry(RULE_conditionalRule, "rule"),
            Map.entry(RULE_fallbackRule, "rule"),
            Map.entry(RULE_whenClause, "when clause"),
            Map.entry(RULE_assertClause, "assert clause"),
            Map.entry(RULE_reportClause, "report clause"),
            Map.entry(RULE_asClause, "as clause"),
            Map.entry(RULE_severity, "severity"),
            Map.entry(RULE_ruleId, "rule identifier"),
            Map.entry(RULE_forClause, "for clause"),
            Map.entry(RULE_inClause, "in clause"),
            Map.entry(RULE_noticeTypeList, "notice type list"),
            Map.entry(RULE_noticeTypeRange, "notice type range"),
            Map.entry(RULE_noticeType, "notice type"),

            // API declarations
            Map.entry(RULE_apiEndpointDeclaration, "API endpoint declaration"),
            Map.entry(RULE_dynamicFunctionDeclaration, "dynamic function declaration"),
            Map.entry(RULE_onErrorClause, "ON ERROR clause")
    );

    /**
     * How a message names the tokens that have no literal name. A message names no token that has
     * neither.
     */
    private static final Map<Integer, String> TOKEN_NAMES = Map.of(
            Token.EOF, END_OF_INPUT,
            EfxLexer.As, "'AS'",
            EfxLexer.RuleIdentifier, "a rule identifier, such as 'R-K7P-M2Q'",
            EfxLexer.EndBlock, "'}'",
            EfxLexer.IncludeDirective, "'#include'",
            EfxLexer.StageHeaderStart, "a stage header");

    /**
     * Reports text that starts none of the constructs the parser can read there. The message names
     * the whole text the parser read before it gave up.
     */
    @Override
    protected void reportNoViableAlternative(Parser recognizer, NoViableAltException e) {
        final Token start = e.getStartToken();
        final String found = start.getType() == Token.EOF ? END_OF_INPUT
                : quoted(recognizer.getTokenStream().getText(start, e.getOffendingToken()));
        recognizer.notifyErrorListeners(e.getOffendingToken(), message(found, recognizer, ""), e);
    }

    @Override
    protected void reportInputMismatch(Parser recognizer, InputMismatchException e) {
        this.reportUnexpected(recognizer, e.getOffendingToken(), e);
    }

    @Override
    protected void reportUnwantedToken(Parser recognizer) {
        if (this.inErrorRecoveryMode(recognizer)) {
            return;
        }
        this.beginErrorCondition(recognizer);
        this.reportUnexpected(recognizer, recognizer.getCurrentToken(), null);
    }

    @Override
    protected void reportMissingToken(Parser recognizer) {
        if (this.inErrorRecoveryMode(recognizer)) {
            return;
        }
        this.beginErrorCondition(recognizer);
        this.reportUnexpected(recognizer, recognizer.getCurrentToken(), null);
    }

    private void reportUnexpected(final Parser recognizer, final Token found,
            final InputMismatchException e) {
        recognizer.notifyErrorListeners(found,
                message(nameOf(found), recognizer, expected(recognizer)), e);
    }

    private static String message(final String found, final Parser recognizer,
            final String expected) {
        return "Syntax error: unexpected " + found + " in " + placeOf(recognizer) + expected + ".";
    }

    /**
     * @return the name of the nearest rule being read that has one in {@link #RULE_NAMES}
     */
    private static String placeOf(final Parser recognizer) {
        for (RuleContext context = recognizer.getContext(); context != null;
                context = context.parent) {
            final String name = RULE_NAMES.get(context.getRuleIndex());
            if (name != null) {
                return name;
            }
        }
        return "input";
    }

    /**
     * @return the tokens the rule being read needs next, as in "; expected ')' or ','"; nothing
     *         when the rule may end there, when it can take more than {@value #MOST_EXPECTED}
     *         tokens, or when one of them has no name a message can use
     */
    private static String expected(final Parser recognizer) {
        final IntervalSet tokens = recognizer.getExpectedTokensWithinCurrentRule();
        if (tokens.contains(Token.EPSILON) || tokens.size() > MOST_EXPECTED) {
            return "";
        }
        final List<String> names = new ArrayList<>();
        for (final int type : tokens.toList()) {
            final String name = nameOf(type, recognizer.getVocabulary());
            if (name == null) {
                return "";
            }
            names.add(name);
        }
        return names.isEmpty() ? "" : "; expected " + String.join(" or ", names);
    }

    private static String nameOf(final int type, final Vocabulary vocabulary) {
        final String literal = vocabulary.getLiteralName(type);
        return literal != null ? literal : TOKEN_NAMES.get(type);
    }

    /**
     * @return the token as it is written, or "end of input"; a text literal keeps its own quotes
     */
    private static String nameOf(final Token token) {
        if (token.getType() == Token.EOF) {
            return END_OF_INPUT;
        }
        return token.getType() == EfxLexer.StringLiteral ? token.getText()
                : quoted(token.getText());
    }

    private static String quoted(final String text) {
        return "'" + text.replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "'";
    }

    /**
     * Words the error of the lexer: a character that starts no token, as in "Syntax error:
     * unexpected character '¤'.", or a token that the lexer cannot finish, as in "Syntax error:
     * unexpected end of input in text literal." and "Syntax error: unexpected 'q' after '\'."
     *
     * @param read the text the lexer read of the token it could not finish, up to the character it
     *        stopped at; empty when that character starts no token
     * @param stop the character the lexer stopped at, or {@link IntStream#EOF} at the end of the
     *        input
     * @return the message of the lexer's error
     */
    static String unreadable(final String read, final int stop) {
        final String place = read.isEmpty() ? ""
                : read.startsWith("'") || read.startsWith("\"") ? " in text literal"
                        : " after " + quoted(read);
        return "Syntax error: unexpected " + nameOfCharacter(stop, read.isEmpty()) + place + ".";
    }

    private static String nameOfCharacter(final int character, final boolean alone) {
        if (character == IntStream.EOF) {
            return END_OF_INPUT;
        }
        if (character == '\n' || character == '\r') {
            return "end of line";
        }
        return (alone ? "character " : "") + quoted(new String(Character.toChars(character)));
    }
}
