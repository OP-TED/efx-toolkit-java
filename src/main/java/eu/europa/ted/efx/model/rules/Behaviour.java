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
package eu.europa.ted.efx.model.rules;

import java.math.BigDecimal;

/**
 * The behaviour that a rule states, after {@code ASSERT} or {@code OTHERWISE ASSERT}, for the field
 * or node that it validates: its kind, and the arguments that the author wrote.
 *
 * <p>A rule keeps its behaviour next to the test that the behaviour is translated to. A generator
 * of validation code needs only the test. A generator for a client application reads the
 * behaviour, to tell the application how to configure the control of the target.</p>
 *
 * <p>Which arguments a behaviour has depends on its kind:</p>
 * <ul>
 * <li>{@link BehaviourKind#LIMIT_COUNT}, {@link BehaviourKind#LIMIT_SIZE} and
 * {@link BehaviourKind#LIMIT_RANGE} have either a comparison and a number, as in
 * {@code OCCURS >= 2}, or a lower and an upper bound, as in {@code OCCURS BETWEEN 2 AND 5};</li>
 * <li>{@link BehaviourKind#LIMIT_VALUES} has the name of a codelist;</li>
 * <li>{@link BehaviourKind#MATCH_PATTERN} has a pattern;</li>
 * <li>the other kinds have no arguments.</li>
 * </ul>
 */
public final class Behaviour {

    private final BehaviourKind kind;
    private final String comparison;
    private final BigDecimal number;
    private final BigDecimal lowerBound;
    private final BigDecimal upperBound;
    private final String codelist;
    private final String pattern;

    private Behaviour(final BehaviourKind kind, final String comparison, final BigDecimal number,
            final BigDecimal lowerBound, final BigDecimal upperBound, final String codelist,
            final String pattern) {
        this.kind = kind;
        this.comparison = comparison;
        this.number = number;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.codelist = codelist;
        this.pattern = pattern;
    }

    /**
     * Makes a behaviour that has no arguments.
     *
     * @param kind the kind of the behaviour
     * @return the behaviour
     */
    public static Behaviour of(final BehaviourKind kind) {
        return new Behaviour(kind, null, null, null, null, null, null);
    }

    /**
     * Makes a limit written with a comparison, as in {@code LENGTH <= 100}.
     *
     * @param kind the kind of the limit
     * @param comparison the comparison operator, as it is written, such as {@code <=}
     * @param number the number that the count, the size or the value is compared with
     * @return the behaviour
     */
    public static Behaviour compared(final BehaviourKind kind, final String comparison,
            final BigDecimal number) {
        return new Behaviour(kind, comparison, number, null, null, null, null);
    }

    /**
     * Makes a limit written with a range, as in {@code VALUE BETWEEN 0 AND 9999}. Both bounds are
     * included.
     *
     * @param kind the kind of the limit
     * @param lowerBound the lowest count, size or value allowed
     * @param upperBound the highest count, size or value allowed
     * @return the behaviour
     */
    public static Behaviour between(final BehaviourKind kind, final BigDecimal lowerBound,
            final BigDecimal upperBound) {
        return new Behaviour(kind, null, null, lowerBound, upperBound, null, null);
    }

    /**
     * Makes the behaviour that limits the value to the codes of a codelist, as in
     * {@code IN [...indicator]}.
     *
     * @param codelist the name of the codelist
     * @return the behaviour
     */
    public static Behaviour inCodelist(final String codelist) {
        return new Behaviour(BehaviourKind.LIMIT_VALUES, null, null, null, null, codelist, null);
    }

    /**
     * Makes the behaviour of a value that matches a pattern, as in {@code LIKE '^[0-9]+$'}.
     *
     * @param pattern the regular expression of the pattern: without the quotes around it, and
     *        with a quote that the author wrote as {@code \'} or {@code \"} given as the quote
     * @return the behaviour
     */
    public static Behaviour matching(final String pattern) {
        return new Behaviour(BehaviourKind.MATCH_PATTERN, null, null, null, null, null, pattern);
    }

    /**
     * @return the kind of the behaviour
     */
    public BehaviourKind getKind() {
        return this.kind;
    }

    /**
     * @return the comparison operator of a limit written with a comparison, such as {@code <=},
     *         or {@code null} for any other behaviour
     */
    public String getComparison() {
        return this.comparison;
    }

    /**
     * @return the number of a limit written with a comparison, or {@code null} for any other
     *         behaviour
     */
    public BigDecimal getNumber() {
        return this.number;
    }

    /**
     * @return the lower bound of a limit written with a range, or {@code null} for any other
     *         behaviour
     */
    public BigDecimal getLowerBound() {
        return this.lowerBound;
    }

    /**
     * @return the upper bound of a limit written with a range, or {@code null} for any other
     *         behaviour
     */
    public BigDecimal getUpperBound() {
        return this.upperBound;
    }

    /**
     * @return the name of the codelist of a {@link BehaviourKind#LIMIT_VALUES} behaviour, or
     *         {@code null} for any other behaviour
     */
    public String getCodelist() {
        return this.codelist;
    }

    /**
     * @return the regular expression of a {@link BehaviourKind#MATCH_PATTERN} behaviour, as the
     *         test of the rule matches it: without the quotes around the pattern, and with a
     *         quote that the author wrote as {@code \'} or {@code \"} given as the quote. It is
     *         {@code null} for any other behaviour.
     */
    public String getPattern() {
        return this.pattern;
    }
}
