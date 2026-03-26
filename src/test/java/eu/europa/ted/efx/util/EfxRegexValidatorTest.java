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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import eu.europa.ted.efx.exceptions.InvalidUsageException;

class EfxRegexValidatorTest {

    @Nested
    class AllowedConstructs {

        @Test
        void testLiteralCharacters() {
            assertDoesNotThrow(() -> EfxRegexValidator.validate("'abc123'"));
        }

        @Test
        void testDot() {
            assertDoesNotThrow(() -> EfxRegexValidator.validate("'a.b'"));
        }

        @Test
        void testQuantifiers() {
            assertDoesNotThrow(() -> EfxRegexValidator.validate("'a*b+c?'"));
        }

        @Test
        void testRepetition_Exact() {
            assertDoesNotThrow(() -> EfxRegexValidator.validate("'a{3}'"));
        }

        @Test
        void testRepetition_AtLeast() {
            assertDoesNotThrow(() -> EfxRegexValidator.validate("'a{3,}'"));
        }

        @Test
        void testRepetition_Range() {
            assertDoesNotThrow(() -> EfxRegexValidator.validate("'a{3,5}'"));
        }

        @Test
        void testNonGreedyQuantifiers() {
            assertDoesNotThrow(() -> EfxRegexValidator.validate("'a*?'"));
            assertDoesNotThrow(() -> EfxRegexValidator.validate("'a+?'"));
            assertDoesNotThrow(() -> EfxRegexValidator.validate("'a??'"));
            assertDoesNotThrow(() -> EfxRegexValidator.validate("'a{2,3}?'"));
        }

        @Test
        void testCharacterClass() {
            assertDoesNotThrow(() -> EfxRegexValidator.validate("'[abc]'"));
        }

        @Test
        void testCharacterClassWithRange() {
            assertDoesNotThrow(() -> EfxRegexValidator.validate("'[a-z0-9]'"));
        }

        @Test
        void testNegatedCharacterClass() {
            assertDoesNotThrow(() -> EfxRegexValidator.validate("'[^abc]'"));
        }

        @Test
        void testCharacterClassWithClosingBracketAsFirst() {
            assertDoesNotThrow(() -> EfxRegexValidator.validate("'[]abc]'"));
        }

        @Test
        void testGrouping() {
            assertDoesNotThrow(() -> EfxRegexValidator.validate("'(abc)'"));
        }

        @Test
        void testNestedGroups() {
            assertDoesNotThrow(() -> EfxRegexValidator.validate("'((a)(b))'"));
        }

        @Test
        void testAlternation() {
            assertDoesNotThrow(() -> EfxRegexValidator.validate("'a|b|c'"));
        }

        @Test
        void testAnchors() {
            assertDoesNotThrow(() -> EfxRegexValidator.validate("'^abc$'"));
        }

        @Test
        void testEscapedMetacharacters() {
            assertDoesNotThrow(() -> EfxRegexValidator.validate("'\\.\\\\\\(\\)\\[\\]\\{\\}\\*\\+\\?\\|\\^\\$'"));
        }

        @Test
        void testWhitespaceEscapes() {
            assertDoesNotThrow(() -> EfxRegexValidator.validate("'[ \\t\\r\\n\\f]+'"));
        }

        @Test
        void testUnicodePropertyEscape() {
            assertDoesNotThrow(() -> EfxRegexValidator.validate("'\\p{L}+'"));
            assertDoesNotThrow(() -> EfxRegexValidator.validate("'\\P{Z}+'"));
            assertDoesNotThrow(() -> EfxRegexValidator.validate("'[\\p{L}\\p{N}]+'"));
        }

        @Test
        void testEscapedQuotes() {
            assertDoesNotThrow(() -> EfxRegexValidator.validate("'a\\'b'"));
            assertDoesNotThrow(() -> EfxRegexValidator.validate("'a\\\"b'"));
        }

        @Test
        void testComplexPattern() {
            assertDoesNotThrow(() -> EfxRegexValidator.validate("'^[a-zA-Z][0-9]{2,4}(\\.[0-9]+)?$'"));
        }

        @Test
        void testEmptyPattern() {
            assertDoesNotThrow(() -> EfxRegexValidator.validate("''"));
        }

        @Test
        void testDoubleQuotedPattern() {
            assertDoesNotThrow(() -> EfxRegexValidator.validate("\"[0-9]+\""));
        }

        @Test
        void testLiteralBraceWhenNotQuantifier() {
            assertDoesNotThrow(() -> EfxRegexValidator.validate("'{abc'"));
        }
    }

    @Nested
    class DisallowedConstructs {

        @Test
        void testShorthandClass_d() {
            InvalidUsageException ex = assertThrows(InvalidUsageException.class,
                    () -> EfxRegexValidator.validate("'\\d'"));
            assertTrue(ex.getMessage().contains("shorthand class"));
        }

        @Test
        void testShorthandClass_w() {
            InvalidUsageException ex = assertThrows(InvalidUsageException.class,
                    () -> EfxRegexValidator.validate("'\\w'"));
            assertTrue(ex.getMessage().contains("shorthand class"));
        }

        @Test
        void testShorthandClass_s() {
            InvalidUsageException ex = assertThrows(InvalidUsageException.class,
                    () -> EfxRegexValidator.validate("'\\s'"));
            assertTrue(ex.getMessage().contains("shorthand class"));
        }

        @Test
        void testShorthandClass_D() {
            InvalidUsageException ex = assertThrows(InvalidUsageException.class,
                    () -> EfxRegexValidator.validate("'\\D'"));
            assertTrue(ex.getMessage().contains("shorthand class"));
        }

        @Test
        void testShorthandClass_W() {
            InvalidUsageException ex = assertThrows(InvalidUsageException.class,
                    () -> EfxRegexValidator.validate("'\\W'"));
            assertTrue(ex.getMessage().contains("shorthand class"));
        }

