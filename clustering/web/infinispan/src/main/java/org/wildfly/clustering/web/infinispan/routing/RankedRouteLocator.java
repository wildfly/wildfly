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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.infinispan.Cache;
import org.infinispan.remoting.transport.Address;
import org.wildfly.clustering.ee.infinispan.GroupedKey;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.infinispan.spi.distribution.CacheKeyDistribution;
import org.wildfly.clustering.infinispan.spi.distribution.KeyDistribution;
import org.wildfly.clustering.registry.Registry;
import org.wildfly.clustering.spi.NodeFactory;
import org.wildfly.clustering.web.routing.RouteLocator;

/**
 * @author Paul Ferraro
 */
public class RankedRouteLocator implements RouteLocator {

    private final KeyDistribution distribution;
    private final Registry<String, Void> registry;
    private final NodeFactory<Address> factory;
    private final String localRoute;
    private final String delimiter;
    private final int maxRoutes;

    public RankedRouteLocator(RankedRouteLocatorConfiguration config) {
        this(config.getCache(), config.getRegistry(), config.getMemberFactory(), config.getDelimiter(), config.getMaxRoutes());
    }

    private RankedRouteLocator(Cache<GroupedKey<String>, ?> cache, Registry<String, Void> registry, NodeFactory<Address> factory, String delimiter, int maxRoutes) {
        this(new CacheKeyDistribution(cache), registry, factory, delimiter, maxRoutes);
    }

    RankedRouteLocator(KeyDistribution distribution, Registry<String, Void> registry, NodeFactory<Address> factory, String delimiter, int maxRoutes) {
        this.distribution = distribution;
        this.registry = registry;
        this.factory = factory;
        this.localRoute = this.registry.getEntry(this.registry.getGroup().getLocalMember()).getKey();
        this.delimiter = delimiter;
        this.maxRoutes = maxRoutes;
    }

    @Override
    public String locate(String sessionId) {
        List<Address> owners = this.distribution.getOwners(new GroupedKey<>(sessionId));
        List<String> routes = new ArrayList<>(this.maxRoutes);
        boolean localIsOwner = false;
        Node localMember = this.registry.getGroup().getLocalMember();
        Iterator<Address> addresses = owners.iterator();
        while (addresses.hasNext() && (routes.size() < this.maxRoutes)) {
            Address address = addresses.next();
            Node member = this.factory.createNode(address);
            if (member.equals(localMember)) {
                localIsOwner = true;
            }
            Map.Entry<String, Void> entry = this.registry.getEntry(member);
            if (entry != null) {
                routes.add(entry.getKey());
            }
        }
        if (!localIsOwner && (routes.size() < this.maxRoutes)) {
            routes.add(this.localRoute);
        }
        return !routes.isEmpty() ? String.join(this.delimiter, routes) : this.localRoute;
    }
}
