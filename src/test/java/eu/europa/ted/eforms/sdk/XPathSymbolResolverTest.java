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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.europa.ted.eforms.sdk.entity.SdkCodelist;
import eu.europa.ted.eforms.sdk.entity.SdkDataType;
import eu.europa.ted.eforms.sdk.entity.SdkField;
import eu.europa.ted.eforms.sdk.entity.SdkNode;
import eu.europa.ted.eforms.sdk.entity.SdkNoticeSubtype;
import eu.europa.ted.eforms.sdk.entity.v1.SdkFieldV1;
import eu.europa.ted.eforms.sdk.entity.v1.SdkNodeV1;
import eu.europa.ted.eforms.sdk.entity.v2.SdkCodelistV2;
import eu.europa.ted.eforms.sdk.entity.v2.SdkFieldV2;
import eu.europa.ted.eforms.sdk.entity.v2.SdkNodeV2;
import eu.europa.ted.eforms.sdk.repository.SdkDataTypeRepository;
import eu.europa.ted.efx.exceptions.SdkInconsistencyException;
import eu.europa.ted.efx.exceptions.SymbolResolutionException;
import eu.europa.ted.efx.exceptions.SymbolResolutionException.ErrorCode;
import eu.europa.ted.efx.model.PrivacySetting;
import eu.europa.ted.efx.model.expressions.PathExpression;
import eu.europa.ted.efx.model.expressions.scalar.BooleanPath;
import eu.europa.ted.efx.model.expressions.scalar.DatePath;
import eu.europa.ted.efx.model.expressions.scalar.DurationPath;
import eu.europa.ted.efx.model.expressions.scalar.NodePath;
import eu.europa.ted.efx.model.expressions.scalar.NumericPath;
import eu.europa.ted.efx.model.expressions.scalar.ScalarPath;
import eu.europa.ted.efx.model.expressions.scalar.StringPath;
import eu.europa.ted.efx.model.expressions.scalar.TimePath;
import eu.europa.ted.efx.model.expressions.sequence.MultilingualStringSequencePath;
import eu.europa.ted.efx.model.expressions.sequence.SequencePath;

/**
 * Comprehensive tests for XPathSymbolResolver covering:
 * 0. Loading-agnostic contract
 * 1. Exception handling for bad inputs
 * 2. Repeatability logic from different contexts
 * 3. PathExpression shape (ScalarPath vs SequencePath)
 * 4. PathExpression data types
 * 5. Context-aware relative paths
 * 6. Basic lookups
 * 7. Alias fallback contract
 * 8. Privacy settings
 */
class XPathSymbolResolverTest {

  private static final String ROOT = "ND-Root";
  private static final String LOT = "ND-Lot";
  private static final String LOT_TERMS = "ND-LotTerms";

  private static final Path FIXTURE_JSON =
      Path.of("src", "test", "resources", "json", "sdk2-fields.json");

  /**
   * The resolver under test, backed by the full SDK-2 fixture and loaded once.
   */
  private static MapBackedSymbolResolver resolver;

  /**
   * A resolver over a minimal dataset (three nodes, three fields), rebuilt for every
   * test. Used where a test has to mutate the resolver, or to observe a graph that nothing has
   * resolved against yet.
   */
  private MapBackedSymbolResolver minimalResolver;

  private static class MapBackedSymbolResolver extends XPathSymbolResolver {
    private final Map<String, SdkField> fields = new LinkedHashMap<>();
    private final Map<String, SdkNode> nodes = new LinkedHashMap<>();
    private final Map<String, SdkCodelist> codelists = new LinkedHashMap<>();
    private final Map<String, SdkField> fieldAliases = new LinkedHashMap<>();
    private final Map<String, SdkNode> nodeAliases = new LinkedHashMap<>();
    private final SdkDataTypeRepository dataTypes = SdkDataTypeRepository.createDefault();

    void addNode(String id, String parentId, String xpathAbsolute, boolean repeatable) {
      this.nodes.put(id, new SdkNodeV1(id, parentId, xpathAbsolute, xpathAbsolute, repeatable));
    }

    void addField(String id, String type, String parentNodeId, String xpathAbsolute,
        boolean repeatable) {
      this.fields.put(id,
          new SdkFieldV1(id, type, parentNodeId, xpathAbsolute, xpathAbsolute, null, repeatable));
    }

