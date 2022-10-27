package eu.europa.ted.eforms.sdk;

import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import eu.europa.ted.eforms.sdk.selector.component.VersionDependentComponentFactory;
import eu.europa.ted.eforms.sdk.selector.component.VersionDependentComponentType;
import eu.europa.ted.efx.interfaces.MarkupGenerator;
import eu.europa.ted.efx.interfaces.ScriptGenerator;
import eu.europa.ted.efx.interfaces.SymbolResolver;

public class ComponentFactory extends VersionDependentComponentFactory {
  public static final ComponentFactory INSTANCE = new ComponentFactory();

  private ComponentFactory() {
    super();
  }


  /**
   * EfxToXpathSymbols is implemented as a "kind-of" singleton. One instance per version of the
   * eForms SDK.
   */
  private static final Map<String, SymbolResolver> instances = new HashMap<>();

  /**
   * Gets the single instance containing the symbols defined in the given version of the eForms SDK.
   *
   * @param sdkVersion Version of the SDK
   */
  public static SymbolResolver getSymbolResolver(final String sdkVersion, final Path sdkRootPath)
      throws InstantiationException {
    return instances.computeIfAbsent(sdkVersion, k -> {
      try {
        return ComponentFactory.INSTANCE.getComponentImpl(sdkVersion,
            VersionDependentComponentType.SYMBOL_RESOLVER, SymbolResolver.class, sdkVersion,
            sdkRootPath);
      } catch (InstantiationException e) {
        throw new RuntimeException(MessageFormat.format(
            "Failed to instantiate SDK Symbol Resolver for SDK version [{0}]", sdkVersion), e);
      }
    });
  }

  public static MarkupGenerator getMarkupGenerator(final String sdkVersion)
      throws InstantiationException {
    return ComponentFactory.INSTANCE.getComponentImpl(sdkVersion,
        VersionDependentComponentType.MARKUP_GENERATOR, MarkupGenerator.class);
  }

  public static ScriptGenerator getScriptGenerator(final String sdkVersion)
      throws InstantiationException {
    return ComponentFactory.INSTANCE.getComponentImpl(sdkVersion,
        VersionDependentComponentType.SCRIPT_GENERATOR, ScriptGenerator.class);
  }
}
