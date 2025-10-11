/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.openapi.deployment;

import static org.wildfly.extension.microprofile.openapi.logging.MicroProfileOpenAPILogger.LOGGER;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import io.smallrye.openapi.api.OpenApiConfig;
import io.smallrye.openapi.api.SmallRyeOpenAPI;
import io.smallrye.openapi.runtime.OpenApiProcessor;
import io.smallrye.openapi.runtime.scanner.spi.AnnotationScanner;
import io.undertow.servlet.api.DeploymentInfo;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.Paths;
import org.eclipse.microprofile.openapi.models.info.Info;
import org.eclipse.microprofile.openapi.models.servers.Server;
import org.jboss.as.network.ClientMapping;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexView;
import org.jboss.metadata.javaee.spec.DescriptionGroupMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.modules.Module;
import org.wildfly.extension.undertow.Host;
import org.wildfly.extension.undertow.UndertowListener;
import org.wildfly.extension.undertow.UndertowService;
import org.wildfly.extension.undertow.deployment.UndertowDeploymentInfoService;
import org.wildfly.microprofile.openapi.OpenAPIProvider;
import org.wildfly.security.manager.WildFlySecurityManager;
import org.wildfly.service.Installer.StartWhen;
import org.wildfly.subsystem.service.DeploymentServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * Installs a service that provides an OpenAPI model provider for a deployment.
 * @author Paul Ferraro
 */
public class DeploymentOpenAPIProviderServiceInstaller implements DeploymentServiceInstaller {
    private static final Set<String> REQUISITE_LISTENERS = Collections.singleton("http");

    private final DeploymentOpenAPIModelConfiguration configuration;

