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
package eu.europa.ted.eforms.sdk;

import static eu.europa.ted.eforms.sdk.XPathSymbolResolverTest.newFixtureResolver;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import eu.europa.ted.eforms.sdk.XPathSymbolResolverTest.MapBackedSymbolResolver;
import eu.europa.ted.eforms.sdk.entity.SdkField;
import eu.europa.ted.eforms.sdk.entity.SdkNode;
import eu.europa.ted.efx.model.expressions.PathExpression;
import eu.europa.ted.efx.model.expressions.scalar.ScalarPath;
import eu.europa.ted.efx.model.expressions.sequence.SequencePath;

/** Tests lazy graph wiring and resolution hooks independently of the shared contract setup. */
class XPathSymbolResolverHooksTest {

  private static final String ROOT = "ND-Root";
  private static final String LOT = "ND-Lot";
  private static final String LOT_TERMS = "ND-LotTerms";

  /**
   * A resolver over a minimal dataset (three nodes, three fields), rebuilt for every
   * test. Used where a test has to mutate the resolver, or to observe a graph that nothing has
   * resolved against yet.
   */
  private MapBackedSymbolResolver minimalResolver;

  @BeforeEach
  void setup() {
    this.minimalResolver = new MapBackedSymbolResolver();

    // ND-Root → ND-Lot (repeatable) → ND-LotTerms
    this.minimalResolver.addNode(ROOT, null, "/*", false);
    this.minimalResolver.addNode(LOT, ROOT, "/*/cac:ProcurementProjectLot", true);
    this.minimalResolver.addNode(LOT_TERMS, LOT, "/*/cac:ProcurementProjectLot/cac:TenderingTerms",
        false);

    this.minimalResolver.addField("BT-Root", "text", ROOT, "/*/cbc:ID", false);
    this.minimalResolver.addField("BT-InLot", "text", LOT_TERMS,
        "/*/cac:ProcurementProjectLot/cac:TenderingTerms/cbc:Note", false);
    this.minimalResolver.addField("BT-Repeatable", "text", ROOT, "/*/cbc:Tag", true);
  }

  // =========================================================================
  // Lazy loading and resolution hooks
  // =========================================================================

  @Nested
  @DisplayName("Lazy loading and resolution hooks")
  class LazyLoadingTests {

    @Test
    @DisplayName("resolves symbols from in-memory maps, with no SDK on the filesystem")
    void resolvesWithoutFilesystem() {
      assertEquals(ROOT, minimalResolver.getRootNodeId());
      assertEquals("text", minimalResolver.getTypeOfField("BT-Root"));
      assertEquals(LOT_TERMS, minimalResolver.getParentNodeOfField("BT-InLot"));
      assertEquals("/*/cbc:ID", minimalResolver.getAbsolutePathOfField("BT-Root").getScript());
    }

    @Test
    @DisplayName("builds node ancestry on demand, without the graph being wired up front")
    void wiresAncestryOnDemand() {
      // Nothing has called setParent, so the chain has to be discovered through nodeById.
      assertEquals(List.of(LOT_TERMS, LOT, ROOT), minimalResolver.ancestry(LOT_TERMS));
      assertEquals(List.of(ROOT), minimalResolver.ancestry(ROOT));
    }

    @Test
    @DisplayName("keeps no ancestry cache of its own: the list is the one cached on SdkNode")
    void ancestryIsCachedOnTheEntity() {
      List<String> first = minimalResolver.ancestry(LOT_TERMS);
      List<String> second = minimalResolver.ancestry(LOT_TERMS);

      // Same instance means the second call came from SdkNode's own cachedAncestry rather than
      // from a duplicate cache held by the minimalResolver.
      assertSame(first, second);
      assertSame(minimalResolver.nodeById(LOT), minimalResolver.nodeById(LOT_TERMS).getParent());
    }

    @Test
    @DisplayName("resolves a field's parent node on demand")
    void wiresFieldParentOnDemand() {
      SdkNode parent = minimalResolver.parentNode("BT-InLot");

      assertEquals(LOT_TERMS, parent.getId());
      assertSame(parent, minimalResolver.parentNode("BT-InLot"));
    }

    @Test
    @DisplayName("picks the path shape from repeatability, exactly as the SDK-backed resolver does")
    void picksPathShapeFromRepeatability() {
      // Field under a repeatable ancestor: many values from the root context.
      assertTrue(minimalResolver.getAbsolutePathOfField("BT-InLot") instanceof SequencePath);
      // Field that is itself repeatable.
      assertTrue(minimalResolver.getAbsolutePathOfField("BT-Repeatable") instanceof SequencePath);
      // Neither the field nor any ancestor repeats.
      assertTrue(minimalResolver.getAbsolutePathOfField("BT-Root") instanceof ScalarPath);
    }

    @Test
    @DisplayName("respects the context boundary when deciding repeatability")
    void respectsContextBoundary() {
      // From the root, BT-InLot repeats because ND-Lot does.
      assertTrue(minimalResolver.isFieldRepeatableFromContext("BT-InLot", null));
      // From inside one lot, it does not: we are already positioned on a single instance.
      assertFalse(minimalResolver.isFieldRepeatableFromContext("BT-InLot", LOT));

      PathExpression relative = minimalResolver.getRelativePathOfField("BT-InLot", LOT);
      assertTrue(relative instanceof ScalarPath);
      assertEquals("cac:TenderingTerms/cbc:Note", relative.getScript());
    }

