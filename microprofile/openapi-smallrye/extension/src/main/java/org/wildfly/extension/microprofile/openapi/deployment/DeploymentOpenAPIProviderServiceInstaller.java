/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.openapi.deployment;

import static org.wildfly.extension.microprofile.openapi.logging.MicroProfileOpenAPILogger.LOGGER;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
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
import org.eclipse.microprofile.openapi.models.Components;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.Paths;
import org.eclipse.microprofile.openapi.models.Reference;
import org.eclipse.microprofile.openapi.models.callbacks.Callback;
import org.eclipse.microprofile.openapi.models.examples.Example;
import org.eclipse.microprofile.openapi.models.headers.Header;
import org.eclipse.microprofile.openapi.models.info.Info;
import org.eclipse.microprofile.openapi.models.links.Link;
import org.eclipse.microprofile.openapi.models.media.Content;
import org.eclipse.microprofile.openapi.models.media.MediaType;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.parameters.RequestBody;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;
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
import org.wildfly.microprofile.openapi.OpenAPIModelProvider;
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
        ServiceDependency<DeploymentInfo> deployment = ServiceDependency.on(UndertowService.deploymentServiceName(unit.getServiceName()).append(UndertowDeploymentInfoService.SERVICE_NAME));

        Supplier<OpenAPIModelProvider> factory = new Supplier<>() {
            @Override
            public OpenAPIModelProvider get() {
                SmallRyeOpenAPI.Builder builder = SmallRyeOpenAPI.builder()
                        // Disable "standard filter" logic so that our filter can run last
                        .enableStandardFilter(false)
                        .withApplicationClassLoader(module.getClassLoader())
                        .withConfig(config)
                        .withIndex(index)
                        .withResourceLocator(resourceResolver)
                        .withScannerClassLoader(WildFlySecurityManager.getClassLoaderPrivileged(AnnotationScanner.class))
                        ;
                // SmallRyeOpenAPI.Builder.build() would otherwise apply the standard filter last
                @SuppressWarnings("deprecation")
                OASFilter filter = OpenApiProcessor.getFilter(openAPIConfig, module.getClassLoader(), index);
                if (filter != null) {
                    builder.addFilter(filter);
                }
                String undertowContextPath = deployment.get().getContextPath();
                // Normalise context path, which should never end in "/"
                String contextPath = undertowContextPath.endsWith("/") ? undertowContextPath.substring(0, undertowContextPath.length() - 1) : undertowContextPath;

                builder.addFilter(new OASFilter() {
                    @Override
                    public APIResponse filterAPIResponse(APIResponse response) {
                        response.setContent(this.filterContent(response.getContent()));
                        return this.filterReference(response);
                    }

                    @Override
                    public Callback filterCallback(Callback callback) {
                        return this.filterReference(callback);
                    }

                    @Override
                    public Header filterHeader(Header header) {
                        for (Map.Entry<String, Example> entry : Optional.ofNullable(header.getExamples()).orElse(Map.of()).entrySet()) {
                            header.addExample(entry.getKey(), this.filterExample(entry.getValue()));
                        }
                        header.setContent(this.filterContent(header.getContent()));
                        return this.filterReference(header);
                    }

                    @Override
                    public Link filterLink(Link link) {
                        this.resolveURI(link::getOperationRef, link::setOperationRef);
                        return this.filterReference(link);
                    }

                    @Override
                    public Parameter filterParameter(Parameter parameter) {
                        for (Map.Entry<String, Example> entry : Optional.ofNullable(parameter.getExamples()).orElse(Map.of()).entrySet()) {
                            parameter.addExample(entry.getKey(), this.filterExample(entry.getValue()));
                        }
                        parameter.setContent(this.filterContent(parameter.getContent()));
                        return this.filterReference(parameter);
                    }

                    @Override
                    public PathItem filterPathItem(PathItem pathItem) {
                        return this.filterReference(pathItem);
                    }

                    @Override
                    public RequestBody filterRequestBody(RequestBody requestBody) {
                        requestBody.setContent(this.filterContent(requestBody.getContent()));
                        return this.filterReference(requestBody);
                    }

                    @Override
                    public Schema filterSchema(Schema schema) {
                        return this.filterReference(schema);
                    }

                    @Override
                    public SecurityScheme filterSecurityScheme(SecurityScheme securityScheme) {
                        return this.filterReference(securityScheme);
                    }

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

                        Components components = model.getComponents();
                        if (components != null) {
                            for (Map.Entry<String, Example> entry : Optional.of(components).map(Components::getExamples).orElse(Map.of()).entrySet()) {
                                components.addExample(entry.getKey(), this.filterExample(entry.getValue()));
                            }
                        }

                        Collection<UndertowListener> listeners = host.get().getServer().getListeners();

                        if (listeners.stream().map(UndertowListener::getProtocol).noneMatch(REQUISITE_LISTENERS::contains)) {
                            LOGGER.requiredListenersNotFound(host.get().getServer().getName(), REQUISITE_LISTENERS);
                        }

                        // N.B. Deprecate this mechanism in favour of host-specific auto-generate-servers property
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

                    private Content filterContent(Content content) {
                        for (Map.Entry<String, MediaType> entry : Optional.ofNullable(content).map(Content::getMediaTypes).orElse(Map.of()).entrySet()) {
                            content.addMediaType(entry.getKey(), this.filterMediaType(entry.getValue()));
                        }
                        return content;
                    }

                    private MediaType filterMediaType(MediaType type) {
                        for (Map.Entry<String, Example> entry : Optional.of(type).map(MediaType::getExamples).orElse(Map.of()).entrySet()) {
                            type.addExample(entry.getKey(), this.filterExample(entry.getValue()));
                        }
                        return type;
                    }

                    private Example filterExample(Example example) {
                        this.resolveURI(example::getExternalValue, example::setExternalValue);
                        return this.filterReference(example);
                    }

                    private <T extends Reference<T>> T filterReference(T reference) {
                        String ref = reference.getRef();
                        if (ref != null) {
                            URI uri = URI.create(ref);
                            // Ignore if ref is a URL
                            if ((uri.getHost() == null) && !uri.getPath().isEmpty()) {
                                this.resolveURI(reference::getRef, reference::setRef);
                            }
                        }
                        return reference;
                    }

                    private void resolveURI(Supplier<String> accessor, Consumer<String> mutator) {
                        // If this application is not deployed to the root context, we need to prepend the context path to any absolute path references.
                        // N.B. Undertow uses "/" for the root context, rather than the more intuitive value of "".
                        if (contextPath.length() > 1) {
                            String ref = accessor.get();
                            if ((ref != null) && ref.startsWith("/")) {
                                mutator.accept(contextPath + ref);
                            }
                        }
                    }
                });
                // If this application is not deployed to the root context, we need to append the context path to all service paths.
                // This ensures that multiple applications sharing the same virtual host can document services with the same path without colliding.
                // N.B. Undertow uses "/" for the root context, rather than the more intuitive value of "".
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
                return OpenAPIModelProvider.of(builder.build().model());
            }
        };
        ServiceInstaller.builder(factory)
                .provides(OpenAPIModelProvider.SERVICE_DESCRIPTOR, serverName, hostName, modelName)
                .requires(List.of(host, deployment))
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
