/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.ee.concurrent.resource.definition;

import org.jboss.as.ee.resource.definition.ResourceDefinitionDescriptorProcessor;
import org.jboss.as.ee.resource.definition.ResourceDefinitionInjectionSource;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.metadata.javaee.spec.Environment;
import org.jboss.metadata.javaee.spec.ManagedExecutorMetaData;
import org.jboss.metadata.javaee.spec.ManagedExecutorsMetaData;
import org.jboss.metadata.javaee.spec.RemoteEnvironment;

import static org.jboss.as.ee.logging.EeLogger.ROOT_LOGGER;

/**
 * The {@link ResourceDefinitionDescriptorProcessor} for {@link jakarta.enterprise.concurrent.ManagedExecutorDefinition}.
 * @author emmartins
 */
public class ManagedExecutorDefinitionDescriptorProcessor extends ResourceDefinitionDescriptorProcessor {

    @Override
    protected void processEnvironment(RemoteEnvironment environment, ResourceDefinitionInjectionSources injectionSources) throws DeploymentUnitProcessingException {
        if (environment instanceof Environment) {
            final ManagedExecutorsMetaData metaDatas = ((Environment)environment).getManagedExecutors();
            if (metaDatas != null) {
                for(ManagedExecutorMetaData metaData : metaDatas) {
                    injectionSources.addResourceDefinitionInjectionSource(getResourceDefinitionInjectionSource(metaData));
                }
            }
        }
    }

    private ResourceDefinitionInjectionSource getResourceDefinitionInjectionSource(final ManagedExecutorMetaData metaData) {
        final String name = metaData.getName();
        if (name == null || name.isEmpty()) {
            throw ROOT_LOGGER.elementAttributeMissing("<managed-executor>", "name");
        }
        final ManagedExecutorDefinitionInjectionSource resourceDefinitionInjectionSource = new ManagedExecutorDefinitionInjectionSource(name);
        resourceDefinitionInjectionSource.setContextServiceRef(metaData.getContextServiceRef());
        final Integer hungTaskThreshold = metaData.getHungTaskThreshold();
        if (hungTaskThreshold != null) {
            resourceDefinitionInjectionSource.setHungTaskThreshold(hungTaskThreshold);
        }
        final Integer maxAsync = metaData.getMaxAsync();
        if (maxAsync != null) {
            resourceDefinitionInjectionSource.setMaxAsync(maxAsync);
        }
        // TODO *FOLLOW UP* XML properties are unused, perhaps we should consider those to configure other managed exec properties we have on server config?
        return resourceDefinitionInjectionSource;
    }
}