    @Test
    @DisplayName("resolveField is an overridable hook for all field resolution")
    void resolveFieldHookIsOverridable() {
      MapBackedSymbolResolver prefixing = new MapBackedSymbolResolver() {
        @Override
        protected SdkField resolveField(String fieldId) {
          // Accept a namespaced form of every field id.
          return super.resolveField(fieldId.startsWith("x:") ? fieldId.substring(2) : fieldId);
        }
      };
      prefixing.addNode(ROOT, null, "/*", false);
      prefixing.addField("BT-Root", "text", ROOT, "/*/cbc:ID", false);

      // Reaches the override through the public API, not just directly.
      assertEquals("/*/cbc:ID", prefixing.getAbsolutePathOfField("x:BT-Root").getScript());
      assertEquals("text", prefixing.getTypeOfField("x:BT-Root"));
    }

    @Test
    @DisplayName("resolveNode is an overridable hook for all node resolution")
    void resolveNodeHookIsOverridable() {
      MapBackedSymbolResolver prefixing = new MapBackedSymbolResolver() {
        @Override
        protected SdkNode resolveNode(String nodeId) {
          return super.resolveNode(nodeId.startsWith("x:") ? nodeId.substring(2) : nodeId);
        }
      };
      prefixing.addNode(ROOT, null, "/*", false);
      prefixing.addNode(LOT, ROOT, "/*/cac:ProcurementProjectLot", true);

      assertEquals("/*/cac:ProcurementProjectLot",
          prefixing.getAbsolutePathOfNode("x:" + LOT).getScript());
      assertTrue(prefixing.isNodeRepeatableFromContext("x:" + LOT, null));
    }

    @Test
    @DisplayName("privacy mask comes from the data types the subclass supplies")
    void privacyMaskUsesSubclassDataTypes() {
      assertEquals("unpublished", minimalResolver.getPrivacyMask("BT-Root"));
    }

    @Test
    @DisplayName("the SDK fixture arrives with fields unwired, and resolution repairs the link")
    void fixtureArrivesWithFieldsUnwired() throws IOException {
      // Verify the loader leaves the graph unwired before any resolution takes place.
      MapBackedSymbolResolver fresh = newFixtureResolver();

      SdkField field = fresh.fieldById("BT-12-Text");
      SdkNode node = fresh.nodeById("ND-SubSubNode2");
      assertEquals("ND-SubSubNode2", field.getParentNodeId(),
          "Precondition: the id is present in the JSON");
      assertNull(field.getParentNode(),
          "Precondition: loading the fixture does no parent wiring");

      // Resolution fills the link in without any help from the loading strategy.
      assertSame(node, fresh.parentNode("BT-12-Text"));
      assertEquals(List.of("ND-SubSubNode2", "ND-SubNode", "ND-Root"),
          fresh.ancestry("ND-SubSubNode2"));
    }
  }

  @Nested
  @DisplayName("Alias fallback hooks")
  class AliasFallbackTests {

    @BeforeEach
    void registerAliases() {
      minimalResolver.addFieldAlias("rootText", "BT-Root");
      minimalResolver.addNodeAlias("lot", LOT);
    }

    @Test
    @DisplayName("resolveField falls back to the fieldByAlias hook")
    void resolveField_fallsBackToAlias() {
      assertEquals("text", minimalResolver.getTypeOfField("rootText"));
      assertEquals("/*/cbc:ID", minimalResolver.getAbsolutePathOfField("rootText").getScript());
    }

    @Test
    @DisplayName("resolveNode falls back to the nodeByAlias hook")
    void resolveNode_fallsBackToAlias() {
      assertEquals("/*/cac:ProcurementProjectLot",
          minimalResolver.getAbsolutePathOfNode("lot").getScript());
      assertTrue(minimalResolver.isNodeRepeatableFromContext("lot", null));
    }

    @Test
    @DisplayName("An identifier wins over an alias of the same name")
    void identifierWinsOverAlias() {
      // "BT-Root" is a real id; registering it as an alias of another field must not shadow it.
      minimalResolver.addFieldAlias("BT-Root", "BT-Repeatable");

      assertEquals("/*/cbc:ID", minimalResolver.getAbsolutePathOfField("BT-Root").getScript(),
          "fieldById is consulted before fieldByAlias");
    }

    @Test
    @DisplayName("getFieldIdFromAlias returns the field ID, or null when unknown")
    void getFieldIdFromAlias_returnsIdOrNull() {
      assertEquals("BT-Root", minimalResolver.getFieldIdFromAlias("rootText"));
      assertNull(minimalResolver.getFieldIdFromAlias("nonexistent-alias"),
          "Unknown alias should return null to enable fallback");
    }

    @Test
    @DisplayName("getNodeIdFromAlias returns the node ID, or null when unknown")
    void getNodeIdFromAlias_returnsIdOrNull() {
      assertEquals(LOT, minimalResolver.getNodeIdFromAlias("lot"));
      assertNull(minimalResolver.getNodeIdFromAlias("nonexistent-alias"),
          "Unknown alias should return null");
    }
  }
}