    void addCodelist(String id, String parentId, String... codes) {
      this.codelists.put(id,
          new SdkCodelistV2(id, "0.0.1", List.of(codes), Optional.ofNullable(parentId)));
    }

    void addFieldAlias(String alias, String fieldId) {
      this.fieldAliases.put(alias, this.fields.get(fieldId));
    }

    void addNodeAlias(String alias, String nodeId) {
      this.nodeAliases.put(alias, this.nodes.get(nodeId));
    }

    void loadFixture() throws IOException {
      final JsonNode json = new ObjectMapper().readTree(FIXTURE_JSON.toFile());

      StreamSupport.stream(json.get("xmlStructure").spliterator(), false)
          .map(SdkNodeV2::new)
          .forEach(node -> this.nodes.put(node.getId(), node));

      StreamSupport.stream(json.get("fields").spliterator(), false)
          .map(SdkFieldV2::new)
          .forEach(field -> this.fields.put(field.getId(), field));
    }

    @Override
    protected SdkField fieldById(String fieldId) {
      return this.fields.get(fieldId);
    }

    @Override
    protected SdkNode nodeById(String nodeId) {
      return this.nodes.get(nodeId);
    }

    @Override
    protected SdkCodelist codelistById(String codelistId) {
      return this.codelists.get(codelistId);
    }

    @Override
    protected SdkDataType dataTypeById(String dataTypeId) {
      return this.dataTypes.get(dataTypeId);
    }

    @Override
    protected SdkField fieldByAlias(String alias) {
      // The contract is to return null for an unknown alias, not to throw.
      return this.fieldAliases.get(alias);
    }

    @Override
    protected SdkNode nodeByAlias(String alias) {
      return this.nodeAliases.get(alias);
    }

    @Override
    protected Collection<SdkNode> allNodes() {
      return this.nodes.values();
    }

    @Override
    public List<SdkNoticeSubtype> getAllNoticeSubtypes() {
      return List.of();
    }

    // Exposed so the tests can assert on the graph helpers directly.
    List<String> ancestry(String nodeId) {
      return this.ancestryOf(this.nodeById(nodeId));
    }

    SdkNode parentNode(String fieldId) {
      return this.parentNodeOf(this.fieldById(fieldId));
    }
  }

  @BeforeAll
  static void loadSharedFixture() throws IOException {
    resolver = newFixtureResolver();
  }

  private static MapBackedSymbolResolver newFixtureResolver() throws IOException {
    MapBackedSymbolResolver loaded = new MapBackedSymbolResolver();
    loaded.loadFixture();
    // Codelists are not part of the fields fixture, so supply the ones it refers to.
    loaded.addCodelist("accessibility", null, "code1", "code2", "code3");
    loaded.addCodelist("authority-activity", "main-activity", "code1", "code2", "code3");
    loaded.addCodelist("main-activity", null, "code1", "code2", "code3");
    return loaded;
  }

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

  private static void assertUnknownSymbol(String symbol, Executable call) {
    SymbolResolutionException ex = assertThrows(SymbolResolutionException.class, call);
    assertEquals(ErrorCode.UNKNOWN_SYMBOL, ex.getErrorCode());
    assertTrue(ex.getMessage().contains(symbol),
        "Message should name the offending symbol, was: " + ex.getMessage());
  }

  // =========================================================================
  // 0. Loading-agnostic contract
  // =========================================================================

  @Nested
  @DisplayName("0. Loading-agnostic contract")
  class ContractTests {

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
      // A fresh load, so that the shared fixture's wiring cannot mask the premise.
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

  // =========================================================================
  // 1. Exception Handling
  // =========================================================================

  @Nested
  @DisplayName("1. Exception Handling")
  class ExceptionTests {

    @Test
    @DisplayName("getAbsolutePathOfField throws UNKNOWN_SYMBOL for nonexistent field")
    void getAbsolutePathOfField_unknownField_throwsException() {
      SymbolResolutionException ex = assertThrows(
          SymbolResolutionException.class,
          () -> resolver.getAbsolutePathOfField("BT-NONEXISTENT"));

      assertEquals(ErrorCode.UNKNOWN_SYMBOL, ex.getErrorCode());
    }

