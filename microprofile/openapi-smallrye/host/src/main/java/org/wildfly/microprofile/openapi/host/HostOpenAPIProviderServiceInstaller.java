/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.microprofile.openapi.host;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import io.smallrye.openapi.spi.OASFactoryResolverImpl;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.servers.Server;
import org.eclipse.microprofile.openapi.spi.OASFactoryResolver;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.ServiceNameFactory;
import org.jboss.as.network.ClientMapping;
import org.jboss.as.network.SocketBinding;
import org.wildfly.common.function.Functions;
import org.wildfly.extension.undertow.Host;
import org.wildfly.extension.undertow.UndertowListener;
import org.wildfly.microprofile.openapi.OpenAPIModelProvider;
import org.wildfly.microprofile.openapi.OpenAPIModelRegistry;
import org.wildfly.service.Installer.StartWhen;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * Installs a service that provides an OpenAPI model for a host.
 * @author Paul Ferraro
 */
public class HostOpenAPIProviderServiceInstaller implements ResourceServiceInstaller {
    static {
        // Set the static OASFactoryResolver eagerly avoiding the need perform TCCL service loading later
        OASFactoryResolver.setInstance(new OASFactoryResolverImpl());
    }

    private final HostOpenAPIModelConfiguration configuration;

    public HostOpenAPIProviderServiceInstaller(HostOpenAPIModelConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public Consumer<OperationContext> install(OperationContext context) {
        if (!this.configuration.isEnabled()) return Functions.discardingConsumer();

        String serverName = this.configuration.getServerName();
        String hostName = this.configuration.getHostName();
        boolean autoGenerateServers = this.configuration.isServerAutoGenerationEnabled();
        String componentKeyFormat = this.configuration.getComponentKeyFormat();
        UnaryOperator<String> resolver = name -> this.configuration.getPropertyValue(name, String.class).orElse(null);

        ServiceDependency<CompositeOpenAPIModelProvider> provider = ServiceDependency.on(Host.SERVICE_DESCRIPTOR, serverName, hostName).map(new Function<>() {
            @Override
            public CompositeOpenAPIModelProvider apply(Host host) {
                // Populate singleton properties of model via MP config
                OpenAPI model = OASFactory.createOpenAPI()
                        .externalDocs(OASFactory.createExternalDocumentation()
                                .description(resolver.apply(HostOpenAPIModelConfiguration.EXTERNAL_DOCUMENTATION_DESCRIPTION))
                                .url(resolver.apply(HostOpenAPIModelConfiguration.EXTERNAL_DOCUMENTATION_URL)))
                        .info(OASFactory.createInfo()
                                .contact(OASFactory.createContact().email(resolver.apply(HostOpenAPIModelConfiguration.INFO_CONTACT_EMAIL)).name(resolver.apply(HostOpenAPIModelConfiguration.INFO_CONTACT_NAME)).url(resolver.apply(HostOpenAPIModelConfiguration.INFO_CONTACT_URL)))
                                .description(resolver.apply(HostOpenAPIModelConfiguration.INFO_DESCRIPTION))
                                .license(OASFactory.createLicense().identifier(resolver.apply(HostOpenAPIModelConfiguration.INFO_LICENSE_IDENTIFIER)).name(resolver.apply(HostOpenAPIModelConfiguration.INFO_LICENSE_NAME)).url(resolver.apply(HostOpenAPIModelConfiguration.INFO_LICENSE_URL)))
                                .summary(resolver.apply(HostOpenAPIModelConfiguration.INFO_SUMMARY))
                                .termsOfService(resolver.apply(HostOpenAPIModelConfiguration.INFO_TERMS_OF_SERVICE))
                                .title(resolver.apply(HostOpenAPIModelConfiguration.INFO_TITLE))
                                .version(resolver.apply(HostOpenAPIModelConfiguration.INFO_VERSION)))
                        .jsonSchemaDialect(resolver.apply(HostOpenAPIModelConfiguration.JSON_SCHEMA_DIALECT))
                        .openapi(resolver.apply(HostOpenAPIModelConfiguration.VERSION))
                        ;

                if (autoGenerateServers) {
                    int aliases = host.getAllAliases().size();
                    Collection<UndertowListener> listeners = host.getServer().getListeners();
                    int size = 0;
                    for (UndertowListener listener : listeners) {
                        size += aliases + listener.getSocketBinding().getClientMappings().size();
                    }
                    List<Server> servers = new ArrayList<>(size);
                    for (UndertowListener listener : listeners) {
                        SocketBinding binding = listener.getSocketBinding();
                        Set<String> virtualHosts = new TreeSet<>(host.getAllAliases());
                        // The name of the host is not a real virtual host (e.g. default-host)
                        virtualHosts.remove(host.getName());

                        InetAddress address = binding.getAddress();
                        // Omit wildcard addresses
                        if (!address.isAnyLocalAddress()) {
                            virtualHosts.add(address.getCanonicalHostName());
                        }

                        for (String virtualHost : virtualHosts) {
                            Server server = createServer(listener.getProtocol(), virtualHost, binding.getPort());
                            if (server != null) {
                                servers.add(server);
                            }
                        }
                        for (ClientMapping mapping : binding.getClientMappings()) {
                            Server server = createServer(listener.getProtocol(), mapping.getDestinationAddress(), mapping.getDestinationPort());
                            if (server != null) {
                                servers.add(server);
                            }
                        }
                    }
                    model.setServers(servers);
                }

                return new CompositeOpenAPIModelProvider(model, (name, key) -> String.format(componentKeyFormat, name, key));
            }

            private static Server createServer(String protocol, String host, int port) {
                try {
                    URL url = new URL(protocol, host, port, "");
                    if (port == url.getDefaultPort()) {
                        url = new URL(protocol, host, "");
                    }
                    return OASFactory.createServer().url(url.toString());
                } catch (MalformedURLException e) {
                    // Skip listeners with no known URLStreamHandler (e.g. AJP)
                    return null;
                }
            }
        });

        return ServiceInstaller.builder(provider)
                .startWhen(StartWhen.REQUIRED)
                .provides(ServiceNameFactory.resolveServiceName(OpenAPIModelProvider.DEFAULT_SERVICE_DESCRIPTOR, serverName, hostName))
                .provides(ServiceNameFactory.resolveServiceName(OpenAPIModelRegistry.SERVICE_DESCRIPTOR, serverName, hostName))
                .build()
                .install(context);
    }
}
