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

package org.wildfly.clustering.web.infinispan.routing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.infinispan.Cache;
import org.infinispan.distribution.DistributionInfo;
import org.infinispan.remoting.transport.Address;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.infinispan.spi.distribution.Key;
import org.wildfly.clustering.registry.Registry;
import org.wildfly.clustering.spi.NodeFactory;
import org.wildfly.clustering.web.routing.RouteLocator;

/**
 * @author Paul Ferraro
 */
public class RankedRouteLocator implements RouteLocator {

    private final NodeFactory<Address> factory;
    private final Registry<String, Void> registry;
    private final Cache<Key<String>, ?> cache;
    private final String localRoute;
    private final boolean preferPrimary;
    private final String delimiter;
    private final int maxRoutes;

    public RankedRouteLocator(RankedRouteLocatorConfiguration config) {
        this.cache = config.getCache();
        this.registry = config.getRegistry();
        this.factory = config.getMemberFactory();
        this.localRoute = this.registry.getEntry(this.registry.getGroup().getLocalMember()).getKey();
        this.preferPrimary = config.getCache().getCacheConfiguration().clustering().cacheMode().isClustered();
        this.delimiter = config.getDelimiter();
        this.maxRoutes = config.getMaxRoutes();
    }

    @Override
    public String locate(String sessionId) {
        DistributionInfo info = this.preferPrimary ? this.cache.getAdvancedCache().getDistributionManager().getCacheTopology().getDistribution(new Key<>(sessionId)) : null;
        List<Address> addresses = (info != null) ? info.writeOwners() : Collections.emptyList();
        int size = Math.min(addresses.size(), this.maxRoutes);
        boolean localOwner = (info == null) || info.isWriteOwner();
        List<String> routes = !addresses.isEmpty() ? new ArrayList<>(localOwner ? size : size + 1) : Collections.emptyList();
        for (Address address : addresses.subList(0, size)) {
            Node member = this.factory.createNode(address);
            Map.Entry<String, Void> entry = this.registry.getEntry(member);
            if (entry != null) {
                routes.add(entry.getKey());
            }
        }
        if (!localOwner && (routes.size() < this.maxRoutes)) {
            Map.Entry<String, Void> entry = this.registry.getEntry(this.registry.getGroup().getLocalMember());
            if (entry != null) {
                routes.add(entry.getKey());
            }
        }
        return !routes.isEmpty() ? String.join(this.delimiter, routes) : this.localRoute;
    }
}