    public DeploymentOpenAPIProviderServiceInstaller(DeploymentOpenAPIModelConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void install(DeploymentPhaseContext context) {
        DeploymentUnit unit = context.getDeploymentUnit();
        String deploymentName = unit.getName();
        // Convert org.jboss.as.server.deployment.annotation.CompositeIndex to org.jboss.jandex.CompositeIndex
        Collection<Index> indexes = new ArrayList<>(unit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX).getIndexes());
        if (unit.getParent() != null) {
            // load all composite indexes of the parent deployment unit
            indexes.addAll(unit.getParent().getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX).getIndexes());
            // load all composite indexes of the parent's accessible sub deployments
            for (DeploymentUnit subUnit : unit.getParent().getAttachment(Attachments.ACCESSIBLE_SUB_DEPLOYMENTS)) {
                indexes.addAll(subUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX).getIndexes());
            }
        }
        CompositeIndex index = CompositeIndex.create(indexes.stream().map(IndexView.class::cast).collect(Collectors.toList()));
        Module module = unit.getAttachment(Attachments.MODULE);
        JBossWebMetaData metaData = unit.getAttachment(WarMetaData.ATTACHMENT_KEY).getMergedJBossWebMetaData();
        Config config = this.configuration.getMicroProfileConfig();
        Function<String, URL> resourceResolver = this.configuration.getResourceResolver();
        boolean useRelativeServerURLs = this.configuration.useRelativeServerURLs();
        String serverName = this.configuration.getServerName();
        String hostName = this.configuration.getHostName();
        String modelName = this.configuration.getModelName();
        OpenApiConfig openAPIConfig = this.configuration.getOpenApiConfig();

        ServiceDependency<Host> host = ServiceDependency.on(Host.SERVICE_DESCRIPTOR, serverName, hostName);
        ServiceDependency<DeploymentInfo> deploymentInfo = ServiceDependency.on(UndertowService.deploymentServiceName(unit.getServiceName()).append(UndertowDeploymentInfoService.SERVICE_NAME));

        Supplier<OpenAPI> factory = new Supplier<>() {
            @Override
            public OpenAPI get() {
                SmallRyeOpenAPI.Builder builder = SmallRyeOpenAPI.builder()
                        // Disable "standard filter" logic so that our filter can run last
                        .enableStandardFilter(false)
                        .withApplicationClassLoader(module.getClassLoader())
                        .withConfig(config)
                        .withIndex(index)
                        .withResourceLocator(resourceResolver)
                        .withScannerClassLoader(WildFlySecurityManager.getClassLoaderPrivileged(AnnotationScanner.class))
                        ;
                // SmallRyeOpenAPI.Builder.build() woud otherwise apply the standard filter last
                @SuppressWarnings("deprecation")
                OASFilter filter = OpenApiProcessor.getFilter(openAPIConfig, module.getClassLoader(), index);
                if (filter != null) {
                    builder.addFilter(filter);
                }
                builder.addFilter(new OASFilter() {
                    @Override
                    public void filterOpenAPI(OpenAPI model) {
                        // Generate default title and description based on web metadata
                        DescriptionGroupMetaData descriptionMetaData = metaData.getDescriptionGroup();
                        String displayName = (descriptionMetaData != null) ? descriptionMetaData.getDisplayName() : null;
                        String title = (displayName != null) ? displayName : deploymentName;
                        String description = (descriptionMetaData != null) ? descriptionMetaData.getDescription() : null;

                        Info info = model.getInfo();
                        if (info == null) {
                            info = OASFactory.createInfo();
                            model.setInfo(info);
                        }
                        if (info.getTitle() == null) {
                            info.setTitle(title);
                        }
                        if (info.getDescription() == null) {
                            info.setDescription(description);
                        }

                        Collection<UndertowListener> listeners = host.get().getServer().getListeners();

                        if (listeners.stream().map(UndertowListener::getProtocol).noneMatch(REQUISITE_LISTENERS::contains)) {
                            LOGGER.requiredListenersNotFound(host.get().getServer().getName(), REQUISITE_LISTENERS);
                        }

                        // Generate Server entries if none exist
                        if ((model.getServers() == null) && !useRelativeServerURLs) {
                            int aliases = host.get().getAllAliases().size();
                            int size = 0;
                            for (UndertowListener listener : listeners) {
                                size += aliases + listener.getSocketBinding().getClientMappings().size();
                            }
                            List<Server> servers = new ArrayList<>(size);
                            for (UndertowListener listener : listeners) {
                                SocketBinding binding = listener.getSocketBinding();
                                Set<String> virtualHosts = new TreeSet<>(host.get().getAllAliases());
                                // The name of the host is not a real virtual host (e.g. default-host)
                                virtualHosts.remove(host.get().getName());

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
                    }
                });
                String contextPath = deploymentInfo.get().getContextPath();
                // If this application is not deployed to the root context, we need to append the context path to all service paths.
                // This ensures that multiple applications sharing the same virtual host can document services with the same path without colliding.
                if (contextPath.length() > 1) {
                    // N.B. The MicroProfile OpenAPI specification API inexplicably lacks a common path API.
                    builder.addFilter(new OASFilter() {
                        @Override
                        public void filterOpenAPI(OpenAPI model) {
                            Paths paths = model.getPaths();
                            if (paths != null) {
                                Map<String, PathItem> items = Optional.ofNullable(paths.getPathItems()).orElse(Map.of());
                                for (Map.Entry<String, PathItem> entry : items.entrySet()) {
                                    String path = entry.getKey();
                                    PathItem item = entry.getValue();

                                    paths.removePathItem(path);
                                    paths.addPathItem(contextPath + path, item);
                                }
                            }
                        }
                    });
                }
                return builder.build().model();
            }
        };
        ServiceInstaller.builder(OpenAPIProvider::of, factory)
                .provides(OpenAPIProvider.SERVICE_DESCRIPTOR, serverName, hostName, modelName)
                .requires(List.of(host, deploymentInfo))
                .startWhen(StartWhen.INSTALLED)
                .build()
                .install(context);
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
}
