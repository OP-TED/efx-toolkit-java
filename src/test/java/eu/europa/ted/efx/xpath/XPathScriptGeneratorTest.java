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
package eu.europa.ted.efx.xpath;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import eu.europa.ted.efx.EfxTranslatorOptions;
import eu.europa.ted.efx.model.expressions.Expression;
import eu.europa.ted.efx.model.expressions.Precedence;
import eu.europa.ted.efx.model.expressions.scalar.BooleanExpression;
import eu.europa.ted.efx.model.expressions.scalar.NodePath;
import eu.europa.ted.efx.model.expressions.scalar.NumericExpression;
import eu.europa.ted.efx.model.expressions.scalar.StringPath;
import eu.europa.ted.efx.model.expressions.sequence.BooleanSequencePath;
import eu.europa.ted.efx.model.expressions.sequence.NodeSequencePath;
import eu.europa.ted.efx.model.expressions.sequence.SequencePath;
import eu.europa.ted.efx.sdk1.xpath.XPathScriptGeneratorV1;

/**
 * A path that the generator composes from a {@code for} expression keeps the precedence of the
 * {@code for}, so that a predicate, an index or a path step after it applies to the whole
 * sequence.
 */
class XPathScriptGeneratorTest {

  private final XPathScriptGenerator generator =
      new XPathScriptGenerator(EfxTranslatorOptions.DEFAULT);

  /** A {@code for} expression that returns each element {@code n} of the root, as a sequence of nodes. */
  private NodeSequencePath loop() {
    return this.generator.composeForExpression(
        this.generator.composeIteratorList(List.of(this.generator.composeIteratorExpression(
            new NodePath("$x"), new NodeSequencePath("/*/n")))),
        new NodePath("$x"), NodeSequencePath.class);
  }

  @Test
  void testPredicateAfterAForExpression() {
    assertEquals("(for $x in /*/n return $x)[true()]",
        this.generator.composeNodeReferenceWithPredicate(this.loop(),
            new BooleanExpression("true()")).getScript());
    assertEquals("(for $x in /*/n return $x)[true()]",
        this.generator.composeFieldReferenceWithPredicate(this.loop(),
            new BooleanExpression("true()")).getScript());
  }

  @Test
  void testFieldInExternalReferenceAfterAForExpression() {
    assertEquals("(for $x in /*/n return $x)/a",
        new XPathScriptGeneratorV1(EfxTranslatorOptions.DEFAULT)
            .composeFieldInExternalReference(this.loop(), new StringPath("/a")).getScript());
  }

  @Test
  void testSdk1PresenceOfAForExpression() {
    final XPathScriptGeneratorV1 v1 = new XPathScriptGeneratorV1(EfxTranslatorOptions.DEFAULT);
    assertEquals("true() and (for $x in /*/n return $x)",
        v1.composeLogicalAnd(new BooleanExpression("true()"),
            v1.composeExistsCondition(this.loop())).getScript());
  }

  @Test
  void testSdk1IndicatorValueOfAForExpression() {
    final XPathScriptGeneratorV1 v1 = new XPathScriptGeneratorV1(EfxTranslatorOptions.DEFAULT);
    assertEquals(Precedence.PREFIX, v1.composeFieldValueReference(
        Expression.from(this.loop(), BooleanSequencePath.class)).getPrecedence());
  }

  @Test
  void testConversionKeepsThePrecedence() {
    final SequencePath converted = this.loop().asScalar().asSequence();
    assertEquals(Precedence.PREFIX, converted.getPrecedence());
    assertEquals("(for $x in /*/n return $x)[1]", this.generator
        .composeIndexer(converted, new NumericExpression("1"), NodePath.class).getScript());
  }
}
