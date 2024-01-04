/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.distribution;

import org.infinispan.configuration.cache.HashConfiguration;
import org.infinispan.distribution.ch.KeyPartitioner;
import org.infinispan.distribution.ch.impl.SingleSegmentKeyPartitioner;
import org.infinispan.factories.AbstractNamedCacheComponentFactory;
import org.infinispan.factories.AutoInstantiableFactory;
import org.infinispan.factories.annotations.DefaultFactoryFor;

/**
 * Custom key partitioner factory that uses the same key partitioner for all clustered caches, including non-tx invalidation caches.
 * @author Paul Ferraro
 */
@DefaultFactoryFor(classes = KeyPartitioner.class)
public class KeyPartitionerFactory extends AbstractNamedCacheComponentFactory implements AutoInstantiableFactory {

    @Override
    public Object construct(String componentName) {
        if (!this.configuration.clustering().cacheMode().isClustered() && !this.configuration.persistence().usingSegmentedStore()) {
            return SingleSegmentKeyPartitioner.getInstance();
        }
        HashConfiguration hashConfiguration = this.configuration.clustering().hash();
        KeyPartitioner partitioner = hashConfiguration.keyPartitioner();
        partitioner.init(hashConfiguration);

        this.basicComponentRegistry.wireDependencies(partitioner, false);

        return new KeyPartitioner() {
            @Override
            public int getSegment(Object key) {
                return partitioner.getSegment((key instanceof KeyGroup) ? ((KeyGroup<?>) key).getId() : key);
            }
        };
    }
}