    @Test
    @DisplayName("getAbsolutePathOfNode throws UNKNOWN_SYMBOL for nonexistent node")
    void getAbsolutePathOfNode_unknownNode_throwsException() {
      SymbolResolutionException ex = assertThrows(
          SymbolResolutionException.class,
          () -> resolver.getAbsolutePathOfNode("ND-NONEXISTENT"));

      assertEquals(ErrorCode.UNKNOWN_SYMBOL, ex.getErrorCode());
    }

    @Test
    @DisplayName("expandCodelist throws UNKNOWN_CODELIST for nonexistent codelist")
    void expandCodelist_unknownCodelist_throwsException() {
      SymbolResolutionException ex = assertThrows(
          SymbolResolutionException.class,
          () -> resolver.expandCodelist("nonexistent-codelist"));

      assertEquals(ErrorCode.UNKNOWN_CODELIST, ex.getErrorCode());
    }

    @Test
    @DisplayName("getRootCodelistOfField throws NO_CODELIST_FOR_FIELD for non-code field")
    void getRootCodelistOfField_nonCodeField_throwsException() {
      SymbolResolutionException ex = assertThrows(
          SymbolResolutionException.class,
          () -> resolver.getRootCodelistOfField("BT-12-Text"));

      assertEquals(ErrorCode.NO_CODELIST_FOR_FIELD, ex.getErrorCode());
    }
  }

  // =========================================================================
  // 2. Repeatability Logic
  // =========================================================================

  @Nested
  @DisplayName("2. Repeatability Logic")
  class RepeatabilityTests {

    @Test
    @DisplayName("Scalar field from root context returns false")
    void isFieldRepeatableFromContext_scalarFieldFromRoot_returnsFalse() {
      assertFalse(resolver.isFieldRepeatableFromContext("BT-12-Text", null),
          "Field with no repeatable ancestors should return false from root");
    }

    @Test
    @DisplayName("Field in self-repeatable node from root returns true")
    void isFieldRepeatableFromContext_fieldInRepNode_fromRoot_returnsTrue() {
      assertTrue(resolver.isFieldRepeatableFromContext("BT-13-Text", null),
          "Field in repeatable node should return true from root");
    }

    @Test
    @DisplayName("Field under repeatable ancestor from root returns true")
    void isFieldRepeatableFromContext_fieldUnderRepAncestor_fromRoot_returnsTrue() {
      assertTrue(resolver.isFieldRepeatableFromContext("BT-21-Number", null),
          "Field under repeatable ancestor should return true from root");
    }

    @Test
    @DisplayName("Field under repeatable ancestor from that ancestor returns false")
    void isFieldRepeatableFromContext_fieldUnderRepAncestor_fromRepAncestor_returnsFalse() {
      assertFalse(resolver.isFieldRepeatableFromContext("BT-21-Number", "ND-RepeatableNode"),
          "Walk should stop at context node, returning false");
    }

    @Test
    @DisplayName("Deeply nested field with inner repeatable node from outer context returns true")
    void isFieldRepeatableFromContext_deeplyNested_fromOuterRepContext_returnsTrue() {
      assertTrue(resolver.isFieldRepeatableFromContext("BT-25-Date", "ND-RepeatableNode"),
          "Should still return true because inner repeatable node is crossed");
    }

    @Test
    @DisplayName("Scalar field from cross-branch repeatable context stays scalar")
    void isFieldRepeatableFromContext_scalarField_fromCrossBranchRepContext_returnsFalse() {
      assertFalse(resolver.isFieldRepeatableFromContext("BT-12-Text", "ND-RepeatableNode"),
          "Scalar field should remain scalar even from rep context on different branch");
    }

    @Test
    @DisplayName("Sequence field from cross-branch non-repeatable context stays sequence")
    void isFieldRepeatableFromContext_sequenceField_fromCrossBranchNonRepContext_returnsTrue() {
      assertTrue(resolver.isFieldRepeatableFromContext("BT-21-Number", "ND-SubNode"),
          "Sequence field should remain sequence from non-rep context on different branch");
    }

    @Test
    @DisplayName("Attribute field in repeatable node from root returns true")
    void isFieldRepeatableFromContext_attributeInRepNode_fromRoot_returnsTrue() {
      assertTrue(resolver.isFieldRepeatableFromContext("BT-00-Attribute-In-Repeatable-Node", null),
          "Attribute field in repeatable node should return true from root");
    }

