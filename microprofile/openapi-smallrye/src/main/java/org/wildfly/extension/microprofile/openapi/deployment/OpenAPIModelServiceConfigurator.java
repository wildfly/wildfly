/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import io.smallrye.openapi.api.OpenApiConfig;
import io.smallrye.openapi.runtime.OpenApiProcessor;
import io.smallrye.openapi.runtime.OpenApiStaticFile;
import io.smallrye.openapi.runtime.io.Format;
import io.smallrye.openapi.runtime.scanner.FilteredIndexView;
import io.smallrye.openapi.runtime.scanner.spi.AnnotationScanner;
import io.smallrye.openapi.spi.OASFactoryResolverImpl;
import io.undertow.servlet.api.DeploymentInfo;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.info.Info;
import org.eclipse.microprofile.openapi.models.servers.Server;
import org.eclipse.microprofile.openapi.spi.OASFactoryResolver;
import org.jboss.as.network.ClientMapping;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexView;
import org.jboss.metadata.javaee.spec.DescriptionGroupMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.modules.Module;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.vfs.VirtualFile;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.extension.microprofile.openapi.logging.MicroProfileOpenAPILogger;
import org.wildfly.extension.undertow.Capabilities;
import org.wildfly.extension.undertow.Host;
import org.wildfly.extension.undertow.UndertowListener;
import org.wildfly.extension.undertow.UndertowService;
import org.wildfly.extension.undertow.deployment.UndertowDeploymentInfoService;

/**
 * Configures a service that provides an OpenAPI model for a deployment.
 * @author Paul Ferraro
 */
public class OpenAPIModelServiceConfigurator extends SimpleServiceNameProvider implements OpenAPIServiceNameProvider, ServiceConfigurator, Supplier<OpenAPI> {

    private static final String PATH = "mp.openapi.extensions.path";
    private static final String DEFAULT_PATH = "/openapi";
    private static final String RELATIVE_SERVER_URLS = "mp.openapi.extensions.servers.relative";
    private static final String DEFAULT_TITLE = "Generated API";
    private static final Set<String> REQUISITE_LISTENERS = Collections.singleton("http");

    static {
        // Set the static OASFactoryResolver eagerly avoiding the need perform TCCL service loading later
        OASFactoryResolver.setInstance(new OASFactoryResolverImpl());
    }

    private final DeploymentConfiguration configuration;
    private final String deploymentName;
    private final VirtualFile root;
    private final CompositeIndex index;
    private final Module module;
    private final JBossWebMetaData metaData;
    private final SupplierDependency<Host> host;
    private final SupplierDependency<DeploymentInfo> info;

