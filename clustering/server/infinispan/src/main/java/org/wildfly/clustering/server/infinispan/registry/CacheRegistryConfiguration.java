/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.server.infinispan.registry;

import org.wildfly.clustering.ee.infinispan.InfinispanConfiguration;
import org.wildfly.clustering.group.Group;

/**
 * Configuration for a {@link CacheRegistryFactory}.
 * @author Paul Ferraro
 */
public interface CacheRegistryConfiguration<K, V> extends InfinispanConfiguration {
    Group getGroup();
}