    @Test
    @DisplayName("Attribute field in repeatable node from that node returns false")
    void isFieldRepeatableFromContext_attributeInRepNode_fromRepNode_returnsFalse() {
      assertFalse(
          resolver.isFieldRepeatableFromContext("BT-00-Attribute-In-Repeatable-Node",
              "ND-RepeatableNode"),
          "Attribute field should not be repeatable when context is its parent repeatable node");
    }

    @Test
    @DisplayName("Repeatable field from root returns true")
    void isFieldRepeatableFromContext_repeatableField_fromRoot_returnsTrue() {
      assertTrue(resolver.isFieldRepeatableFromContext("BT-00-Repeatable-Text", null),
          "Repeatable field should return true from root context");
    }

    @Test
    @DisplayName("Repeatable node from root returns true")
    void isNodeRepeatableFromContext_repeatableNode_fromRoot_returnsTrue() {
      assertTrue(resolver.isNodeRepeatableFromContext("ND-RepeatableNode", null),
          "Repeatable node should return true from root context");
    }
  }

  // =========================================================================
  // 3. PathExpression Shape Tests
  // =========================================================================

  @Nested
  @DisplayName("3. PathExpression Shape Tests")
  class PathExpressionShapeTests {

    @Test
    @DisplayName("Non-repeatable field returns ScalarPath")
    void nonRepeatableField_shouldReturnScalarPath() {
      assertFalse(resolver.isFieldRepeatableFromContext("BT-00-Text", null),
          "Precondition: field should NOT be repeatable from root");

      PathExpression path = resolver.getAbsolutePathOfField("BT-00-Text");

      assertTrue(path instanceof ScalarPath,
          "Non-repeatable field should return ScalarPath, got: " + path.getClass().getSimpleName());
    }

    @Test
    @DisplayName("Repeatable field returns SequencePath")
    void repeatableField_shouldReturnSequencePath() {
      assertTrue(resolver.isFieldRepeatableFromContext("BT-00-Repeatable-Text", null),
          "Precondition: field should be repeatable from root");

      PathExpression path = resolver.getAbsolutePathOfField("BT-00-Repeatable-Text");

      assertTrue(path instanceof SequencePath,
          "Repeatable field should return SequencePath, got: " + path.getClass().getSimpleName());
    }

    @Test
    @DisplayName("Field in repeatable node returns SequencePath")
    void fieldInRepeatableNode_shouldReturnSequencePath() {
      assertTrue(resolver.isFieldRepeatableFromContext("BT-00-Text-In-Repeatable-Node", null),
          "Precondition: field in repeatable node should be repeatable from root");

      PathExpression path = resolver.getAbsolutePathOfField("BT-00-Text-In-Repeatable-Node");

      assertTrue(path instanceof SequencePath,
          "Field in repeatable node should return SequencePath, got: " + path.getClass().getSimpleName());
    }

    @Test
    @DisplayName("Field in non-repeatable subnode of repeatable node returns SequencePath")
    void fieldInNonRepeatableSubNode_shouldReturnSequencePath() {
      assertTrue(resolver.isFieldRepeatableFromContext("BT-00-Text-In-NonRepeatableSubNode", null),
          "Precondition: field under repeatable ancestor should be repeatable from root");

      PathExpression path = resolver.getAbsolutePathOfField("BT-00-Text-In-NonRepeatableSubNode");

      assertTrue(path instanceof SequencePath,
          "Field under repeatable ancestor should return SequencePath, got: " + path.getClass().getSimpleName());
    }

    @Test
    @DisplayName("Field in double-repeatable context returns SequencePath")
    void fieldInDoubleRepeatableContext_shouldReturnSequencePath() {
      assertTrue(resolver.isFieldRepeatableFromContext("BT-00-Text-In-RepeatableSubSubNode", null),
          "Precondition: field in double-repeatable context should be repeatable from root");

      PathExpression path = resolver.getAbsolutePathOfField("BT-00-Text-In-RepeatableSubSubNode");

      assertTrue(path instanceof SequencePath,
          "Field in double-repeatable context should return SequencePath, got: " + path.getClass().getSimpleName());
    }

