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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import eu.europa.ted.eforms.sdk.entity.SdkNoticeSubtype;
import eu.europa.ted.efx.exceptions.SymbolResolutionException;
import eu.europa.ted.efx.exceptions.SymbolResolutionException.ErrorCode;
import eu.europa.ted.efx.interfaces.SymbolResolver;
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
 * Shared behavioral contract for resolvers using the SDK-2 fixture.
 * Each concrete test supplies independently loaded entities. A fresh resolver per test keeps
 * eager parent wiring and cached ancestry from masking lazy-loading defects in another test.
 */
public abstract class SymbolResolverContractTest {

  protected SymbolResolver resolver;

  protected abstract SymbolResolver createResolver() throws Exception;

  @BeforeEach
  protected void initializeResolver() throws Exception {
    this.resolver = createResolver();
  }

  // Exception handling

  @Test
  @DisplayName("getAbsolutePathOfField throws UNKNOWN_SYMBOL for nonexistent field")
  protected void getAbsolutePathOfField_unknownField_throwsException() {
    SymbolResolutionException ex = assertThrows(
        SymbolResolutionException.class,
        () -> resolver.getAbsolutePathOfField("BT-NONEXISTENT"));

    assertEquals(ErrorCode.UNKNOWN_SYMBOL, ex.getErrorCode());
  }

  @Test
  @DisplayName("getAbsolutePathOfNode throws UNKNOWN_SYMBOL for nonexistent node")
  protected void getAbsolutePathOfNode_unknownNode_throwsException() {
    SymbolResolutionException ex = assertThrows(
        SymbolResolutionException.class,
        () -> resolver.getAbsolutePathOfNode("ND-NONEXISTENT"));

    assertEquals(ErrorCode.UNKNOWN_SYMBOL, ex.getErrorCode());
  }

  @Test
  @DisplayName("expandCodelist throws UNKNOWN_CODELIST for nonexistent codelist")
  protected void expandCodelist_unknownCodelist_throwsException() {
    SymbolResolutionException ex = assertThrows(
        SymbolResolutionException.class,
        () -> resolver.expandCodelist("nonexistent-codelist"));

    assertEquals(ErrorCode.UNKNOWN_CODELIST, ex.getErrorCode());
  }

  @Test
  @DisplayName("getRootCodelistOfField throws NO_CODELIST_FOR_FIELD for non-code field")
  protected void getRootCodelistOfField_nonCodeField_throwsException() {
    SymbolResolutionException ex = assertThrows(
        SymbolResolutionException.class,
        () -> resolver.getRootCodelistOfField("BT-12-Text"));

    assertEquals(ErrorCode.NO_CODELIST_FOR_FIELD, ex.getErrorCode());
  }

  // Repeatability

  @Test
  @DisplayName("Scalar field from root context returns false")
  protected void isFieldRepeatableFromContext_scalarFieldFromRoot_returnsFalse() {
    assertFalse(resolver.isFieldRepeatableFromContext("BT-12-Text", null),
        "Field with no repeatable ancestors should return false from root");
  }

  @Test
  @DisplayName("Field in self-repeatable node from root returns true")
  protected void isFieldRepeatableFromContext_fieldInRepNode_fromRoot_returnsTrue() {
    assertTrue(resolver.isFieldRepeatableFromContext("BT-13-Text", null),
        "Field in repeatable node should return true from root");
  }

  @Test
  @DisplayName("Field under repeatable ancestor from root returns true")
  protected void isFieldRepeatableFromContext_fieldUnderRepAncestor_fromRoot_returnsTrue() {
    assertTrue(resolver.isFieldRepeatableFromContext("BT-21-Number", null),
        "Field under repeatable ancestor should return true from root");
  }

