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
package eu.europa.ted.efx;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.antlr.v4.runtime.BaseErrorListener;
import org.junit.jupiter.api.Test;

import eu.europa.ted.eforms.sdk.component.SdkComponent;
import eu.europa.ted.eforms.sdk.component.SdkComponentType;
import eu.europa.ted.efx.interfaces.MarkupGenerator;
import eu.europa.ted.efx.interfaces.ScriptGenerator;
import eu.europa.ted.efx.interfaces.SymbolResolver;
import eu.europa.ted.efx.interfaces.TranslatorDependencyFactory;
import eu.europa.ted.efx.interfaces.TranslatorOptions;
import eu.europa.ted.efx.interfaces.ValidatorGenerator;
import eu.europa.ted.efx.mock.DependencyFactoryMock;
import eu.europa.ted.efx.mock.sdk1.SymbolResolverMockV1;
import eu.europa.ted.efx.mock.sdk2.SymbolResolverMockV2;
import eu.europa.ted.efx.model.expressions.PathExpression;
import eu.europa.ted.efx.model.expressions.scalar.StringPath;

/**
 * Translates with symbol resolvers that give the paths of text fields as a class of their own: a
 * subclass of the toolkit's {@link StringPath}, with the public constructor that takes the script
 * alone, which {@link SymbolResolver} requires. A context iterator and a context variable take the
 * class of their element from the path the resolver gives, and the script generator builds
 * expressions of that class around what it composes. The translations are those of the mock
 * resolvers.
 */
class ResolverPathClassesTest {

  private static final String SDK_1 = "eforms-sdk-1.0";
  private static final String SDK_2 = "eforms-sdk-2.0";

  @Test
  void testResolvers_GiveTheirOwnClass() {
    for (final String sdkVersion : new String[] {SDK_1, SDK_2}) {
      final SymbolResolver symbols = OwnPathsDependencies.INSTANCE.createSymbolResolver(sdkVersion,
          OwnPathsDependencies.QUALIFIER);
      assertEquals(OwnTextPath.class,
          symbols.getRelativePathOfField("BT-00-Text", "ND-Root").getClass());
      assertEquals(OwnTextPath.class, symbols.getAbsolutePathOfField("BT-00-Text").getClass());
    }
  }

  // #region EFX-2 expressions ------------------------------------------------

  @Test
  void testContextIterator() {
    assertExpression(SDK_2, "for $t in PathNode/TextField return $t/normalize-space(text())",
        "for context:$t in BT-00-Text return $t");
  }

  @Test
  void testContextIterator_InFunction() {
    assertExpression(SDK_2,
        "for $t in PathNode/TextField return upper-case($t/normalize-space(text()))",
        "for context:$t in BT-00-Text return upper-case($t)");
  }

  @Test
  void testContextIterator_InQuantifier() {
    assertExpression(SDK_2,
        "some $t in PathNode/TextField satisfies $t/normalize-space(text()) = 'a'",
        "some context:$t in BT-00-Text satisfies $t == 'a'");
  }

  @Test
  void testContextIterator_ContextOverride() {
    assertExpression(SDK_2, "for $t in PathNode/TextField return $t/../NumberField/number()",
        "for context:$t in BT-00-Text return $t::BT-00-Number");
  }

  @Test
  void testContextIterator_PredicateOnItsSource() {
    assertExpression(SDK_2,
        "for $t in PathNode/TextField[exists(../NumberField)] return $t/normalize-space(text())",
        "for context:$t in BT-00-Text[BT-00-Number is present] return $t");
  }

  @Test
  void testIndex() {
    assertExpression(SDK_2, "exists((PathNode/TextField)[1])", "BT-00-Text[at 1] is present");
  }

  // #endregion EFX-2 expressions ---------------------------------------------

  // #region EFX-2 templates --------------------------------------------------

  @Test
  void testContextVariable_AsValue() {
    assertContextVariableTemplate("$ctx/normalize-space(text())", "$ctx");
  }

  @Test
  void testContextVariable_InFunction() {
    assertContextVariableTemplate("upper-case($ctx/normalize-space(text()))", "upper-case($ctx)");
  }

  @Test
  void testContextVariable_InComparison() {
    assertContextVariableTemplate("$ctx/normalize-space(text()) = 'a'", "$ctx == 'a'");
  }

  @Test
  void testContextVariable_InIterator() {
    assertContextVariableTemplate("for $x in $ctx/normalize-space(text()) return $x",
        "for text:$x in $ctx return $x");
  }

  // #endregion EFX-2 templates -----------------------------------------------

  // #region EFX-1 ------------------------------------------------------------

  @Test
  void testContextIterator_Efx1() {
    assertExpression(SDK_1, "for $t in PathNode/TextField return $t",
        "for context:$t in BT-00-Text return $t");
  }

  @Test
  void testContextIterator_InQuantifier_Efx1() {
    assertExpression(SDK_1, "some $t in PathNode/TextField satisfies $t = 'a'",
        "some context:$t in BT-00-Text satisfies $t == 'a'");
  }

  // #endregion EFX-1 ---------------------------------------------------------