    @Test
    @DisplayName("Non-repeatable root node returns NodePath (scalar)")
    void nonRepeatableRootNode_shouldReturnScalarPath() {
      PathExpression path = resolver.getAbsolutePathOfNode("ND-Root");

      assertTrue(path instanceof ScalarPath,
          "Non-repeatable node should return ScalarPath, got: " + path.getClass().getSimpleName());
      assertEquals(NodePath.class, path.getClass(),
          "Non-repeatable node should return NodePath");
    }

    @Test
    @DisplayName("Repeatable node returns SequencePath")
    void repeatableNode_shouldReturnSequencePath() {
      PathExpression path = resolver.getAbsolutePathOfNode("ND-RepeatableNode");

      assertTrue(path instanceof SequencePath,
          "Repeatable node should return SequencePath, got: " + path.getClass().getSimpleName());
    }

    @Test
    @DisplayName("Non-repeatable subnode under repeatable parent returns SequencePath")
    void nonRepeatableSubNode_underRepeatableParent_shouldReturnSequencePath() {
      assertTrue(resolver.isNodeRepeatableFromContext("ND-NonRepeatableSubNode", null),
          "Precondition: node under repeatable ancestor should be repeatable from root");

      PathExpression path = resolver.getAbsolutePathOfNode("ND-NonRepeatableSubNode");

      assertTrue(path instanceof SequencePath,
          "Node under repeatable ancestor should return SequencePath, got: " + path.getClass().getSimpleName());
    }

    @Test
    @DisplayName("Repeatable subsubnode returns SequencePath")
    void repeatableSubSubNode_shouldReturnSequencePath() {
      PathExpression path = resolver.getAbsolutePathOfNode("ND-RepeatableSubSubNode");

      assertTrue(path instanceof SequencePath,
          "Repeatable subsubnode should return SequencePath, got: " + path.getClass().getSimpleName());
    }

    @Test
    @DisplayName("Non-repeatable attribute field without attribute returns ScalarPath")
    void nonRepeatableAttributeField_withoutAttribute_shouldReturnScalarPath() {
      assertFalse(resolver.isFieldRepeatableFromContext("BT-00-Attribute", null),
          "Precondition: attribute field should NOT be repeatable from root");

      PathExpression path = resolver.getAbsolutePathOfFieldWithoutTheAttribute("BT-00-Attribute");

      assertTrue(path instanceof ScalarPath,
          "Non-repeatable attribute field without @attribute should return ScalarPath, got: " + path.getClass().getSimpleName());
    }

    @Test
    @DisplayName("Attribute field in repeatable node without attribute returns SequencePath")
    void attributeInRepeatableNode_withoutAttribute_shouldReturnSequencePath() {
      assertTrue(resolver.isFieldRepeatableFromContext("BT-00-Attribute-In-Repeatable-Node", null),
          "Precondition: attribute field in repeatable node should be repeatable from root");

      PathExpression path = resolver.getAbsolutePathOfFieldWithoutTheAttribute("BT-00-Attribute-In-Repeatable-Node");

      assertTrue(path instanceof SequencePath,
          "Attribute field in repeatable node without @attribute should return SequencePath, got: " + path.getClass().getSimpleName());
    }
  }

  // =========================================================================
  // 4. PathExpression Data Type Tests
  // =========================================================================

  @Nested
  @DisplayName("4. PathExpression Data Type Tests")
  class PathExpressionDataTypeTests {

    @Test
    @DisplayName("Text field returns StringPath")
    void textField_shouldReturnStringPath() {
      PathExpression path = resolver.getAbsolutePathOfField("BT-00-Text");
      assertEquals(StringPath.class, path.getClass(),
          "Text field should return StringPath");
    }

    @Test
    @DisplayName("Indicator field returns BooleanPath")
    void indicatorField_shouldReturnBooleanPath() {
      PathExpression path = resolver.getAbsolutePathOfField("BT-00-Indicator");
      assertEquals(BooleanPath.class, path.getClass(),
          "Indicator field should return BooleanPath");
    }

    @Test
    @DisplayName("Code field returns StringPath")
    void codeField_shouldReturnStringPath() {
      PathExpression path = resolver.getAbsolutePathOfField("BT-00-Code");
      assertEquals(StringPath.class, path.getClass(),
          "Code field should return StringPath");
    }

