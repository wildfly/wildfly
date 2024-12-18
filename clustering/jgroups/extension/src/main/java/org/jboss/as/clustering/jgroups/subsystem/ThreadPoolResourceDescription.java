/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.util.stream.Stream;

import org.jboss.as.clustering.controller.DurationAttributeDefinition;
import org.jboss.as.clustering.controller.ResourceDescription;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;

/**
 * @author Paul Ferraro
 */
public interface ThreadPoolResourceDescription extends ResourceDescription, UnaryServiceDescriptor<ThreadPoolConfiguration> {

    @Override
    default String getName() {
        PathElement path = this.getPathElement();
        return String.join(".", ChannelFactory.SERVICE_DESCRIPTOR.getName(), path.getKey(), path.getValue());
    }

    @Override
    default Class<ThreadPoolConfiguration> getType() {
        return ThreadPoolConfiguration.class;
    }

    AttributeDefinition getMinThreads();
    AttributeDefinition getMaxThreads();
    DurationAttributeDefinition getKeepAlive();

    @Override
    default Stream<AttributeDefinition> getAttributes() {
        return Stream.of(this.getMinThreads(), this.getMaxThreads(), this.getKeepAlive());
    }
}