  @Test
  @DisplayName("Field under repeatable ancestor from that ancestor returns false")
  protected void isFieldRepeatableFromContext_fieldUnderRepAncestor_fromRepAncestor_returnsFalse() {
    assertFalse(resolver.isFieldRepeatableFromContext("BT-21-Number", "ND-RepeatableNode"),
        "Walk should stop at context node, returning false");
  }

  @Test
  @DisplayName("Deeply nested field with inner repeatable node from outer context returns true")
  protected void isFieldRepeatableFromContext_deeplyNested_fromOuterRepContext_returnsTrue() {
    assertTrue(resolver.isFieldRepeatableFromContext("BT-25-Date", "ND-RepeatableNode"),
        "Should still return true because inner repeatable node is crossed");
  }

  @Test
  @DisplayName("Scalar field from cross-branch repeatable context stays scalar")
  protected void isFieldRepeatableFromContext_scalarField_fromCrossBranchRepContext_returnsFalse() {
    assertFalse(resolver.isFieldRepeatableFromContext("BT-12-Text", "ND-RepeatableNode"),
        "Scalar field should remain scalar even from rep context on different branch");
  }

  @Test
  @DisplayName("Sequence field from cross-branch non-repeatable context stays sequence")
  protected void isFieldRepeatableFromContext_sequenceField_fromCrossBranchNonRepContext_returnsTrue() {
    assertTrue(resolver.isFieldRepeatableFromContext("BT-21-Number", "ND-SubNode"),
        "Sequence field should remain sequence from non-rep context on different branch");
  }

  @Test
  @DisplayName("Attribute field in repeatable node from root returns true")
  protected void isFieldRepeatableFromContext_attributeInRepNode_fromRoot_returnsTrue() {
    assertTrue(resolver.isFieldRepeatableFromContext("BT-00-Attribute-In-Repeatable-Node", null),
        "Attribute field in repeatable node should return true from root");
  }

  @Test
  @DisplayName("Attribute field in repeatable node from that node returns false")
  protected void isFieldRepeatableFromContext_attributeInRepNode_fromRepNode_returnsFalse() {
    assertFalse(
        resolver.isFieldRepeatableFromContext("BT-00-Attribute-In-Repeatable-Node",
            "ND-RepeatableNode"),
        "Attribute field should not be repeatable when context is its parent repeatable node");
  }

  @Test
  @DisplayName("Repeatable field from root returns true")
  protected void isFieldRepeatableFromContext_repeatableField_fromRoot_returnsTrue() {
    assertTrue(resolver.isFieldRepeatableFromContext("BT-00-Repeatable-Text", null),
        "Repeatable field should return true from root context");
  }

  @Test
  @DisplayName("Repeatable node from root returns true")
  protected void isNodeRepeatableFromContext_repeatableNode_fromRoot_returnsTrue() {
    assertTrue(resolver.isNodeRepeatableFromContext("ND-RepeatableNode", null),
        "Repeatable node should return true from root context");
  }

  // Path cardinality

  @Test
  @DisplayName("Non-repeatable field returns ScalarPath")
  protected void nonRepeatableField_shouldReturnScalarPath() {
    assertFalse(resolver.isFieldRepeatableFromContext("BT-00-Text", null),
        "Precondition: field should NOT be repeatable from root");

    PathExpression path = resolver.getAbsolutePathOfField("BT-00-Text");

    assertTrue(path instanceof ScalarPath,
        "Non-repeatable field should return ScalarPath, got: " + path.getClass().getSimpleName());
  }

  @Test
  @DisplayName("Repeatable field returns SequencePath")
  protected void repeatableField_shouldReturnSequencePath() {
    assertTrue(resolver.isFieldRepeatableFromContext("BT-00-Repeatable-Text", null),
        "Precondition: field should be repeatable from root");

    PathExpression path = resolver.getAbsolutePathOfField("BT-00-Repeatable-Text");

    assertTrue(path instanceof SequencePath,
        "Repeatable field should return SequencePath, got: " + path.getClass().getSimpleName());
  }

