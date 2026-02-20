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
package eu.europa.ted.efx.util;

import java.util.Set;

import eu.europa.ted.efx.exceptions.InvalidUsageException;

/**
 * Validates that a regex pattern used in an EFX {@code like} expression only uses constructs
 * from the portable EFX regex subset. This subset is designed to work identically across all
 * EFX target languages (XPath, Java, JavaScript, Python, C#, Swift).
 *
 * Allowed constructs:
 *
 *   Literal characters
 *   {@code .} (any character)
 *   {@code *}, {@code +}, {@code ?} (quantifiers, greedy or non-greedy)
 *   {@code {n}}, {@code {n,}}, {@code {n,m}} (repetition, greedy or non-greedy)
 *   {@code [...]}, {@code [^...]} (character classes with ranges)
 *   {@code (...)} (grouping), {@code |} (alternation)
 *   {@code ^}, {@code $} (anchors)
 *   Escaped metacharacters: {@code \.} {@code \\} {@code \(} {@code \)}
 *   {@code \[} {@code \]} {@code \{} {@code \}} {@code \*} {@code \+}
 *   {@code \?} {@code \|} {@code \^} {@code \$}
 *   {@code \p{Category}}, {@code \P{Category}} — Unicode property escapes,
 *   e.g. {@code \p{L}} (any Unicode letter). All EFX target languages support these
 *   (JavaScript requires the {@code u} flag; Python requires the {@code regex} module
 *   instead of {@code re} — both are handled transparently by the EFX translator).
 *
 * Disallowed constructs (not portable):
 *
 *   {@code \d}, {@code \D}, {@code \w}, {@code \W}, {@code \s}, {@code \S}
 *   — shorthand character classes have inconsistent semantics across target languages
 *   (ASCII in XPath/JavaScript, Unicode in Python/C#/Swift). Use explicit character
 *   classes instead, e.g. {@code [0-9]}, {@code [a-zA-Z0-9_]}, {@code [ \t\r\n]}.
 */
public final class EfxRegexValidator {

    private static final Set<Character> SHORTHAND_CLASSES = Set.of('d', 'D', 'w', 'W', 's', 'S');

    private static final Set<Character> ESCAPABLE_METACHARACTERS =
            Set.of('.', '\\', '(', ')', '[', ']', '{', '}', '*', '+', '?', '|', '^', '$');

    private EfxRegexValidator() {}

    /**
     * Validates that the given EFX regex pattern only uses portable constructs.
     *
     * @param rawPattern the raw token text of the pattern including delimiters (e.g. {@code '[0-9]+'})
     * @throws InvalidUsageException if the pattern uses unsupported constructs
     */
    public static void validate(String rawPattern) {
        if (rawPattern == null || rawPattern.length() < 2) {
            return;
        }

        String content = rawPattern.substring(1, rawPattern.length() - 1);
        int groupDepth = 0;

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);

