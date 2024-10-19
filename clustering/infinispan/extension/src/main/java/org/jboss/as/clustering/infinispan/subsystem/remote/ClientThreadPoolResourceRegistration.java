/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem.remote;

import java.util.concurrent.ThreadFactory;

import org.infinispan.client.hotrod.configuration.ExecutorFactoryConfiguration;
import org.jboss.as.clustering.infinispan.subsystem.ThreadPoolResourceRegistration;

/**
 * A resource registration for a thread pool of a remote cache container.
 * @author Paul Ferraro
 */
public interface ClientThreadPoolResourceRegistration extends ThreadPoolResourceRegistration<ExecutorFactoryConfiguration> {
    ThreadFactory getThreadFactory();
}
