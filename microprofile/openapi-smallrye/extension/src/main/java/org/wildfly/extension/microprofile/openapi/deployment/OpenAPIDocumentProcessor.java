/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.openapi.deployment;

import java.util.AbstractMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import org.eclipse.microprofile.openapi.models.ExternalDocumentation;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.info.Contact;
import org.eclipse.microprofile.openapi.models.info.Info;
import org.eclipse.microprofile.openapi.models.info.License;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.jaxrs.JaxrsAnnotations;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.msc.service.DuplicateServiceException;
import org.wildfly.extension.microprofile.openapi.logging.MicroProfileOpenAPILogger;
import org.wildfly.extension.undertow.UndertowService;
import org.wildfly.extension.undertow.deployment.UndertowDeploymentInfoService;
import org.wildfly.microprofile.openapi.OpenAPIModelConfiguration;
import org.wildfly.microprofile.openapi.OpenAPIModelProvider;
import org.wildfly.microprofile.openapi.OpenAPIModelRegistry;
import org.wildfly.microprofile.openapi.OpenAPIModelRegistry.Registration;
import org.wildfly.microprofile.openapi.host.HostOpenAPIModelConfiguration;
import org.wildfly.microprofile.openapi.host.OpenAPIHttpHandlerServiceInstaller;
import org.wildfly.service.Installer.StartWhen;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

import io.smallrye.openapi.api.OpenApiConfig;
import io.undertow.servlet.api.DeploymentInfo;

/**
 * Processes the OpenAPI model for a deployment.
 * @author Michael Edgar
 * @author Paul Ferraro
 */
public class OpenAPIDocumentProcessor implements DeploymentUnitProcessor {
    private static final Function<Optional<Info>, Optional<Contact>> INFO_CONTACT = optional(Info::getContact);
    private static final Function<Optional<Info>, Optional<License>> INFO_LICENSE = optional(Info::getLicense);
    private static final Function<Optional<OpenAPI>, Optional<ExternalDocumentation>> EXTERNAL_DOCUMENTATION = optional(OpenAPI::getExternalDocs);
    private static final Function<Optional<OpenAPI>, Optional<Info>> INFO = optional(OpenAPI::getInfo);
    private static final Function<Optional<OpenAPI>, Optional<Contact>> CONTACT = INFO.andThen(INFO_CONTACT);
    private static final Function<Optional<OpenAPI>, Optional<License>> LICENSE = INFO.andThen(INFO_LICENSE);

    private static final List<Map.Entry<String, Function<Optional<OpenAPI>, Optional<String>>>> SINGLETON_PROPERTIES = List.of(
                Map.entry(HostOpenAPIModelConfiguration.EXTERNAL_DOCUMENTATION_DESCRIPTION, EXTERNAL_DOCUMENTATION.andThen(optional(ExternalDocumentation::getDescription))),
                Map.entry(HostOpenAPIModelConfiguration.EXTERNAL_DOCUMENTATION_URL, EXTERNAL_DOCUMENTATION.andThen(optional(ExternalDocumentation::getUrl))),
                Map.entry(HostOpenAPIModelConfiguration.INFO_CONTACT_EMAIL, CONTACT.andThen(optional(Contact::getEmail))),
                Map.entry(HostOpenAPIModelConfiguration.INFO_CONTACT_NAME, CONTACT.andThen(optional(Contact::getName))),
                Map.entry(HostOpenAPIModelConfiguration.INFO_CONTACT_URL, CONTACT.andThen(optional(Contact::getUrl))),
                Map.entry(HostOpenAPIModelConfiguration.INFO_DESCRIPTION, INFO.andThen(optional(Info::getDescription))),
                Map.entry(HostOpenAPIModelConfiguration.INFO_LICENSE_IDENTIFIER, LICENSE.andThen(optional(License::getIdentifier))),
                Map.entry(HostOpenAPIModelConfiguration.INFO_LICENSE_NAME, LICENSE.andThen(optional(License::getName))),
                Map.entry(HostOpenAPIModelConfiguration.INFO_LICENSE_URL, LICENSE.andThen(optional(License::getUrl))),
                Map.entry(HostOpenAPIModelConfiguration.INFO_SUMMARY, INFO.andThen(optional(Info::getSummary))),
                Map.entry(HostOpenAPIModelConfiguration.INFO_TERMS_OF_SERVICE, INFO.andThen(optional(Info::getTermsOfService))),
                Map.entry(HostOpenAPIModelConfiguration.INFO_TITLE, INFO.andThen(optional(Info::getTitle))),
                Map.entry(HostOpenAPIModelConfiguration.INFO_VERSION, INFO.andThen(optional(Info::getVersion))),
                Map.entry(HostOpenAPIModelConfiguration.JSON_SCHEMA_DIALECT, optional(OpenAPI::getJsonSchemaDialect)),
                Map.entry(HostOpenAPIModelConfiguration.VERSION, optional(OpenAPI::getOpenapi)));

    private static <T, R> Function<Optional<T>, Optional<R>> optional(Function<T, R> function) {
        return new Function<>() {
            @Override
            public Optional<R> apply(Optional<T> value) {
                return value.map(function);
            }
        };
    }

