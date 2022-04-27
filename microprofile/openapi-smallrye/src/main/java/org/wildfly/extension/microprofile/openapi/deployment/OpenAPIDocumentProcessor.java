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

import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.jaxrs.JaxrsAnnotations;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.DuplicateServiceException;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.vfs.VirtualFile;
import org.wildfly.extension.microprofile.openapi.logging.MicroProfileOpenAPILogger;
import org.wildfly.extension.undertow.DeploymentDefinition;
import org.wildfly.extension.undertow.UndertowExtension;

import io.smallrye.openapi.api.OpenApiConfig;
import io.smallrye.openapi.api.OpenApiConfigImpl;
import io.smallrye.openapi.runtime.io.Format;

/**
 * Processes the Open API model for a deployment.
 * @author Michael Edgar
 * @author Paul Ferraro
 */
public class OpenAPIDocumentProcessor implements DeploymentUnitProcessor {

    private static final String ENABLED = "mp.openapi.extensions.enabled";

    @Override
    public void deploy(DeploymentPhaseContext context) throws DeploymentUnitProcessingException {
        DeploymentUnit unit = context.getDeploymentUnit();

        if (DeploymentTypeMarker.isType(DeploymentType.WAR, unit)) {
            DeploymentConfiguration configuration = new DeploymentOpenAPIConfiguration(unit);
            OpenApiConfig config = configuration.getOpenApiConfig();

            if (!configuration.getProperty(ENABLED, Boolean.TRUE).booleanValue()) {
                MicroProfileOpenAPILogger.LOGGER.disabled(unit.getName());
                return;
            }

            // The MicroProfile OpenAPI specification expects the container to register an OpenAPI endpoint if any of the following conditions are met:
            // * An OASModelReader was configured
            // * An OASFilter was configured
            // * A static OpenAPI file is present
            // * Application contains Jakarta RESTful Web Services
            if ((config.modelReader() != null) || (config.filter() != null) || (configuration.getStaticFile() != null) || isRestful(unit)) {
                ServiceTarget target = context.getServiceTarget();
                OpenAPIModelServiceConfigurator configurator = new OpenAPIModelServiceConfigurator(unit, configuration);
                ServiceName modelServiceName = configurator.getServiceName();

                try {
                    // Only one deployment can register the same OpenAPI endpoint with a given host
                    // Let the first one to register win
                    if (context.getServiceRegistry().getService(modelServiceName) != null) {
                        throw new DuplicateServiceException(modelServiceName.getCanonicalName());
                    }

                    configurator.build(target).install();

                    new OpenAPIHttpHandlerServiceConfigurator(configurator).build(target).install();
                } catch (DuplicateServiceException e) {
                    MicroProfileOpenAPILogger.LOGGER.endpointAlreadyRegistered(configuration.getHostName(), unit.getName());
                }
            }
        }
    }

    private static boolean isRestful(DeploymentUnit unit) {
        CompositeIndex index = unit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
        for (JaxrsAnnotations annotation : EnumSet.allOf(JaxrsAnnotations.class)) {
            if (!index.getAnnotations(annotation.getDotName()).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static class DeploymentOpenAPIConfiguration implements DeploymentConfiguration {

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

        private static Map.Entry<VirtualFile, Format> findStaticFile(VirtualFile root) {
            // Format search order
            for (Format format : EnumSet.of(Format.YAML, Format.JSON)) {
                for (String resource : STATIC_FILES.get(format)) {
                    VirtualFile file = root.getChild(resource);
                    if (file.exists()) {
                        return Map.entry(file, format);
                    }
                }
            }
            return null;
        }

        private final Config config;
        private final OpenApiConfig openApiConfig;
        private final Map.Entry<VirtualFile, Format> staticFile;
        private final String serverName;
        private final String hostName;

        DeploymentOpenAPIConfiguration(DeploymentUnit unit) {
            this.config = ConfigProvider.getConfig(unit.getAttachment(Attachments.MODULE).getClassLoader());
            this.openApiConfig = OpenApiConfigImpl.fromConfig(this.config);
            this.staticFile = findStaticFile(unit.getAttachment(Attachments.DEPLOYMENT_ROOT).getRoot());
            // Fetch server/host as determined by Undertow DUP
            ModelNode model = unit.getAttachment(Attachments.DEPLOYMENT_RESOURCE_SUPPORT).getDeploymentSubsystemModel(UndertowExtension.SUBSYSTEM_NAME);
            this.serverName = model.get(DeploymentDefinition.SERVER.getName()).asString();
            this.hostName = model.get(DeploymentDefinition.VIRTUAL_HOST.getName()).asString();
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T getProperty(String name, T defaultValue) {
            return this.config.getOptionalValue(name, (Class<T>) defaultValue.getClass()).orElse(defaultValue);
        }

        @Override
        public OpenApiConfig getOpenApiConfig() {
            return this.openApiConfig;
        }

        @Override
        public Map.Entry<VirtualFile, Format> getStaticFile() {
            return this.staticFile;
        }

        @Override
        public String getServerName() {
            return this.serverName;
        }

        @Override
        public String getHostName() {
            return this.hostName;
        }
    }
}
