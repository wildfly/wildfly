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
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import io.smallrye.openapi.spi.OASFactoryResolverImpl;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.servers.Server;
import org.eclipse.microprofile.openapi.spi.OASFactoryResolver;
import org.jboss.as.controller.RequirementServiceTarget;
import org.jboss.as.controller.ServiceNameFactory;
import org.jboss.as.network.ClientMapping;
import org.jboss.as.network.SocketBinding;
import org.jboss.msc.service.ServiceController;
import org.wildfly.extension.undertow.Host;
import org.wildfly.extension.undertow.UndertowListener;
import org.wildfly.microprofile.openapi.OpenAPIModelProvider;
import org.wildfly.microprofile.openapi.OpenAPIModelRegistry;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * Installs a service that provides an OpenAPI model for a host.
 * @author Paul Ferraro
 */
public class HostOpenAPIProviderServiceInstaller implements ServiceInstaller {
    private static final String HTTP = "http";
    private static final String HTTPS = "https";
    private static final Set<String> SCHEMES = Set.of(HTTP, HTTPS);

    static {
        // Set the static OASFactoryResolver eagerly avoiding the need perform TCCL service loading later
        OASFactoryResolver.setInstance(new OASFactoryResolverImpl());
    }

    private final HostOpenAPIModelConfiguration configuration;

    public HostOpenAPIProviderServiceInstaller(HostOpenAPIModelConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public ServiceController<?> install(RequirementServiceTarget target) {
        String serverName = this.configuration.getServerName();
        String hostName = this.configuration.getHostName();
        Set<String> listenerNames = this.configuration.getAutoDocumentedListeners();
        String componentKeyFormat = this.configuration.getComponentKeyFormat();
        UnaryOperator<String> resolver = name -> this.configuration.getPropertyValue(name, String.class).orElse(null);

        // Establish listener dependencies in lieu of calling Server.getListeners() as these are concurrently started and are not guaranteed to be registered yet
        List<ServiceDependency<UndertowListener>> requiredListeners = new ArrayList<>(listenerNames.size());
        for (String listenerName : listenerNames) {
            requiredListeners.add(ServiceDependency.on(UndertowListener.SERVICE_DESCRIPTOR, serverName, listenerName));
        }
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

                if (!requiredListeners.isEmpty()) {
                    Collection<UndertowListener> listeners = requiredListeners.stream().map(Supplier::get).toList();
                    List<Server> servers = new LinkedList<>();
                    for (UndertowListener listener : listeners) {
                        SocketBinding binding = listener.getSocketBinding();
                        List<ClientMapping> clientMappings = binding.getClientMappings();
                        // Prefer client mappings, if defined, as this implies that the listener port is mapped or proxied
                        if (!clientMappings.isEmpty()) {
                            for (ClientMapping clientMapping : clientMappings) {
                                // N.B. This could be an AJP listener
                                Server server = createServer(listener.isSecure() ? HTTPS : HTTP, clientMapping.getDestinationAddress(), clientMapping.getDestinationPort());
                                if (server != null) {
                                    servers.add(server);
                                }
                            }
                        } else if (SCHEMES.contains(listener.getProtocol())) { // Skip listeners w/out client mappings for which we can not generate a usable URL
                            // If listener has no client mappings, generate URLS using virtual hosts and (potentially offset) port of the listener socket binding
                            Set<String> aliases = host.getAllAliases();
                            List<String> socketBindingHosts = new ArrayList<>(aliases.size());
                            for (String alias : aliases) {
                                // Host.getAllAliases() includes the name of the host resource, even though this is not a virtual host, e.g. default-host
                                if (!alias.equals(hostName)) {
                                    socketBindingHosts.add(alias);
                                }
                            }
                            // If no virtual hosts, use host of socket binding itself
                            if (socketBindingHosts.isEmpty()) {
                                InetAddress address = binding.getAddress();
                                // Omit wildcard addresses
                                if (!address.isAnyLocalAddress()) {
                                    socketBindingHosts.add(address.getCanonicalHostName());
                                }
                            }

                            for (String socketBindingHost : socketBindingHosts) {
                                Server server = createServer(listener.getProtocol(), socketBindingHost, binding.getAbsolutePort());
                                if (server != null) {
                                    servers.add(server);
                                }
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

        return ServiceInstaller.BlockingBuilder.of(provider)
                .provides(ServiceNameFactory.resolveServiceName(OpenAPIModelProvider.DEFAULT_SERVICE_DESCRIPTOR, serverName, hostName))
                .provides(ServiceNameFactory.resolveServiceName(OpenAPIModelRegistry.SERVICE_DESCRIPTOR, serverName, hostName))
                .requires(requiredListeners)
                .build()
                .install(target);
    }
}
