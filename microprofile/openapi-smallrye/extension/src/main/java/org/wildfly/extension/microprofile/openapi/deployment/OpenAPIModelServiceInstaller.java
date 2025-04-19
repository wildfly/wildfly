/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.openapi.deployment;

import static org.wildfly.extension.microprofile.openapi.logging.MicroProfileOpenAPILogger.LOGGER;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import io.smallrye.openapi.api.SmallRyeOpenAPI;
import io.smallrye.openapi.runtime.scanner.spi.AnnotationScanner;
import io.smallrye.openapi.spi.OASFactoryResolverImpl;
import io.undertow.servlet.api.DeploymentInfo;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.info.Info;
import org.eclipse.microprofile.openapi.models.servers.Server;
import org.eclipse.microprofile.openapi.spi.OASFactoryResolver;
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
import org.jboss.vfs.VirtualFile;
import org.wildfly.extension.undertow.Host;
import org.wildfly.extension.undertow.UndertowListener;
import org.wildfly.extension.undertow.UndertowService;
import org.wildfly.extension.undertow.deployment.UndertowDeploymentInfoService;
import org.wildfly.security.manager.WildFlySecurityManager;
import org.wildfly.subsystem.service.DeploymentServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * Configures a service that provides an OpenAPI model for a deployment.
 * @author Paul Ferraro
 */
public class OpenAPIModelServiceInstaller implements DeploymentServiceInstaller {
    private static final Set<String> REQUISITE_LISTENERS = Collections.singleton("http");

    static {
        // Set the static OASFactoryResolver eagerly avoiding the need perform TCCL service loading later
        OASFactoryResolver.setInstance(new OASFactoryResolverImpl());
    }

    private final OpenAPIModelConfiguration configuration;

    public OpenAPIModelServiceInstaller(OpenAPIModelConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void install(DeploymentPhaseContext context) {
        DeploymentUnit unit = context.getDeploymentUnit();
        String deploymentName = unit.getName();
        VirtualFile root = unit.getAttachment(Attachments.DEPLOYMENT_ROOT).getRoot();
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
        boolean useRelativeServerURLs = this.configuration.useRelativeServerURLs();
        String serverName = this.configuration.getServerName();
        String hostName = this.configuration.getHostName();

        ServiceDependency<Host> host = ServiceDependency.on(Host.SERVICE_DESCRIPTOR, serverName, hostName);
        ServiceDependency<DeploymentInfo> deploymentInfo = ServiceDependency.on(UndertowService.deploymentServiceName(unit.getServiceName()).append(UndertowDeploymentInfoService.SERVICE_NAME));

        Supplier<SmallRyeOpenAPI> factory = new Supplier<>() {
            @Override
            public SmallRyeOpenAPI get() {
                return SmallRyeOpenAPI.builder()
                        .withApplicationClassLoader(module.getClassLoader())
                        .withConfig(config)
                        .withIndex(index)
                        .withResourceLocator(new Function<>() {
                            @Override
                            public URL apply(String path) {
                                try {
                                    VirtualFile file = root.getChild(path);
                                    return file.exists() ? file.toURL() : null;
                                } catch (IOException e) {
                                    throw new IllegalArgumentException(e);
                                }
                            }
                        })
                        .withScannerClassLoader(WildFlySecurityManager.getClassLoaderPrivileged(AnnotationScanner.class))
                        .addFilter(new OASFilter() {
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

                                List<UndertowListener> listeners = host.get().getServer().getListeners();

                                if (model.getServers() == null) {
                                    // Generate Server entries if none exist
                                    String contextPath = deploymentInfo.get().getContextPath();
                                    if (useRelativeServerURLs) {
                                        model.setServers(Collections.singletonList(OASFactory.createServer().url(contextPath)));
                                    } else {
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
                                                Server server = createServer(listener.getProtocol(), virtualHost, binding.getPort(), contextPath);
                                                if (server != null) {
                                                    servers.add(server);
                                                }
                                            }
                                            for (ClientMapping mapping : binding.getClientMappings()) {
                                                Server server = createServer(listener.getProtocol(), mapping.getDestinationAddress(), mapping.getDestinationPort(), contextPath);
                                                if (server != null) {
                                                    servers.add(server);
                                                }
                                            }
                                        }
                                        model.setServers(servers);
                                    }
                                }

                                if (listeners.stream().map(UndertowListener::getProtocol).noneMatch(REQUISITE_LISTENERS::contains)) {
                                    LOGGER.requiredListenersNotFound(host.get().getServer().getName(), REQUISITE_LISTENERS);
                                }
                            }
                        })
                        .build();
            }
        };
        ServiceInstaller.builder(factory)
                .provides(OpenAPIModelConfiguration.SERVICE_DESCRIPTOR, serverName, hostName, this.configuration.getPath())
                .requires(List.of(host, deploymentInfo))
                .build()
                .install(context);
    }

    private static Server createServer(String protocol, String host, int port, String path) {
        try {
            URL url = new URL(protocol, host, port, path);
            if (port == url.getDefaultPort()) {
                url = new URL(protocol, host, path);
            }
            return OASFactory.createServer().url(url.toString());
        } catch (MalformedURLException e) {
            // Skip listeners with no known URLStreamHandler (e.g. AJP)
            return null;
        }
    }
}
