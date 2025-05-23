/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.microprofile.openapi.host;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.smallrye.openapi.api.SmallRyeOASConfig;
import io.smallrye.openapi.spi.OASFactoryResolverImpl;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.spi.OASFactoryResolver;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.ServiceNameFactory;
import org.wildfly.common.function.Functions;
import org.wildfly.extension.undertow.Host;
import org.wildfly.microprofile.openapi.OpenAPIModelConfiguration;
import org.wildfly.microprofile.openapi.OpenAPIProvider;
import org.wildfly.microprofile.openapi.OpenAPIRegistry;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * Installs a service that provides an OpenAPI model for a host.
 * @author Paul Ferraro
 */
public class HostOpenAPIProviderServiceInstaller implements ResourceServiceInstaller {
    // Starting index for properties names: mp.openapi.extensions.smallrye.
    static final int START_INDEX = SmallRyeOASConfig.VERSION.length() - "openapi".length();

    static {
        // Set the static OASFactoryResolver eagerly avoiding the need perform TCCL service loading later
        OASFactoryResolver.setInstance(new OASFactoryResolverImpl());
    }

    private final OpenAPIModelConfiguration configuration;

    public HostOpenAPIProviderServiceInstaller(OpenAPIModelConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public Consumer<OperationContext> install(OperationContext context) {
        if (!this.configuration.isEnabled()) return Functions.discardingConsumer();

        Config config = this.configuration.getMicroProfileConfig();
        String serverName = this.configuration.getServerName();
        String hostName = this.configuration.getHostName();

        ServiceDependency<Host> host = ServiceDependency.on(Host.SERVICE_DESCRIPTOR, serverName, hostName);

        Supplier<CompositeOpenAPIProvider> factory = new Supplier<>() {
            @Override
            public CompositeOpenAPIProvider get() {
                OpenAPI model = OASFactory.createOpenAPI();
                // Populate global components of model via MP config
                model.setExternalDocs(OASFactory.createExternalDocumentation().url(this.getExternalDocumentationPropertyValue("url")).description(this.getExternalDocumentationPropertyValue("description")));
                model.setInfo(OASFactory.createInfo()
                        .contact(OASFactory.createContact().name(this.getSmallRyePropertyValue(SmallRyeOASConfig.INFO_CONTACT_NAME)).email(this.getSmallRyePropertyValue(SmallRyeOASConfig.INFO_CONTACT_EMAIL)).url(this.getSmallRyePropertyValue(SmallRyeOASConfig.INFO_CONTACT_URL)))
                        .license(OASFactory.createLicense().name(this.getSmallRyePropertyValue(SmallRyeOASConfig.INFO_LICENSE_NAME)).identifier(this.getSmallRyePropertyValue(SmallRyeOASConfig.INFO_LICENSE_IDENTIFIER)).url(this.getSmallRyePropertyValue(SmallRyeOASConfig.INFO_LICENSE_URL)))
                        .description(this.getSmallRyePropertyValue(SmallRyeOASConfig.INFO_DESCRIPTION))
                        .summary(this.getSmallRyePropertyValue(SmallRyeOASConfig.INFO_SUMMARY))
                        .termsOfService(this.getSmallRyePropertyValue(SmallRyeOASConfig.INFO_TERMS))
                        .title(this.getSmallRyePropertyValue(SmallRyeOASConfig.INFO_TITLE))
                        .version(this.getOptionalSmallRyePropertyValue(SmallRyeOASConfig.INFO_TITLE).orElse(SmallRyeOASConfig.Defaults.VERSION))
                        );

                return new CompositeOpenAPIProvider(model);
            }

            private String getSmallRyePropertyValue(String propertyName) {
                return this.getOptionalSmallRyePropertyValue(propertyName).orElse(null);
            }

            private Optional<String> getOptionalSmallRyePropertyValue(String propertyName) {
                // Convert SmallRye property name to host property name
                return this.getOptionalHostPropertyValue(propertyName.substring(START_INDEX));
            }

            private String getExternalDocumentationPropertyValue(String name) {
                // SmallRye has no constants for these properties
                return this.getHostPropertyValue(Stream.of("externalDocs", name));
            }

            private String getHostPropertyValue(Stream<String> names) {
                return this.getHostPropertyValue(names.collect(Collectors.joining(".")));
            }

            private String getHostPropertyValue(String propertyName) {
                return this.getOptionalHostPropertyValue(propertyName).orElse(null);
            }

            private Optional<String> getOptionalHostPropertyValue(String propertyName) {
                return HostOpenAPIModelConfiguration.getProperty(config, serverName, hostName, propertyName, String.class);
            }
        };
        return ServiceInstaller.builder(factory)
                .provides(ServiceNameFactory.resolveServiceName(OpenAPIProvider.DEFAULT_SERVICE_DESCRIPTOR, serverName, hostName))
                .provides(ServiceNameFactory.resolveServiceName(OpenAPIRegistry.SERVICE_DESCRIPTOR, serverName, hostName))
                .requires(List.of(host))
                .build()
                .install(context);
    }
}
