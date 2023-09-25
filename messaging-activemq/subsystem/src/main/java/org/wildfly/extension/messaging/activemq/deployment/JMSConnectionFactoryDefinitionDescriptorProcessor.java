/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.deployment;

import org.jboss.as.ee.resource.definition.ResourceDefinitionDescriptorProcessor;
import org.jboss.as.ee.resource.definition.ResourceDefinitionInjectionSource;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.metadata.javaee.spec.JMSConnectionFactoriesMetaData;
import org.jboss.metadata.javaee.spec.JMSConnectionFactoryMetaData;
import org.jboss.metadata.javaee.spec.RemoteEnvironment;

/**
 * Process jms-connection-factory from deployment descriptor.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 * @author Eduardo Martins
 */
public class JMSConnectionFactoryDefinitionDescriptorProcessor extends ResourceDefinitionDescriptorProcessor {

    @Override
    protected void processEnvironment(RemoteEnvironment environment, ResourceDefinitionInjectionSources injectionSources) throws DeploymentUnitProcessingException {
        final JMSConnectionFactoriesMetaData metaDatas = environment.getJmsConnectionFactories();
        if (metaDatas != null) {
            for(JMSConnectionFactoryMetaData metaData : metaDatas) {
                injectionSources.addResourceDefinitionInjectionSource(getResourceDefinitionInjectionSource(metaData));
            }
        }
    }

    private ResourceDefinitionInjectionSource getResourceDefinitionInjectionSource(JMSConnectionFactoryMetaData metadata) {
        final JMSConnectionFactoryDefinitionInjectionSource source = new JMSConnectionFactoryDefinitionInjectionSource(metadata.getName());
        source.setInterfaceName(metadata.getInterfaceName());
        source.setClassName(metadata.getClassName());
        source.setResourceAdapter(metadata.getResourceAdapter());
        source.setUser(metadata.getUser());
        source.setPassword(metadata.getPassword());
        source.setClientId(metadata.getClientId());
        source.addProperties(metadata.getProperties());
        source.setTransactional(metadata.isTransactional());
        source.setMaxPoolSize(metadata.getMaxPoolSize());
        source.setMinPoolSize(metadata.getMinPoolSize());
        return source;
    }
}
