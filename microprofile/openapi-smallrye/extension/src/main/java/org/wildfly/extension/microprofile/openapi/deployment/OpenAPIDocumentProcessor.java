/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.openapi.deployment;

import java.util.EnumSet;

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

import io.smallrye.openapi.api.OpenApiConfig;

/**
 * Processes the Open API model for a deployment.
 * @author Michael Edgar
 * @author Paul Ferraro
 */
public class OpenAPIDocumentProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext context) throws DeploymentUnitProcessingException {
        DeploymentUnit unit = context.getDeploymentUnit();

        if (DeploymentTypeMarker.isType(DeploymentType.WAR, unit)) {
            OpenAPIModelConfiguration configuration = new DeploymentUnitOpenAPIModelConfiguration(unit);

            if (configuration.isEnabled()) {
                OpenApiConfig config = OpenApiConfig.fromConfig(configuration.getMicroProfileConfig());

                // The MicroProfile OpenAPI specification expects the container to register an OpenAPI endpoint if any of the following conditions are met:
                // * An OASModelReader was configured
                // * An OASFilter was configured
                // * A static OpenAPI file is present
                // * The application contains Jakarta RESTful Web Services
                if ((config.modelReader() != null) || (config.filter() != null) || configuration.getStaticFile().isPresent() || isRestful(unit)) {
                    try {
                        new OpenAPIModelServiceInstaller(configuration).install(context);

                        new OpenAPIHttpHandlerServiceInstaller(configuration).install(context);
                    } catch (DuplicateServiceException e) {
                        // Only one deployment can register the same OpenAPI endpoint with a given host
                        // Let the first one to register win
                        MicroProfileOpenAPILogger.LOGGER.endpointAlreadyRegistered(configuration.getHostName(), unit.getName());
                    }
                }
            } else {
                MicroProfileOpenAPILogger.LOGGER.disabled(unit.getName());
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
