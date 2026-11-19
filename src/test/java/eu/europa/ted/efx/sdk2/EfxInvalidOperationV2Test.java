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

import static eu.europa.ted.efx.exceptions.TypeMismatchException.ErrorCode.CANNOT_CONVERT;
import static eu.europa.ted.efx.exceptions.TypeMismatchException.ErrorCode.FIELD_IS_MULTILINGUAL;
import static eu.europa.ted.efx.exceptions.TypeMismatchException.ErrorCode.FIELD_MAY_REPEAT;
import static eu.europa.ted.efx.exceptions.TypeMismatchException.ErrorCode.INCOMPATIBLE_OPERANDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import eu.europa.ted.efx.EfxTestsBase;
import eu.europa.ted.efx.exceptions.ThrowingErrorListener;
import eu.europa.ted.efx.exceptions.TypeMismatchException;

/**
 * The table of operations that EFX rejects: one row per expression, grouped by the rule the
 * expression breaks.
 * <p>
 * Each row states the error its expression fails with, so that a row holds whether the parser or
 * the translator rejects the expression, and why. A syntax error is exactly a
 * {@link ParseCancellationException}, as the {@link ThrowingErrorListener} raises it, not one of
 * the compilation errors that extend it. A type error is a {@link TypeMismatchException} with its
 * code and its first sentence, which names the construct and the types of all its operands: a row
 * that broke another rule than its own would name another construct or other types. The rest of
 * the message is pinned once for each construct, in the tests of the translator that raises it.
 * <p>
 * Binary operations appear in both operand orders, and with literals as well as field references,
 * because the two kinds of operand are rejected along different paths.
 * <p>
 * Template-only functions are rejected outside templates whatever their arguments, so their rows
 * are translated inside a template, where only the arguments can be wrong.
 * <p>
 * Some references can be written only since the grammar stopped checking types, and only the
 * translator rejects them, so their rows expect a type error.
 */
class EfxInvalidOperationV2Test extends EfxTestsBase {

