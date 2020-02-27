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

import java.util.Map;
import java.util.function.Function;

import org.infinispan.configuration.cache.CacheMode;
import org.wildfly.clustering.ee.infinispan.PrimaryOwnerLocator;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.infinispan.spi.distribution.Key;
import org.wildfly.clustering.registry.Registry;
import org.wildfly.clustering.web.routing.RouteLocator;

/**
 * @author Paul Ferraro
 */
public class PrimaryOwnerRouteLocator implements RouteLocator {

    private final Function<Key<String>, Node> primaryOwnerLocator;
    private final Registry<String, Void> registry;
    private final boolean preferPrimary;
    private final String localRoute;

    public PrimaryOwnerRouteLocator(PrimaryOwnerRouteLocatorConfiguration config) {
        this.registry = config.getRegistry();
        this.primaryOwnerLocator = new PrimaryOwnerLocator<>(config.getCache(), config.getMemberFactory(), this.registry.getGroup());
        CacheMode mode = config.getCache().getCacheConfiguration().clustering().cacheMode();
        this.preferPrimary = mode.isClustered() && !mode.isScattered();
        this.localRoute = this.registry.getEntry(this.registry.getGroup().getLocalMember()).getKey();
    }

    @Override
    public String locate(String sessionId) {
        Node primaryMember = this.preferPrimary ? this.primaryOwnerLocator.apply(new Key<>(sessionId)) : null;
        Map.Entry<String, Void> entry = (primaryMember != null) ? this.registry.getEntry(primaryMember) : null;
        return (entry != null) ? entry.getKey() : this.localRoute;
    }
}