    @Test
    @DisplayName("Multilingual field returns MultilingualStringSequencePath")
    void multilingualField_shouldReturnMultilingualStringSequencePath() {
      PathExpression path = resolver.getAbsolutePathOfField("BT-00-Text-Multilingual");
      assertEquals(MultilingualStringSequencePath.class, path.getClass(),
          "Multilingual field should return MultilingualStringSequencePath");
    }

    @Test
    @DisplayName("Date field returns DatePath")
    void dateField_shouldReturnDatePath() {
      PathExpression path = resolver.getAbsolutePathOfField("BT-00-StartDate");
      assertEquals(DatePath.class, path.getClass(),
          "Date field should return DatePath");
    }

    @Test
    @DisplayName("Time field returns TimePath")
    void timeField_shouldReturnTimePath() {
      PathExpression path = resolver.getAbsolutePathOfField("BT-00-StartTime");
      assertEquals(TimePath.class, path.getClass(),
          "Time field should return TimePath");
    }

    @Test
    @DisplayName("Duration field returns DurationPath")
    void durationField_shouldReturnDurationPath() {
      PathExpression path = resolver.getAbsolutePathOfField("BT-00-Duration");
      assertEquals(DurationPath.class, path.getClass(),
          "Duration field should return DurationPath");
    }

    @Test
    @DisplayName("Integer field returns NumericPath")
    void integerField_shouldReturnNumericPath() {
      PathExpression path = resolver.getAbsolutePathOfField("BT-00-Integer");
      assertEquals(NumericPath.class, path.getClass(),
          "Integer field should return NumericPath");
    }

    @Test
    @DisplayName("Amount field returns NumericPath")
    void amountField_shouldReturnNumericPath() {
      PathExpression path = resolver.getAbsolutePathOfField("BT-00-Amount");
      assertEquals(NumericPath.class, path.getClass(),
          "Amount field should return NumericPath");
    }

    @Test
    @DisplayName("Number field returns NumericPath")
    void numberField_shouldReturnNumericPath() {
      PathExpression path = resolver.getAbsolutePathOfField("BT-00-Number");
      assertEquals(NumericPath.class, path.getClass(),
          "Number field should return NumericPath");
    }
  }

  // =========================================================================
  // 5. Context-Aware Relative Paths
  // =========================================================================

  @Nested
  @DisplayName("5. Context-Aware Relative Paths")
  class RelativePathTests {

    @Test
    @DisplayName("Field in repeatable node from root returns SequencePath")
    void fromRoot_repeatableField_shouldReturnSequencePath() {
      assertTrue(resolver.isFieldRepeatableFromContext("BT-00-Text-In-Repeatable-Node", null),
          "Precondition: field should be repeatable from root");

      PathExpression path = resolver.getRelativePathOfField("BT-00-Text-In-Repeatable-Node", "ND-Root");

      assertTrue(path instanceof SequencePath,
          "Field in repeatable node from root should return SequencePath, got: " + path.getClass().getSimpleName());
    }

    @Test
    @DisplayName("Field in repeatable node from same node context returns ScalarPath")
    void fromRepeatableNode_sameField_shouldReturnScalarPath() {
      assertFalse(resolver.isFieldRepeatableFromContext("BT-00-Text-In-Repeatable-Node", "ND-RepeatableNode"),
          "Precondition: field should NOT be repeatable from its own parent node context");

      PathExpression path = resolver.getRelativePathOfField("BT-00-Text-In-Repeatable-Node", "ND-RepeatableNode");

      assertTrue(path instanceof ScalarPath,
          "Field from its parent repeatable node context should return ScalarPath, got: " + path.getClass().getSimpleName());
    }

    @Test
    @DisplayName("Non-repeatable field from root returns ScalarPath")
    void fromRoot_nonRepeatableField_shouldReturnScalarPath() {
      assertFalse(resolver.isFieldRepeatableFromContext("BT-00-Text", null),
          "Precondition: field should NOT be repeatable");

      PathExpression path = resolver.getRelativePathOfField("BT-00-Text", "ND-Root");

      assertTrue(path instanceof ScalarPath,
          "Non-repeatable field should return ScalarPath, got: " + path.getClass().getSimpleName());
    }

    @Test
    @DisplayName("Repeatable node from root returns SequencePath")
    void fromRoot_repeatableNode_shouldReturnSequencePath() {
      PathExpression path = resolver.getRelativePathOfNode("ND-RepeatableNode", "ND-Root");

      assertTrue(path instanceof SequencePath,
          "Repeatable node from root should return SequencePath, got: " + path.getClass().getSimpleName());
    }

