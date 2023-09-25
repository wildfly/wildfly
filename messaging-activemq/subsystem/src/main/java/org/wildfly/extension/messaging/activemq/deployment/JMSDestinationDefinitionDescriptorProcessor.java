/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.deployment;

import org.jboss.as.ee.resource.definition.ResourceDefinitionDescriptorProcessor;
import org.jboss.as.ee.resource.definition.ResourceDefinitionInjectionSource;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.metadata.javaee.spec.JMSDestinationMetaData;
import org.jboss.metadata.javaee.spec.JMSDestinationsMetaData;
import org.jboss.metadata.javaee.spec.RemoteEnvironment;

/**
 * Process jms-destination from deployment descriptor.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 * @author Eduardo Martins
 */
public class JMSDestinationDefinitionDescriptorProcessor extends ResourceDefinitionDescriptorProcessor {

    @Override
    protected void processEnvironment(RemoteEnvironment environment, ResourceDefinitionInjectionSources injectionSources) throws DeploymentUnitProcessingException {
        final JMSDestinationsMetaData metaDatas = environment.getJmsDestinations();
        if (metaDatas != null) {
            for(JMSDestinationMetaData metaData : metaDatas) {
                injectionSources.addResourceDefinitionInjectionSource(getResourceDefinitionInjectionSource(metaData));
            }
        }
    }

    private ResourceDefinitionInjectionSource getResourceDefinitionInjectionSource(JMSDestinationMetaData metadata) {
        final JMSDestinationDefinitionInjectionSource source = new JMSDestinationDefinitionInjectionSource(metadata.getName(), metadata.getInterfaceName());
        source.setDestinationName(metadata.getDestinationName());
        source.setResourceAdapter(metadata.getResourceAdapter());
        source.setClassName(metadata.getClassName());
        source.addProperties(metadata.getProperties());
        return source;
    }

}