            if (c == '\\') {
                i = validateEscape(rawPattern, content, i);
            } else if (c == '[') {
                i = validateCharacterClass(rawPattern, content, i);
            } else if (c == '(') {
                i = validateGroupOpen(rawPattern, content, i);
                groupDepth++;
            } else if (c == ')') {
                if (groupDepth <= 0) {
                    throw InvalidUsageException.unsupportedRegexConstruct(rawPattern, i + 1, "unmatched closing parenthesis");
                }
                groupDepth--;
            } else if (c == '{') {
                i = validateRepetition(rawPattern, content, i);
            }
            // '.', '|', '^', '$', and literal characters are allowed as-is
        }

        if (groupDepth > 0) {
            throw InvalidUsageException.unsupportedRegexConstruct(rawPattern, content.length(), "unclosed group — missing ')'");
        }
    }

    private static int validateEscape(String rawPattern, String content, int pos) {
        if (pos + 1 >= content.length()) {
            throw InvalidUsageException.unsupportedRegexConstruct(rawPattern, pos + 1, "trailing backslash");
        }

        char next = content.charAt(pos + 1);

        if (ESCAPABLE_METACHARACTERS.contains(next)) {
            return pos + 1;
        }

        // Portable whitespace escapes — same meaning in all target languages
        if (next == 't' || next == 'r' || next == 'n' || next == 'f') {
            return pos + 1;
        }

        // EFX quote escapes (\' and \") — these represent literal quote characters
        if (next == '\'' || next == '"') {
            return pos + 1;
        }

        if (SHORTHAND_CLASSES.contains(next)) {
            throw InvalidUsageException.unsupportedRegexConstruct(rawPattern, pos + 1,
                    "shorthand class '\\" + next + "' is not allowed in EFX regex — its semantics differ across target languages. "
                    + "Use an explicit character class instead (e.g. [0-9], [a-zA-Z0-9_], [ \\t\\r\\n])");
        }

        if (next == 'b' || next == 'B') {
            throw InvalidUsageException.unsupportedRegexConstruct(rawPattern, pos + 1,
                    "word boundary '\\" + next + "' is not allowed in EFX regex — use '^' and '$' anchors instead");
        }

        if (next >= '1' && next <= '9') {
            throw InvalidUsageException.unsupportedRegexConstruct(rawPattern, pos + 1,
                    "backreference '\\" + next + "' is not allowed in EFX regex");
        }

        if (next == 'p' || next == 'P') {
            // Unicode property escape \p{Category} or \P{Category} — allowed
            if (pos + 2 >= content.length() || content.charAt(pos + 2) != '{') {
                throw InvalidUsageException.unsupportedRegexConstruct(rawPattern, pos + 1,
                        "Unicode property escape '\\" + next + "' must be followed by '{Category}' (e.g. \\p{L})");
            }
            int closeIdx = content.indexOf('}', pos + 3);
            if (closeIdx < 0) {
                throw InvalidUsageException.unsupportedRegexConstruct(rawPattern, pos + 1,
                        "Unicode property escape '\\" + next + "{...}' is missing closing '}'");
            }
            return closeIdx;
        }

        if (next == '0' || next == 'x' || next == 'u') {
            throw InvalidUsageException.unsupportedRegexConstruct(rawPattern, pos + 1,
                    "numeric character escape '\\" + next + "' is not allowed in EFX regex — use the literal character instead");
        }

        throw InvalidUsageException.unsupportedRegexConstruct(rawPattern, pos + 1,
                "escape sequence '\\" + next + "' is not allowed in EFX regex");
    }

    private static int validateCharacterClass(String rawPattern, String content, int startPos) {
        int i = startPos + 1;

        // Allow ^ for negation at the start
        if (i < content.length() && content.charAt(i) == '^') {
            i++;
        }

        // Allow ] as a literal if it's the first character in the class (or after ^)
        if (i < content.length() && content.charAt(i) == ']') {
            i++;
        }

        while (i < content.length()) {
            char c = content.charAt(i);

            if (c == ']') {
                return i; // end of character class
            }

            if (c == '\\') {
                if (i + 1 >= content.length()) {
                    throw InvalidUsageException.unsupportedRegexConstruct(rawPattern, i + 1, "trailing backslash in character class");
                }
                char next = content.charAt(i + 1);
                if (ESCAPABLE_METACHARACTERS.contains(next)
                        || next == 't' || next == 'r' || next == 'n' || next == 'f'
                        || next == '\'' || next == '"' || next == '-') {
                    i += 2;
                    continue;
                }
                // Validate using the same rules as outside a character class
                i = validateEscape(rawPattern, content, i) + 1;
                continue;
            }

            // Regular characters and ranges (a-z) are allowed
            i++;
        }

        throw InvalidUsageException.unsupportedRegexConstruct(rawPattern, startPos + 1, "unclosed character class — missing ']'");
    }

    private static int validateGroupOpen(String rawPattern, String content, int pos) {
        if (pos + 1 < content.length() && content.charAt(pos + 1) == '?') {
            String message;
            if (pos + 2 < content.length()) {
                char modifier = content.charAt(pos + 2);
                if (modifier == '=') {
                    message = "lookahead '(?=...)' is not allowed in EFX regex";
                } else if (modifier == '!') {
                    message = "negative lookahead '(?!...)' is not allowed in EFX regex";
                } else if (modifier == '<') {
                    message = "lookbehind '(?<...)' is not allowed in EFX regex";
                } else if (modifier == ':') {
                    message = "non-capturing group '(?:...)' is not allowed in EFX regex — use plain '(...)' instead";
                } else {
                    message = "extended group syntax '(?" + modifier + "...)' is not allowed in EFX regex";
                }
            } else {
                message = "extended group syntax '(?...)' is not allowed in EFX regex";
            }
            throw InvalidUsageException.unsupportedRegexConstruct(rawPattern, pos + 1, message);
        }
        return pos;
    }

    private static int validateRepetition(String rawPattern, String content, int startPos) {
        int i = startPos + 1;

        // Expect at least one digit
        if (i >= content.length() || !Character.isDigit(content.charAt(i))) {
            // Treat { as a literal character (some regex engines allow this)
            return startPos;
        }

        // Parse first number
        while (i < content.length() && Character.isDigit(content.charAt(i))) {
            i++;
        }

        if (i >= content.length()) {
            throw InvalidUsageException.unsupportedRegexConstruct(rawPattern, startPos + 1, "unclosed repetition quantifier — missing '}'");
        }

        if (content.charAt(i) == '}') {
            return i;
        }

        if (content.charAt(i) == ',') {
            i++;
            // Optional second number
            while (i < content.length() && Character.isDigit(content.charAt(i))) {
                i++;
            }
            if (i >= content.length() || content.charAt(i) != '}') {
                throw InvalidUsageException.unsupportedRegexConstruct(rawPattern, startPos + 1, "unclosed repetition quantifier — missing '}'");
            }
            return i;
        }

        throw InvalidUsageException.unsupportedRegexConstruct(rawPattern, startPos + 1, "invalid repetition quantifier");
    }
}
