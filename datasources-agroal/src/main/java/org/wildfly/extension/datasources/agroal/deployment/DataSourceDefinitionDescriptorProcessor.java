/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.datasources.agroal.deployment;

import org.jboss.as.ee.resource.definition.ResourceDefinitionDescriptorProcessor;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.metadata.javaee.spec.DataSourceMetaData;
import org.jboss.metadata.javaee.spec.DataSourcesMetaData;
import org.jboss.metadata.javaee.spec.RemoteEnvironment;
import org.wildfly.extension.datasources.agroal.logging.AgroalLogger;

/**
 * Processor of the data-source element in ejb-jar.xml
 *
 * @author <a href="lbarreiro@redhat.com">Luis Barreiro</a>
 */
public class DataSourceDefinitionDescriptorProcessor extends ResourceDefinitionDescriptorProcessor {

    @Override
    protected void processEnvironment(RemoteEnvironment environment, ResourceDefinitionDescriptorProcessor.ResourceDefinitionInjectionSources injectionSources) throws DeploymentUnitProcessingException {
        DataSourcesMetaData dataSources = environment.getDataSources();
        if (dataSources != null) {
            for (DataSourceMetaData dataSource : dataSources) {

                if (dataSource.getName() == null || dataSource.getName().isEmpty()) {
                    throw AgroalLogger.SERVICE_LOGGER.missingAttributeInDatasourceMetadata("name");
                }
                if (dataSource.getClassName() == null || dataSource.getClassName().isEmpty()) {
                    throw AgroalLogger.SERVICE_LOGGER.missingAttributeInDatasourceMetadata("className");
                }

                DataSourceDefinitionInjectionSource injectionSource = new DataSourceDefinitionInjectionSource(dataSource.getName());

                injectionSource.setClassName(dataSource.getClassName());
                injectionSource.setDatabaseName(dataSource.getDatabaseName());
                injectionSource.setInitialPoolSize(dataSource.getInitialPoolSize());
                injectionSource.setLoginTimeout(dataSource.getLoginTimeout());
                injectionSource.setMaxIdleTime(dataSource.getMaxIdleTime());
                injectionSource.setMaxStatements(dataSource.getMaxStatements());
                injectionSource.setMaxPoolSize(dataSource.getMaxPoolSize());
                injectionSource.setMinPoolSize(dataSource.getMinPoolSize());
                injectionSource.setPassword(dataSource.getPassword());
                injectionSource.setPortNumber(dataSource.getPortNumber());
                injectionSource.addProperties(dataSource.getProperties());
                injectionSource.setServerName(dataSource.getServerName());
                injectionSource.setTransactional(dataSource.getTransactional());
                injectionSource.setUrl(dataSource.getUrl());
                injectionSource.setUser(dataSource.getUser());

                if (dataSource.getDescriptions() != null) {
                    injectionSource.setDescription(dataSource.getDescriptions().toString());
                }
                if (dataSource.getIsolationLevel() != null) {
                    injectionSource.setIsolationLevel(dataSource.getIsolationLevel().ordinal());
                }

                injectionSources.addResourceDefinitionInjectionSource(injectionSource);
            }
        }
    }
}