    @Override
    public void deploy(DeploymentPhaseContext context) throws DeploymentUnitProcessingException {
        DeploymentUnit unit = context.getDeploymentUnit();

        if (DeploymentTypeMarker.isType(DeploymentType.WAR, unit)) {
            DeploymentOpenAPIModelConfiguration configuration = new DeploymentUnitOpenAPIModelConfiguration(unit);
            OpenAPIModelConfiguration hostConfiguration = new HostOpenAPIModelConfiguration(configuration.getServerName(), configuration.getHostName());

            if (configuration.isEnabled()) {
                OpenApiConfig config = OpenApiConfig.fromConfig(configuration.getMicroProfileConfig());

                // The MicroProfile OpenAPI specification expects the container to register an OpenAPI endpoint if any of the following conditions are met:
                // * An OASModelReader was configured
                // * An OASFilter was configured
                // * A static OpenAPI file is present
                // * The application contains Jakarta RESTful Web Services
                if ((config.modelReader() != null) || (config.filter() != null) || configuration.getStaticFile().isPresent() || isRestful(unit)) {
                    try {
                        new DeploymentOpenAPIProviderServiceInstaller(configuration).install(context);

                        if (!hostConfiguration.getPath().equals(configuration.getPath())) {
                            // If this is a non-standard endpoint for this host, register a handler
                            MicroProfileOpenAPILogger.LOGGER.nonStandardEndpoint(configuration.getModelName(), configuration.getPath(), hostConfiguration.getPath());
                            new OpenAPIHttpHandlerServiceInstaller(configuration).install(context);
                        } else {
                            // Otherwise, register the deployment model with the registry for this host
                            String serverName = configuration.getServerName();
                            String hostName = configuration.getHostName();
                            String modelName = configuration.getModelName();
                            ServiceDependency<DeploymentInfo> deployment = ServiceDependency.on(UndertowService.deploymentServiceName(unit.getServiceName()).append(UndertowDeploymentInfoService.SERVICE_NAME));
                            ServiceDependency<OpenAPIModelRegistry.Registration> registration = ServiceDependency.on(OpenAPIModelRegistry.SERVICE_DESCRIPTOR, serverName, hostName).combine(ServiceDependency.on(OpenAPIModelProvider.SERVICE_DESCRIPTOR, serverName, hostName, modelName), new BiFunction<>() {
                                @Override
                                public Registration apply(OpenAPIModelRegistry registry, OpenAPIModelProvider provider) {
                                    OpenAPI model = provider.getModel();
                                    String contextName = deployment.get().getContextPath().replace("/", "");
                                    Map<String, Optional<OpenAPI>> models = registry.getModels();
                                    if (!models.isEmpty()) {
                                        Optional<OpenAPI> defaultModel = registry.getDefaultModel();
                                        Optional<OpenAPI> localModel = Optional.of(model);
                                        for (Map.Entry<String, Function<Optional<OpenAPI>, Optional<String>>> entry : SINGLETON_PROPERTIES) {
                                            Function<Optional<OpenAPI>, Optional<String>> property = entry.getValue();
                                            Optional<String> localValue = property.apply(localModel);
                                            if (localValue.isPresent()) {
                                                if (property.apply(defaultModel).isEmpty()) {
                                                    // Detect and log any singleton property conflicts
                                                    Map<String, String> conflicts = new TreeMap<>(Map.of(contextName, localValue.get()));
                                                    for (Map.Entry<String, Optional<OpenAPI>> modelEntry : models.entrySet()) {
                                                        property.apply(modelEntry.getValue()).filter(Predicate.not(localValue.get()::equals)).ifPresent(value -> conflicts.put(modelEntry.getKey(), value));
                                                    }
                                                    if (conflicts.size() > 1) {
                                                        MicroProfileOpenAPILogger.LOGGER.propertyValueConflicts(entry.getKey(), conflicts);
                                                    }
                                                } else {
                                                    MicroProfileOpenAPILogger.LOGGER.propertyValueOverride(entry.getKey(), localValue.get());
                                                }
                                            }
                                        }
                                    }
                                    return registry.register(contextName, model);
                                }
                            });
                            ServiceInstaller.builder(registration)
                                    .requires(deployment)
                                    .startWhen(StartWhen.INSTALLED)
                                    .onStop(OpenAPIModelRegistry.Registration::close)
                                    .build()
                                    .install(context);
                        }
                    } catch (DuplicateServiceException e) {
                        // Only one deployment can register the same OpenAPI endpoint with a given host
                        // Let the first one to register win
                        MicroProfileOpenAPILogger.LOGGER.endpointAlreadyRegistered(configuration.getHostName(), configuration.getModelName());
                    }
                }
            } else {
                MicroProfileOpenAPILogger.LOGGER.disabled(configuration.getModelName());
            }
        }
    }

    static <K, V, KR, VR> Function<Map.Entry<K, V>, Map.Entry<KR, VR>> entryMapper(Function<K, KR> keyMapper, Function<V, VR> valueMapper) {
        return new Function<>() {
            @Override
            public Map.Entry<KR, VR> apply(Map.Entry<K, V> entry) {
                return new AbstractMap.SimpleImmutableEntry<>(keyMapper.apply(entry.getKey()), valueMapper.apply(entry.getValue()));
            }
        };
    }

    static <K, V> Predicate<Map.Entry<K, V>> entryFilter(Predicate<K> keyFilter, Predicate<V> valueFilter) {
        return new Predicate<>() {
            @Override
            public boolean test(Map.Entry<K, V> entry) {
                return keyFilter.test(entry.getKey()) || valueFilter.test(entry.getValue());
            }
        };
    }

    private static boolean isRestful(DeploymentUnit unit) {
        CompositeIndex index = unit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
        for (JaxrsAnnotations annotation : EnumSet.allOf(JaxrsAnnotations.class)) {
            if (!index.getAnnotations(annotation.getDotName()).isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