    @Test
    @DisplayName("Non-repeatable subnode from repeatable parent returns ScalarPath")
    void fromRepeatableNode_nonRepeatableSubNode_shouldReturnScalarPath() {
      PathExpression path = resolver.getRelativePathOfNode("ND-NonRepeatableSubNode", "ND-RepeatableNode");

      assertTrue(path instanceof ScalarPath,
          "Non-repeatable subnode from parent context should return ScalarPath, got: " + path.getClass().getSimpleName());
    }

    @Test
    @DisplayName("Field relative to another field with repeatable ancestor returns SequencePath")
    void fieldToField_withRepeatableAncestor_shouldReturnSequencePath() {
      // BT-21-Number is in ND-NonRepeatableSubNode, whose parent ND-RepeatableNode is repeatable
      // BT-00-Text is in ND-Root
      // Walking up from field: ND-NonRepeatableSubNode (not rep) → ND-RepeatableNode (rep!) → sequence
      PathExpression path = resolver.getRelativePathOfField("BT-21-Number", "BT-00-Text");

      assertTrue(path instanceof SequencePath,
          "Field with repeatable ancestor relative to field in root should return SequencePath, got: " + path.getClass().getSimpleName());
    }

    @Test
    @DisplayName("Field relative to sibling field in same node returns ScalarPath")
    void fieldToField_siblingInSameNode_shouldReturnScalarPath() {
      // Both BT-21-Number and BT-21-TextMultilingual are in ND-NonRepeatableSubNode
      // Context ancestry includes ND-NonRepeatableSubNode, so walk stops immediately
      PathExpression path = resolver.getRelativePathOfField("BT-21-Number", "BT-21-TextMultilingual");

      assertTrue(path instanceof ScalarPath,
          "Field relative to sibling field in same node should return ScalarPath, got: " + path.getClass().getSimpleName());
    }
  }

  // =========================================================================
  // 6. Basic Lookups
  // =========================================================================

  @Nested
  @DisplayName("6. Basic Lookups")
  class BasicLookupTests {

    @Test
    @DisplayName("getTypeOfField returns correct type for text field")
    void getTypeOfField_textField_returnsText() {
      assertEquals("text", resolver.getTypeOfField("BT-12-Text"));
    }

    @Test
    @DisplayName("getTypeOfField returns correct type for number field")
    void getTypeOfField_numberField_returnsNumber() {
      assertEquals("number", resolver.getTypeOfField("BT-21-Number"));
    }

    @Test
    @DisplayName("getParentNodeOfField returns correct parent node")
    void getParentNodeOfField_returnsCorrectParent() {
      assertEquals("ND-SubSubNode2", resolver.getParentNodeOfField("BT-12-Text"));
    }

    @Test
    @DisplayName("isAttributeField returns true for attribute field")
    void isAttributeField_attributeField_returnsTrue() {
      assertTrue(resolver.isAttributeField("BT-00-Attribute"),
          "Attribute field should return true");
    }

    @Test
    @DisplayName("isAttributeField returns false for element field")
    void isAttributeField_elementField_returnsFalse() {
      assertFalse(resolver.isAttributeField("BT-12-Text"),
          "Element field should return false");
    }

    @Test
    @DisplayName("getAttributeNameFromAttributeField returns attribute name")
    void getAttributeNameFromAttributeField_returnsAttributeName() {
      String attrName = resolver.getAttributeNameFromAttributeField("BT-00-Attribute");
      assertNotNull(attrName, "Should return attribute name");
      assertFalse(attrName.isEmpty(), "Attribute name should not be empty");
    }

    @Test
    @DisplayName("expandCodelist returns non-empty list for valid codelist")
    void expandCodelist_validCodelist_returnsNonEmptyList() {
      var codes = resolver.expandCodelist("accessibility");
      assertFalse(codes.isEmpty(), "Codelist should have at least one code");
    }

    @Test
    @DisplayName("getRootCodelistOfField returns root codelist for code field")
    void getRootCodelistOfField_codeField_returnsRootCodelist() {
      String rootCodelist = resolver.getRootCodelistOfField("BT-00-Code");
      assertEquals("main-activity", rootCodelist,
          "authority-activity is a child of main-activity");
    }

