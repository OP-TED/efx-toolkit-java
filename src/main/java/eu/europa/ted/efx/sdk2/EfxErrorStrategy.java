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

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.antlr.v4.runtime.InputMismatchException;
import org.antlr.v4.runtime.IntStream;
import org.antlr.v4.runtime.NoViableAltException;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.misc.IntervalSet;

/**
 * Words every syntax error of EFX-2 in one style, as in "Syntax error: unexpected '1' in function
 * call; expected ','." The message names what was found, as written, or the end of the input; the
 * place it was found in, which is the nearest enclosing rule that has a name in
 * {@link #RULE_NAMES}; and, when the rule being read needs a token next and the tokens it can
 * take have at most {@value #MOST_EXPECTED} names, those names. {@link EfxErrorReportingLexer}
 * words the error of the lexer in the same style.
 */
public class EfxErrorStrategy extends DefaultErrorStrategy {

    /** The most names of tokens a message gives as expected. */
    private static final int MOST_EXPECTED = 3;

    private static final String END_OF_INPUT = "end of input";
    private static final String HEADING_INVOKES =
            "A heading cannot invoke a template. Invoke it from a line under the heading.";
    private static final String DISPLAY_AND_INVOKE_MIXED =
            "A line cannot both display content and invoke a template. Use DISPLAY in all its"
                    + " alternatives, or INVOKE in all of them.";
    private static final String TAG_SPELLING = "A tag after '#' is written in lower-case letters, digits and hyphens, and starts with a letter, as in #notice-info.";

    private static final String CONTEXT_OVERRIDE_HINT =
            "Only a context variable can stand before '::', as in "
                    + "'for context:$c in ND-1 return $c::BT-1'. To keep the BT-2 next to a BT-1, "
                    + "write 'BT-2[BT-1 is present]'.";

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
            Map.entry(RULE_templateBody, "template"),
            Map.entry(RULE_templateSection, "template section"),
            Map.entry(RULE_parametersDeclaration, "parameters declaration"),
            Map.entry(RULE_mainSectionWithHeader, "main section"),
            Map.entry(RULE_mainSection, "main section"),
            Map.entry(RULE_mainSectionLines, "main section"),
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
            Map.entry(RULE_chooseDisplayTemplate, "choose block"),
            Map.entry(RULE_chooseInvokeTemplate, "choose block"),
            Map.entry(RULE_displayTemplate, "display clause"),
            Map.entry(RULE_invokeTemplate, "invoke clause"),
            Map.entry(RULE_whenDisplayTemplate, "when clause"),
            Map.entry(RULE_whenInvokeTemplate, "when clause"),
            Map.entry(RULE_otherwiseDisplayTemplate, "otherwise clause"),
            Map.entry(RULE_otherwiseInvokeTemplate, "otherwise clause"),

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

