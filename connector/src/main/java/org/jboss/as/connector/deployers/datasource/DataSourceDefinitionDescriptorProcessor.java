/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.deployers.datasource;

import org.jboss.as.ee.resource.definition.ResourceDefinitionDescriptorProcessor;
import org.jboss.as.ee.resource.definition.ResourceDefinitionInjectionSource;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.metadata.javaee.spec.DataSourceMetaData;
import org.jboss.metadata.javaee.spec.DataSourcesMetaData;
import org.jboss.metadata.javaee.spec.RemoteEnvironment;

import static org.jboss.as.ee.logging.EeLogger.ROOT_LOGGER;

/**
 * Deployment processor responsible for processing data-source deployment descriptor elements
 *
 * @author Stuart Douglas
 * @author Eduardo Martins
 */
public class DataSourceDefinitionDescriptorProcessor extends ResourceDefinitionDescriptorProcessor {

    @Override
    protected void processEnvironment(RemoteEnvironment environment, ResourceDefinitionInjectionSources injectionSources) throws DeploymentUnitProcessingException {
        final DataSourcesMetaData metaDatas = environment.getDataSources();
        if (metaDatas != null) {
            for(DataSourceMetaData metaData : metaDatas) {
                injectionSources.addResourceDefinitionInjectionSource(getResourceDefinitionInjectionSource(metaData));
            }
        }
    }

    private ResourceDefinitionInjectionSource getResourceDefinitionInjectionSource(final DataSourceMetaData metaData) {
        final String name = metaData.getName();
        if (name == null || name.isEmpty()) {
            throw ROOT_LOGGER.elementAttributeMissing("<data-source>", "name");
        }
        final DataSourceDefinitionInjectionSource resourceDefinitionInjectionSource = new DataSourceDefinitionInjectionSource(name);
        final String className = metaData.getClassName();
        if (className == null || className.equals(Object.class.getName())) {
            throw ROOT_LOGGER.elementAttributeMissing("<data-source>", "className");
        }
        resourceDefinitionInjectionSource.setClassName(className);
        resourceDefinitionInjectionSource.setDatabaseName(metaData.getDatabaseName());
        if (metaData.getDescriptions() != null) {
            resourceDefinitionInjectionSource.setDescription(metaData.getDescriptions().toString());
        }
        resourceDefinitionInjectionSource.setInitialPoolSize(metaData.getInitialPoolSize());
        if (metaData.getIsolationLevel() != null) {
            resourceDefinitionInjectionSource.setIsolationLevel(metaData.getIsolationLevel().ordinal());
        }
        resourceDefinitionInjectionSource.setLoginTimeout(metaData.getLoginTimeout());
        resourceDefinitionInjectionSource.setMaxIdleTime(metaData.getMaxIdleTime());
        resourceDefinitionInjectionSource.setMaxStatements(metaData.getMaxStatements());
        resourceDefinitionInjectionSource.setMaxPoolSize(metaData.getMaxPoolSize());
        resourceDefinitionInjectionSource.setMinPoolSize(metaData.getMinPoolSize());
        resourceDefinitionInjectionSource.setInitialPoolSize(metaData.getInitialPoolSize());
        resourceDefinitionInjectionSource.setPassword(metaData.getPassword());
        resourceDefinitionInjectionSource.setPortNumber(metaData.getPortNumber());
        resourceDefinitionInjectionSource.addProperties(metaData.getProperties());
        resourceDefinitionInjectionSource.setServerName(metaData.getServerName());
        resourceDefinitionInjectionSource.setTransactional(metaData.getTransactional());
        resourceDefinitionInjectionSource.setUrl(metaData.getUrl());
        resourceDefinitionInjectionSource.setUser(metaData.getUser());
        return resourceDefinitionInjectionSource;
    }

}
