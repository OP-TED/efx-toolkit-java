package eu.europa.ted.eforms.sdk.factory;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.atteo.classindex.ClassIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import eu.europa.ted.eforms.sdk.annotation.SdkComponent;
import eu.europa.ted.eforms.sdk.component.SdkComponentDescriptor;
import eu.europa.ted.eforms.sdk.component.SdkComponentTypeEnum;

public abstract class AbstractSdkObjectFactory {
  private static final String FALLBACK_SDK_VERSION = SdkComponent.ANY;

  private static final Logger log = LoggerFactory.getLogger(AbstractSdkObjectFactory.class);

  private Map<String, Map<SdkComponentTypeEnum, SdkComponentDescriptor<?>>> componentsMap;

  protected AbstractSdkObjectFactory() {
    populateComponents();
  }

  private void populateComponents() {
    if (componentsMap == null) {
      componentsMap = new HashMap<>();
    }

    ClassIndex.getAnnotated(SdkComponent.class).forEach((Class<?> clazz) -> {
      SdkComponent annotation = clazz.getAnnotation(SdkComponent.class);

      String[] supportedSdkVersions = annotation.versions();
      SdkComponentTypeEnum componentType = annotation.componentType();

      for (String sdkVersion : supportedSdkVersions) {
        Map<SdkComponentTypeEnum, SdkComponentDescriptor<?>> components =
            componentsMap.get(sdkVersion);

        if (components == null) {
          components = new HashMap<>();
          componentsMap.put(sdkVersion, components);
        }

        SdkComponentDescriptor<?> component =
            new SdkComponentDescriptor<>(sdkVersion, componentType, clazz);
        SdkComponentDescriptor<?> existingComponent = components.get(componentType);

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
  protected <T> T getComponentImpl(String sdkVersion, final SdkComponentTypeEnum componentType,
      final Class<T> intf) throws InstantiationException {
    SdkComponentDescriptor<T> descriptor =
        (SdkComponentDescriptor<T>) Optional.ofNullable(componentsMap.get(sdkVersion))
            .orElseGet(Collections::emptyMap).get(componentType);

    if (descriptor == null) {
      String fallbackSdkVersion = FALLBACK_SDK_VERSION;

      log.warn(
          "No implementation found for component type [{}] of SDK [{}]. Trying with fallback SDK [{}]",
          componentType, sdkVersion, fallbackSdkVersion);

      descriptor =
          (SdkComponentDescriptor<T>) Optional.ofNullable(componentsMap.get(fallbackSdkVersion))
              .orElseGet(Collections::emptyMap).get(componentType);

      if (descriptor == null) {
        throw new IllegalArgumentException(
            MessageFormat.format("No implementation found for component type [{0}] of SDK [{1}].",
                componentType, fallbackSdkVersion));
      }
    }

    return descriptor.createInstance();
  }
}