  @Test
  @DisplayName("Field in repeatable node returns SequencePath")
  protected void fieldInRepeatableNode_shouldReturnSequencePath() {
    assertTrue(resolver.isFieldRepeatableFromContext("BT-00-Text-In-Repeatable-Node", null),
        "Precondition: field in repeatable node should be repeatable from root");

    PathExpression path = resolver.getAbsolutePathOfField("BT-00-Text-In-Repeatable-Node");

    assertTrue(path instanceof SequencePath,
        "Field in repeatable node should return SequencePath, got: " + path.getClass().getSimpleName());
  }

  @Test
  @DisplayName("Field in non-repeatable subnode of repeatable node returns SequencePath")
  protected void fieldInNonRepeatableSubNode_shouldReturnSequencePath() {
    assertTrue(resolver.isFieldRepeatableFromContext("BT-00-Text-In-NonRepeatableSubNode", null),
        "Precondition: field under repeatable ancestor should be repeatable from root");

    PathExpression path = resolver.getAbsolutePathOfField("BT-00-Text-In-NonRepeatableSubNode");

    assertTrue(path instanceof SequencePath,
        "Field under repeatable ancestor should return SequencePath, got: " + path.getClass().getSimpleName());
  }

  @Test
  @DisplayName("Field in double-repeatable context returns SequencePath")
  protected void fieldInDoubleRepeatableContext_shouldReturnSequencePath() {
    assertTrue(resolver.isFieldRepeatableFromContext("BT-00-Text-In-RepeatableSubSubNode", null),
        "Precondition: field in double-repeatable context should be repeatable from root");

    PathExpression path = resolver.getAbsolutePathOfField("BT-00-Text-In-RepeatableSubSubNode");

    assertTrue(path instanceof SequencePath,
        "Field in double-repeatable context should return SequencePath, got: " + path.getClass().getSimpleName());
  }

  @Test
  @DisplayName("Non-repeatable root node returns NodePath (scalar)")
  protected void nonRepeatableRootNode_shouldReturnScalarPath() {
    PathExpression path = resolver.getAbsolutePathOfNode("ND-Root");

    assertTrue(path instanceof ScalarPath,
        "Non-repeatable node should return ScalarPath, got: " + path.getClass().getSimpleName());
    assertEquals(NodePath.class, path.getClass(),
        "Non-repeatable node should return NodePath");
  }

  @Test
  @DisplayName("Repeatable node returns SequencePath")
  protected void repeatableNode_shouldReturnSequencePath() {
    PathExpression path = resolver.getAbsolutePathOfNode("ND-RepeatableNode");

    assertTrue(path instanceof SequencePath,
        "Repeatable node should return SequencePath, got: " + path.getClass().getSimpleName());
  }

  @Test
  @DisplayName("Non-repeatable subnode under repeatable parent returns SequencePath")
  protected void nonRepeatableSubNode_underRepeatableParent_shouldReturnSequencePath() {
    assertTrue(resolver.isNodeRepeatableFromContext("ND-NonRepeatableSubNode", null),
        "Precondition: node under repeatable ancestor should be repeatable from root");

    PathExpression path = resolver.getAbsolutePathOfNode("ND-NonRepeatableSubNode");

    assertTrue(path instanceof SequencePath,
        "Node under repeatable ancestor should return SequencePath, got: " + path.getClass().getSimpleName());
  }

  @Test
  @DisplayName("Repeatable subsubnode returns SequencePath")
  protected void repeatableSubSubNode_shouldReturnSequencePath() {
    PathExpression path = resolver.getAbsolutePathOfNode("ND-RepeatableSubSubNode");

    assertTrue(path instanceof SequencePath,
        "Repeatable subsubnode should return SequencePath, got: " + path.getClass().getSimpleName());
  }

