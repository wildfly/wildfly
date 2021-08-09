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
package org.wildfly.clustering.infinispan.spi.distribution;

import org.infinispan.Cache;
import org.infinispan.distribution.ch.ConsistentHash;
import org.infinispan.distribution.ch.KeyPartitioner;
import org.infinispan.remoting.transport.Address;

/**
 * {@link Locality} implementation based on a {@link ConsistentHash}.
 * @author Paul Ferraro
 */
public class ConsistentHashLocality implements Locality {

    private final KeyDistribution distribution;
    private final Address localAddress;

    @SuppressWarnings("deprecation")
    public ConsistentHashLocality(Cache<?, ?> cache, ConsistentHash hash) {
        this(cache.getAdvancedCache().getComponentRegistry().getLocalComponent(KeyPartitioner.class), hash, cache.getAdvancedCache().getDistributionManager().getCacheTopology().getLocalAddress());
    }

    private ConsistentHashLocality(KeyPartitioner partitioner, ConsistentHash hash, Address localAddress) {
        this(new ConsistentHashKeyDistribution(partitioner, hash), localAddress);
    }

    ConsistentHashLocality(KeyDistribution distribution, Address localAddress) {
        this.distribution = distribution;
        this.localAddress = localAddress;
    }

    @Override
    public boolean isLocal(Object key) {
        Address primary = this.distribution.getPrimaryOwner(key);
        return this.localAddress.equals(primary);
    }
}
