/*
 * Copyright 2025 European Union
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

import java.util.Map;

import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.antlr.v4.runtime.InputMismatchException;
import org.antlr.v4.runtime.NoViableAltException;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.Token;

/**
 * Custom ANTLR error strategy that produces human-readable syntax error messages for EFX code.
 * Replaces cryptic ANTLR-generated messages like "no viable alternative at input '...'" with
 * clearer descriptions of what went wrong.
 */
public class EfxErrorStrategy extends DefaultErrorStrategy {

    private static final Map<Integer, String> RULE_NAMES = Map.ofEntries(
            // Expression entry points
            Map.entry(RULE_singleExpression, "expression"),
            Map.entry(RULE_expression, "expression"),
            Map.entry(RULE_scalarExpression, "expression"),
            Map.entry(RULE_standardExpressionBlock, "expression block"),
            Map.entry(RULE_expressionBlock, "expression block"),
            Map.entry(RULE_shorthandFieldValueReferenceFromContextField, "field value shorthand"),

            // Typed expressions
            Map.entry(RULE_booleanExpression, "boolean expression"),
            Map.entry(RULE_stringExpression, "text expression"),
            Map.entry(RULE_numericExpression, "numeric expression"),
            Map.entry(RULE_dateExpression, "date expression"),
            Map.entry(RULE_timeExpression, "time expression"),
            Map.entry(RULE_durationExpression, "duration expression"),
            Map.entry(RULE_sequenceExpression, "sequence expression"),

            // Sequences
            Map.entry(RULE_stringSequence, "text sequence"),
            Map.entry(RULE_booleanSequence, "boolean sequence"),
            Map.entry(RULE_numericSequence, "numeric sequence"),
            Map.entry(RULE_dateSequence, "date sequence"),
            Map.entry(RULE_timeSequence, "time sequence"),
            Map.entry(RULE_durationSequence, "duration sequence"),

            // Literals
            Map.entry(RULE_stringLiteral, "text literal"),
            Map.entry(RULE_numericLiteral, "number"),
            Map.entry(RULE_booleanLiteral, "boolean value"),
            Map.entry(RULE_dateLiteral, "date"),
            Map.entry(RULE_timeLiteral, "time"),
            Map.entry(RULE_durationLiteral, "duration"),

            // Functions and arguments
            Map.entry(RULE_functionInvocation, "function call"),
            Map.entry(RULE_argumentList, "argument list"),
            Map.entry(RULE_argument, "argument"),
            Map.entry(RULE_parameterList, "parameter list"),
            Map.entry(RULE_parameterDeclaration, "parameter declaration"),
            Map.entry(RULE_parameterValue, "parameter value"),

            // Iterators
            Map.entry(RULE_predicate, "condition"),
            Map.entry(RULE_iteratorList, "iterator list"),
            Map.entry(RULE_iteratorExpression, "iterator"),
            Map.entry(RULE_indexer, "indexer"),

            // References
            Map.entry(RULE_fieldReference, "field reference"),
            Map.entry(RULE_simpleFieldReference, "field reference"),
            Map.entry(RULE_absoluteFieldReference, "field reference"),
            Map.entry(RULE_linkedFieldReference, "field reference"),
            Map.entry(RULE_fieldReferenceWithPredicate, "field reference"),
            Map.entry(RULE_fieldContext, "field reference"),
            Map.entry(RULE_nodeReference, "node reference"),
            Map.entry(RULE_simpleNodeReference, "node reference"),
            Map.entry(RULE_absoluteNodeReference, "node reference"),
            Map.entry(RULE_nodeReferenceWithPredicate, "node reference"),
            Map.entry(RULE_nodeContext, "node reference"),
            Map.entry(RULE_noticeReference, "notice reference"),
            Map.entry(RULE_codelistReference, "codelist reference"),
            Map.entry(RULE_variableReference, "variable reference"),

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
            Map.entry(RULE_dictionaryLookup, "dictionary lookup"),

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
            Map.entry(RULE_noticeType, "notice type")
    );

    @Override
    protected void reportNoViableAlternative(Parser recognizer, NoViableAltException e) {
        String ruleName = getHumanReadableRuleName(recognizer);
        String msg;
        if (e.getStartToken().getType() == Token.EOF) {
            msg = "Syntax error: unexpected end of " + ruleName;
        } else {
            String input = getInputText(e.getStartToken(), e.getOffendingToken(), recognizer);
            msg = "Syntax error at " + input + " in " + ruleName;
        }
        recognizer.notifyErrorListeners(e.getOffendingToken(), msg, e);
    }

    @Override
    protected void reportInputMismatch(Parser recognizer, InputMismatchException e) {
        String expected = e.getExpectedTokens().toString(recognizer.getVocabulary());
        String found = getTokenErrorDisplay(e.getOffendingToken());
        String msg = "Syntax error: expected " + expected + " but found " + found;
        recognizer.notifyErrorListeners(e.getOffendingToken(), msg, e);
    }

    @Override
    protected String getTokenErrorDisplay(Token t) {
        if (t == null) {
            return "''";
        }
        if (t.getType() == Token.EOF) {
            return "end of input";
        }
        return super.getTokenErrorDisplay(t);
    }

    private String getHumanReadableRuleName(Parser recognizer) {
        int ruleIndex = recognizer.getContext().getRuleIndex();
        String name = RULE_NAMES.get(ruleIndex);
        if (name != null) {
            return name;
        }
        String[] ruleNames = recognizer.getRuleNames();
        if (ruleIndex >= 0 && ruleIndex < ruleNames.length) {
            return ruleNames[ruleIndex];
        }
        return "unknown";
    }

    private String getInputText(Token startToken, Token offendingToken, Parser recognizer) {
        if (startToken.getType() == Token.EOF) {
            return "end of input";
        }
        String text = recognizer.getTokenStream()
                .getText(startToken, offendingToken);
        return escapeWSAndQuote(text);
    }
}
