/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.server.infinispan.provider;

import org.infinispan.remoting.transport.Address;
import org.wildfly.clustering.ee.infinispan.InfinispanConfiguration;
import org.wildfly.clustering.server.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.server.group.Group;

/**
 * Configuration for a {@link CacheServiceProviderRegistry}.
 * @author Paul Ferraro
 */
public interface CacheServiceProviderRegistryConfiguration<T> extends InfinispanConfiguration {
    Object getId();
    Group<Address> getGroup();
    CommandDispatcherFactory getCommandDispatcherFactory();
}
