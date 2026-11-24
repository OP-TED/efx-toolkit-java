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

/**
 * The kinds of behaviour that a rule can state, after {@code ASSERT} or {@code OTHERWISE ASSERT},
 * for the field or node that it validates.
 *
 * <p>A behaviour says how the target behaves, so that a client application can configure its
 * controls and the user cannot enter a wrong value.</p>
 */
public enum BehaviourKind {

    /** {@code IS NOT EMPTY}: the target is mandatory. */
    MANDATORY,

    /** {@code IS NOT PRESENT}: the target is forbidden. */
    FORBIDDEN,

    /** {@code NOT REPEATED}: the target appears at most once. */
    NOT_REPEATED,

    /** {@code NOT MODIFIED VIA}: the value of the target may not be modified. */
    NOT_MODIFIED,

    /** {@code NOT ADDED VIA}: no occurrence of the target may be added. */
    NOT_ADDED,

    /** {@code NOT REMOVED VIA}: no occurrence of the target may be removed. */
    NOT_REMOVED,

    /** {@code OCCURS}: the number of times that the target appears is limited. */
    LIMIT_COUNT,

    /** {@code LENGTH}: the number of characters of the value is limited. */
    LIMIT_SIZE,

    /** {@code VALUE}: the value, which is a number, is limited. */
    LIMIT_RANGE,

    /** {@code IN}: the value is one of the codes of a codelist. */
    LIMIT_VALUES,

    /** {@code LIKE}: the value, written as text, matches a pattern. */
    MATCH_PATTERN
}