            // API declarations
            Map.entry(RULE_apiEndpointDeclaration, "API endpoint declaration"),
            Map.entry(RULE_dynamicFunctionDeclaration, "dynamic function declaration"),
            Map.entry(RULE_onErrorClause, "ON ERROR clause")
    );

    /**
     * How a message names each token that the parser reads and that has no literal name: a keyword
     * by its spelling in the lexer grammar, as in "'WITH'", and any other token by what it is.
     * EfxErrorStrategyTest checks that no such token is missing.
     */
    private static final Map<Integer, String> TOKEN_NAMES = Map.ofEntries(
            Map.entry(Token.EOF, END_OF_INPUT),

            // Lines and indentation
            Map.entry(EfxLexer.EndOfLine, "end of line"),
            Map.entry(EfxLexer.Tabs, "tabs"),
            Map.entry(EfxLexer.Spaces, "spaces"),
            Map.entry(EfxLexer.MixedIndent, "tabs and spaces"),
            Map.entry(EfxLexer.OutlineNumber, "an outline number"),
            Map.entry(EfxLexer.AutoOutlineNumber, "'?'"),
            Map.entry(EfxLexer.NoOutlineNumber, "'#'"),
            Map.entry(EfxLexer.OutlineTag, "a tag"),

            // Headers and directives
            Map.entry(EfxLexer.TemplateSectionHeaderStart, "a template section header"),
            Map.entry(EfxLexer.TemplateSectionHeaderEnd, "'---'"),
            Map.entry(EfxLexer.MainSectionHeader, "a main section header"),
            Map.entry(EfxLexer.SummarySectionHeader, "a summary section header"),
            Map.entry(EfxLexer.NavigationSectionHeader, "a navigation section header"),
            Map.entry(EfxLexer.StageHeaderStart, "a stage header"),
            Map.entry(EfxLexer.StageIdentifier, "a stage identifier"),
            Map.entry(EfxLexer.StageHeaderEnd, "'---'"),
            Map.entry(EfxLexer.IncludeDirective, "'#include'"),
            Map.entry(EfxLexer.IncludePath, "a path in double quotes"),

            // Keywords
            Map.entry(EfxLexer.LET, "'LET'"),
            Map.entry(EfxLexer.ENDPOINT, "'ENDPOINT'"),
            Map.entry(EfxLexer.WITH, "'WITH'"),
            Map.entry(EfxLexer.WHEN, "'WHEN'"),
            Map.entry(EfxLexer.OTHERWISE, "'OTHERWISE'"),
            Map.entry(EfxLexer.DISPLAY, "'DISPLAY'"),
            Map.entry(EfxLexer.INVOKE, "'INVOKE'"),
            Map.entry(EfxLexer.PARAMETERS, "'PARAMETERS'"),
            Map.entry(EfxLexer.INDEX, "'INDEX'"),
            Map.entry(EfxLexer.BY, "'BY'"),
            Map.entry(EfxLexer.ASSERT, "'ASSERT'"),
            Map.entry(EfxLexer.REPORT, "'REPORT'"),
            Map.entry(EfxLexer.OTHERWISE_ASSERT, "'OTHERWISE ASSERT'"),
            Map.entry(EfxLexer.OTHERWISE_REPORT, "'OTHERWISE REPORT'"),
            Map.entry(EfxLexer.COMPUTE, "'COMPUTE'"),
            Map.entry(EfxLexer.SELECT, "'SELECT'"),

            // Template text and blocks
            Map.entry(EfxLexer.FreeText, "text"),
            Map.entry(EfxLexer.Whitespace, "whitespace"),
            Map.entry(EfxLexer.NewLine, "'\\n'"),
            Map.entry(EfxLexer.OtherEscapeSequence, "an escape sequence"),
            Map.entry(EfxLexer.CharacterReference, "a character reference, such as '&amp;'"),
            Map.entry(EfxLexer.Semicolon, "';'"),
            Map.entry(EfxLexer.StartExpressionBlock, "'${'"),
            Map.entry(EfxLexer.StartLabelBlock, "'#{'"),
            Map.entry(EfxLexer.StartHyperlinkBlock, "'@{'"),
            Map.entry(EfxLexer.EndBlock, "'}'"),
            Map.entry(EfxLexer.ShorthandFieldValueReferenceFromContextField, "'$value'"),
            Map.entry(EfxLexer.ShorthandIndirectLabelReferenceFromContextField, "'#value'"),
            Map.entry(EfxLexer.FormatDecimals, "a number of decimals"),
            Map.entry(EfxLexer.AssetType, "an asset type, such as 'field'"),
            Map.entry(EfxLexer.LabelType, "a label type, such as 'name'"),
            Map.entry(EfxLexer.OtherAssetId, "an asset identifier"),

            // Identifiers and prefixes
            Map.entry(EfxLexer.FieldId, "a field identifier"),
            Map.entry(EfxLexer.NodeId, "a node identifier"),
            Map.entry(EfxLexer.BtId, "a business term identifier"),
            Map.entry(EfxLexer.RuleIdentifier, "a rule identifier, such as 'R-K7P-M2Q'"),
            Map.entry(EfxLexer.Identifier, "an identifier"),
            Map.entry(EfxLexer.VariablePrefix, "'$'"),
            Map.entry(EfxLexer.AtPrefix, "'@'"),
            Map.entry(EfxLexer.HashPrefix, "'#'"),
            Map.entry(EfxLexer.FunctionPrefix, "'?'"),

            // Literals and operators
            Map.entry(EfxLexer.StringLiteral, "a text literal"),
            Map.entry(EfxLexer.IntegerLiteral, "an integer"),
            Map.entry(EfxLexer.NoticeSubtype, "a notice subtype"),
            Map.entry(EfxLexer.DecimalLiteral, "a decimal number"),
            Map.entry(EfxLexer.DateLiteral, "a date"),
            Map.entry(EfxLexer.TimeLiteral, "a time"),
            Map.entry(EfxLexer.DayTimeDurationLiteral, "a duration"),
            Map.entry(EfxLexer.YearMonthDurationLiteral, "a duration"),
            Map.entry(EfxLexer.UuidV4Literal, "a UUID"),
            Map.entry(EfxLexer.Comparison, "a comparison operator"));

    /**
     * The characters that are drawn as nothing, or as a blank, although their category, a letter, a
     * combining mark or a symbol, says they are seen: the default ignorable code points of Unicode
     * (Default_Ignorable_Code_Point in DerivedCoreProperties.txt) that are neither format
     * characters nor unassigned, and the blank Braille pattern.
     */
    private static final IntervalSet DRAWN_BLANK = new IntervalSet();

    static {
        DRAWN_BLANK.add(0x034F); // combining grapheme joiner
        DRAWN_BLANK.add(0x115F, 0x1160); // Hangul choseong and jungseong fillers
        DRAWN_BLANK.add(0x17B4, 0x17B5); // Khmer inherent vowels
        DRAWN_BLANK.add(0x180B, 0x180D); // Mongolian free variation selectors
        DRAWN_BLANK.add(0x180F); // Mongolian free variation selector four
        DRAWN_BLANK.add(0x2800); // blank Braille pattern
        DRAWN_BLANK.add(0x3164); // Hangul filler
        DRAWN_BLANK.add(0xFE00, 0xFE0F); // variation selectors
        DRAWN_BLANK.add(0xFFA0); // halfwidth Hangul filler
        DRAWN_BLANK.add(0xE0100, 0xE01EF); // variation selectors supplement
        DRAWN_BLANK.setReadonly(true);
    }

    /**
     * Reports text that starts none of the constructs the parser can read there. The message names
     * the whole text the parser read before it gave up, at the position where that text starts.
     */
    @Override
    protected void reportNoViableAlternative(Parser recognizer, NoViableAltException e) {
        final Token start = e.getStartToken();
        final String found = start.getType() == Token.EOF ? END_OF_INPUT
                : quoted(recognizer.getTokenStream().getText(start, e.getOffendingToken()));
        recognizer.notifyErrorListeners(start, message(found, recognizer, ""), e);
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
                message(nameOf(found), recognizer, expected(recognizer))
                        + hintFor(recognizer, found), e);
    }

    /**
     * Gives a hint for {@code ::} written right after an expression, as in {@code ND-1::BT-1}, which
     * is a context override that the grammar no longer takes. Elsewhere, as in a context declaration
     * or after the type of a parameter, {@code ::} is a mistake of another kind, so it gets no hint.
     *
     * @return the hint, after a space, or nothing
     */
    private static String hintFor(final Parser recognizer, final Token found) {
        final String templateLineHint = templateLineHintFor(recognizer, found);
        if (!templateLineHint.isEmpty()) {
            return templateLineHint;
        }
        if (found.getType() == HashPrefix) {
            return inAnExpression(recognizer) ? codelistHintFor(recognizer.getTokenStream(), found)
                    : "";
        }
        if (found.getType() != ColonColon) {
            return "";
        }
        final int before = tokenBefore(recognizer.getTokenStream(), found.getTokenIndex());
        // A rule being read is not yet a child of the rule it is read in, so each one is searched.
        for (RuleContext context = recognizer.getContext(); before >= 0 && context != null;
                context = context.parent) {
            if (endsAnExpression((ParserRuleContext) context, before)) {
                return " " + CONTEXT_OVERRIDE_HINT;
            }
        }
        return "";
    }

    /**
     * Gives a hint for a template line that breaks the rules of a line that invokes a template: a
     * heading that invokes one, or a line whose alternatives both display content and invoke a
     * template. The template is displayed in the place of the line that invokes it, so that line is
     * neither a heading nor a line with content of its own.
     *
     * @return the hint, after a space, or nothing
     */
    private static String templateLineHintFor(final Parser recognizer, final Token found) {
        final RuleContext context = recognizer.getContext();
        TemplateLineContext line = null;
        for (RuleContext above = context; above != null && line == null; above = above.parent) {
            if (above instanceof TemplateLineContext) {
                line = (TemplateLineContext) above;
            }
        }
        if (line == null) {
            return "";
        }
        final boolean invoke = found.getType() == INVOKE;
        final boolean marked = line.OutlineNumber() != null || line.AutoOutlineNumber() != null
                || line.NoOutlineNumber() != null || line.OutlineTag() != null;
        // Only where a template could be invoked or displayed: an INVOKE where the line or a display
        // clause is read, or a DISPLAY where an invoke clause is read. An error inside a condition,
        // an argument list or a template keeps its own message.
        final boolean readingDisplay = context instanceof DisplayTemplateContext
                || context instanceof OtherwiseDisplayTemplateContext
                || context instanceof ChooseDisplayTemplateContext;
        if (invoke && marked && (readingDisplay || context instanceof TemplateLineContext)) {
            return " " + HEADING_INVOKES;
        }
        final boolean readingInvoke = context instanceof InvokeTemplateContext
                || context instanceof OtherwiseInvokeTemplateContext
                || context instanceof ChooseInvokeTemplateContext;
        // After OTHERWISE, DISPLAY may be left out, and the lexer reads "OTHERWISE DISPLAY" as a
        // single token: anything but INVOKE after OTHERWISE is a displaying alternative.
        final int before = tokenBefore(recognizer.getTokenStream(), found.getTokenIndex());
        final Token previous = before >= 0 ? recognizer.getTokenStream().get(before) : null;
        final boolean display = found.getType() == DISPLAY || previous != null
                && (previous.getType() == DISPLAY || previous.getType() == OTHERWISE && !invoke);
        if (invoke && readingDisplay || display && readingInvoke) {
            return " " + DISPLAY_AND_INVOKE_MIXED;
        }
        return "";
    }

    /**
     * @return whether the parser is reading an expression, where a {@code #} can only be meant as
     *         the old form of a codelist reference, and not, as after {@code SCOPE}, a flag
     */
    private static boolean inAnExpression(final Parser recognizer) {
        for (RuleContext context = recognizer.getContext(); context != null;
                context = context.parent) {
            if (context instanceof ExpressionContext) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gives a hint for a codelist written {@code #name}, a form that EFX 2 no longer takes.
     *
     * @return the hint, after a space, or nothing when no name follows the {@code #}
     */
    private static String codelistHintFor(final TokenStream tokens, final Token found) {
        for (int i = found.getTokenIndex() + 1; i < tokens.size(); i++) {
            final Token next = tokens.get(i);
            if (next.getChannel() == Token.DEFAULT_CHANNEL) {
                return next.getType() == Identifier
                        ? " Write a codelist as '[..." + next.getText() + "]'."
                        : "";
            }
        }
        return "";
    }

    /**
     * @return the index of the last token before the given one that the parser reads, or -1
     */
    private static int tokenBefore(final TokenStream tokens, final int index) {
        for (int i = index - 1; i >= 0; i--) {
            if (tokens.get(i).getChannel() == Token.DEFAULT_CHANNEL) {
                return i;
            }
        }
        return -1;
    }

    /**
     * @return whether the rule, or a rule it has read, is an expression that ends at the given
     *         token
     */
    private static boolean endsAnExpression(final ParserRuleContext context, final int tokenIndex) {
        if (context instanceof ExpressionContext && context.getStop() != null
                && context.getStop().getTokenIndex() == tokenIndex) {
            return true;
        }
        if (context.getStart() != null && context.getStart().getTokenIndex() > tokenIndex
                || context.getStop() != null && context.getStop().getTokenIndex() < tokenIndex) {
            return false;
        }
        for (int i = context.getChildCount() - 1; i >= 0; i--) {
            if (context.getChild(i) instanceof ParserRuleContext
                    && endsAnExpression((ParserRuleContext) context.getChild(i), tokenIndex)) {
                return true;
            }
        }
        return false;
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
     *         when the rule may end there, or when the tokens it can take have more than
     *         {@value #MOST_EXPECTED} names
     */
    private static String expected(final Parser recognizer) {
        final IntervalSet tokens = recognizer.getExpectedTokensWithinCurrentRule();
        if (tokens.contains(Token.EPSILON)) {
            return "";
        }
        final Set<String> names = new LinkedHashSet<>();
        for (final int type : tokens.toList()) {
            names.add(nameOf(type));
        }
        return names.isEmpty() || names.size() > MOST_EXPECTED ? ""
                : "; expected " + String.join(" or ", names);
    }

    /**
     * @param type the type of a token that the parser reads
     * @return the name of the token in a message: its literal name, or its name in
     *         {@link #TOKEN_NAMES}
     */
    static String nameOf(final int type) {
        final String literal = EfxParser.VOCABULARY.getLiteralName(type);
        return literal != null ? literal : TOKEN_NAMES.get(type);
    }

    /**
     * @return the token as it is written, or "end of input"; a text literal keeps its own quotes
     */
    private static String nameOf(final Token token) {
        if (token.getType() == Token.EOF) {
            return END_OF_INPUT;
        }
        return token.getType() == EfxLexer.StringLiteral ? visible(token.getText())
                : quoted(token.getText());
    }

    private static String quoted(final String text) {
        return "'" + visible(text) + "'";
    }

    /**
     * Writes the text so that each of its characters can be seen: the line breaks and the tab as
     * {@code \n}, {@code \r} and {@code \t}, and a character that cannot be seen as an escape of
     * its code point, as <code>&#92;u200B</code>, or <code>&#92;u{E0001}</code> outside the Basic
     * Multilingual Plane. Every other character is written as it is.
     */
    private static String visible(final String text) {
        final StringBuilder visible = new StringBuilder();
        text.codePoints().forEach(character -> {
            if (character == '\n') {
                visible.append("\\n");
            } else if (character == '\r') {
                visible.append("\\r");
            } else if (character == '\t') {
                visible.append("\\t");
            } else if (!cannotBeSeen(character)) {
                visible.appendCodePoint(character);
            } else if (Character.isBmpCodePoint(character)) {
                visible.append(String.format(Locale.ROOT, "\\u%04X", character));
            } else {
                visible.append(String.format(Locale.ROOT, "\\u{%X}", character));
            }
        });
        return visible.toString();
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
     * @param mode the mode of the lexer, as {@link EfxLexer#INCLUDE_PATH}
     * @param previousTokenType the type of the token the lexer read last, as
     *        {@link EfxLexer#NoOutlineNumber}, so that an error right after the '#' of a template
     *        line is reported as a tag that is not written as one, at the first character after
     *        the '#'
     * @return the message of the lexer's error
     */
    static String unreadable(final String read, final int stop, final int mode,
            final int previousTokenType) {
        final int first = read.isEmpty() ? stop : read.codePointAt(0);
        if (previousTokenType == EfxLexer.NoOutlineNumber && first != IntStream.EOF
                && first != '\n' && first != '\r') {
            return "Syntax error: unexpected " + nameOfCharacter(first, false) + " after '#'. "
                    + TAG_SPELLING;
        }
        return "Syntax error: unexpected " + nameOfCharacter(stop, read.isEmpty())
                + placeOf(read, mode) + ".";
    }

    /**
     * @return where the lexer stopped: in an #include directive, whose path is all the lexer reads
     *         in mode {@link EfxLexer#INCLUDE_PATH}; in a text literal; after the text it read of
     *         another token; or nothing, when the character it stopped at starts no token
     */
    private static String placeOf(final String read, final int mode) {
        if (mode == EfxLexer.INCLUDE_PATH) {
            return " in #include directive";
        }
        if (read.isEmpty()) {
            return "";
        }
        return read.startsWith("'") || read.startsWith("\"") ? " in text literal"
                : " after " + quoted(read);
    }

    /**
     * @param alone whether the character starts no token, so that the message calls it a
     *        character
     * @return the character in quotes, as in "'¤'"; the end of the input or of the line; or, for
     *         a character that cannot be shown between quotes, its code point, as in "character
     *         U+200B"
     */
    private static String nameOfCharacter(final int character, final boolean alone) {
        if (character == IntStream.EOF) {
            return END_OF_INPUT;
        }
        if (character == '\n' || character == '\r') {
            return "end of line";
        }
        if (cannotBeShownInQuotes(character)) {
            return String.format(Locale.ROOT, "character U+%04X", character);
        }
        return (alone ? "character " : "") + quoted(new String(Character.toChars(character)));
    }

    /**
     * @return whether the character cannot be shown alone between quotes: it cannot be seen, or it
     *         is a combining mark, which joins the quote before it; not the tab, which a message
     *         writes as '\t'
     */
    private static boolean cannotBeShownInQuotes(final int character) {
        if (character == '\t') {
            return false;
        }
        switch (Character.getType(character)) {
            case Character.NON_SPACING_MARK:
            case Character.COMBINING_SPACING_MARK:
            case Character.ENCLOSING_MARK:
                return true;
            default:
                return cannotBeSeen(character);
        }
    }

    /**
     * @return whether the character is drawn as nothing, or as a blank other than the space ' ':
     *         a space of another kind, a line or paragraph separator, a control, a format
     *         character, a private-use, surrogate or unassigned code point, or one of
     *         {@link #DRAWN_BLANK}
     */
    private static boolean cannotBeSeen(final int character) {
        if (character == ' ') {
            return false;
        }
        if (DRAWN_BLANK.contains(character)) {
            return true;
        }
        switch (Character.getType(character)) {
            case Character.SPACE_SEPARATOR:
            case Character.LINE_SEPARATOR:
            case Character.PARAGRAPH_SEPARATOR:
            case Character.CONTROL:
            case Character.FORMAT:
            case Character.PRIVATE_USE:
            case Character.SURROGATE:
            case Character.UNASSIGNED:
                return true;
            default:
                return false;
        }
    }
}