    @Test
    @DisplayName("Attribute field without attribute returns StringPath")
    void getAbsolutePathOfFieldWithoutTheAttribute_returnsStringPath() {
      PathExpression path = resolver.getAbsolutePathOfFieldWithoutTheAttribute("BT-00-Attribute");

      assertEquals(StringPath.class, path.getClass(),
          "Attribute field without attribute should return StringPath");
    }

    @Test
    @DisplayName("Code attribute field without attribute returns StringPath")
    void getAbsolutePathOfFieldWithoutTheAttribute_codeAttribute_returnsStringPath() {
      PathExpression path = resolver.getAbsolutePathOfFieldWithoutTheAttribute("BT-00-CodeAttribute");

      assertEquals(StringPath.class, path.getClass(),
          "Code attribute field without attribute should return StringPath");
    }
  }

  // =========================================================================
  // 7. Alias fallback contract
  // =========================================================================

  @Nested
  @DisplayName("7. Alias fallback contract")
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

  // =========================================================================
  // 8. Privacy settings
  // =========================================================================

  @Nested
  @DisplayName("8. Privacy settings")
  class PrivacyTests {

    @Test
    @DisplayName("getPrivacyCodeOfField returns the privacy code")
    void getPrivacyCodeOfField_returnsCode() {
      assertEquals("test-priv", resolver.getPrivacyCodeOfField("BT-00-Text-In-Repeatable-Node"));
    }

    @Test
    @DisplayName("getPrivacySettingOfField returns the companion field of each kind")
    void getPrivacySettingOfField_returnsCompanionFields() {
      final String fieldId = "BT-00-Text-In-Repeatable-Node";

      assertAll(
          () -> assertEquals("BT-195(BT-00)-Text-In-Repeatable-Node",
              resolver.getPrivacySettingOfField(fieldId, PrivacySetting.PRIVACY_CODE_FIELD)),
          () -> assertEquals("BT-198(BT-00)-Text-In-Repeatable-Node",
              resolver.getPrivacySettingOfField(fieldId, PrivacySetting.PUBLICATION_DATE_FIELD)),
          () -> assertEquals("BT-197(BT-00)-Text-In-Repeatable-Node",
              resolver.getPrivacySettingOfField(fieldId, PrivacySetting.JUSTIFICATION_CODE_FIELD)),
          () -> assertEquals("BT-196(BT-00)-Text-In-Repeatable-Node",
              resolver.getPrivacySettingOfField(fieldId,
                  PrivacySetting.JUSTIFICATION_DESCRIPTION_FIELD)));
    }

    @Test
    @DisplayName("getPrivacySettingOfField returns null for a field with no privacy settings")
    void getPrivacySettingOfField_withoutPrivacySettings_returnsNull() {
      assertAll(
          () -> assertNull(resolver.getPrivacySettingOfField("BT-00-Text",
              PrivacySetting.PRIVACY_CODE_FIELD)),
          () -> assertNull(resolver.getPrivacySettingOfField("BT-00-Text",
              PrivacySetting.PUBLICATION_DATE_FIELD)),
          () -> assertNull(resolver.getPrivacySettingOfField("BT-00-Text",
              PrivacySetting.JUSTIFICATION_CODE_FIELD)),
          () -> assertNull(resolver.getPrivacySettingOfField("BT-00-Text",
              PrivacySetting.JUSTIFICATION_DESCRIPTION_FIELD)));
    }

    @Test
    @DisplayName("getPrivacyMask returns the mask of the field's data type")
    void getPrivacyMask_returnsMaskForDataType() {
      assertAll(
          () -> assertEquals("unpublished", resolver.getPrivacyMask("BT-00-Text")),
          () -> assertEquals("unpublished", resolver.getPrivacyMask("BT-00-Code")),
          () -> assertEquals("1970-01-01Z", resolver.getPrivacyMask("BT-00-StartDate")),
          () -> assertEquals("00:00:00Z", resolver.getPrivacyMask("BT-00-StartTime")),
          () -> assertEquals("0", resolver.getPrivacyMask("BT-00-Indicator")),
          () -> assertEquals("-1", resolver.getPrivacyMask("BT-00-Number")),
          () -> assertEquals("-1", resolver.getPrivacyMask("BT-00-Duration")));
    }
  }
}
