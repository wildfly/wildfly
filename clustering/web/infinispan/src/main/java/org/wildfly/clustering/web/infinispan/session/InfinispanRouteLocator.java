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
package org.wildfly.clustering.web.infinispan.session;

import java.util.Map;

import org.infinispan.Cache;
import org.infinispan.distribution.DistributionManager;
import org.infinispan.remoting.transport.Address;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.infinispan.spi.distribution.Key;
import org.wildfly.clustering.registry.Registry;
import org.wildfly.clustering.spi.NodeFactory;
import org.wildfly.clustering.web.session.RouteLocator;

/**
 * Uses Infinispan's {@link org.infinispan.distribution.DistributionManager} to determine the best node (i.e. the primary lock owner) to handle a given session.
 * The {@link Address} is then converted to a route using a {@link Registry}, which maps the route identifier per node.
 * @author Paul Ferraro
 */
public class InfinispanRouteLocator implements RouteLocator {

    private final NodeFactory<Address> factory;
    private final Registry<String, Void> registry;
    private final Cache<String, ?> cache;

    public InfinispanRouteLocator(InfinispanRouteLocatorConfiguration config) {
        this.cache = config.getCache();
        this.registry = config.getRegistry();
        this.factory = config.getMemberFactory();
    }

    @Override
    public String locate(String sessionId) {
        DistributionManager dist = this.cache.getAdvancedCache().getDistributionManager();
        Address address = (dist != null) && !this.cache.getCacheConfiguration().clustering().cacheMode().isScattered() ? dist.getCacheTopology().getDistribution(new Key<>(sessionId)).primary() : this.cache.getCacheManager().getAddress();
        Node node = (address != null) ? this.factory.createNode(address) : null;
        Map.Entry<String, Void> entry = this.registry.getEntry((node != null) ? node : this.registry.getGroup().getLocalMember());
        return (entry != null) ? entry.getKey() : null;
    }
}
