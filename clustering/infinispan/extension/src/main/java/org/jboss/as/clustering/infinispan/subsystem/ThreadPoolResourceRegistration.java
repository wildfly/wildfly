/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.controller.AttributeDefinition;

/**
 * A resource registration for a thread pool.
 * @author Paul Ferraro
 */
public interface ThreadPoolResourceRegistration<C> extends ScheduledThreadPoolResourceRegistration<C> {
    AttributeDefinition getMaxThreads();
    AttributeDefinition getQueueLength();
}
