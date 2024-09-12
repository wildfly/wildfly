/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.configuration.global.ThreadPoolConfiguration;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceRegistration;
import org.wildfly.clustering.infinispan.service.InfinispanServiceDescriptor;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;

/**
 * @author Paul Ferraro
 */
public interface ThreadPoolServiceDescriptor extends UnaryServiceDescriptor<ThreadPoolConfiguration>, ResourceRegistration {

    @Override
    default String getName() {
        PathElement path = this.getPathElement();
        return String.join(".", InfinispanServiceDescriptor.CACHE_CONTAINER_CONFIGURATION.getName(), path.getKey(), path.getValue());
    }

    @Override
    default Class<ThreadPoolConfiguration> getType() {
        return ThreadPoolConfiguration.class;
    }
}