  @Test
  @DisplayName("Non-repeatable attribute field without attribute returns ScalarPath")
  protected void nonRepeatableAttributeField_withoutAttribute_shouldReturnScalarPath() {
    assertFalse(resolver.isFieldRepeatableFromContext("BT-00-Attribute", null),
        "Precondition: attribute field should NOT be repeatable from root");

    PathExpression path = resolver.getAbsolutePathOfFieldWithoutTheAttribute("BT-00-Attribute");

    assertTrue(path instanceof ScalarPath,
        "Non-repeatable attribute field without @attribute should return ScalarPath, got: " + path.getClass().getSimpleName());
  }

  @Test
  @DisplayName("Attribute field in repeatable node without attribute returns SequencePath")
  protected void attributeInRepeatableNode_withoutAttribute_shouldReturnSequencePath() {
    assertTrue(resolver.isFieldRepeatableFromContext("BT-00-Attribute-In-Repeatable-Node", null),
        "Precondition: attribute field in repeatable node should be repeatable from root");

    PathExpression path = resolver.getAbsolutePathOfFieldWithoutTheAttribute("BT-00-Attribute-In-Repeatable-Node");

    assertTrue(path instanceof SequencePath,
        "Attribute field in repeatable node without @attribute should return SequencePath, got: " + path.getClass().getSimpleName());
  }

  // Path data types

  @Test
  @DisplayName("Text field returns StringPath")
  protected void textField_shouldReturnStringPath() {
    PathExpression path = resolver.getAbsolutePathOfField("BT-00-Text");
    assertEquals(StringPath.class, path.getClass(),
        "Text field should return StringPath");
  }

  @Test
  @DisplayName("Indicator field returns BooleanPath")
  protected void indicatorField_shouldReturnBooleanPath() {
    PathExpression path = resolver.getAbsolutePathOfField("BT-00-Indicator");
    assertEquals(BooleanPath.class, path.getClass(),
        "Indicator field should return BooleanPath");
  }

  @Test
  @DisplayName("Code field returns StringPath")
  protected void codeField_shouldReturnStringPath() {
    PathExpression path = resolver.getAbsolutePathOfField("BT-00-Code");
    assertEquals(StringPath.class, path.getClass(),
        "Code field should return StringPath");
  }

  @Test
  @DisplayName("Multilingual field returns MultilingualStringSequencePath")
  protected void multilingualField_shouldReturnMultilingualStringSequencePath() {
    PathExpression path = resolver.getAbsolutePathOfField("BT-00-Text-Multilingual");
    assertEquals(MultilingualStringSequencePath.class, path.getClass(),
        "Multilingual field should return MultilingualStringSequencePath");
  }

  @Test
  @DisplayName("Date field returns DatePath")
  protected void dateField_shouldReturnDatePath() {
    PathExpression path = resolver.getAbsolutePathOfField("BT-00-StartDate");
    assertEquals(DatePath.class, path.getClass(),
        "Date field should return DatePath");
  }

  @Test
  @DisplayName("Time field returns TimePath")
  protected void timeField_shouldReturnTimePath() {
    PathExpression path = resolver.getAbsolutePathOfField("BT-00-StartTime");
    assertEquals(TimePath.class, path.getClass(),
        "Time field should return TimePath");
  }

  @Test
  @DisplayName("Duration field returns DurationPath")
  protected void durationField_shouldReturnDurationPath() {
    PathExpression path = resolver.getAbsolutePathOfField("BT-00-Duration");
    assertEquals(DurationPath.class, path.getClass(),
        "Duration field should return DurationPath");
  }

  @Test
  @DisplayName("Integer field returns NumericPath")
  protected void integerField_shouldReturnNumericPath() {
    PathExpression path = resolver.getAbsolutePathOfField("BT-00-Integer");
    assertEquals(NumericPath.class, path.getClass(),
        "Integer field should return NumericPath");
  }

