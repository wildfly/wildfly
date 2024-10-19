/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.Collection;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;

/**
 * @author Paul Ferraro
 *
 */
public interface ThreadPoolResourceResourceRegistration<C> extends ScheduledThreadPoolResourceRegistration<C> {
    AttributeDefinition getMaxThreads();
    AttributeDefinition getQueueLength();

    @Override
    default Collection<AttributeDefinition> getAttributes() {
        return List.of(this.getMinThreads(), this.getMaxThreads(), this.getQueueLength(), this.getKeepAlive());
    }
}
