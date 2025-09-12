/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.microprofile.openapi.host;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

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
                model.setExternalDocs(OASFactory.createExternalDocumentation().description(this.getHostPropertyValue(OpenAPIModelConfiguration.EXTERNAL_DOCUMENTATION_DESCRIPTION)).url(this.getHostPropertyValue(OpenAPIModelConfiguration.EXTERNAL_DOCUMENTATION_URL)));
                model.setInfo(OASFactory.createInfo()
                        .contact(OASFactory.createContact().email(this.getHostPropertyValue(OpenAPIModelConfiguration.INFO_CONTACT_EMAIL)).name(this.getHostPropertyValue(OpenAPIModelConfiguration.INFO_CONTACT_NAME)).url(this.getHostPropertyValue(OpenAPIModelConfiguration.INFO_CONTACT_URL)))
                        .description(this.getHostPropertyValue(OpenAPIModelConfiguration.INFO_DESCRIPTION))
                        .license(OASFactory.createLicense().identifier(this.getHostPropertyValue(OpenAPIModelConfiguration.INFO_LICENSE_IDENTIFIER)).name(this.getHostPropertyValue(OpenAPIModelConfiguration.INFO_LICENSE_NAME)).url(this.getHostPropertyValue(OpenAPIModelConfiguration.INFO_LICENSE_URL)))
                        .summary(this.getHostPropertyValue(OpenAPIModelConfiguration.INFO_SUMMARY))
                        .termsOfService(this.getHostPropertyValue(OpenAPIModelConfiguration.INFO_TERMS_OF_SERVICE))
                        .title(this.getHostPropertyValue(OpenAPIModelConfiguration.INFO_TITLE))
                        .version(this.getHostPropertyValue(OpenAPIModelConfiguration.INFO_VERSION))
                        );
                model.setOpenapi(this.getHostPropertyValue(OpenAPIModelConfiguration.VERSION, SmallRyeOASConfig.Defaults.VERSION));

                return new CompositeOpenAPIProvider(model);
            }

            private String getHostPropertyValue(String propertyName) {
                return this.getHostPropertyValue(propertyName, null);
            }

            private String getHostPropertyValue(String propertyName, String defaultValue) {
                return HostOpenAPIModelConfiguration.getProperty(config, serverName, hostName, propertyName, String.class).orElse(defaultValue);
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
