/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
