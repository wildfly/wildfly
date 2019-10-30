/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
