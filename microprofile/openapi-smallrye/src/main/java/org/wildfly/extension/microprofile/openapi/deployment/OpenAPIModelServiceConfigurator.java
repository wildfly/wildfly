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

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.spi.OASFactoryResolver;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexView;
import org.jboss.modules.Module;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.vfs.VirtualFile;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.extension.microprofile.openapi.logging.MicroProfileOpenAPILogger;
import org.wildfly.extension.undertow.Capabilities;

import io.smallrye.openapi.api.OpenApiConfig;
import io.smallrye.openapi.api.OpenApiConfigImpl;
import io.smallrye.openapi.runtime.OpenApiProcessor;
import io.smallrye.openapi.runtime.OpenApiStaticFile;
import io.smallrye.openapi.runtime.io.OpenApiSerializer.Format;
import io.smallrye.openapi.runtime.scanner.FilteredIndexView;
import io.smallrye.openapi.spi.OASFactoryResolverImpl;

/**
 * Configures a service that provides an OpenAPI model for a deployment.
 * @author Paul Ferraro
 */
public class OpenAPIModelServiceConfigurator extends SimpleServiceNameProvider implements OpenAPIServiceNameProvider, ServiceConfigurator, Supplier<OpenAPI> {

    private static final String PATH = "mp.openapi.extensions.path";
    private static final String DEFAULT_PATH = "/openapi";

    private static final Map<Format, List<String>> STATIC_FILES = new EnumMap<>(Format.class);
    static {
        // Order resource names by search order
        STATIC_FILES.put(Format.YAML, Arrays.asList(
                "/META-INF/openapi.yaml",
                "/WEB-INF/classes/META-INF/openapi.yaml",
                "/META-INF/openapi.yml",
                "/WEB-INF/classes/META-INF/openapi.yml"));
        STATIC_FILES.put(Format.JSON, Arrays.asList(
                "/META-INF/openapi.json",
                "/WEB-INF/classes/META-INF/openapi.json"));

        // Set the static OASFactoryResolver eagerly avoiding the need perform TCCL service loading later
        OASFactoryResolver.setInstance(new OASFactoryResolverImpl());
    }

    private final Config config;
    private final String deploymentName;
    private final VirtualFile root;
    private final CompositeIndex index;
    private final Module module;

    public OpenAPIModelServiceConfigurator(DeploymentUnit unit, String serverName, String hostName, Config config) {
        super(unit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT).getCapabilityServiceName(Capabilities.CAPABILITY_HOST, serverName, hostName).append(config.getOptionalValue(PATH, String.class).orElse(DEFAULT_PATH)));
        this.deploymentName = unit.getName();
        this.root = unit.getAttachment(Attachments.DEPLOYMENT_ROOT).getRoot();
        // Convert org.jboss.as.server.deployment.annotation.CompositeIndex to org.jboss.jandex.CompositeIndex
        Collection<Index> indexes = unit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX).getIndexes();
        this.index = CompositeIndex.create(indexes.stream().map(IndexView.class::cast).collect(Collectors.toList()));
        this.module = unit.getAttachment(Attachments.MODULE);
        this.config = config;

        if (!this.getPath().equals(DEFAULT_PATH)) {
            MicroProfileOpenAPILogger.LOGGER.nonStandardEndpoint(unit.getName(), this.getPath(), DEFAULT_PATH);
        }
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceName name = this.getServiceName();
        ServiceBuilder<?> builder = target.addService(name);
        Consumer<OpenAPI> model = builder.provides(name);
        Service service = new FunctionalService<>(model, Function.identity(), this);
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }

    @Override
    public OpenAPI get() {
        OpenApiConfig config = new OpenApiConfigImpl(this.config);
        IndexView indexView = new FilteredIndexView(this.index, config);

        OpenAPIDocumentBuilder builder = new OpenAPIDocumentBuilder();
        builder.config(config);

        Map.Entry<VirtualFile, Format> entry = findStaticFile(this.root);
        if (entry != null) {
            VirtualFile file = entry.getKey();
            Format format = entry.getValue();
            try (OpenApiStaticFile staticFile = new OpenApiStaticFile(file.openStream(), format)) {
                builder.staticFileModel(OpenApiProcessor.modelFromStaticFile(staticFile));
            } catch (IOException e) {
                throw MicroProfileOpenAPILogger.LOGGER.failedToLoadStaticFile(e, file.getPathNameRelativeTo(this.root), this.deploymentName);
            }
        }

        builder.annotationsModel(OpenApiProcessor.modelFromAnnotations(config, indexView));
        builder.readerModel(OpenApiProcessor.modelFromReader(config, this.module.getClassLoader()));
        builder.filter(OpenApiProcessor.getFilter(config, this.module.getClassLoader()));
        return builder.build();
    }

    private static Map.Entry<VirtualFile, Format> findStaticFile(VirtualFile root) {
        // Format search order
        for (Format format : EnumSet.of(Format.YAML, Format.JSON)) {
            for (String resource : STATIC_FILES.get(format)) {
                VirtualFile file = root.getChild(resource);
                if (file.exists()) {
                    return new AbstractMap.SimpleImmutableEntry<>(file, format);
                }
            }
        }
        return null;
    }
}