  @Test
  @DisplayName("Amount field returns NumericPath")
  protected void amountField_shouldReturnNumericPath() {
    PathExpression path = resolver.getAbsolutePathOfField("BT-00-Amount");
    assertEquals(NumericPath.class, path.getClass(),
        "Amount field should return NumericPath");
  }

  @Test
  @DisplayName("Number field returns NumericPath")
  protected void numberField_shouldReturnNumericPath() {
    PathExpression path = resolver.getAbsolutePathOfField("BT-00-Number");
    assertEquals(NumericPath.class, path.getClass(),
        "Number field should return NumericPath");
  }

  // Relative paths

  @Test
  @DisplayName("Field in repeatable node from root returns SequencePath")
  protected void fromRoot_repeatableField_shouldReturnSequencePath() {
    assertTrue(resolver.isFieldRepeatableFromContext("BT-00-Text-In-Repeatable-Node", null),
        "Precondition: field should be repeatable from root");

    PathExpression path = resolver.getRelativePathOfField("BT-00-Text-In-Repeatable-Node", "ND-Root");

    assertTrue(path instanceof SequencePath,
        "Field in repeatable node from root should return SequencePath, got: " + path.getClass().getSimpleName());
    assertEquals("RepeatableNode/TextField", path.getScript());
  }

  @Test
  @DisplayName("Field in repeatable node from same node context returns ScalarPath")
  protected void fromRepeatableNode_sameField_shouldReturnScalarPath() {
    assertFalse(resolver.isFieldRepeatableFromContext("BT-00-Text-In-Repeatable-Node", "ND-RepeatableNode"),
        "Precondition: field should NOT be repeatable from its own parent node context");

    PathExpression path = resolver.getRelativePathOfField("BT-00-Text-In-Repeatable-Node", "ND-RepeatableNode");

    assertTrue(path instanceof ScalarPath,
        "Field from its parent repeatable node context should return ScalarPath, got: " + path.getClass().getSimpleName());
    assertEquals("TextField", path.getScript());
  }

  @Test
  @DisplayName("Non-repeatable field from root returns ScalarPath")
  protected void fromRoot_nonRepeatableField_shouldReturnScalarPath() {
    assertFalse(resolver.isFieldRepeatableFromContext("BT-00-Text", null),
        "Precondition: field should NOT be repeatable");

    PathExpression path = resolver.getRelativePathOfField("BT-00-Text", "ND-Root");

    assertTrue(path instanceof ScalarPath,
        "Non-repeatable field should return ScalarPath, got: " + path.getClass().getSimpleName());
    assertEquals("PathNode/TextField", path.getScript());
  }

  @Test
  @DisplayName("Repeatable node from root returns SequencePath")
  protected void fromRoot_repeatableNode_shouldReturnSequencePath() {
    PathExpression path = resolver.getRelativePathOfNode("ND-RepeatableNode", "ND-Root");

    assertTrue(path instanceof SequencePath,
        "Repeatable node from root should return SequencePath, got: " + path.getClass().getSimpleName());
    assertEquals("RepeatableNode", path.getScript());
  }

  @Test
  @DisplayName("Non-repeatable subnode from repeatable parent returns ScalarPath")
  protected void fromRepeatableNode_nonRepeatableSubNode_shouldReturnScalarPath() {
    PathExpression path = resolver.getRelativePathOfNode("ND-NonRepeatableSubNode", "ND-RepeatableNode");

    assertTrue(path instanceof ScalarPath,
        "Non-repeatable subnode from parent context should return ScalarPath, got: " + path.getClass().getSimpleName());
    assertEquals("NonRepeatableSubNode", path.getScript());
  }