    public OpenAPIModelServiceConfigurator(DeploymentUnit unit, DeploymentConfiguration configuration) {
        super(unit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT).getCapabilityServiceName(Capabilities.CAPABILITY_HOST, configuration.getServerName(), configuration.getHostName()).append(configuration.getProperty(PATH, DEFAULT_PATH)));
        this.deploymentName = unit.getName();
        this.root = unit.getAttachment(Attachments.DEPLOYMENT_ROOT).getRoot();
        // Convert org.jboss.as.server.deployment.annotation.CompositeIndex to org.jboss.jandex.CompositeIndex
        Collection<Index> indexes = new ArrayList<>(unit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX).getIndexes());
        if (unit.getParent() != null) {
            // load all composite indexes of the parent deployment unit
            indexes.addAll(unit.getParent().getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX).getIndexes());
            // load all composite indexes of the parent's accessible sub deployments
            unit.getParent()
                .getAttachment(Attachments.ACCESSIBLE_SUB_DEPLOYMENTS)
                .forEach(subdeployment -> indexes.addAll(
                        subdeployment.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX).getIndexes()));
        }
        this.index = CompositeIndex.create(indexes.stream().map(IndexView.class::cast).collect(Collectors.toList()));
        this.module = unit.getAttachment(Attachments.MODULE);
        this.metaData = unit.getAttachment(WarMetaData.ATTACHMENT_KEY).getMergedJBossWebMetaData();
        this.configuration = configuration;
        this.host = new ServiceSupplierDependency<>(this.getHostServiceName());
        this.info = new ServiceSupplierDependency<>(UndertowService.deploymentServiceName(unit.getServiceName()).append(UndertowDeploymentInfoService.SERVICE_NAME));

        if (!this.getPath().equals(DEFAULT_PATH)) {
            MicroProfileOpenAPILogger.LOGGER.nonStandardEndpoint(unit.getName(), this.getPath(), DEFAULT_PATH);
        }
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceName name = this.getServiceName();
        ServiceBuilder<?> builder = target.addService(name);
        Consumer<OpenAPI> model = new CompositeDependency(this.host, this.info).register(builder).provides(name);
        Service service = new FunctionalService<>(model, Function.identity(), this);
        return builder.setInstance(service);
    }

    @Override
    public OpenAPI get() {
        OpenApiConfig config = this.configuration.getOpenApiConfig();
        IndexView indexView = new FilteredIndexView(this.index, config);

        OpenAPIDocumentBuilder builder = new OpenAPIDocumentBuilder();
        builder.config(config);

        Map.Entry<VirtualFile, Format> entry = this.configuration.getStaticFile();
        if (entry != null) {
            VirtualFile file = entry.getKey();
            Format format = entry.getValue();
            try (OpenApiStaticFile staticFile = new OpenApiStaticFile(file.openStream(), format)) {
                builder.staticFileModel(OpenApiProcessor.modelFromStaticFile(config, staticFile));
            } catch (IOException e) {
                throw MicroProfileOpenAPILogger.LOGGER.failedToLoadStaticFile(e, file.getPathNameRelativeTo(this.root), this.deploymentName);
            }
        }

        // Workaround  for https://github.com/smallrye/smallrye-open-api/issues/1508
        ClassLoader scannerClassLoader = new CompositeClassLoader(List.of(AnnotationScanner.class.getClassLoader(), this.module.getClassLoader()));
        builder.annotationsModel(OpenApiProcessor.modelFromAnnotations(config, scannerClassLoader, indexView));
        builder.readerModel(OpenApiProcessor.modelFromReader(config, this.module.getClassLoader()));
        builder.filter(OpenApiProcessor.getFilter(config, this.module.getClassLoader()));
        OpenAPI model = builder.build();

        // Generate default title and description based on web metadata
        DescriptionGroupMetaData descriptionMetaData = this.metaData.getDescriptionGroup();
        String displayName = (descriptionMetaData != null) ? descriptionMetaData.getDisplayName() : null;
        String title = (displayName != null) ? displayName : this.deploymentName;
        String description = (descriptionMetaData != null) ? descriptionMetaData.getDescription() : null;

        Info info = model.getInfo();
        // Override SmallRye's default title
        if (info.getTitle().equals(DEFAULT_TITLE)) {
            info.setTitle(title);
        }
        if (info.getDescription() == null) {
            info.setDescription(description);
        }

        Host host = this.host.get();
        List<UndertowListener> listeners = host.getServer().getListeners();

        if (model.getServers() == null) {
            // Generate Server entries if none exist
            String contextPath = this.info.get().getContextPath();
            if (this.configuration.getProperty(RELATIVE_SERVER_URLS, Boolean.TRUE).booleanValue()) {
                model.setServers(Collections.singletonList(OASFactory.createServer().url(contextPath)));
            } else {
                int aliases = host.getAllAliases().size();
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
            LOGGER.requiredListenersNotFound(host.getServer().getName(), REQUISITE_LISTENERS);
        }

        return model;
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

    private static class CompositeClassLoader extends ClassLoader {
        private final List<ClassLoader> loaders;

        CompositeClassLoader(List<ClassLoader> loaders) {
            this.loaders = loaders;
        }

        @Override
        public Enumeration<URL> getResources(String name) throws IOException {
            List<URL> result = new LinkedList<>();
            for (ClassLoader loader : this.loaders) {
                result.addAll(Collections.list(loader.getResources(name)));
            }
            return Collections.enumeration(result);
        }

        @Override
        protected URL findResource(String name) {
            for (ClassLoader loader : this.loaders) {
                URL url = loader.getResource(name);
                if (url != null) {
                    return url;
                }
            }
            return super.findResource(name);
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            for (ClassLoader loader : this.loaders) {
                try {
                    return loader.loadClass(name);
                } catch (ClassNotFoundException e) {
                    // try again
                }
            }
            return super.findClass(name);
        }
    }
}
