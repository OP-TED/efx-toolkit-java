package eu.europa.ted.efx.mock;

import static java.util.Map.entry;
import java.text.MessageFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import eu.europa.ted.efx.interfaces.SymbolResolver;
import eu.europa.ted.efx.mock.sdk1.SymbolResolverMockV1;
import eu.europa.ted.efx.mock.sdk2.SymbolResolverMockV2;

public class SymbolResolverMockFactory {
  private static final Map<String, Class<? extends SymbolResolver>> symbolResolversBySdkVersion =
      Map.ofEntries(
          entry("eforms-sdk-1.0", SymbolResolverMockV1.class),
          entry("eforms-sdk-2.0", SymbolResolverMockV2.class));

  // Concurrent, as tests that run in parallel ask for the same instance at once
  private static final Map<String, SymbolResolver> instances = new ConcurrentHashMap<>();

  public static SymbolResolver getInstance(final String sdkVersion) {
    return instances.computeIfAbsent(sdkVersion, k -> {
      try {
        Class<? extends SymbolResolver> symbolResolverClass =
            symbolResolversBySdkVersion.get(sdkVersion);

        if (symbolResolverClass == null) {
          throw new IllegalArgumentException(
              MessageFormat.format("Unsupported SDK version [{0}]", sdkVersion));
        }

        return symbolResolverClass.getConstructor().newInstance();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }
}