  private static void assertExpression(final String sdkVersion, final String expected,
      final String expression) {
    try {
      final String translation = EfxTranslator.translateExpression(OwnPathsDependencies.INSTANCE,
          sdkVersion, String.format(SDK_1.equals(sdkVersion) ? "{ND-Root} ${%s}"
              : "WITH ND-Root COMPUTE %s", expression), EfxTestsBase.DEFAULT_OPTIONS);
      EfxTestsBase.assertValidXPath(translation);
      assertEquals(expected, translation);
    } catch (final InstantiationException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Asserts the translation of a template line whose context is a context variable assigned
   * {@code BT-00-Text}.
   *
   * @param expected the XPath of the expression block
   * @param expression the expression block
   */
  private static void assertContextVariableTemplate(final String expected,
      final String expression) {
    try {
      assertEquals(String.join("\n", "TEMPLATES:",
          "let body01(string:ctx) -> { eval(" + expected + ") }", "MAIN:",
          "for-each(/*/PathNode/TextField).call(body01(string:ctx=.))"),
          EfxTranslator.translateTemplate(OwnPathsDependencies.INSTANCE, SDK_2,
              "WITH context:$ctx = BT-00-Text DISPLAY ${" + expression + "};\n",
              EfxTestsBase.DEFAULT_OPTIONS));
    } catch (final InstantiationException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * The path of a text field, as a class of the resolver's own.
   */
  public static final class OwnTextPath extends StringPath {

    public OwnTextPath(final String script) {
      super(script);
    }

    /**
     * @param path a path the mock resolver gives
     * @return the path as an {@link OwnTextPath} if it is the path of a text field, or as it is
     */
    static PathExpression of(final PathExpression path) {
      return path.getClass() == StringPath.class ? new OwnTextPath(path.getScript()) : path;
    }
  }

  /**
   * The EFX-2 mock resolver, giving the paths of text fields as {@link OwnTextPath}. It is
   * declared with a qualifier of its own, as it would otherwise inherit the declaration of the
   * mock it extends, and two resolvers would be declared as one.
   */
  @SdkComponent(versions = {"2"}, componentType = SdkComponentType.SYMBOL_RESOLVER,
      qualifier = OwnPathsDependencies.QUALIFIER)
  static final class OwnPathsResolverV2 extends SymbolResolverMockV2 {

    OwnPathsResolverV2() throws InstantiationException {
      super();
    }

    @Override
    public PathExpression getRelativePathOfField(final String fieldId, final String contextId) {
      return OwnTextPath.of(super.getRelativePathOfField(fieldId, contextId));
    }

    @Override
    public PathExpression getAbsolutePathOfField(final String fieldId) {
      return OwnTextPath.of(super.getAbsolutePathOfField(fieldId));
    }
  }

  /**
   * The EFX-1 mock resolver, giving the paths of text fields as {@link OwnTextPath}, with a
   * qualifier of its own for the same reason.
   */
  @SdkComponent(versions = {"1"}, componentType = SdkComponentType.SYMBOL_RESOLVER,
      qualifier = OwnPathsDependencies.QUALIFIER)
  static final class OwnPathsResolverV1 extends SymbolResolverMockV1 {

    OwnPathsResolverV1() throws InstantiationException {
      super();
    }

    @Override
    public PathExpression getRelativePathOfField(final String fieldId, final String contextId) {
      return OwnTextPath.of(super.getRelativePathOfField(fieldId, contextId));
    }

    @Override
    public PathExpression getAbsolutePathOfField(final String fieldId) {
      return OwnTextPath.of(super.getAbsolutePathOfField(fieldId));
    }
  }

  /**
   * The dependencies of the unit tests, with the resolvers above.
   */
  private static final class OwnPathsDependencies implements TranslatorDependencyFactory {

    static final String QUALIFIER = "own-path-classes";
    static final OwnPathsDependencies INSTANCE = new OwnPathsDependencies();

    private final Map<String, SymbolResolver> resolvers;

    private OwnPathsDependencies() {
      try {
        this.resolvers = Map.of(SDK_1, new OwnPathsResolverV1(), SDK_2, new OwnPathsResolverV2());
      } catch (final InstantiationException e) {
        throw new IllegalStateException(e);
      }
    }

    @Override
    public SymbolResolver createSymbolResolver(final String sdkVersion, final String qualifier) {
      return this.resolvers.get(sdkVersion);
    }

    @Override
    public ScriptGenerator createScriptGenerator(final String sdkVersion, final String qualifier,
        final TranslatorOptions options) {
      return DependencyFactoryMock.INSTANCE.createScriptGenerator(sdkVersion, qualifier, options);
    }

    @Override
    public MarkupGenerator createMarkupGenerator(final String sdkVersion, final String qualifier,
        final TranslatorOptions options) {
      return DependencyFactoryMock.INSTANCE.createMarkupGenerator(sdkVersion, qualifier, options);
    }

    @Override
    public ValidatorGenerator createValidatorGenerator(final String sdkVersion,
        final String qualifier, final TranslatorOptions options) {
      return DependencyFactoryMock.INSTANCE.createValidatorGenerator(sdkVersion, qualifier,
          options);
    }

    @Override
    public BaseErrorListener createErrorListener() {
      return DependencyFactoryMock.INSTANCE.createErrorListener();
    }
  }
}
