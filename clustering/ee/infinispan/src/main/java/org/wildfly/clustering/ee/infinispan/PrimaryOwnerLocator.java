/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.ee.infinispan;

import java.util.function.Function;

import org.infinispan.Cache;
import org.infinispan.remoting.transport.Address;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.infinispan.spi.distribution.CacheKeyDistribution;
import org.wildfly.clustering.infinispan.spi.distribution.KeyDistribution;
import org.wildfly.clustering.spi.NodeFactory;

/**
 * Function that returns the primary owner for a given cache key.
 * @author Paul Ferraro
 */
public class PrimaryOwnerLocator<K> implements Function<K, Node> {
    private final KeyDistribution distribution;
    private final NodeFactory<Address> memberFactory;

    public PrimaryOwnerLocator(Cache<? extends K, ?> cache, NodeFactory<Address> memberFactory) {
        this(new CacheKeyDistribution(cache), memberFactory);
    }

    PrimaryOwnerLocator(KeyDistribution distribution, NodeFactory<Address> memberFactory) {
        this.distribution = distribution;
        this.memberFactory = memberFactory;
    }

    @Override
    public Node apply(K key) {
        Address address = this.distribution.getPrimaryOwner(key);
        return this.memberFactory.createNode(address);
    }
}
