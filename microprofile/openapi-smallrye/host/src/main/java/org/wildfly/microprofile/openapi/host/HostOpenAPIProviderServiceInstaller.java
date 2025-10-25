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
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Function;

import io.smallrye.openapi.api.SmallRyeOASConfig;
import io.smallrye.openapi.spi.OASFactoryResolverImpl;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.openapi.OASConfig;
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
import org.wildfly.microprofile.openapi.OpenAPIModelConfiguration;
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

        ServiceDependency<CompositeOpenAPIProvider> provider = ServiceDependency.on(Host.SERVICE_DESCRIPTOR, serverName, hostName).map(new Function<>() {
            @Override
            public CompositeOpenAPIProvider apply(Host host) {
                OpenAPI model = OASFactory.createOpenAPI();
                // Populate global components of model via MP config
                model.setExternalDocs(OASFactory.createExternalDocumentation().description(this.getString(OpenAPIModelConfiguration.EXTERNAL_DOCUMENTATION_DESCRIPTION)).url(this.getString(OpenAPIModelConfiguration.EXTERNAL_DOCUMENTATION_URL)));
                model.setInfo(OASFactory.createInfo()
                        .contact(OASFactory.createContact().email(this.getString(OpenAPIModelConfiguration.INFO_CONTACT_EMAIL)).name(this.getString(OpenAPIModelConfiguration.INFO_CONTACT_NAME)).url(this.getString(OpenAPIModelConfiguration.INFO_CONTACT_URL)))
                        .description(this.getString(OpenAPIModelConfiguration.INFO_DESCRIPTION))
                        .license(OASFactory.createLicense().identifier(this.getString(OpenAPIModelConfiguration.INFO_LICENSE_IDENTIFIER)).name(this.getString(OpenAPIModelConfiguration.INFO_LICENSE_NAME)).url(this.getString(OpenAPIModelConfiguration.INFO_LICENSE_URL)))
                        .summary(this.getString(OpenAPIModelConfiguration.INFO_SUMMARY))
                        .termsOfService(this.getString(OpenAPIModelConfiguration.INFO_TERMS_OF_SERVICE))
                        .title(this.getString(OpenAPIModelConfiguration.INFO_TITLE))
                        .version(this.getString(OpenAPIModelConfiguration.INFO_VERSION))
                        );
                model.setOpenapi(this.getString(OpenAPIModelConfiguration.VERSION, SmallRyeOASConfig.Defaults.VERSION));

                if (this.getOptional(OpenAPIModelConfiguration.AUTO_GENERATE_SERVERS, Boolean.class).orElse(Boolean.FALSE)) {
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

                String format = this.getString(OpenAPIModelConfiguration.COMPONENT_KEY_FORMAT, OpenAPIModelConfiguration.DEFAULT_FORMAT);

                return new CompositeOpenAPIProvider(model, (name, key) -> String.format(format, name, key));
            }

            private String getString(String propertyName) {
                return this.getString(propertyName, null);
            }

            private String getString(String propertyName, String defaultValue) {
                return this.getOptional(propertyName, String.class).orElse(defaultValue);
            }

            private <T> Optional<T> getOptional(String propertyName, Class<T> type) {
                Optional<T> value = HostOpenAPIModelConfiguration.getProperty(config, serverName, hostName, propertyName, type);
                return value.isEmpty() ? config.getOptionalValue(OASConfig.EXTENSIONS_PREFIX + propertyName, type) : value;
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
