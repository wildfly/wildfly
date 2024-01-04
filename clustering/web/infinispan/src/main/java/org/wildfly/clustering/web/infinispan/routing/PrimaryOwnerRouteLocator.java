/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.infinispan.routing;

import java.util.Map;
import java.util.function.Function;

import org.wildfly.clustering.ee.infinispan.GroupedKey;
import org.wildfly.clustering.ee.infinispan.PrimaryOwnerLocator;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.registry.Registry;
import org.wildfly.clustering.web.routing.RouteLocator;

/**
 * @author Paul Ferraro
 */
public class PrimaryOwnerRouteLocator implements RouteLocator {

    private final Function<GroupedKey<String>, Node> primaryOwnerLocator;
    private final Registry<String, Void> registry;
    private final String localRoute;

    public PrimaryOwnerRouteLocator(PrimaryOwnerRouteLocatorConfiguration config) {
        this(new PrimaryOwnerLocator<>(config.getCache(), config.getMemberFactory()), config.getRegistry());
    }

    PrimaryOwnerRouteLocator(Function<GroupedKey<String>, Node> primaryOwnerLocator, Registry<String, Void> registry) {
        this.primaryOwnerLocator = primaryOwnerLocator;
        this.registry = registry;
        this.localRoute = this.registry.getEntry(this.registry.getGroup().getLocalMember()).getKey();
    }

    @Override
    public String locate(String sessionId) {
        Node primaryMember = this.primaryOwnerLocator.apply(new GroupedKey<>(sessionId));
        Map.Entry<String, Void> entry = this.registry.getEntry(primaryMember);
        return (entry != null) ? entry.getKey() : this.localRoute;
    }
}
