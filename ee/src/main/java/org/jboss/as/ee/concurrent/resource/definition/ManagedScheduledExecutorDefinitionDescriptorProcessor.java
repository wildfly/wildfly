/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.concurrent.resource.definition;

import org.jboss.as.ee.resource.definition.ResourceDefinitionDescriptorProcessor;
import org.jboss.as.ee.resource.definition.ResourceDefinitionInjectionSource;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.metadata.javaee.spec.Environment;
import org.jboss.metadata.javaee.spec.ManagedScheduledExecutorMetaData;
import org.jboss.metadata.javaee.spec.ManagedScheduledExecutorsMetaData;
import org.jboss.metadata.javaee.spec.RemoteEnvironment;

import static org.jboss.as.ee.logging.EeLogger.ROOT_LOGGER;

/**
 * The {@link ResourceDefinitionDescriptorProcessor} for {@link jakarta.enterprise.concurrent.ManagedScheduledExecutorDefinition}.
 * @author emmartins
 */
public class ManagedScheduledExecutorDefinitionDescriptorProcessor extends ResourceDefinitionDescriptorProcessor {

    @Override
    protected void processEnvironment(RemoteEnvironment environment, ResourceDefinitionInjectionSources injectionSources) throws DeploymentUnitProcessingException {
        if (environment instanceof Environment) {
            final ManagedScheduledExecutorsMetaData metaDatas = ((Environment)environment).getManagedScheduledExecutors();
            if (metaDatas != null) {
                for(ManagedScheduledExecutorMetaData metaData : metaDatas) {
                    injectionSources.addResourceDefinitionInjectionSource(getResourceDefinitionInjectionSource(metaData));
                }
            }
        }
    }
    private ResourceDefinitionInjectionSource getResourceDefinitionInjectionSource(final ManagedScheduledExecutorMetaData metaData) {
        final String name = metaData.getName();
        if (name == null || name.isEmpty()) {
            throw ROOT_LOGGER.elementAttributeMissing("<managed-scheduled-executor>", "name");
        }
        final ManagedScheduledExecutorDefinitionInjectionSource resourceDefinitionInjectionSource = new ManagedScheduledExecutorDefinitionInjectionSource(name);
        resourceDefinitionInjectionSource.setContextServiceRef(metaData.getContextServiceRef());
        final Integer hungTaskThreshold = metaData.getHungTaskThreshold();
        if (hungTaskThreshold != null) {
            resourceDefinitionInjectionSource.setHungTaskThreshold(hungTaskThreshold);
        }
        final Integer maxAsync = metaData.getMaxAsync();
        if (maxAsync != null) {
            resourceDefinitionInjectionSource.setMaxAsync(maxAsync);
        }
        // TODO *FOLLOW UP* XML properties are unused, perhaps we should consider those to configure other managed scheduled exec properties we have on server config?
        return resourceDefinitionInjectionSource;
    }
}
