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
  private static final String FALLBACK_SDK_VERSION = VersionDependentComponent.ANY_VERSION;

  private static final Logger log = LoggerFactory.getLogger(VersionDependentComponentFactory.class);

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
    VersionDependentComponentDescriptor<T> descriptor =
        (VersionDependentComponentDescriptor<T>) Optional.ofNullable(componentsMap.get(sdkVersion))
            .orElseGet(Collections::emptyMap).get(componentType);

    if (descriptor == null) {
      String fallbackSdkVersion = FALLBACK_SDK_VERSION;

      log.warn(
          "No implementation found for component type [{}] of SDK [{}]. Trying with fallback SDK [{}]",
          componentType, sdkVersion, fallbackSdkVersion);

      descriptor =
          (VersionDependentComponentDescriptor<T>) Optional.ofNullable(componentsMap.get(fallbackSdkVersion))
              .orElseGet(Collections::emptyMap).get(componentType);

      if (descriptor == null) {
        throw new IllegalArgumentException(
            MessageFormat.format("No implementation found for component type [{0}] of SDK [{1}].",
                componentType, fallbackSdkVersion));
      }
    }

    return descriptor.createInstance(initArgs);
  }
}
