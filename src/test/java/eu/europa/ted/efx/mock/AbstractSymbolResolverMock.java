package eu.europa.ted.efx.mock;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europa.ted.eforms.sdk.entity.SdkCodelist;
import eu.europa.ted.eforms.sdk.entity.SdkField;
import eu.europa.ted.eforms.sdk.entity.SdkNode;
import eu.europa.ted.efx.interfaces.SymbolResolver;
import eu.europa.ted.efx.model.expressions.path.NodePathExpression;
import eu.europa.ted.efx.model.expressions.path.PathExpression;
import eu.europa.ted.efx.model.types.FieldTypes;
import eu.europa.ted.efx.xpath.XPathContextualizer;

public abstract class AbstractSymbolResolverMock<F extends SdkField, N extends SdkNode, C extends SdkCodelist>
    implements SymbolResolver {
  protected final Logger logger = LoggerFactory.getLogger(getClass());

  private static final Path TEST_JSON_DIR = Path.of("src", "test", "resources", "json");

  private static final ObjectMapper mapper = new ObjectMapper();

  protected Map<String, F> fieldById;
  protected Map<String, N> nodeById;
  protected Map<String, C> codelistById;

  public AbstractSymbolResolverMock() throws IOException {
    this.loadMapData();
  }

  protected abstract Map<String, N> createNodeById();

  protected abstract Map<String, C> createCodelistById();

  protected abstract Class<F> getSdkFieldClass();

  protected abstract String getFieldsJsonFilename();

  protected void loadMapData() throws IOException {
    this.fieldById = createFieldById();
    this.nodeById = createNodeById();
    this.codelistById = createCodelistById();
  }

  protected Map<String, F> createFieldById() throws IOException {
    Path fieldsJsonPath = Path.of(TEST_JSON_DIR.toString(), getFieldsJsonFilename());

    if (!Files.isRegularFile(fieldsJsonPath)) {
      throw new FileNotFoundException(fieldsJsonPath.toString());
    }

    List<F> fields = mapper
        .readerForListOf(getSdkFieldClass())
        .readValue(fieldsJsonPath.toFile());

    return fields.stream().collect(Collectors.toMap(SdkField::getId, Function.identity()));
  }

  protected F getFieldById(String fieldId) {
    return this.fieldById.get(fieldId);
  }

  protected N getNodeById(String nodeId) {
    return this.nodeById.get(nodeId);
  }

  protected C getCodelistById(String codelistId) {
    return this.codelistById.get(codelistId);
  }

  @Override
  public PathExpression getRelativePathOfField(String fieldId, PathExpression contextPath) {
    return XPathContextualizer.contextualize(contextPath, getAbsolutePathOfField(fieldId));
  }

  @Override
  public PathExpression getRelativePathOfNode(String nodeId, PathExpression contextPath) {
    return XPathContextualizer.contextualize(contextPath, getAbsolutePathOfNode(nodeId));
  }

  @Override
  public PathExpression getRelativePath(PathExpression absolutePath, PathExpression contextPath) {
    return XPathContextualizer.contextualize(contextPath, absolutePath);
  }

  @Override
  public String getTypeOfField(String fieldId) {
    final SdkField sdkField = getFieldById(fieldId);
    if (sdkField == null) {
      throw new ParseCancellationException(String.format("Unknown field '%s'.", fieldId));
    }
    return sdkField.getType();
  }

  @Override
  public String getRootCodelistOfField(final String fieldId) {
    final SdkField sdkField = getFieldById(fieldId);
    if (sdkField == null) {
      throw new ParseCancellationException(String.format("Unknown field '%s'.", fieldId));
    }
    final String codelistId = sdkField.getCodelistId();
    if (codelistId == null) {
      throw new ParseCancellationException(String.format("No codelist for field '%s'.", fieldId));
    }

    final SdkCodelist sdkCodelist = getCodelistById(codelistId);
    if (sdkCodelist == null) {
      throw new ParseCancellationException(String.format("Unknown codelist '%s'.", codelistId));
    }

    return sdkCodelist.getRootCodelistId();
  }

  @Override
  public List<String> expandCodelist(String codelistId) {
    SdkCodelist codelist = getCodelistById(codelistId);
    if (codelist == null) {
      throw new ParseCancellationException(String.format("Codelist '%s' not found.", codelistId));
    }
    return codelist.getCodes();
  }

  /**
   * Gets the id of the parent node of a given field.
   *
   * @param fieldId The id of the field who's parent node we are looking for.
   * @return The id of the parent node of the given field.
   */
  @Override
  public String getParentNodeOfField(final String fieldId) {
    final SdkField sdkField = getFieldById(fieldId);
    if (sdkField != null) {
      return sdkField.getParentNodeId();
    }
    throw new ParseCancellationException(String.format("Unknown field '%s'", fieldId));
  }

  /**
   * @param fieldId The id of a field.
   * @return The xPath of the given field.
   */
  @Override
  public PathExpression getAbsolutePathOfField(final String fieldId) {
    final SdkField sdkField = getFieldById(fieldId);
    if (sdkField == null) {
      throw new ParseCancellationException(
          String.format("Unknown field identifier '%s'.", fieldId));
    }
    return PathExpression.instantiate(sdkField.getXpathAbsolute(), FieldTypes.fromString(sdkField.getType()));
  }

  /**
   * @param nodeId The id of a node or a field.
   * @return The xPath of the given node or field.
   */
  @Override
  public PathExpression getAbsolutePathOfNode(final String nodeId) {
    final SdkNode sdkNode = getNodeById(nodeId);

    if (sdkNode == null) {
      throw new ParseCancellationException(String.format("Unknown node identifier '%s'.", nodeId));
    }

    return new NodePathExpression(sdkNode.getXpathAbsolute());
  }
}
