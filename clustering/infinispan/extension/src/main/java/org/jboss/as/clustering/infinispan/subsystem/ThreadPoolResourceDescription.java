/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.stream.Stream;

import org.jboss.as.controller.AttributeDefinition;

/**
 * @author Paul Ferraro
 *
 */
public interface ThreadPoolResourceDescription extends ScheduledThreadPoolResourceDescription {
    AttributeDefinition getMaxThreads();
    AttributeDefinition getQueueLength();

    @Override
    default Stream<AttributeDefinition> getAttributes() {
        return Stream.of(this.getMinThreads(), this.getMaxThreads(), this.getQueueLength(), this.getKeepAlive());
    }
}
