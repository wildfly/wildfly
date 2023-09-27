/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web;

import org.jboss.as.controller.PathAddress;
import org.wildfly.clustering.ee.infinispan.InfinispanCacheConfiguration;
import org.wildfly.clustering.web.service.routing.RouteLocatorServiceConfiguratorFactory;
import org.wildfly.extension.clustering.web.routing.infinispan.PrimaryOwnerRouteLocatorServiceConfiguratorFactory;

/**
 * @author Paul Ferraro
 */
public class PrimaryOwnerAffinityServiceConfigurator<C extends InfinispanCacheConfiguration> extends AffinityServiceConfigurator<C> {

    public PrimaryOwnerAffinityServiceConfigurator(PathAddress address) {
        super(PrimaryOwnerAffinityResourceDefinition.Capability.AFFINITY, address);
    }

    @Override
    public RouteLocatorServiceConfiguratorFactory<C> get() {
        return new PrimaryOwnerRouteLocatorServiceConfiguratorFactory<>();
    }
}
