/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.deployers.ra;

import org.jboss.as.ee.resource.definition.ResourceDefinitionDescriptorProcessor;
import org.jboss.as.ee.resource.definition.ResourceDefinitionInjectionSource;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.metadata.javaee.spec.ConnectionFactoriesMetaData;
import org.jboss.metadata.javaee.spec.ConnectionFactoryMetaData;
import org.jboss.metadata.javaee.spec.RemoteEnvironment;

import jakarta.resource.spi.TransactionSupport;

import static org.jboss.as.ee.logging.EeLogger.ROOT_LOGGER;

/**
 * Deployment processor responsible for processing connection-factory deployment descriptor elements
 *
 * @author Eduardo Martins
 */
public class ConnectionFactoryDefinitionDescriptorProcessor extends ResourceDefinitionDescriptorProcessor {

    @Override
    protected void processEnvironment(RemoteEnvironment environment, ResourceDefinitionInjectionSources injectionSources) throws DeploymentUnitProcessingException {
        final ConnectionFactoriesMetaData metaDatas = environment.getConnectionFactories();
        if (metaDatas != null) {
            for(ConnectionFactoryMetaData metaData : metaDatas) {
                injectionSources.addResourceDefinitionInjectionSource(getResourceDefinitionInjectionSource(metaData));
            }
        }
    }

    private ResourceDefinitionInjectionSource getResourceDefinitionInjectionSource(final ConnectionFactoryMetaData metaData) {
        final String name = metaData.getName();
        if (name == null || name.isEmpty()) {
            throw ROOT_LOGGER.elementAttributeMissing("<connection-factory>", "name");
        }
        final String interfaceName = metaData.getInterfaceName();
        if (interfaceName == null || interfaceName.isEmpty()) {
            throw ROOT_LOGGER.elementAttributeMissing("<connection-factory>", "interfaceName");
        }
        final String resourceAdapter = metaData.getResourceAdapter();
        if (resourceAdapter == null || resourceAdapter.isEmpty()) {
            throw ROOT_LOGGER.elementAttributeMissing("<connection-factory>", "resourceAdapter");
        }
        final ConnectionFactoryDefinitionInjectionSource resourceDefinitionInjectionSource = new ConnectionFactoryDefinitionInjectionSource(name, interfaceName, resourceAdapter);
        if (metaData.getDescriptions() != null) {
            resourceDefinitionInjectionSource.setDescription(metaData.getDescriptions().toString());
        }
        resourceDefinitionInjectionSource.setMaxPoolSize(metaData.getMaxPoolSize());
        resourceDefinitionInjectionSource.setMinPoolSize(metaData.getMinPoolSize());
        if (metaData.getTransactionSupport() != null) {
            // FIXME use TransactionSupport.TransactionSupportLevel in metadata object
            switch (metaData.getTransactionSupport()) {
                case NoTransaction:
                    resourceDefinitionInjectionSource.setTransactionSupportLevel(TransactionSupport.TransactionSupportLevel.NoTransaction);
                    break;
                case LocalTransaction:
                    resourceDefinitionInjectionSource.setTransactionSupportLevel(TransactionSupport.TransactionSupportLevel.LocalTransaction);
                    break;
                case XATransaction:
                    resourceDefinitionInjectionSource.setTransactionSupportLevel(TransactionSupport.TransactionSupportLevel.XATransaction);
                    break;
            }
        }
        resourceDefinitionInjectionSource.addProperties(metaData.getProperties());
        return resourceDefinitionInjectionSource;
    }

}
