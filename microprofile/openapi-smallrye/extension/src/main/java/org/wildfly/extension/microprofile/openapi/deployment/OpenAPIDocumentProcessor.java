/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.openapi.deployment;

import java.util.AbstractMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
import org.wildfly.microprofile.openapi.OpenAPIModelConfiguration;
import org.wildfly.microprofile.openapi.OpenAPIProvider;
import org.wildfly.microprofile.openapi.OpenAPIRegistry;
import org.wildfly.microprofile.openapi.OpenAPIRegistry.Registration;
import org.wildfly.microprofile.openapi.host.HostOpenAPIModelConfiguration;
import org.wildfly.microprofile.openapi.host.OpenAPIHttpHandlerServiceInstaller;
import org.wildfly.service.Installer.StartWhen;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

import io.smallrye.openapi.api.OpenApiConfig;

/**
 * Processes the OpenAPI model for a deployment.
 * @author Michael Edgar
 * @author Paul Ferraro
 */
public class OpenAPIDocumentProcessor implements DeploymentUnitProcessor {
    interface OptionalFunction<T, R> extends Function<T, R> {
        @Override
        default <V> OptionalFunction<T, V> andThen(Function<? super R, ? extends V> after) {
            return value -> Optional.ofNullable(value).map(this).map(after).orElse(null);
        }

        @Override
        default <V> OptionalFunction<V, R> compose(Function<? super V, ? extends T> before) {
            return value -> Optional.ofNullable(value).map(before).map(this).orElse(null);
        }
    }

    private static final OptionalFunction<OpenAPI, ExternalDocumentation> EXTERNAL_DOCUMENTATION = OpenAPI::getExternalDocs;
    private static final OptionalFunction<OpenAPI, Info> INFO = OpenAPI::getInfo;
    private static final OptionalFunction<OpenAPI, Contact> CONTACT = INFO.andThen(Info::getContact);
    private static final OptionalFunction<OpenAPI, License> LICENSE = INFO.andThen(Info::getLicense);
    private static final Predicate<String> NOT_NULL = Objects::nonNull;

    private static final Map<String, Function<OpenAPI, String>> HOST_PROPERTIES = Map.ofEntries(
                Map.entry(OpenAPIModelConfiguration.EXTERNAL_DOCUMENTATION_DESCRIPTION, EXTERNAL_DOCUMENTATION.andThen(ExternalDocumentation::getDescription)),
                Map.entry(OpenAPIModelConfiguration.EXTERNAL_DOCUMENTATION_URL, EXTERNAL_DOCUMENTATION.andThen(ExternalDocumentation::getUrl)),
                Map.entry(OpenAPIModelConfiguration.INFO_CONTACT_EMAIL, CONTACT.andThen(Contact::getEmail)),
                Map.entry(OpenAPIModelConfiguration.INFO_CONTACT_NAME, CONTACT.andThen(Contact::getName)),
                Map.entry(OpenAPIModelConfiguration.INFO_CONTACT_URL, CONTACT.andThen(Contact::getUrl)),
                Map.entry(OpenAPIModelConfiguration.INFO_DESCRIPTION, INFO.andThen(Info::getDescription)),
                Map.entry(OpenAPIModelConfiguration.INFO_LICENSE_IDENTIFIER, LICENSE.andThen(License::getIdentifier)),
                Map.entry(OpenAPIModelConfiguration.INFO_LICENSE_NAME, LICENSE.andThen(License::getName)),
                Map.entry(OpenAPIModelConfiguration.INFO_LICENSE_URL, LICENSE.andThen(License::getUrl)),
                Map.entry(OpenAPIModelConfiguration.INFO_SUMMARY, INFO.andThen(Info::getSummary)),
                Map.entry(OpenAPIModelConfiguration.INFO_TERMS_OF_SERVICE, INFO.andThen(Info::getTermsOfService)),
                Map.entry(OpenAPIModelConfiguration.INFO_TITLE, INFO.andThen(Info::getTitle)),
                Map.entry(OpenAPIModelConfiguration.INFO_VERSION, INFO.andThen(Info::getVersion)),
                Map.entry(OpenAPIModelConfiguration.VERSION, OpenAPI::getOpenapi));

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
                            ServiceDependency<OpenAPIRegistry.Registration> registration = ServiceDependency.on(OpenAPIRegistry.SERVICE_DESCRIPTOR, serverName, hostName).combine(ServiceDependency.on(OpenAPIProvider.SERVICE_DESCRIPTOR, serverName, hostName, modelName), new BiFunction<>() {
                                @Override
                                public Registration apply(OpenAPIRegistry registry, OpenAPIProvider provider) {
                                    OpenAPI model = provider.get();
                                    Registration registration = registry.register(modelName, model);
                                    Map<String, OpenAPI> models = registry.getModels();
                                    if (models.size() > 1) {
                                        // Detect and log any host property conflicts
                                        for (Map.Entry<String, Function<OpenAPI, String>> entry : HOST_PROPERTIES.entrySet()) {
                                            Function<OpenAPI, String> function = entry.getValue();
                                            String value = function.apply(model);
                                            if (value != null) {
                                                Map<String, String> values = models.entrySet().stream()
                                                        .map(entryMapper(Function.identity(), function))
                                                        .filter(entryFilter(modelName::equals, NOT_NULL.and(Predicate.not(value::equals)))) // Drop concordant values
                                                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                                                if (values.size() > 1) {
                                                    MicroProfileOpenAPILogger.LOGGER.conflictingPropertyValues(entry.getKey(), values);
                                                }
                                            }
                                        }
                                    }
                                    return registration;
                                }
                            });
                            ServiceInstaller.builder(registration)
                                    .startWhen(StartWhen.INSTALLED)
                                    .onStop(OpenAPIRegistry.Registration::close)
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