  @Override
  protected String getSdkVersion() {
    return "eforms-sdk-2.0";
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidOperations")
  void testInvalidOperation_IsRejected(final String expression, final Outcome outcome) {
    outcome.assertRaisedBy(() -> translateExpressionWithContext("ND-Root", expression));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidTemplateOperations")
  void testInvalidTemplateOperation_IsRejected(final String expression, final Outcome outcome) {
    outcome.assertRaisedBy(() -> translateTemplate("{ND-Root} ${" + expression + "}"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidReferences")
  void testInvalidReference_IsTypeMismatch(final String expression, final Outcome outcome) {
    outcome.assertRaisedBy(() -> translateExpressionWithContext("ND-Root", expression));
  }

  static Stream<Arguments> invalidOperations() {
    return Stream.of(
        // A comparison needs two operands of the same type
        typeError("'a' == 1", INCOMPATIBLE_OPERANDS,
            "Operator '==' cannot take 'text' and 'number'"),
        typeError("1 == 'a'", INCOMPATIBLE_OPERANDS,
            "Operator '==' cannot take 'number' and 'text'"),
        typeError("BT-00-Text == BT-00-Number", INCOMPATIBLE_OPERANDS,
            "Operator '==' cannot take 'text' and 'number'"),
        typeError("BT-00-Number == 'a'", INCOMPATIBLE_OPERANDS,
            "Operator '==' cannot take 'number' and 'text'"),
        typeError("'a' != BT-00-Number", INCOMPATIBLE_OPERANDS,
            "Operator '!=' cannot take 'text' and 'number'"),
        typeError("'a' < TRUE", INCOMPATIBLE_OPERANDS,
            "Operator '<' cannot take 'text' and 'indicator'"),
        typeError("BT-00-Indicator > 1", INCOMPATIBLE_OPERANDS,
            "Operator '>' cannot take 'indicator' and 'number'"),
        typeError("'a' <= 2020-01-01Z", INCOMPATIBLE_OPERANDS,
            "Operator '<=' cannot take 'text' and 'date'"),
        typeError("BT-00-StartTime == 'a'", INCOMPATIBLE_OPERANDS,
            "Operator '==' cannot take 'time' and 'text'"),
        typeError("'a' >= P1D", INCOMPATIBLE_OPERANDS,
            "Operator '>=' cannot take 'text' and 'duration'"),
        typeError("1 == TRUE", INCOMPATIBLE_OPERANDS,
            "Operator '==' cannot take 'number' and 'indicator'"),
        typeError("BT-00-Number < BT-00-StartDate", INCOMPATIBLE_OPERANDS,
            "Operator '<' cannot take 'number' and 'date'"),
        typeError("10:00:00Z > 1", INCOMPATIBLE_OPERANDS,
            "Operator '>' cannot take 'time' and 'number'"),
        typeError("1 != BT-00-Duration", INCOMPATIBLE_OPERANDS,
            "Operator '!=' cannot take 'number' and 'duration'"),
        typeError("TRUE == 2020-01-01Z", INCOMPATIBLE_OPERANDS,
            "Operator '==' cannot take 'indicator' and 'date'"),
        typeError("BT-00-StartTime != BT-00-Indicator", INCOMPATIBLE_OPERANDS,
            "Operator '!=' cannot take 'time' and 'indicator'"),
        typeError("BT-00-Indicator == P1D", INCOMPATIBLE_OPERANDS,
            "Operator '==' cannot take 'indicator' and 'duration'"),
        typeError("2020-01-01Z == 10:00:00Z", INCOMPATIBLE_OPERANDS,
            "Operator '==' cannot take 'date' and 'time'"),
        typeError("BT-00-StartDate == BT-00-StartTime", INCOMPATIBLE_OPERANDS,
            "Operator '==' cannot take 'date' and 'time'"),
        typeError("2020-01-01Z < BT-00-Duration", INCOMPATIBLE_OPERANDS,
            "Operator '<' cannot take 'date' and 'duration'"),
        typeError("P1D > 10:00:00Z", INCOMPATIBLE_OPERANDS,
            "Operator '>' cannot take 'duration' and 'time'"),
        typeError("BT-00-StartTime <= BT-00-Duration", INCOMPATIBLE_OPERANDS,
            "Operator '<=' cannot take 'time' and 'duration'"),

        // + adds two numbers, two durations, or a duration to a date
        typeError("'a' + 'b'", INCOMPATIBLE_OPERANDS,
            "Operator '+' cannot take 'text' and 'text'"),
        typeError("BT-00-Text + BT-00-Text", INCOMPATIBLE_OPERANDS,
            "Operator '+' cannot take 'text' and 'text'"),
        typeError("BT-00-Text + 1", INCOMPATIBLE_OPERANDS,
            "Operator '+' cannot take 'text' and 'number'"),
        typeError("'a' + 1", INCOMPATIBLE_OPERANDS,
            "Operator '+' cannot take 'text' and 'number'"),
        typeError("1 + 'a'", INCOMPATIBLE_OPERANDS,
            "Operator '+' cannot take 'number' and 'text'"),
        typeError("TRUE + 1", INCOMPATIBLE_OPERANDS,
            "Operator '+' cannot take 'indicator' and 'number'"),
        typeError("BT-00-Number + BT-00-Indicator", INCOMPATIBLE_OPERANDS,
            "Operator '+' cannot take 'number' and 'indicator'"),
        typeError("2020-01-01Z + 2020-01-02Z", INCOMPATIBLE_OPERANDS,
            "Operator '+' cannot take 'date' and 'date'"),
        typeError("BT-00-StartDate + BT-00-EndDate", INCOMPATIBLE_OPERANDS,
            "Operator '+' cannot take 'date' and 'date'"),
        typeError("1 + BT-00-StartDate", INCOMPATIBLE_OPERANDS,
            "Operator '+' cannot take 'number' and 'date'"),
        typeError("10:00:00Z + P1D", INCOMPATIBLE_OPERANDS,
            "Operator '+' cannot take 'time' and 'duration'"),
        typeError("BT-00-StartTime + BT-00-Duration", INCOMPATIBLE_OPERANDS,
            "Operator '+' cannot take 'time' and 'duration'"),
        typeError("BT-00-StartTime + P1D", INCOMPATIBLE_OPERANDS,
            "Operator '+' cannot take 'time' and 'duration'"),
        typeError("1 + P1D", INCOMPATIBLE_OPERANDS,
            "Operator '+' cannot take 'number' and 'duration'"),
        typeError("BT-00-Duration + BT-00-Number", INCOMPATIBLE_OPERANDS,
            "Operator '+' cannot take 'duration' and 'number'"),
        typeError("P1D + BT-00-Number", INCOMPATIBLE_OPERANDS,
            "Operator '+' cannot take 'duration' and 'number'"),
        typeError("1 + 10:00:00Z", INCOMPATIBLE_OPERANDS,
            "Operator '+' cannot take 'number' and 'time'"),
        typeError("2020-01-01Z + 1", INCOMPATIBLE_OPERANDS,
            "Operator '+' cannot take 'date' and 'number'"),

        // - subtracts two numbers, two durations, two dates, or a duration from a date
        typeError("BT-00-Text - 1", INCOMPATIBLE_OPERANDS,
            "Operator '-' cannot take 'text' and 'number'"),
        typeError("'a' - 'b'", INCOMPATIBLE_OPERANDS,
            "Operator '-' cannot take 'text' and 'text'"),
        typeError("1 - BT-00-Indicator", INCOMPATIBLE_OPERANDS,
            "Operator '-' cannot take 'number' and 'indicator'"),
        typeError("TRUE - FALSE", INCOMPATIBLE_OPERANDS,
            "Operator '-' cannot take 'indicator' and 'indicator'"),
        typeError("1 - 2020-01-01Z", INCOMPATIBLE_OPERANDS,
            "Operator '-' cannot take 'number' and 'date'"),
        typeError("BT-00-Number - BT-00-StartDate", INCOMPATIBLE_OPERANDS,
            "Operator '-' cannot take 'number' and 'date'"),
        typeError("2020-01-01Z - 1", INCOMPATIBLE_OPERANDS,
            "Operator '-' cannot take 'date' and 'number'"),
        typeError("11:00:00Z - 10:00:00Z", INCOMPATIBLE_OPERANDS,
            "Operator '-' cannot take 'time' and 'time'"),
        typeError("BT-00-EndTime - BT-00-StartTime", INCOMPATIBLE_OPERANDS,
            "Operator '-' cannot take 'time' and 'time'"),
        typeError("BT-00-Duration - BT-00-Number", INCOMPATIBLE_OPERANDS,
            "Operator '-' cannot take 'duration' and 'number'"),
        typeError("P1D - 2020-01-01Z", INCOMPATIBLE_OPERANDS,
            "Operator '-' cannot take 'duration' and 'date'"),
        typeError("BT-00-Duration - BT-00-StartDate", INCOMPATIBLE_OPERANDS,
            "Operator '-' cannot take 'duration' and 'date'"),
        typeError("TRUE - 1", INCOMPATIBLE_OPERANDS,
            "Operator '-' cannot take 'indicator' and 'number'"),
        typeError("10:00:00Z - 1", INCOMPATIBLE_OPERANDS,
            "Operator '-' cannot take 'time' and 'number'"),
        typeError("1 - 'a'", INCOMPATIBLE_OPERANDS,
            "Operator '-' cannot take 'number' and 'text'"),
        typeError("1 - 10:00:00Z", INCOMPATIBLE_OPERANDS,
            "Operator '-' cannot take 'number' and 'time'"),
        typeError("1 - P1D", INCOMPATIBLE_OPERANDS,
            "Operator '-' cannot take 'number' and 'duration'"),

        // * multiplies two numbers, or a number and a duration
        typeError("'a' * 2", INCOMPATIBLE_OPERANDS,
            "Operator '*' cannot take 'text' and 'number'"),
        typeError("2 * BT-00-Text", INCOMPATIBLE_OPERANDS,
            "Operator '*' cannot take 'number' and 'text'"),
        typeError("TRUE * 2", INCOMPATIBLE_OPERANDS,
            "Operator '*' cannot take 'indicator' and 'number'"),
        typeError("BT-00-Indicator * BT-00-Number", INCOMPATIBLE_OPERANDS,
            "Operator '*' cannot take 'indicator' and 'number'"),
        typeError("2020-01-01Z * 2", INCOMPATIBLE_OPERANDS,
            "Operator '*' cannot take 'date' and 'number'"),
        typeError("2 * BT-00-StartDate", INCOMPATIBLE_OPERANDS,
            "Operator '*' cannot take 'number' and 'date'"),
        typeError("10:00:00Z * 2", INCOMPATIBLE_OPERANDS,
            "Operator '*' cannot take 'time' and 'number'"),
        typeError("P1D * P2D", INCOMPATIBLE_OPERANDS,
            "Operator '*' cannot take 'duration' and 'duration'"),
        typeError("BT-00-Duration * BT-00-Duration", INCOMPATIBLE_OPERANDS,
            "Operator '*' cannot take 'duration' and 'duration'"),
        typeError("2 * TRUE", INCOMPATIBLE_OPERANDS,
            "Operator '*' cannot take 'number' and 'indicator'"),
        typeError("2 * 10:00:00Z", INCOMPATIBLE_OPERANDS,
            "Operator '*' cannot take 'number' and 'time'"),

        // / and % divide numbers only
        typeError("'a' / 2", INCOMPATIBLE_OPERANDS,
            "Operator '/' cannot take 'text' and 'number'"),
        typeError("BT-00-Number / BT-00-Text", INCOMPATIBLE_OPERANDS,
            "Operator '/' cannot take 'number' and 'text'"),
        typeError("TRUE / 2", INCOMPATIBLE_OPERANDS,
            "Operator '/' cannot take 'indicator' and 'number'"),
        typeError("2 / 2020-01-01Z", INCOMPATIBLE_OPERANDS,
            "Operator '/' cannot take 'number' and 'date'"),
        typeError("BT-00-StartTime / 2", INCOMPATIBLE_OPERANDS,
            "Operator '/' cannot take 'time' and 'number'"),
        typeError("P2D / 2", INCOMPATIBLE_OPERANDS,
            "Operator '/' cannot take 'duration' and 'number'"),
        typeError("BT-00-Duration / 2", INCOMPATIBLE_OPERANDS,
            "Operator '/' cannot take 'duration' and 'number'"),
        typeError("2 / P1D", INCOMPATIBLE_OPERANDS,
            "Operator '/' cannot take 'number' and 'duration'"),
        typeError("5 % 'a'", INCOMPATIBLE_OPERANDS,
            "Operator '%' cannot take 'number' and 'text'"),
        typeError("TRUE % 2", INCOMPATIBLE_OPERANDS,
            "Operator '%' cannot take 'indicator' and 'number'"),
        typeError("2020-01-01Z % 2", INCOMPATIBLE_OPERANDS,
            "Operator '%' cannot take 'date' and 'number'"),
        typeError("5 % BT-00-StartTime", INCOMPATIBLE_OPERANDS,
            "Operator '%' cannot take 'number' and 'time'"),
        typeError("BT-00-Duration % 2", INCOMPATIBLE_OPERANDS,
            "Operator '%' cannot take 'duration' and 'number'"),
        typeError("2020-01-01Z / 2", INCOMPATIBLE_OPERANDS,
            "Operator '/' cannot take 'date' and 'number'"),
        typeError("2 / TRUE", INCOMPATIBLE_OPERANDS,
            "Operator '/' cannot take 'number' and 'indicator'"),
        typeError("2 / 10:00:00Z", INCOMPATIBLE_OPERANDS,
            "Operator '/' cannot take 'number' and 'time'"),
        typeError("'a' % 2", INCOMPATIBLE_OPERANDS,
            "Operator '%' cannot take 'text' and 'number'"),
        typeError("10:00:00Z % 2", INCOMPATIBLE_OPERANDS,
            "Operator '%' cannot take 'time' and 'number'"),
        typeError("2 % TRUE", INCOMPATIBLE_OPERANDS,
            "Operator '%' cannot take 'number' and 'indicator'"),
        typeError("2 % 2020-01-01Z", INCOMPATIBLE_OPERANDS,
            "Operator '%' cannot take 'number' and 'date'"),
        typeError("2 % P1D", INCOMPATIBLE_OPERANDS,
            "Operator '%' cannot take 'number' and 'duration'"),

        // and, or and not() take booleans
        typeError("1 and TRUE", INCOMPATIBLE_OPERANDS,
            "Operator 'and' cannot take 'number' and 'indicator'"),
        typeError("TRUE or 'a'", INCOMPATIBLE_OPERANDS,
            "Operator 'or' cannot take 'indicator' and 'text'"),
        typeError("BT-00-Text and TRUE", INCOMPATIBLE_OPERANDS,
            "Operator 'and' cannot take 'text' and 'indicator'"),
        typeError("TRUE or BT-00-Number", INCOMPATIBLE_OPERANDS,
            "Operator 'or' cannot take 'indicator' and 'number'"),
        typeError("BT-00-Text and BT-00-Indicator", INCOMPATIBLE_OPERANDS,
            "Operator 'and' cannot take 'text' and 'indicator'"),
        typeError("2020-01-01Z or TRUE", INCOMPATIBLE_OPERANDS,
            "Operator 'or' cannot take 'date' and 'indicator'"),
        typeError("P1D and BT-00-StartTime", INCOMPATIBLE_OPERANDS,
            "Operator 'and' cannot take 'duration' and 'time'"),
        typeError("not(1)", CANNOT_CONVERT,
            "Function 'not' cannot take 'number'"),
        typeError("not(BT-00-Text)", CANNOT_CONVERT,
            "Function 'not' cannot take 'text'"),
        typeError("not(BT-00-Duration)", CANNOT_CONVERT,
            "Function 'not' cannot take 'duration'"),
        typeError("TRUE and 2020-01-01Z", INCOMPATIBLE_OPERANDS,
            "Operator 'and' cannot take 'indicator' and 'date'"),
        typeError("10:00:00Z or TRUE", INCOMPATIBLE_OPERANDS,
            "Operator 'or' cannot take 'time' and 'indicator'"),
        typeError("TRUE or P1D", INCOMPATIBLE_OPERANDS,
            "Operator 'or' cannot take 'indicator' and 'duration'"),
        typeError("not(2020-01-01Z)", CANNOT_CONVERT,
            "Function 'not' cannot take 'date'"),
        typeError("not(10:00:00Z)", CANNOT_CONVERT,
            "Function 'not' cannot take 'time'"),
        typeError("2020-01-01Z and TRUE", INCOMPATIBLE_OPERANDS,
            "Operator 'and' cannot take 'date' and 'indicator'"),
        typeError("10:00:00Z and TRUE", INCOMPATIBLE_OPERANDS,
            "Operator 'and' cannot take 'time' and 'indicator'"),
        typeError("P1D and TRUE", INCOMPATIBLE_OPERANDS,
            "Operator 'and' cannot take 'duration' and 'indicator'"),
        typeError("TRUE and 1", INCOMPATIBLE_OPERANDS,
            "Operator 'and' cannot take 'indicator' and 'number'"),
        typeError("TRUE and 'a'", INCOMPATIBLE_OPERANDS,
            "Operator 'and' cannot take 'indicator' and 'text'"),
        typeError("TRUE and BT-00-StartTime", INCOMPATIBLE_OPERANDS,
            "Operator 'and' cannot take 'indicator' and 'time'"),
        typeError("TRUE and P1D", INCOMPATIBLE_OPERANDS,
            "Operator 'and' cannot take 'indicator' and 'duration'"),
        typeError("1 or TRUE", INCOMPATIBLE_OPERANDS,
            "Operator 'or' cannot take 'number' and 'indicator'"),
        typeError("'a' or TRUE", INCOMPATIBLE_OPERANDS,
            "Operator 'or' cannot take 'text' and 'indicator'"),
        typeError("P1D or TRUE", INCOMPATIBLE_OPERANDS,
            "Operator 'or' cannot take 'duration' and 'indicator'"),
        typeError("TRUE or 2020-01-01Z", INCOMPATIBLE_OPERANDS,
            "Operator 'or' cannot take 'indicator' and 'date'"),
        typeError("TRUE or 10:00:00Z", INCOMPATIBLE_OPERANDS,
            "Operator 'or' cannot take 'indicator' and 'time'"),

        // like matches text
        typeError("1 like '[0-9]'", CANNOT_CONVERT,
            "Operator 'like' cannot take 'number'"),
        typeError("BT-00-Number like '[0-9]*'", CANNOT_CONVERT,
            "Operator 'like' cannot take 'number'"),
        typeError("TRUE like 'true'", CANNOT_CONVERT,
            "Operator 'like' cannot take 'indicator'"),
        typeError("BT-00-StartDate like '2020'", CANNOT_CONVERT,
            "Operator 'like' cannot take 'date'"),
        typeError("10:00:00Z like '10'", CANNOT_CONVERT,
            "Operator 'like' cannot take 'time'"),
        typeError("BT-00-Duration like 'P1D'", CANNOT_CONVERT,
            "Operator 'like' cannot take 'duration'"),

        // in looks for a value in a sequence of the same type
        typeError("1 in ['a', 'b']", INCOMPATIBLE_OPERANDS,
            "Operator 'in' cannot take 'number' and 'text*'"),
        typeError("'a' in [1, 2]", INCOMPATIBLE_OPERANDS,
            "Operator 'in' cannot take 'text' and 'number*'"),
        typeError("BT-00-Number in ['a', 'b']", INCOMPATIBLE_OPERANDS,
            "Operator 'in' cannot take 'number' and 'text*'"),
        typeError("'a' in BT-00-Repeatable-Number", INCOMPATIBLE_OPERANDS,
            "Operator 'in' cannot take 'text' and 'number*'"),
        typeError("BT-00-Text in BT-00-Repeatable-Number", INCOMPATIBLE_OPERANDS,
            "Operator 'in' cannot take 'text' and 'number*'"),
        typeError("TRUE in [1, 2]", INCOMPATIBLE_OPERANDS,
            "Operator 'in' cannot take 'indicator' and 'number*'"),
        typeError("2020-01-01Z not in [10:00:00Z]", INCOMPATIBLE_OPERANDS,
            "Operator 'not in' cannot take 'date' and 'time*'"),
        typeError("P1D in [1, 2]", INCOMPATIBLE_OPERANDS,
            "Operator 'in' cannot take 'duration' and 'number*'"),
        typeError("BT-00-StartTime in BT-00-Repeatable-Date", INCOMPATIBLE_OPERANDS,
            "Operator 'in' cannot take 'time' and 'date*'"),

        // is unique in looks for a value in a sequence of the same type
        typeError("'a' is unique in [1, 2]", INCOMPATIBLE_OPERANDS,
            "Operator 'is unique in' cannot take 'text' and 'number*'"),
        typeError("1 is unique in BT-00-Repeatable-Text", INCOMPATIBLE_OPERANDS,
            "Operator 'is unique in' cannot take 'number' and 'text*'"),
        typeError("BT-00-Number is unique in ['a']", INCOMPATIBLE_OPERANDS,
            "Operator 'is unique in' cannot take 'number' and 'text*'"),
        typeError("BT-00-Text is unique in BT-00-Repeatable-Number", INCOMPATIBLE_OPERANDS,
            "Operator 'is unique in' cannot take 'text' and 'number*'"),
        typeError("2020-01-01Z is unique in [P1D]", INCOMPATIBLE_OPERANDS,
            "Operator 'is unique in' cannot take 'date' and 'duration*'"),
        typeError("TRUE is not unique in [10:00:00Z]", INCOMPATIBLE_OPERANDS,
            "Operator 'is not unique in' cannot take 'indicator' and 'time*'"),

        // The condition of if ... then ... else is a boolean
        typeError("if 1 then 'a' else 'b'", CANNOT_CONVERT,
            "Conditional 'if' cannot take 'number', 'text' and 'text'"),
        typeError("if 'a' then 1 else 2", CANNOT_CONVERT,
            "Conditional 'if' cannot take 'text', 'number' and 'number'"),
        typeError("if BT-00-Text then 1 else 2", CANNOT_CONVERT,
            "Conditional 'if' cannot take 'text', 'number' and 'number'"),
        typeError("if BT-00-Number then 'a' else 'b'", CANNOT_CONVERT,
            "Conditional 'if' cannot take 'number', 'text' and 'text'"),
        typeError("if 2020-01-01Z then TRUE else FALSE", CANNOT_CONVERT,
            "Conditional 'if' cannot take 'date', 'indicator' and 'indicator'"),
        typeError("if 10:00:00Z then 1 else 2", CANNOT_CONVERT,
            "Conditional 'if' cannot take 'time', 'number' and 'number'"),
        typeError("if P1D then 1 else 2", CANNOT_CONVERT,
            "Conditional 'if' cannot take 'duration', 'number' and 'number'"),
        typeError("if 1 then [1] else 2", CANNOT_CONVERT,
            "Conditional 'if' cannot take 'number', 'number*' and 'number'"),

        // The two branches of if ... then ... else have the same primitive
        typeError("if TRUE then 1 else 'a'", CANNOT_CONVERT,
            "Conditional 'if' cannot take 'indicator', 'number' and 'text'"),
        typeError("if TRUE then 'a' else BT-00-Number", CANNOT_CONVERT,
            "Conditional 'if' cannot take 'indicator', 'text' and 'number'"),
        typeError("if TRUE then BT-00-StartDate else BT-00-Text", CANNOT_CONVERT,
            "Conditional 'if' cannot take 'indicator', 'date' and 'text'"),
        typeError("if TRUE then 2020-01-01Z else P1D", CANNOT_CONVERT,
            "Conditional 'if' cannot take 'indicator', 'date' and 'duration'"),
        typeError("if TRUE then 10:00:00Z else BT-00-StartDate", CANNOT_CONVERT,
            "Conditional 'if' cannot take 'indicator', 'time' and 'date'"),
        typeError("if TRUE then BT-00-Indicator else 1", CANNOT_CONVERT,
            "Conditional 'if' cannot take 'indicator', 'indicator' and 'number'"),
        typeError("if TRUE then [1] else ['a']", CANNOT_CONVERT,
            "Conditional 'if' cannot take 'indicator', 'number*' and 'text*'"),
        typeError("if TRUE then 1 else ['a']", CANNOT_CONVERT,
            "Conditional 'if' cannot take 'indicator', 'number' and 'text*'"),
        typeError("if TRUE then ['a'] else 1", CANNOT_CONVERT,
            "Conditional 'if' cannot take 'indicator', 'text*' and 'number'"),
        typeError("if TRUE then BT-00-Repeatable-Number else 'a'", CANNOT_CONVERT,
            "Conditional 'if' cannot take 'indicator', 'number*' and 'text'"),

        // The body of every ... satisfies and some ... satisfies is a boolean
        typeError("every text:$x in ['a', 'b'] satisfies $x", CANNOT_CONVERT,
            "Quantifier 'every' cannot take 'text'"),
        typeError("some number:$n in [1, 2] satisfies $n + 1", CANNOT_CONVERT,
            "Quantifier 'some' cannot take 'number'"),
        typeError("every text:$x in ['a'] satisfies 'a'", CANNOT_CONVERT,
            "Quantifier 'every' cannot take 'text'"),
        typeError("some date:$d in [2020-01-01Z] satisfies $d", CANNOT_CONVERT,
            "Quantifier 'some' cannot take 'date'"),
        typeError("some time:$t in [10:00:00Z] satisfies $t", CANNOT_CONVERT,
            "Quantifier 'some' cannot take 'time'"),
        typeError("every duration:$d in [P1D] satisfies $d", CANNOT_CONVERT,
            "Quantifier 'every' cannot take 'duration'"),

        // The values of a list have one type
        typeError("[1, 'a']", CANNOT_CONVERT,
            "List cannot take 'number' and 'text'"),
        typeError("['a', TRUE]", CANNOT_CONVERT,
            "List cannot take 'text' and 'indicator'"),
        typeError("[2020-01-01Z, 10:00:00Z]", CANNOT_CONVERT,
            "List cannot take 'date' and 'time'"),
        typeError("[P1D, 1]", CANNOT_CONVERT,
            "List cannot take 'duration' and 'number'"),
        typeError("[1, BT-00-Text]", CANNOT_CONVERT,
            "List cannot take 'number' and 'text'"),
        typeError("[BT-00-Number, BT-00-Text]", CANNOT_CONVERT,
            "List cannot take 'number' and 'text'"),
        typeError("[BT-00-StartDate, BT-00-StartTime]", CANNOT_CONVERT,
            "List cannot take 'date' and 'time'"),
        typeError("count([1, 'a'])", CANNOT_CONVERT,
            "List cannot take 'number' and 'text'"),

        // An iterator variable has the type of the sequence it iterates over
        typeError("for number:$n in ['a'] return $n", CANNOT_CONVERT,
            "Iterator 'number:$n' cannot take 'text*'"),
        typeError("for text:$t in [1, 2] return $t", CANNOT_CONVERT,
            "Iterator 'text:$t' cannot take 'number*'"),
        typeError("for date:$d in BT-00-Repeatable-Text return $d", CANNOT_CONVERT,
            "Iterator 'date:$d' cannot take 'text*'"),
        typeError("for indicator:$i in [1] return $i", CANNOT_CONVERT,
            "Iterator 'indicator:$i' cannot take 'number*'"),
        typeError("for text:$t in BT-00-Repeatable-Number return $t", CANNOT_CONVERT,
            "Iterator 'text:$t' cannot take 'number*'"),
        typeError("every time:$t in [2020-01-01Z] satisfies TRUE", CANNOT_CONVERT,
            "Iterator 'time:$t' cannot take 'date*'"),
        typeError("some duration:$d in BT-00-Repeatable-Number satisfies TRUE", CANNOT_CONVERT,
            "Iterator 'duration:$d' cannot take 'number*'"),

        // A sequence cannot stand where a single value is required
        typeError("BT-00-Repeatable-Text == 'x'", FIELD_MAY_REPEAT,
            "Operator '==' cannot take 'text*' and 'text'"),
        typeError("'x' == BT-00-Repeatable-Text", FIELD_MAY_REPEAT,
            "Operator '==' cannot take 'text' and 'text*'"),
        typeError("['a', 'b'] == 'a'", INCOMPATIBLE_OPERANDS,
            "Operator '==' cannot take 'text*' and 'text'"),
        typeError("BT-00-Repeatable-Number + 1", FIELD_MAY_REPEAT,
            "Operator '+' cannot take 'number*' and 'number'"),
        typeError("BT-00-Repeatable-Indicator and TRUE", FIELD_MAY_REPEAT,
            "Operator 'and' cannot take 'indicator*' and 'indicator'"),
        typeError("if BT-00-Repeatable-Indicator then 1 else 2", FIELD_MAY_REPEAT,
            "Conditional 'if' cannot take 'indicator*', 'number' and 'number'"),
        typeError("BT-00-Repeatable-Text like 'a'", FIELD_MAY_REPEAT,
            "Operator 'like' cannot take 'text*'"),
        typeError("['a', 'b'] like 'a'", CANNOT_CONVERT,
            "Operator 'like' cannot take 'text*'"),
        typeError("BT-00-Repeatable-Number in [1, 2]", FIELD_MAY_REPEAT,
            "Operator 'in' cannot take 'number*' and 'number*'"),
        typeError("[1, 2] in [1, 2, 3]", INCOMPATIBLE_OPERANDS,
            "Operator 'in' cannot take 'number*' and 'number*'"),
        typeError("string-length(BT-00-Repeatable-Text)", FIELD_MAY_REPEAT,
            "Function 'string-length' cannot take 'text*'"),
        typeError("upper-case(['a'])", CANNOT_CONVERT,
            "Function 'upper-case' cannot take 'text*'"),
        typeError("BT-00-Text-Multilingual == 'a'", FIELD_IS_MULTILINGUAL,
            "Operator '==' cannot take 'text*' and 'text'"),
        typeError("['a', BT-00-Repeatable-Text]", FIELD_MAY_REPEAT,
            "List cannot take 'text' and 'text*'"),
        typeError("[1, [2, 3]]", CANNOT_CONVERT,
            "List cannot take 'number' and 'number*'"),
        typeError("1 + BT-00-Repeatable-Number", FIELD_MAY_REPEAT,
            "Operator '+' cannot take 'number' and 'number*'"),
        typeError("1 + [2]", INCOMPATIBLE_OPERANDS,
            "Operator '+' cannot take 'number' and 'number*'"),
        typeError("TRUE and BT-00-Repeatable-Indicator", FIELD_MAY_REPEAT,
            "Operator 'and' cannot take 'indicator' and 'indicator*'"),

        // A node has no value
        typeError("ND-SubNode == 'a'", INCOMPATIBLE_OPERANDS,
            "Operator '==' cannot take 'node' and 'text'"),
        typeError("'a' == ND-Root", INCOMPATIBLE_OPERANDS,
            "Operator '==' cannot take 'text' and 'node'"),
        typeError("ND-SubNode + 1", INCOMPATIBLE_OPERANDS,
            "Operator '+' cannot take 'node' and 'number'"),
        typeError("ND-SubNode in ['a']", INCOMPATIBLE_OPERANDS,
            "Operator 'in' cannot take 'node' and 'text*'"),
        typeError("string-length(ND-SubNode)", CANNOT_CONVERT,
            "Function 'string-length' cannot take 'node'"),
        typeError("ND-SubNode is empty", CANNOT_CONVERT,
            "Operator 'is empty' cannot take 'node'"),
        typeError("ND-SubNode has duplicates", CANNOT_CONVERT,
            "Operator 'has duplicates' cannot take 'node'"),
        typeError("distinct-values(ND-RepeatableNode)", CANNOT_CONVERT,
            "Function 'distinct-values' cannot take 'node*'"),
        typeError("count-duplicates(ND-RepeatableNode)", CANNOT_CONVERT,
            "Function 'count-duplicates' cannot take 'node*'"),
        typeError("get-duplicates(ND-RepeatableNode)", CANNOT_CONVERT,
            "Function 'get-duplicates' cannot take 'node*'"),
        typeError("sort(ND-RepeatableNode)", CANNOT_CONVERT,
            "Function 'sort' cannot take 'node*'"),
        typeError("reverse(ND-RepeatableNode)", CANNOT_CONVERT,
            "Function 'reverse' cannot take 'node*'"),
        typeError("subsequence(ND-RepeatableNode, 1)", CANNOT_CONVERT,
            "Function 'subsequence' cannot take 'node*' and 'number'"),

        // is present applies to a field or node reference
        typeError("1 is present", CANNOT_CONVERT,
            "Operator 'is present' cannot take 'number'"),
        typeError("'a' is present", CANNOT_CONVERT,
            "Operator 'is present' cannot take 'text'"),
        typeError("(1 + 1) is present", CANNOT_CONVERT,
            "Operator 'is present' cannot take 'number'"),
        typeError("upper-case(BT-00-Text) is present", CANNOT_CONVERT,
            "Operator 'is present' cannot take 'text'"),
        typeError("some text:$x in ['a'] satisfies $x is present", CANNOT_CONVERT,
            "Operator 'is present' cannot take 'text'"),
        typeError("for text:$x in ['a'] return $x is present", CANNOT_CONVERT,
            "Operator 'is present' cannot take 'text'"),

        // A predicate is a boolean after a field or a node
        typeError("BT-00-Text['x']", CANNOT_CONVERT,
            "Predicate cannot take 'text' and 'text'"),
        typeError("BT-00-Repeatable-Text[2020-01-01Z]", CANNOT_CONVERT,
            "Predicate cannot take 'text*' and 'date'"),
        typeError("BT-00-Repeatable-Text[P1D]", CANNOT_CONVERT,
            "Predicate cannot take 'text*' and 'duration'"),
        typeError("BT-00-Repeatable-Text[BT-00-Text]", CANNOT_CONVERT,
            "Predicate cannot take 'text*' and 'text'"),
        typeError("ND-SubNode['x'] is present", CANNOT_CONVERT,
            "Predicate cannot take 'node' and 'text'"),
        typeError("['a', 'b']['x']", CANNOT_CONVERT,
            "Predicate cannot take 'text*' and 'text'"),
        typeError("['a', 'b'][TRUE]", CANNOT_CONVERT,
            "Predicate cannot take 'text*' and 'indicator'"),
        typeError("['a', 'b'][BT-00-Text]", CANNOT_CONVERT,
            "Predicate cannot take 'text*' and 'text'"),
        typeError("[1, 2][2020-01-01Z]", CANNOT_CONVERT,
            "Predicate cannot take 'number*' and 'date'"),

        // A raw value is read from a field
        typeError("ND-SubNode:rawValue", CANNOT_CONVERT,
            "Operator ':rawValue' cannot take 'node'"),
        typeError("'a':rawValue", CANNOT_CONVERT,
            "Operator ':rawValue' cannot take 'text'"),

        // Each argument of a built-in function has the type its parameter declares
        typeError("string-length(1)", CANNOT_CONVERT,
            "Function 'string-length' cannot take 'number'"),
        typeError("string-length(BT-00-Number)", CANNOT_CONVERT,
            "Function 'string-length' cannot take 'number'"),
        typeError("upper-case(TRUE)", CANNOT_CONVERT,
            "Function 'upper-case' cannot take 'indicator'"),
        typeError("substring(1, 2)", CANNOT_CONVERT,
            "Function 'substring' cannot take 'number' and 'number'"),
        typeError("substring('abc', 'x')", CANNOT_CONVERT,
            "Function 'substring' cannot take 'text' and 'text'"),
        typeError("contains(1, 2)", CANNOT_CONVERT,
            "Function 'contains' cannot take 'number' and 'number'"),
        typeError("contains(BT-00-Text, BT-00-Number)", CANNOT_CONVERT,
            "Function 'contains' cannot take 'text' and 'number'"),
        typeError("starts-with('a', 1)", CANNOT_CONVERT,
            "Function 'starts-with' cannot take 'text' and 'number'"),
        typeError("concat('a', 1)", CANNOT_CONVERT,
            "Function 'concat' cannot take 'text' and 'number'"),
        typeError("concat('a', BT-00-StartDate)", CANNOT_CONVERT,
            "Function 'concat' cannot take 'text' and 'date'"),
        typeError("string-join([1, 2], ',')", CANNOT_CONVERT,
            "Function 'string-join' cannot take 'number*' and 'text'"),
        typeError("string-join(['a'], 1)", CANNOT_CONVERT,
            "Function 'string-join' cannot take 'text*' and 'number'"),
        typeError("replace('a', 1, 'b')", CANNOT_CONVERT,
            "Function 'replace' cannot take 'text', 'number' and 'text'"),
        typeError("pad-left('a', 'b', 'c')", CANNOT_CONVERT,
            "Function 'pad-left' cannot take 'text', 'text' and 'text'"),
        typeError("split(1, ',')", CANNOT_CONVERT,
            "Function 'split' cannot take 'number' and 'text'"),
        typeError("empty(1)", CANNOT_CONVERT,
            "Function 'empty' cannot take 'number'"),
        typeError("sum(['a', 'b'])", CANNOT_CONVERT,
            "Function 'sum' cannot take 'text*'"),
        typeError("sum(BT-00-Repeatable-Text)", CANNOT_CONVERT,
            "Function 'sum' cannot take 'text*'"),
        typeError("min([2020-01-01Z])", CANNOT_CONVERT,
            "Function 'min' cannot take 'date*'"),
        typeError("average(BT-00-Repeatable-Date)", CANNOT_CONVERT,
            "Function 'average' cannot take 'date*'"),
        typeError("absolute('a')", CANNOT_CONVERT,
            "Function 'absolute' cannot take 'text'"),
        typeError("round(TRUE)", CANNOT_CONVERT,
            "Function 'round' cannot take 'indicator'"),
        typeError("round(BT-00-Text)", CANNOT_CONVERT,
            "Function 'round' cannot take 'text'"),
        typeError("year('2020')", CANNOT_CONVERT,
            "Function 'year' cannot take 'text'"),
        typeError("year(BT-00-StartTime)", CANNOT_CONVERT,
            "Function 'year' cannot take 'time'"),
        typeError("day(10:00:00Z)", CANNOT_CONVERT,
            "Function 'day' cannot take 'time'"),
        typeError("hours(2020-01-01Z)", CANNOT_CONVERT,
            "Function 'hours' cannot take 'date'"),
        typeError("minutes(BT-00-StartDate)", CANNOT_CONVERT,
            "Function 'minutes' cannot take 'date'"),
        typeError("years(1)", CANNOT_CONVERT,
            "Function 'years' cannot take 'number'"),
        typeError("days(BT-00-Number)", CANNOT_CONVERT,
            "Function 'days' cannot take 'number'"),
        typeError("date(1)", CANNOT_CONVERT,
            "Function 'date' cannot take 'number'"),
        typeError("date(BT-00-Number)", CANNOT_CONVERT,
            "Function 'date' cannot take 'number'"),
        typeError("time(TRUE)", CANNOT_CONVERT,
            "Function 'time' cannot take 'indicator'"),
        typeError("day-time-duration(1)", CANNOT_CONVERT,
            "Function 'day-time-duration' cannot take 'number'"),
        typeError("year-month-duration(BT-00-Number)", CANNOT_CONVERT,
            "Function 'year-month-duration' cannot take 'number'"),
        typeError("indicator('x')", CANNOT_CONVERT,
            "Function 'indicator' cannot take 'text'"),
        typeError("indicator(BT-00-Text)", CANNOT_CONVERT,
            "Function 'indicator' cannot take 'text'"),
        typeError("number(2020-01-01Z)", CANNOT_CONVERT,
            "Function 'number' cannot take 'date'"),
        typeError("number(P1D)", CANNOT_CONVERT,
            "Function 'number' cannot take 'duration'"),
        typeError("add-duration(10:00:00Z, P1D)", CANNOT_CONVERT,
            "Function 'add-duration' cannot take 'time' and 'duration'"),
        typeError("add-duration(2020-01-01Z, 1)", CANNOT_CONVERT,
            "Function 'add-duration' cannot take 'date' and 'number'"),
        typeError("add-duration(BT-00-StartDate, BT-00-Number)", CANNOT_CONVERT,
            "Function 'add-duration' cannot take 'date' and 'number'"),
        typeError("subtract-duration(P1D, 2020-01-01Z)", CANNOT_CONVERT,
            "Function 'subtract-duration' cannot take 'duration' and 'date'"),
        typeError("index-of(['a', 'b'], 1)", CANNOT_CONVERT,
            "Function 'index-of' cannot take 'text*' and 'number'"),
        typeError("index-of(BT-00-Repeatable-Text, 1)", CANNOT_CONVERT,
            "Function 'index-of' cannot take 'text*' and 'number'"),
        typeError("sequence-equal(['a'], [1])", CANNOT_CONVERT,
            "Function 'sequence-equal' cannot take 'text*' and 'number*'"),
        typeError("sequence-equal(BT-00-Repeatable-Text, BT-00-Repeatable-Number)", CANNOT_CONVERT,
            "Function 'sequence-equal' cannot take 'text*' and 'number*'"),
        typeError("value-union(['a'], [1])", CANNOT_CONVERT,
            "Function 'value-union' cannot take 'text*' and 'number*'"),
        typeError("value-intersect([1], [2020-01-01Z])", CANNOT_CONVERT,
            "Function 'value-intersect' cannot take 'number*' and 'date*'"),
        typeError("value-except(BT-00-Repeatable-Date, BT-00-Repeatable-Time)", CANNOT_CONVERT,
            "Function 'value-except' cannot take 'date*' and 'time*'"),
        typeError("subsequence(['a', 'b'], 'x')", CANNOT_CONVERT,
            "Function 'subsequence' cannot take 'text*' and 'text'"),
        typeError("subsequence(['a', 'b'], 1, 'x')", CANNOT_CONVERT,
            "Function 'subsequence' cannot take 'text*', 'number' and 'text'"),
        typeError("contains(1, 'a')", CANNOT_CONVERT,
            "Function 'contains' cannot take 'number' and 'text'"),
        typeError("starts-with(1, 'a')", CANNOT_CONVERT,
            "Function 'starts-with' cannot take 'number' and 'text'"),
        typeError("ends-with(1, 'a')", CANNOT_CONVERT,
            "Function 'ends-with' cannot take 'number' and 'text'"),
        typeError("ends-with('a', 1)", CANNOT_CONVERT,
            "Function 'ends-with' cannot take 'text' and 'number'"),
        typeError("max(['a'])", CANNOT_CONVERT,
            "Function 'max' cannot take 'text*'"),
        typeError("index-of-substring(1, 'a')", CANNOT_CONVERT,
            "Function 'index-of-substring' cannot take 'number' and 'text'"),
        typeError("index-of-substring('a', 1)", CANNOT_CONVERT,
            "Function 'index-of-substring' cannot take 'text' and 'number'"),
        typeError("round-down('a')", CANNOT_CONVERT,
            "Function 'round-down' cannot take 'text'"),
        typeError("round-up(TRUE)", CANNOT_CONVERT,
            "Function 'round-up' cannot take 'indicator'"),
        typeError("month(10:00:00Z)", CANNOT_CONVERT,
            "Function 'month' cannot take 'time'"),
        typeError("seconds(2020-01-01Z)", CANNOT_CONVERT,
            "Function 'seconds' cannot take 'date'"),
        typeError("months('P1M')", CANNOT_CONVERT,
            "Function 'months' cannot take 'text'"),
        typeError("substring('abc', 1, 'x')", CANNOT_CONVERT,
            "Function 'substring' cannot take 'text', 'number' and 'text'"),
        typeError("substring-before(1, 'a')", CANNOT_CONVERT,
            "Function 'substring-before' cannot take 'number' and 'text'"),
        typeError("substring-before('a', 1)", CANNOT_CONVERT,
            "Function 'substring-before' cannot take 'text' and 'number'"),
        typeError("substring-after(1, 'a')", CANNOT_CONVERT,
            "Function 'substring-after' cannot take 'number' and 'text'"),
        typeError("substring-after('a', 1)", CANNOT_CONVERT,
            "Function 'substring-after' cannot take 'text' and 'number'"),
        typeError("concat(1, 'a')", CANNOT_CONVERT,
            "Function 'concat' cannot take 'number' and 'text'"),
        typeError("concat('a', 'b', 1)", CANNOT_CONVERT,
            "Function 'concat' cannot take 'text', 'text' and 'number'"),
        typeError("lower-case(1)", CANNOT_CONVERT,
            "Function 'lower-case' cannot take 'number'"),
        typeError("normalize-space(1)", CANNOT_CONVERT,
            "Function 'normalize-space' cannot take 'number'"),
        typeError("trim(1)", CANNOT_CONVERT,
            "Function 'trim' cannot take 'number'"),
        typeError("trim-left(1)", CANNOT_CONVERT,
            "Function 'trim-left' cannot take 'number'"),
        typeError("trim-right(1)", CANNOT_CONVERT,
            "Function 'trim-right' cannot take 'number'"),
        typeError("pad-left(1, 2, ' ')", CANNOT_CONVERT,
            "Function 'pad-left' cannot take 'number', 'number' and 'text'"),
        typeError("pad-left('a', 2, 1)", CANNOT_CONVERT,
            "Function 'pad-left' cannot take 'text', 'number' and 'number'"),
        typeError("pad-right(1, 2, ' ')", CANNOT_CONVERT,
            "Function 'pad-right' cannot take 'number', 'number' and 'text'"),
        typeError("pad-right('a', 'b', ' ')", CANNOT_CONVERT,
            "Function 'pad-right' cannot take 'text', 'text' and 'text'"),
        typeError("pad-right('a', 2, 1)", CANNOT_CONVERT,
            "Function 'pad-right' cannot take 'text', 'number' and 'number'"),
        typeError("replace(1, 'a', 'b')", CANNOT_CONVERT,
            "Function 'replace' cannot take 'number', 'text' and 'text'"),
        typeError("replace('a', 'b', 1)", CANNOT_CONVERT,
            "Function 'replace' cannot take 'text', 'text' and 'number'"),
        typeError("replace-regex(1, 'a', 'b')", CANNOT_CONVERT,
            "Function 'replace-regex' cannot take 'number', 'text' and 'text'"),
        typeError("replace-regex('a', 1, 'b')", CANNOT_CONVERT,
            "Function 'replace-regex' cannot take 'text', 'number' and 'text'"),
        typeError("replace-regex('a', 'b', 1)", CANNOT_CONVERT,
            "Function 'replace-regex' cannot take 'text', 'text' and 'number'"),
        typeError("repeat(1, 2)", CANNOT_CONVERT,
            "Function 'repeat' cannot take 'number' and 'number'"),
        typeError("repeat('a', 'b')", CANNOT_CONVERT,
            "Function 'repeat' cannot take 'text' and 'text'"),
        typeError("url-encode(1)", CANNOT_CONVERT,
            "Function 'url-encode' cannot take 'number'"),
        typeError("capitalize-first(1)", CANNOT_CONVERT,
            "Function 'capitalize-first' cannot take 'number'"),
        typeError("split('a', 1)", CANNOT_CONVERT,
            "Function 'split' cannot take 'text' and 'number'"),
        typeError("subtract-duration(1, P1D)", CANNOT_CONVERT,
            "Function 'subtract-duration' cannot take 'number' and 'duration'"),
        typeError("subtract-duration(2020-01-01Z, 1)", CANNOT_CONVERT,
            "Function 'subtract-duration' cannot take 'date' and 'number'"),
        typeError("text([1])", CANNOT_CONVERT,
            "Function 'text' cannot take 'number*'"),
        typeError("string(BT-00-Repeatable-Number)", FIELD_MAY_REPEAT,
            "Function 'string' cannot take 'number*'"),

        // A built-in function takes the number of arguments it declares
        syntaxError("upper-case()"),
        syntaxError("concat()"),
        syntaxError("count()"),
        syntaxError("count([1], [2])"),
        syntaxError("string-length('a', 'b')"),
        syntaxError("contains('a')"),
        syntaxError("substring('a')"),
        syntaxError("current-date(1)"),
        syntaxError("add-duration(2020-01-01Z)"),
        syntaxError("not(TRUE, FALSE)"),
        syntaxError("value-union(['a'])"),
        syntaxError("sequence-equal(['a'], ['b'], ['c'])"),
        syntaxError("current-time(1)"),

        // The result of a built-in function has the type the function declares
        typeError("count(['a']) like 'a'", CANNOT_CONVERT,
            "Operator 'like' cannot take 'number'"),
        typeError("string-length('a') and TRUE", INCOMPATIBLE_OPERANDS,
            "Operator 'and' cannot take 'number' and 'indicator'"),
        typeError("upper-case('a') + 1", INCOMPATIBLE_OPERANDS,
            "Operator '+' cannot take 'text' and 'number'"),
        typeError("current-date() + 1", INCOMPATIBLE_OPERANDS,
            "Operator '+' cannot take 'date' and 'number'"),
        typeError("year(current-date()) == 'a'", INCOMPATIBLE_OPERANDS,
            "Operator '==' cannot take 'number' and 'text'"),
        typeError("contains('a', 'b') + 1", INCOMPATIBLE_OPERANDS,
            "Operator '+' cannot take 'indicator' and 'number'"));
  }

  static Stream<Arguments> invalidReferences() {
    return Stream.of(
        // The left side of :: is a context: a field, a node or a context variable
        typeError("'a'::BT-00-Text", CANNOT_CONVERT,
            "Operator '::' cannot take 'text'"),
        typeError("(BT-00-Text)::BT-00-Number", CANNOT_CONVERT,
            "Operator '::' cannot take 'text'"),

        // An attribute and the language properties are read from a field
        typeError("ND-SubNode/@listName", CANNOT_CONVERT,
            "Operator '/@listName' cannot take 'node'"),
        typeError("'a'/@listName", CANNOT_CONVERT,
            "Operator '/@listName' cannot take 'text'"),
        typeError("1:preferredLanguage", CANNOT_CONVERT,
            "Operator ':preferredLanguage' cannot take 'number'"),
        typeError("1:preferredLanguageText", CANNOT_CONVERT,
            "Operator ':preferredLanguageText' cannot take 'number'"),

        // A node has no value, so it cannot stand in parentheses
        typeError("(ND-SubNode) is present", CANNOT_CONVERT,
            "Parenthesised expression cannot take 'node'"),

        // A node has no value, so an expression cannot give one, nor an iteration return one
        typeError("ND-SubNode", CANNOT_CONVERT,
            "Expression block cannot take 'node'"),
        typeError("ND-RepeatableNode", CANNOT_CONVERT,
            "Expression block cannot take 'node*'"),
        typeError("/ND-SubNode", CANNOT_CONVERT,
            "Expression block cannot take 'node'"),
        typeError("ND-Root::ND-SubNode", CANNOT_CONVERT,
            "Expression block cannot take 'node'"),
        typeError("for text:$x in ['a'] return ND-SubNode", CANNOT_CONVERT,
            "Iteration 'for' cannot take 'node'"));
  }

  static Stream<Arguments> invalidTemplateOperations() {
    return Stream.of(
        // Each argument of a template-only function has the type its parameter declares
        typeError("format-number('x', '0.00')", CANNOT_CONVERT,
            "Function 'format-number' cannot take 'text' and 'text'"),
        typeError("format-number(1, 2)", CANNOT_CONVERT,
            "Function 'format-number' cannot take 'number' and 'number'"),
        typeError("format-number(BT-00-Text, '0.00')", CANNOT_CONVERT,
            "Function 'format-number' cannot take 'text' and 'text'"),
        typeError("format-number(BT-00-Number, BT-00-Number)", CANNOT_CONVERT,
            "Function 'format-number' cannot take 'number' and 'number'"),
        typeError("format-short(1)", CANNOT_CONVERT,
            "Function 'format-short' cannot take 'number'"),
        typeError("format-short('a')", CANNOT_CONVERT,
            "Function 'format-short' cannot take 'text'"),
        typeError("format-short(1, 10:00:00Z)", CANNOT_CONVERT,
            "Function 'format-short' cannot take 'number' and 'time'"),
        typeError("format-short(2020-01-01Z, 1)", CANNOT_CONVERT,
            "Function 'format-short' cannot take 'date' and 'number'"),
        typeError("format-medium(TRUE)", CANNOT_CONVERT,
            "Function 'format-medium' cannot take 'indicator'"),
        typeError("format-medium(P1D, 10:00:00Z)", CANNOT_CONVERT,
            "Function 'format-medium' cannot take 'duration' and 'time'"),
        typeError("format-medium(2020-01-01Z, 'a')", CANNOT_CONVERT,
            "Function 'format-medium' cannot take 'date' and 'text'"),
        typeError("format-long(BT-00-Number)", CANNOT_CONVERT,
            "Function 'format-long' cannot take 'number'"),
        typeError("format-long(10:00:00Z, 10:00:00Z)", CANNOT_CONVERT,
            "Function 'format-long' cannot take 'time' and 'time'"),
        typeError("format-long(BT-00-StartDate, BT-00-StartDate)", CANNOT_CONVERT,
            "Function 'format-long' cannot take 'date' and 'date'"),
        typeError("preferred-language('a')", CANNOT_CONVERT,
            "Function 'preferred-language' cannot take 'text'"),
        typeError("preferred-language(BT-00-Text)", CANNOT_CONVERT,
            "Function 'preferred-language' cannot take 'text'"),
        typeError("preferred-language-text(1)", CANNOT_CONVERT,
            "Function 'preferred-language-text' cannot take 'number'"),
        typeError("preferred-language-text(BT-00-Number)", CANNOT_CONVERT,
            "Function 'preferred-language-text' cannot take 'number'"),

        // A template-only function takes the number of arguments it declares
        syntaxError("format-number(1)"),
        syntaxError("format-short()"),
        syntaxError("preferred-language()"));
  }

  /**
   * A row whose expression fails with a type error.
   *
   * @param expression the expression
   * @param code the code of the error
   * @param sentence the first sentence of its message, without its full stop
   * @return the row
   */
  private static Arguments typeError(final String expression,
      final TypeMismatchException.ErrorCode code, final String sentence) {
    return Arguments.of(expression, new TypeError(code, sentence));
  }

  /**
   * A row whose expression fails with a syntax error.
   *
   * @param expression the expression
   * @return the row
   */
  private static Arguments syntaxError(final String expression) {
    return Arguments.of(expression, new SyntaxError());
  }

  /**
   * The error a row expects its expression to fail with.
   */
  private interface Outcome {

    /**
     * @param translation the translation of the row's expression
     */
    void assertRaisedBy(Executable translation);
  }

  /**
   * A {@link TypeMismatchException} with its code and the first sentence of its message.
   */
  private static final class TypeError implements Outcome {

    private static final String TYPE_MISMATCH = "Type mismatch. ";
    private static final String END_OF_SENTENCE = ". ";

    private final TypeMismatchException.ErrorCode code;
    private final String sentence;

    TypeError(final TypeMismatchException.ErrorCode code, final String sentence) {
      this.code = code;
      this.sentence = sentence;
    }

    @Override
    public void assertRaisedBy(final Executable translation) {
      final TypeMismatchException exception =
          assertThrows(TypeMismatchException.class, translation);
      assertEquals(this.code, exception.getErrorCode(), exception::getMessage);
      assertEquals(this.sentence, firstSentenceOf(exception.getMessage()));
    }

    private static String firstSentenceOf(final String message) {
      final int start = message.indexOf(TYPE_MISMATCH);
      assertTrue(start >= 0, message);
      final String sentences = message.substring(start + TYPE_MISMATCH.length());
      final int end = sentences.indexOf(END_OF_SENTENCE);
      assertTrue(end >= 0, message);
      return sentences.substring(0, end);
    }
  }

  /**
   * A syntax error: the parser's own {@link ParseCancellationException}, not a subclass of it.
   */
  private static final class SyntaxError implements Outcome {

    @Override
    public void assertRaisedBy(final Executable translation) {
      assertSyntaxError(translation);
    }
  }
}
