/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import org.wildfly.clustering.cache.infinispan.embedded.EmbeddedCacheConfiguration;
import org.wildfly.clustering.ejb.timer.TimerManagerFactoryConfiguration;
import org.wildfly.clustering.marshalling.ByteBufferMarshaller;
import org.wildfly.clustering.server.infinispan.dispatcher.CacheContainerCommandDispatcherFactory;

/**
 * @author Paul Ferraro
 */
public interface InfinispanTimerManagerFactoryConfiguration<I> extends TimerManagerFactoryConfiguration<I>, EmbeddedCacheConfiguration {

    ByteBufferMarshaller getMarshaller();
    CacheContainerCommandDispatcherFactory getCommandDispatcherFactory();
}
