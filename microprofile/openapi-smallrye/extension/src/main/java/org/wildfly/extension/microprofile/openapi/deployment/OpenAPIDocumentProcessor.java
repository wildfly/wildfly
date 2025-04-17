/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.openapi.deployment;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Supplier;

import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.jaxrs.JaxrsAnnotations;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.msc.service.DuplicateServiceException;
import org.wildfly.extension.microprofile.openapi.logging.MicroProfileOpenAPILogger;
import org.wildfly.microprofile.openapi.OpenAPIModelConfiguration;
import org.wildfly.microprofile.openapi.OpenAPIProvider;
import org.wildfly.microprofile.openapi.OpenAPIRegistry;
import org.wildfly.microprofile.openapi.host.HostOpenAPIModelConfiguration;
import org.wildfly.microprofile.openapi.host.OpenAPIHttpHandlerServiceInstaller;
import org.wildfly.service.Installer.StartWhen;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

import io.smallrye.openapi.api.OpenApiConfig;

/**
 * Processes the OpenAPI model for a deployment.
 * @author Michael Edgar
 * @author Paul Ferraro
 */
public class OpenAPIDocumentProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext context) throws DeploymentUnitProcessingException {
        DeploymentUnit unit = context.getDeploymentUnit();

        if (DeploymentTypeMarker.isType(DeploymentType.WAR, unit)) {
            DeploymentOpenAPIModelConfiguration configuration = new DeploymentUnitOpenAPIModelConfiguration(unit);
            OpenAPIModelConfiguration hostConfiguration = new HostOpenAPIModelConfiguration(configuration.getServerName(), configuration.getHostName());

            if (configuration.isEnabled()) {
                OpenApiConfig config = OpenApiConfig.fromConfig(configuration.getMicroProfileConfig());

                // The MicroProfile OpenAPI specification expects the container to register an OpenAPI endpoint if any of the following conditions are met:
                // * An OASModelReader was configured
                // * An OASFilter was configured
                // * A static OpenAPI file is present
                // * The application contains Jakarta RESTful Web Services
                if ((config.modelReader() != null) || (config.filter() != null) || configuration.getStaticFile().isPresent() || isRestful(unit)) {
                    try {
                        new DeploymentOpenAPIProviderServiceInstaller(configuration).install(context);

                        if (!hostConfiguration.getPath().equals(configuration.getPath())) {
                            // If this is a non-standard endpoint for this host, register a handler
                            MicroProfileOpenAPILogger.LOGGER.nonStandardEndpoint(configuration.getModelName(), configuration.getPath(), hostConfiguration.getPath());
                            new OpenAPIHttpHandlerServiceInstaller(configuration).install(context);
                        } else {
                            // Otherwise, register the deployment model with the registry for this host
                            String serverName = configuration.getServerName();
                            String hostName = configuration.getHostName();
                            String modelName = configuration.getModelName();
                            ServiceDependency<OpenAPIProvider> provider = ServiceDependency.on(OpenAPIProvider.SERVICE_DESCRIPTOR, serverName, hostName, modelName);
                            ServiceDependency<OpenAPIRegistry> registry = ServiceDependency.on(OpenAPIRegistry.SERVICE_DESCRIPTOR, serverName, hostName);
                            Supplier<OpenAPIRegistry.Registration> factory = () -> registry.get().register(modelName, provider.get().get());
                            ServiceInstaller.builder(factory)
                                    .requires(List.of(provider, registry))
                                    .startWhen(StartWhen.INSTALLED)
                                    .onStop(OpenAPIRegistry.Registration::close)
                                    .build()
                                    .install(context);
                        }
                    } catch (DuplicateServiceException e) {
                        // Only one deployment can register the same OpenAPI endpoint with a given host
                        // Let the first one to register win
                        MicroProfileOpenAPILogger.LOGGER.endpointAlreadyRegistered(configuration.getHostName(), configuration.getModelName());
                    }
                }
            } else {
                MicroProfileOpenAPILogger.LOGGER.disabled(configuration.getModelName());
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
}