  @Test
  @DisplayName("Field relative to another field with repeatable ancestor returns SequencePath")
  protected void fieldToField_withRepeatableAncestor_shouldReturnSequencePath() {
    // BT-21-Number is in ND-NonRepeatableSubNode, whose parent ND-RepeatableNode is repeatable
    // BT-00-Text is in ND-Root
    // Walking up from field: ND-NonRepeatableSubNode (not rep) → ND-RepeatableNode (rep!) → sequence
    PathExpression path = resolver.getRelativePathOfField("BT-21-Number", "BT-00-Text");

    assertTrue(path instanceof SequencePath,
        "Field with repeatable ancestor relative to field in root should return SequencePath, got: " + path.getClass().getSimpleName());
    assertEquals("../../RepeatableNode/NonRepeatableSubNode/Number", path.getScript());
  }

  @Test
  @DisplayName("Field relative to sibling field in same node returns ScalarPath")
  protected void fieldToField_siblingInSameNode_shouldReturnScalarPath() {
    // Both BT-21-Number and BT-21-TextMultilingual are in ND-NonRepeatableSubNode
    // Context ancestry includes ND-NonRepeatableSubNode, so walk stops immediately
    PathExpression path = resolver.getRelativePathOfField("BT-21-Number", "BT-21-TextMultilingual");

    assertTrue(path instanceof ScalarPath,
        "Field relative to sibling field in same node should return ScalarPath, got: " + path.getClass().getSimpleName());
    assertEquals("../Number", path.getScript());
  }

  // Basic lookups

  @Test
  @DisplayName("getTypeOfField returns correct type for text field")
  protected void getTypeOfField_textField_returnsText() {
    assertEquals("text", resolver.getTypeOfField("BT-12-Text"));
  }

  @Test
  @DisplayName("getTypeOfField returns correct type for number field")
  protected void getTypeOfField_numberField_returnsNumber() {
    assertEquals("number", resolver.getTypeOfField("BT-21-Number"));
  }

  @Test
  @DisplayName("getParentNodeOfField returns correct parent node")
  protected void getParentNodeOfField_returnsCorrectParent() {
    assertEquals("ND-SubSubNode2", resolver.getParentNodeOfField("BT-12-Text"));
  }

  @Test
  @DisplayName("isAttributeField returns true for attribute field")
  protected void isAttributeField_attributeField_returnsTrue() {
    assertTrue(resolver.isAttributeField("BT-00-Attribute"),
        "Attribute field should return true");
  }

  @Test
  @DisplayName("isAttributeField returns false for element field")
  protected void isAttributeField_elementField_returnsFalse() {
    assertFalse(resolver.isAttributeField("BT-12-Text"),
        "Element field should return false");
  }

  @Test
  @DisplayName("getAttributeNameFromAttributeField returns attribute name")
  protected void getAttributeNameFromAttributeField_returnsAttributeName() {
    String attrName = resolver.getAttributeNameFromAttributeField("BT-00-Attribute");
    assertEquals("Attribute", attrName);
  }

  @Test
  @DisplayName("expandCodelist returns non-empty list for valid codelist")
  protected void expandCodelist_validCodelist_returnsNonEmptyList() {
    var codes = resolver.expandCodelist("accessibility");
    assertEquals(List.of("code1", "code2", "code3"), codes);
  }

  @Test
  @DisplayName("getRootCodelistOfField returns root codelist for code field")
  protected void getRootCodelistOfField_codeField_returnsRootCodelist() {
    String rootCodelist = resolver.getRootCodelistOfField("BT-00-Code");
    assertEquals("main-activity", rootCodelist,
        "authority-activity is a child of main-activity");
  }

  @Test
  @DisplayName("Attribute field without attribute returns StringPath")
  protected void getAbsolutePathOfFieldWithoutTheAttribute_returnsStringPath() {
    PathExpression path = resolver.getAbsolutePathOfFieldWithoutTheAttribute("BT-00-Attribute");

    assertEquals(StringPath.class, path.getClass(),
        "Attribute field without attribute should return StringPath");
    assertEquals("/*/PathNode/TextField", path.getScript());
  }