        @Test
        void testShorthandClass_S() {
            InvalidUsageException ex = assertThrows(InvalidUsageException.class,
                    () -> EfxRegexValidator.validate("'\\S'"));
            assertTrue(ex.getMessage().contains("shorthand class"));
        }

        @Test
        void testShorthandClassInsideCharacterClass() {
            InvalidUsageException ex = assertThrows(InvalidUsageException.class,
                    () -> EfxRegexValidator.validate("'[\\d\\w]'"));
            assertTrue(ex.getMessage().contains("shorthand class"));
        }

        @Test
        void testWordBoundary() {
            InvalidUsageException ex = assertThrows(InvalidUsageException.class,
                    () -> EfxRegexValidator.validate("'\\b'"));
            assertTrue(ex.getMessage().contains("word boundary"));
        }

        @Test
        void testNonWordBoundary() {
            InvalidUsageException ex = assertThrows(InvalidUsageException.class,
                    () -> EfxRegexValidator.validate("'\\B'"));
            assertTrue(ex.getMessage().contains("word boundary"));
        }

        @Test
        void testBackreference() {
            InvalidUsageException ex = assertThrows(InvalidUsageException.class,
                    () -> EfxRegexValidator.validate("'(a)\\1'"));
            assertTrue(ex.getMessage().contains("backreference"));
        }

        @Test
        void testUnicodePropertyEscape_MissingBraces() {
            InvalidUsageException ex = assertThrows(InvalidUsageException.class,
                    () -> EfxRegexValidator.validate("'\\p'"));
            assertTrue(ex.getMessage().contains("\\p"));
        }

        @Test
        void testUnicodePropertyEscape_UpperCase_MissingBraces() {
            InvalidUsageException ex = assertThrows(InvalidUsageException.class,
                    () -> EfxRegexValidator.validate("'\\P'"));
            assertTrue(ex.getMessage().contains("\\P"));
        }

        @Test
        void testUnicodePropertyEscape_MissingClosingBrace() {
            InvalidUsageException ex = assertThrows(InvalidUsageException.class,
                    () -> EfxRegexValidator.validate("'\\p{L'"));
            assertTrue(ex.getMessage().contains("\\p"));
        }

        @Test
        void testUnicodePropertyEscape_UpperCase_MissingClosingBrace() {
            InvalidUsageException ex = assertThrows(InvalidUsageException.class,
                    () -> EfxRegexValidator.validate("'\\P{Z'"));
            assertTrue(ex.getMessage().contains("\\P"));
        }

        @Test
        void testNumericEscape_Hex() {
            InvalidUsageException ex = assertThrows(InvalidUsageException.class,
                    () -> EfxRegexValidator.validate("'\\x41'"));
            assertTrue(ex.getMessage().contains("numeric character"));
        }

        @Test
        void testNumericEscape_Unicode() {
            InvalidUsageException ex = assertThrows(InvalidUsageException.class,
                    () -> EfxRegexValidator.validate("'\\u0041'"));
            assertTrue(ex.getMessage().contains("numeric character"));
        }

        @Test
        void testNumericEscape_Null() {
            InvalidUsageException ex = assertThrows(InvalidUsageException.class,
                    () -> EfxRegexValidator.validate("'\\0'"));
            assertTrue(ex.getMessage().contains("numeric character"));
        }

        @Test
        void testLookahead() {
            InvalidUsageException ex = assertThrows(InvalidUsageException.class,
                    () -> EfxRegexValidator.validate("'a(?=b)'"));
            assertTrue(ex.getMessage().contains("lookahead"));
        }

        @Test
        void testNegativeLookahead() {
            InvalidUsageException ex = assertThrows(InvalidUsageException.class,
                    () -> EfxRegexValidator.validate("'a(?!b)'"));
            assertTrue(ex.getMessage().contains("negative lookahead"));
        }

        @Test
        void testLookbehind() {
            InvalidUsageException ex = assertThrows(InvalidUsageException.class,
                    () -> EfxRegexValidator.validate("'(?<a)b'"));
            assertTrue(ex.getMessage().contains("lookbehind"));
        }

        @Test
        void testNonCapturingGroup() {
            InvalidUsageException ex = assertThrows(InvalidUsageException.class,
                    () -> EfxRegexValidator.validate("'(?:abc)'"));
            assertTrue(ex.getMessage().contains("non-capturing group"));
        }


        @Test
        void testUnsupportedEscapeSequence() {
            assertThrows(InvalidUsageException.class,
                    () -> EfxRegexValidator.validate("'\\a'"));
        }

        @Test
        void testUnclosedCharacterClass() {
            InvalidUsageException ex = assertThrows(InvalidUsageException.class,
                    () -> EfxRegexValidator.validate("'[abc'"));
            assertTrue(ex.getMessage().contains("unclosed character class"));
        }

        @Test
        void testUnclosedGroup() {
            InvalidUsageException ex = assertThrows(InvalidUsageException.class,
                    () -> EfxRegexValidator.validate("'(abc'"));
            assertTrue(ex.getMessage().contains("unclosed group"));
        }

        @Test
        void testUnmatchedClosingParenthesis() {
            InvalidUsageException ex = assertThrows(InvalidUsageException.class,
                    () -> EfxRegexValidator.validate("'abc)'"));
            assertTrue(ex.getMessage().contains("unmatched closing parenthesis"));
        }

        @Test
        void testTrailingBackslash() {
            InvalidUsageException ex = assertThrows(InvalidUsageException.class,
                    () -> EfxRegexValidator.validate("'abc\\'"));
            assertTrue(ex.getMessage().contains("trailing backslash"));
        }
    }
}
