/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.HashConfigurationBuilder;
import org.infinispan.distribution.ch.ConsistentHash;
import org.infinispan.distribution.ch.ConsistentHashFactory;
import org.infinispan.distribution.ch.impl.DefaultConsistentHashFactory;
import org.infinispan.distribution.ch.impl.ReplicatedConsistentHashFactory;
import org.infinispan.distribution.ch.impl.SyncConsistentHashFactory;
import org.infinispan.distribution.ch.impl.TopologyAwareConsistentHashFactory;
import org.infinispan.distribution.ch.impl.TopologyAwareSyncConsistentHashFactory;

/**
 * Defines the consistent hash behavior for a distributed cache.
 * @author Paul Ferraro
 */
public enum ConsistentHashStrategy {
    INTER_CACHE() {
        @Override
        ConsistentHashFactory<? extends ConsistentHash> createConsistentHashFactory(boolean topologyAware) {
            return topologyAware ? new TopologyAwareSyncConsistentHashFactory() : new SyncConsistentHashFactory();
        }
    },
    INTRA_CACHE() {
        @Override
        ConsistentHashFactory<? extends ConsistentHash> createConsistentHashFactory(boolean topologyAware) {
            return topologyAware ? new TopologyAwareConsistentHashFactory() : new DefaultConsistentHashFactory();
        }
    };

    public static final ConsistentHashStrategy DEFAULT = INTRA_CACHE;

    abstract ConsistentHashFactory<? extends ConsistentHash> createConsistentHashFactory(boolean topologyAware);

    public void buildHashConfiguration(HashConfigurationBuilder builder, CacheMode mode, boolean topologyAware) {
        if (mode.isClustered()) {
            if (mode.isDistributed()) {
                builder.consistentHashFactory(this.createConsistentHashFactory(topologyAware));
            } else {
                // Used for REPL and INVALIDATION
                builder.consistentHashFactory(new ReplicatedConsistentHashFactory());
            }
        }
    }
}
