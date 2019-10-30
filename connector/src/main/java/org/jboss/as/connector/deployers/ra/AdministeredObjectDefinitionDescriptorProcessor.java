/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.connector.deployers.ra;

import org.jboss.as.ee.resource.definition.ResourceDefinitionDescriptorProcessor;
import org.jboss.as.ee.resource.definition.ResourceDefinitionInjectionSource;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.metadata.javaee.spec.AdministeredObjectMetaData;
import org.jboss.metadata.javaee.spec.AdministeredObjectsMetaData;
import org.jboss.metadata.javaee.spec.RemoteEnvironment;

import static org.jboss.as.ee.logging.EeLogger.ROOT_LOGGER;

/**
 * Deployment processor responsible for processing administered-object deployment descriptor elements
 *
 * @author Eduardo Martins
 */
public class AdministeredObjectDefinitionDescriptorProcessor extends ResourceDefinitionDescriptorProcessor {

    @Override
    protected void processEnvironment(RemoteEnvironment environment, ResourceDefinitionInjectionSources injectionSources) throws DeploymentUnitProcessingException {
        final AdministeredObjectsMetaData metaDatas = environment.getAdministeredObjects();
        if (metaDatas != null) {
            for(AdministeredObjectMetaData metaData : metaDatas) {
                injectionSources.addResourceDefinitionInjectionSource(getResourceDefinitionInjectionSource(metaData));
            }
        }
    }

    private ResourceDefinitionInjectionSource getResourceDefinitionInjectionSource(final AdministeredObjectMetaData metaData) {
        final String name = metaData.getName();
        if (name == null || name.isEmpty()) {
            throw ROOT_LOGGER.elementAttributeMissing("<administered-object>", "name");
        }
        final String className = metaData.getClassName();
        if (className == null || className.isEmpty()) {
            throw ROOT_LOGGER.elementAttributeMissing("<administered-object>", "className");
        }
        final String resourceAdapter = metaData.getResourceAdapter();
        if (resourceAdapter == null || resourceAdapter.isEmpty()) {
            throw ROOT_LOGGER.elementAttributeMissing("<administered-object>", "resourceAdapter");
        }
        final AdministeredObjectDefinitionInjectionSource resourceDefinitionInjectionSource = new AdministeredObjectDefinitionInjectionSource(name, className, resourceAdapter);
        resourceDefinitionInjectionSource.setInterface(metaData.getInterfaceName());
        if (metaData.getDescriptions() != null) {
            resourceDefinitionInjectionSource.setDescription(metaData.getDescriptions().toString());
        }
        resourceDefinitionInjectionSource.addProperties(metaData.getProperties());
        return resourceDefinitionInjectionSource;
    }

}
