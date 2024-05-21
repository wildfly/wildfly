/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem.remote;

import org.infinispan.client.hotrod.configuration.ExecutorFactoryConfiguration;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceRegistration;
import org.wildfly.clustering.infinispan.client.service.HotRodServiceDescriptor;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;

/**
 * @author Paul Ferraro
 */
public interface ExecutorFactoryServiceDescriptor extends UnaryServiceDescriptor<ExecutorFactoryConfiguration>, ResourceRegistration {

    @Override
    default String getName() {
        PathElement path = this.getPathElement();
        return String.join(".", HotRodServiceDescriptor.REMOTE_CACHE_CONTAINER_CONFIGURATION.getName(), path.getKey(), path.getValue());
    }

    @Override
    default Class<ExecutorFactoryConfiguration> getType() {
        return ExecutorFactoryConfiguration.class;
    }
}
