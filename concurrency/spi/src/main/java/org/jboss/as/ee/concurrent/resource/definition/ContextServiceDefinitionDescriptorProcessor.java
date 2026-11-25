/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.concurrent.resource.definition;

import org.jboss.as.ee.concurrent.ContextServiceTypesConfiguration;
import org.jboss.as.ee.resource.definition.ResourceDefinitionDescriptorProcessor;
import org.jboss.as.ee.resource.definition.ResourceDefinitionInjectionSource;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.metadata.javaee.spec.ContextServiceMetaData;
import org.jboss.metadata.javaee.spec.ContextServicesMetaData;
import org.jboss.metadata.javaee.spec.Environment;
import org.jboss.metadata.javaee.spec.RemoteEnvironment;

import static org.jboss.as.ee.logging.EeLogger.ROOT_LOGGER;

/**
 * The {@link ResourceDefinitionDescriptorProcessor} for {@link jakarta.enterprise.concurrent.ContextServiceDefinition}.
 * @author emmartins
 */
public class ContextServiceDefinitionDescriptorProcessor extends ResourceDefinitionDescriptorProcessor {

    @Override
    protected void processEnvironment(RemoteEnvironment environment, ResourceDefinitionInjectionSources injectionSources) throws DeploymentUnitProcessingException {
        if (environment instanceof Environment) {
            final ContextServicesMetaData metaDatas = ((Environment)environment).getContextServices();
            if (metaDatas != null) {
                for(ContextServiceMetaData metaData : metaDatas) {
                    injectionSources.addResourceDefinitionInjectionSource(getResourceDefinitionInjectionSource(metaData));
                }
            }
        }
    }

    private ResourceDefinitionInjectionSource getResourceDefinitionInjectionSource(final ContextServiceMetaData metaData) {
        final String name = metaData.getName();
        if (name == null || name.isEmpty()) {
            throw ROOT_LOGGER.elementAttributeMissing("<context-service>", "name");
        }
        final ContextServiceTypesConfiguration contextServiceTypesConfiguration = new ContextServiceTypesConfiguration.Builder()
                .setCleared(metaData.getCleared())
                .setPropagated(metaData.getPropagated())
                .setUnchanged(metaData.getUnchanged())
                .build();
        return new ContextServiceDefinitionInjectionSource(name, contextServiceTypesConfiguration);
    }
}
