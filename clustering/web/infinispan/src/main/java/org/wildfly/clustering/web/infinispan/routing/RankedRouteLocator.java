/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
import org.wildfly.clustering.infinispan.distribution.CacheKeyDistribution;
import org.wildfly.clustering.infinispan.distribution.KeyDistribution;
import org.wildfly.clustering.registry.Registry;
import org.wildfly.clustering.server.NodeFactory;
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
            if (member != null) {
                if (member.equals(localMember)) {
                    localIsOwner = true;
                }
                Map.Entry<String, Void> entry = this.registry.getEntry(member);
                if (entry != null) {
                    routes.add(entry.getKey());
                }
            }
        }
        if (!localIsOwner && (routes.size() < this.maxRoutes)) {
            routes.add(this.localRoute);
        }
        return !routes.isEmpty() ? String.join(this.delimiter, routes) : this.localRoute;
    }
}
