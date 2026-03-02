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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import eu.europa.ted.efx.exceptions.SymbolResolutionException;
import eu.europa.ted.efx.exceptions.SymbolResolutionException.ErrorCode;
import eu.europa.ted.efx.interfaces.SymbolResolver;
import eu.europa.ted.efx.mock.sdk2.SymbolResolverMockV2;
import eu.europa.ted.efx.model.expressions.PathExpression;
import eu.europa.ted.efx.model.expressions.scalar.BooleanPath;
import eu.europa.ted.efx.model.expressions.scalar.DatePath;
import eu.europa.ted.efx.model.expressions.scalar.DurationPath;
import eu.europa.ted.efx.model.expressions.scalar.MultilingualStringPath;
import eu.europa.ted.efx.model.expressions.scalar.NodePath;
import eu.europa.ted.efx.model.expressions.scalar.NumericPath;
import eu.europa.ted.efx.model.expressions.scalar.ScalarPath;
import eu.europa.ted.efx.model.expressions.scalar.StringPath;
import eu.europa.ted.efx.model.expressions.scalar.TimePath;
import eu.europa.ted.efx.model.expressions.sequence.SequencePath;

/**
 * Comprehensive tests for SdkSymbolResolver covering:
 * 1. Exception handling for bad inputs
 * 2. Repeatability logic from different contexts
 * 3. PathExpression shape (ScalarPath vs SequencePath)
 * 4. PathExpression data types
 * 5. Context-aware relative paths
 * 6. Basic lookups
 * 7. Alias resolution
 * 8. Root and notice type methods
 */
class SdkSymbolResolverTest {

  private static SymbolResolver resolver;

  @BeforeAll
  static void setup() throws Exception {
    resolver = new SymbolResolverMockV2();
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
      assertFalse(resolver.isFieldRepeatableFromContext("BT-00-Attribute-In-Repeatable-Node", "ND-RepeatableNode"),
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
    @DisplayName("Multilingual field returns MultilingualStringPath")
    void multilingualField_shouldReturnMultilingualStringPath() {
      PathExpression path = resolver.getAbsolutePathOfField("BT-00-Text-Multilingual");
      assertEquals(MultilingualStringPath.class, path.getClass(),
          "Multilingual field should return MultilingualStringPath");
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
      assertNotNull(rootCodelist, "Should return root codelist");
      assertFalse(rootCodelist.isEmpty(), "Root codelist should not be empty");
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
  // 7. Alias Resolution
  // =========================================================================

  @Nested
  @DisplayName("7. Alias Resolution")
  class AliasResolutionTests {

    @Test
    @DisplayName("getFieldIdFromAlias returns field ID for valid alias")
    void getFieldIdFromAlias_validAlias_returnsFieldId() {
      String fieldId = resolver.getFieldIdFromAlias("textField");
      assertEquals("BT-00-Text", fieldId, "Should return field ID for alias 'textField'");
    }

    @Test
    @DisplayName("getNodeIdFromAlias returns node ID for valid alias")
    void getNodeIdFromAlias_validAlias_returnsNodeId() {
      String nodeId = resolver.getNodeIdFromAlias("Root");
      assertEquals("ND-Root", nodeId, "Should return node ID for alias 'Root'");
    }

    @Test
    @DisplayName("getNodeIdFromAlias returns null for unknown alias")
    void getNodeIdFromAlias_unknownAlias_returnsNull() {
      assertNull(resolver.getNodeIdFromAlias("nonexistent-alias"),
          "Unknown alias should return null");
    }

    @Test
    @DisplayName("getFieldIdFromAlias returns null for unknown alias")
    void getFieldIdFromAlias_unknownAlias_returnsNull() {
      assertNull(resolver.getFieldIdFromAlias("nonexistent-alias"),
          "Unknown alias should return null to enable fallback");
    }
  }

  // =========================================================================
  // 8. Root and Notice Type Methods
  // =========================================================================

  @Nested
  @DisplayName("8. Root and Notice Type Methods")
  class RootAndNoticeTypeTests {

    @Test
    @DisplayName("getRootNodeId returns root node ID")
    void getRootNodeId_returnsRootNodeId() {
      String rootNodeId = resolver.getRootNodeId();
      assertEquals("ND-Root", rootNodeId, "Should return ND-Root");
    }

    @Test
    @DisplayName("getRootPath returns NodePath to root")
    void getRootPath_returnsNodePathToRoot() {
      PathExpression rootPath = resolver.getRootPath();

      assertNotNull(rootPath, "Should return root path");
      assertEquals(NodePath.class, rootPath.getClass(),
          "Root path should be NodePath");
    }

    @Test
    @DisplayName("getAllNoticeSubtypes returns notice types")
    void getAllNoticeSubtypes_returnsNoticeTypes() {
      var noticeTypes = resolver.getAllNoticeSubtypes();

      assertNotNull(noticeTypes, "Should return list of notice types");
      assertFalse(noticeTypes.isEmpty(), "Notice types list should not be empty");
    }
  }
}
