/*
 * Copyright 2026 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European
 * Commission - subsequent versions of the EUPL (the "Licence"); You may not use this work except in
 * compliance with the Licence. You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence
 * is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the Licence for the specific language governing permissions and limitations under
 * the Licence.
 */
package eu.europa.ted.efx.autocomplete;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BuiltInElementTest {
    @Test
    public void testFunctionLabels() {
        assertEquals("count", EfxBuiltInFunction.COUNT_TEXT.getLabel());
        assertEquals("substring-before", EfxBuiltInFunction.SUBSTRING_BEFORE.getLabel());
        assertEquals("not", EfxBuiltInFunction.NOT.getLabel());
    }

    @Test
    public void testKeywordLabels() {
        assertEquals("TRUE", EfxKeyword.TRUE.getLabel());
        assertEquals("text", EfxKeyword.TEXT.getLabel());
    }

    @Test
    public void testPropertyLabels() {
        assertEquals(":wasWithheld", EfxLinkedProperty.WAS_WITHHELD.getLabel());
        assertEquals(":publicationDate", EfxLinkedProperty.PUBLICATION_DATE.getLabel());
    }
}
