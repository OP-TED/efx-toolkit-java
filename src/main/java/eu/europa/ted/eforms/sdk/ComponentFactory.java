package eu.europa.ted.eforms.sdk;

import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import eu.europa.ted.eforms.sdk.component.SdkComponentFactory;
import eu.europa.ted.eforms.sdk.component.SdkComponentType;
import eu.europa.ted.efx.interfaces.MarkupGenerator;
import eu.europa.ted.efx.interfaces.ScriptGenerator;
import eu.europa.ted.efx.interfaces.SymbolResolver;
import eu.europa.ted.efx.interfaces.TranslatorOptions;

public class ComponentFactory extends SdkComponentFactory {
  public static final ComponentFactory INSTANCE = new ComponentFactory();

  private ComponentFactory() {
    super();
  }

  class VersionQualifier {
    private final String sdkVersion;
    private final String qualifier;

    VersionQualifier(String sdkVersion, String qualifier) {
      this.sdkVersion = sdkVersion;
      this.qualifier = qualifier;
    }

    @Override
    public int hashCode() {
      return Objects.hash(sdkVersion, qualifier);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      VersionQualifier other = (VersionQualifier) obj;
      if (!getEnclosingInstance().equals(other.getEnclosingInstance()))
        return false;
      return Objects.equals(sdkVersion, other.sdkVersion)
          && Objects.equals(qualifier, other.qualifier);
    }

    private ComponentFactory getEnclosingInstance() {
      return ComponentFactory.this;
    }
  }

  /**
   * Symbol resolver is a "kind-of" singleton. One instance per version of the
   * eForms SDK and per qualifier.
   */
  private static final Map<VersionQualifier, SymbolResolver> instances = new HashMap<>();

  /**
   * Gets the single instance containing the symbols defined in the given version of the eForms SDK.
   *
   * @param sdkVersion Version of the SDK
   * @param sdkRootPath Path to the root of the SDK
   * @return The single instance containing the symbols defined in the given version of the eForms
   *        SDK.
   * @throws InstantiationException If the SDK version is not supported.
   */
  public static SymbolResolver getSymbolResolver(final String sdkVersion, final Path sdkRootPath)
      throws InstantiationException {
    return getSymbolResolver(sdkVersion, "", sdkRootPath);
  }

  /**
   * Gets the single instance containing the symbols defined in the given version of the eForms SDK.
   *
   * @param sdkVersion Version of the SDK
   * @param qualifier Qualifier to choose between several implementations
   * @param sdkRootPath Path to the root of the SDK
   * @return The single instance containing the symbols defined in the given version of the eForms
   *        SDK.
   * @throws InstantiationException If the SDK version is not supported.
   */
  public static SymbolResolver getSymbolResolver(final String sdkVersion, final String qualifier,
      final Path sdkRootPath) throws InstantiationException {
    return getSymbolResolver(sdkVersion, qualifier, sdkRootPath);
  }

  /**
   * Gets the single instance containing the symbols defined in the given version of the eForms SDK.
   *
   * @param sdkVersion Version of the SDK
   * @param qualifier Qualifier to choose between several implementations
   * @param parameters Array of objects to be passed as arguments to the constructor of the
   * SymbolResolver implementation
   * @return The single instance containing the symbols defined in the given version of the eForms
   *        SDK.
   * @throws InstantiationException If the SDK version is not supported.
   */
  public static SymbolResolver getSymbolResolver(final String sdkVersion, final String qualifier,
      Object... parameters) throws InstantiationException {

    VersionQualifier key = ComponentFactory.INSTANCE.new VersionQualifier(sdkVersion, qualifier);

    return instances.computeIfAbsent(key, k -> {
      try {
        return ComponentFactory.INSTANCE.getComponentImpl(sdkVersion,
            SdkComponentType.SYMBOL_RESOLVER, qualifier, SymbolResolver.class, sdkVersion,
            parameters);
      } catch (InstantiationException e) {
        throw new RuntimeException(MessageFormat.format(
            "Failed to instantiate SDK Symbol Resolver for SDK version [{0}]", sdkVersion), e);
      }
    });
  }

  public static MarkupGenerator getMarkupGenerator(final String sdkVersion, TranslatorOptions options)
      throws InstantiationException {
    return getMarkupGenerator(sdkVersion, "", options);
  }

  public static MarkupGenerator getMarkupGenerator(final String sdkVersion, final String qualifier,
      TranslatorOptions options) throws InstantiationException {
    return ComponentFactory.INSTANCE.getComponentImpl(sdkVersion,
        SdkComponentType.MARKUP_GENERATOR, qualifier, MarkupGenerator.class, options);
  }

  public static ScriptGenerator getScriptGenerator(final String sdkVersion, TranslatorOptions options)
      throws InstantiationException {
    return getScriptGenerator(sdkVersion, "", options);
  }

  public static ScriptGenerator getScriptGenerator(final String sdkVersion, final String qualifier,
      TranslatorOptions options) throws InstantiationException {
    return ComponentFactory.INSTANCE.getComponentImpl(sdkVersion,
        SdkComponentType.SCRIPT_GENERATOR, qualifier, ScriptGenerator.class, options);
  }
}
