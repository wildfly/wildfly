/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.openapi.host;

import static org.wildfly.extension.microprofile.openapi.logging.MicroProfileOpenAPILogger.LOGGER;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.smallrye.openapi.spi.OASFactoryResolverImpl;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.spi.OASFactoryResolver;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.ServiceNameFactory;
import org.wildfly.common.function.Functions;
import org.wildfly.extension.microprofile.openapi.model.CompositeOpenAPIProvider;
import org.wildfly.extension.microprofile.openapi.model.OpenAPIProvider;
import org.wildfly.extension.microprofile.openapi.model.OpenAPIRegistry;
import org.wildfly.extension.undertow.Host;
import org.wildfly.extension.undertow.UndertowListener;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * Installs a service that provides an OpenAPI model for a host.
 * @author Paul Ferraro
 */
public class HostOpenAPIProviderServiceInstaller implements ResourceServiceInstaller {
    private static final Set<String> REQUISITE_LISTENERS = Collections.singleton("http");

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
                // Surprisingly, neither the spec module nor SmallRye exposes constants for these property names...
                model.setExternalDocs(OASFactory.createExternalDocumentation().url(this.getExternalDocumentationProperty("url")).description(getExternalDocumentationProperty("description")));
                model.setInfo(OASFactory.createInfo()
                        .contact(OASFactory.createContact().name(this.getContactProperty("name")).email(this.getContactProperty("email")).url(this.getContactProperty("url")))
                        .license(OASFactory.createLicense().name(this.getLicenseProperty("name")).identifier(this.getLicenseProperty("identifier")).url(this.getLicenseProperty("url")))
                        .description(this.getInfoProperty("description"))
                        .summary(this.getInfoProperty("summary"))
                        .termsOfService(this.getInfoProperty("termsOfService"))
                        .title(this.getInfoProperty("title"))
                        .version(this.getInfoProperty("version"))
                        );

                Collection<UndertowListener> listeners = host.get().getServer().getListeners();

                if (listeners.stream().map(UndertowListener::getProtocol).noneMatch(REQUISITE_LISTENERS::contains)) {
                    LOGGER.requiredListenersNotFound(host.get().getServer().getName(), REQUISITE_LISTENERS);
                }

                return new CompositeOpenAPIProvider(model);
            }

            private String getExternalDocumentationProperty(String name) {
                return this.getProperty(Stream.of("externalDocs", name));
            }

            private String getInfoProperty(String name) {
                return this.getInfoProperty(Stream.of(name));
            }

            private String getInfoProperty(Stream<String> names) {
                return this.getProperty(Stream.concat(Stream.of("info"), names));
            }

            private String getContactProperty(String name) {
                return this.getInfoProperty(Stream.of("contact", name));
            }

            private String getLicenseProperty(String name) {
                return this.getInfoProperty(Stream.of("license", name));
            }

            private String getProperty(Stream<String> names) {
                return config.getOptionalValue(String.format("mp.openapi.extensions.%s.%s.%s", serverName, hostName, names.collect(Collectors.joining("."))), String.class).orElse(null);
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
