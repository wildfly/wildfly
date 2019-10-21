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
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.dmr.ModelNode;
import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexView;
import org.jboss.modules.Module;
import org.jboss.msc.Service;
import org.jboss.msc.service.DuplicateServiceException;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.vfs.VirtualFile;
import org.wildfly.extension.microprofile.openapi.logging.MicroProfileOpenAPILogger;
import org.wildfly.extension.undertow.Capabilities;
import org.wildfly.extension.undertow.DeploymentDefinition;
import org.wildfly.extension.undertow.Host;
import org.wildfly.extension.undertow.UndertowExtension;
import org.wildfly.security.manager.WildFlySecurityManager;

import io.smallrye.openapi.api.OpenApiConfig;
import io.smallrye.openapi.api.OpenApiConfigImpl;
import io.smallrye.openapi.runtime.OpenApiProcessor;
import io.smallrye.openapi.runtime.OpenApiStaticFile;
import io.smallrye.openapi.runtime.io.OpenApiSerializer.Format;
import io.smallrye.openapi.runtime.scanner.FilteredIndexView;
import io.undertow.server.HttpHandler;

/**
 * Processes the Open API model for a deployment.
 * @author Michael Edgar
 * @author Paul Ferraro
 */
public class OpenAPIDocumentProcessor implements DeploymentUnitProcessor {

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
    }

    @Override
    public void deploy(DeploymentPhaseContext context) throws DeploymentUnitProcessingException {
        DeploymentUnit unit = context.getDeploymentUnit();

        if (DeploymentTypeMarker.isType(DeploymentType.WAR, unit)) {
            // Fetch server/host as determined by Undertow DUP
            ModelNode model = unit.getAttachment(Attachments.DEPLOYMENT_RESOURCE_SUPPORT).getDeploymentSubsystemModel(UndertowExtension.SUBSYSTEM_NAME);
            String serverName = model.get(DeploymentDefinition.SERVER.getName()).asString();
            String hostName = model.get(DeploymentDefinition.VIRTUAL_HOST.getName()).asString();

            CapabilityServiceSupport support = unit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);
            ServiceName hostServiceName = support.getCapabilityServiceName(Capabilities.CAPABILITY_HOST, serverName, hostName);
            ServiceName serviceName = hostServiceName.append("openapi");

            try {
                // Only one deployment can register the OpenAPI endpoint with a given host
                // Let the first one to register win
                if (context.getServiceRegistry().getService(serviceName) != null) {
                    throw new DuplicateServiceException(serviceName.getCanonicalName());
                }

                ServiceBuilder<?> builder = context.getServiceTarget().addService(serviceName);
                Supplier<Host> host = builder.requires(hostServiceName);

                HttpHandler handler = new OpenAPIHttpHandler(createOpenAPIModel(unit));
                Service service = new OpenAPIHttpHandlerService(host, handler);

                builder.setInstance(service).install();
            } catch (DuplicateServiceException e) {
                MicroProfileOpenAPILogger.LOGGER.endpointAlreadyRegistered(hostName, unit.getName());
            }
        }
    }

    @Override
    public void undeploy(DeploymentUnit unit) {
    }

    private static OpenAPI createOpenAPIModel(DeploymentUnit unit) throws DeploymentUnitProcessingException {
        Module module = unit.getAttachment(Attachments.MODULE);
        ClassLoader loader = module.getClassLoader();

        OpenApiConfig config = new OpenApiConfigImpl(ConfigProvider.getConfig(loader));

        VirtualFile root = unit.getAttachment(Attachments.DEPLOYMENT_ROOT).getRoot();
        // Convert org.jboss.as.server.deployment.annotation.CompositeIndex to org.jboss.jandex.CompositeIndex
        Collection<Index> indexes = unit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX).getIndexes();
        CompositeIndex index = CompositeIndex.create(indexes.stream().map(IndexView.class::cast).collect(Collectors.toList()));
        IndexView indexView = new FilteredIndexView(index, config);

        OpenAPIDocumentBuilder builder = new OpenAPIDocumentBuilder();
        builder.archiveName(unit.getName());
        builder.config(config);

        Map.Entry<VirtualFile, Format> entry = findStaticFile(root);
        if (entry != null) {
            VirtualFile file = entry.getKey();
            Format format = entry.getValue();
            try (OpenApiStaticFile staticFile = new OpenApiStaticFile(file.openStream(), format)) {
                builder.staticFileModel(OpenApiProcessor.modelFromStaticFile(staticFile));
            } catch (IOException e) {
                throw MicroProfileOpenAPILogger.LOGGER.failedToLoadStaticFile(e, file.getPathNameRelativeTo(root), unit.getName());
            }
        }

        builder.annotationsModel(OpenApiProcessor.modelFromAnnotations(config, indexView));
        builder.readerModel(OpenApiProcessor.modelFromReader(config, loader));
        builder.filter(OpenApiProcessor.getFilter(config, loader));

        // OASFactoryResolver service loading requires TCCL
        ClassLoader existingLoader = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(OpenApiConfig.class.getClassLoader());
        try {
            return builder.build();
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(existingLoader);
        }
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
