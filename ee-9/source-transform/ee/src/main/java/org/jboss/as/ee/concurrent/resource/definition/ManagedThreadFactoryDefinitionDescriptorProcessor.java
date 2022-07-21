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
import org.jboss.metadata.javaee.spec.ManagedThreadFactoriesMetaData;
import org.jboss.metadata.javaee.spec.ManagedThreadFactoryMetaData;
import org.jboss.metadata.javaee.spec.RemoteEnvironment;

import static org.jboss.as.ee.logging.EeLogger.ROOT_LOGGER;

/**
 * The {@link ResourceDefinitionDescriptorProcessor} for {@link jakarta.enterprise.concurrent.ManagedThreadFactoryDefinition}.
 * @author emmartins
 */
public class ManagedThreadFactoryDefinitionDescriptorProcessor extends ResourceDefinitionDescriptorProcessor {

    @Override
    protected void processEnvironment(RemoteEnvironment environment, ResourceDefinitionInjectionSources injectionSources) throws DeploymentUnitProcessingException {
        if (environment instanceof Environment) {
            final ManagedThreadFactoriesMetaData metaDatas = ((Environment)environment).getManagedThreadFactories();
            if (metaDatas != null) {
                for(ManagedThreadFactoryMetaData metaData : metaDatas) {
                    injectionSources.addResourceDefinitionInjectionSource(getResourceDefinitionInjectionSource(metaData));
                }
            }
        }
    }

    private ResourceDefinitionInjectionSource getResourceDefinitionInjectionSource(final ManagedThreadFactoryMetaData metaData) {
        final String name = metaData.getName();
        if (name == null || name.isEmpty()) {
            throw ROOT_LOGGER.elementAttributeMissing("<managed-thread-factory>", "name");
        }
        final ManagedThreadFactoryDefinitionInjectionSource resourceDefinitionInjectionSource = new ManagedThreadFactoryDefinitionInjectionSource(name);
        resourceDefinitionInjectionSource.setContextServiceRef(metaData.getContextServiceRef());
        final Integer priority = metaData.getPriority();
        if (priority != null) {
            resourceDefinitionInjectionSource.setPriority(priority);
        }
        return resourceDefinitionInjectionSource;
    }
}
