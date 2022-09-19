package eu.europa.ted.eforms.sdk.selector.component;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.atteo.classindex.ClassIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class VersionDependentComponentFactory {
  private static final Logger logger = LoggerFactory.getLogger(VersionDependentComponentFactory.class);

  private Map<String, Map<VersionDependentComponentType, VersionDependentComponentDescriptor<?>>> componentsMap;

  protected VersionDependentComponentFactory() {
    populateComponents();
  }

  private void populateComponents() {
    if (componentsMap == null) {
      componentsMap = new HashMap<>();
    }

    ClassIndex.getAnnotated(VersionDependentComponent.class).forEach((Class<?> clazz) -> {
      VersionDependentComponent annotation = clazz.getAnnotation(VersionDependentComponent.class);

      String[] supportedSdkVersions = annotation.versions();
      VersionDependentComponentType componentType = annotation.componentType();

      for (String sdkVersion : supportedSdkVersions) {
        Map<VersionDependentComponentType, VersionDependentComponentDescriptor<?>> components =
            componentsMap.get(sdkVersion);

        if (components == null) {
          components = new HashMap<>();
          componentsMap.put(sdkVersion, components);
        }

        VersionDependentComponentDescriptor<?> component =
            new VersionDependentComponentDescriptor<>(sdkVersion, componentType, clazz);
        VersionDependentComponentDescriptor<?> existingComponent = components.get(componentType);

        if (existingComponent != null && !existingComponent.equals(component)) {
          throw new IllegalArgumentException(MessageFormat.format(
              "More than one components of type [{0}] have been found for SDK version [{1}]:\n\t- {2}\n\t- {3}",
              componentType, sdkVersion, existingComponent.getImplType().getName(),
              clazz.getName()));
        }

        components.put(componentType, component);
      }
    });
  }

  @SuppressWarnings("unchecked")
  protected <T> T getComponentImpl(String sdkVersion, final VersionDependentComponentType componentType,
      final Class<T> intf, Object... initArgs) throws InstantiationException {
  
    String normalizedVersion = normalizeVersion(sdkVersion); 

    VersionDependentComponentDescriptor<T> descriptor =
        (VersionDependentComponentDescriptor<T>) Optional.ofNullable(componentsMap.get(normalizedVersion))
            .orElseGet(Collections::emptyMap).get(componentType);

    if (descriptor == null) {
      logger.error("Failed to load required components of SDK [{}]", sdkVersion);
      throw new IllegalArgumentException(MessageFormat
          .format("No implementation found for component type [{0}] of SDK [{1}].", componentType, sdkVersion));
    }

    return descriptor.createInstance(initArgs);
  }

  private static String normalizeVersion(final String sdkVersion) {
    String normalizedVersion = sdkVersion;

    if (normalizedVersion.startsWith("eforms-sdk-")) {
      normalizedVersion = normalizedVersion.substring(11);
    }

    String[] numbers = normalizedVersion.split("\\.", -2);

    if (numbers.length < 1) {
      throw new IllegalArgumentException("Invalid SDK version: " + sdkVersion);
    }

    return numbers[0]
        + ((numbers.length > 1 && Integer.parseInt(numbers[0]) > 0) ? "" : "." + numbers[1]);
  }
}