  @Test
  @DisplayName("Code attribute field without attribute returns StringPath")
  protected void getAbsolutePathOfFieldWithoutTheAttribute_codeAttribute_returnsStringPath() {
    PathExpression path = resolver.getAbsolutePathOfFieldWithoutTheAttribute("BT-00-CodeAttribute");

    assertEquals(StringPath.class, path.getClass(),
        "Code attribute field without attribute should return StringPath");
    assertEquals("/*/PathNode/CodeField", path.getScript());
  }

  // Alias resolution

  @Test
  @DisplayName("getFieldIdFromAlias returns field ID for valid alias")
  protected void getFieldIdFromAlias_validAlias_returnsFieldId() {
    String fieldId = resolver.getFieldIdFromAlias("textField");
    assertEquals("BT-00-Text", fieldId, "Should return field ID for alias 'textField'");
    assertEquals("text", resolver.getTypeOfField("textField"));
    assertEquals("/*/PathNode/TextField", resolver.getAbsolutePathOfField("textField").getScript());
  }

  @Test
  @DisplayName("getNodeIdFromAlias returns node ID for valid alias")
  protected void getNodeIdFromAlias_validAlias_returnsNodeId() {
    String nodeId = resolver.getNodeIdFromAlias("Root");
    assertEquals("ND-Root", nodeId, "Should return node ID for alias 'Root'");
    assertEquals("/*", resolver.getAbsolutePathOfNode("Root").getScript());
  }

  @Test
  @DisplayName("getNodeIdFromAlias returns null for unknown alias")
  protected void getNodeIdFromAlias_unknownAlias_returnsNull() {
    assertNull(resolver.getNodeIdFromAlias("nonexistent-alias"),
        "Unknown alias should return null");
  }

  @Test
  @DisplayName("getFieldIdFromAlias returns null for unknown alias")
  protected void getFieldIdFromAlias_unknownAlias_returnsNull() {
    assertNull(resolver.getFieldIdFromAlias("nonexistent-alias"),
        "Unknown alias should return null to enable fallback");
  }

  // Root and notice subtypes

  @Test
  @DisplayName("getRootNodeId returns root node ID")
  protected void getRootNodeId_returnsRootNodeId() {
    String rootNodeId = resolver.getRootNodeId();
    assertEquals("ND-Root", rootNodeId, "Should return ND-Root");
  }

  @Test
  @DisplayName("getRootPath returns NodePath to root")
  protected void getRootPath_returnsNodePathToRoot() {
    PathExpression rootPath = resolver.getRootPath();

    assertNotNull(rootPath, "Should return root path");
    assertEquals(NodePath.class, rootPath.getClass(),
        "Root path should be NodePath");
    assertEquals("/*", rootPath.getScript());
  }

  @Test
  @DisplayName("getAllNoticeSubtypes returns notice types")
  protected void getAllNoticeSubtypes_returnsNoticeTypes() {
    var noticeTypes = resolver.getAllNoticeSubtypes();

    assertNotNull(noticeTypes, "Should return list of notice types");
    assertEquals(List.of("1", "10", "11", "2", "3", "4", "5", "9", "E1", "E2", "X01"),
        noticeTypes.stream().map(SdkNoticeSubtype::getId).sorted().collect(Collectors.toList()));
  }

  // Privacy settings

  @Test
  @DisplayName("getPrivacyCodeOfField returns the privacy code")
  protected void getPrivacyCodeOfField_returnsCode() {
    assertEquals("test-priv", resolver.getPrivacyCodeOfField("BT-00-Text-In-Repeatable-Node"));
  }

  @Test
  @DisplayName("getPrivacySettingOfField returns the companion field of each kind")
  protected void getPrivacySettingOfField_returnsCompanionFields() {
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
  protected void getPrivacySettingOfField_withoutPrivacySettings_returnsNull() {
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
  protected void getPrivacyMask_returnsMaskForDataType() {
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
