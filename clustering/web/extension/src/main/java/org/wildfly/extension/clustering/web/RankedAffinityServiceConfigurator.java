/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web;

import static org.wildfly.extension.clustering.web.RankedAffinityResourceDefinition.Attribute.DELIMITER;
import static org.wildfly.extension.clustering.web.RankedAffinityResourceDefinition.Attribute.MAX_ROUTES;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.ee.infinispan.InfinispanCacheConfiguration;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.web.infinispan.routing.RankedRoutingConfiguration;
import org.wildfly.clustering.web.service.routing.RouteLocatorServiceConfiguratorFactory;
import org.wildfly.extension.clustering.web.routing.infinispan.RankedRouteLocatorServiceConfiguratorFactory;

/**
 * @author Paul Ferraro
 */
public class RankedAffinityServiceConfigurator<C extends InfinispanCacheConfiguration> extends AffinityServiceConfigurator<C> implements RankedRoutingConfiguration {

    private volatile String delimiter;
    private volatile int maxRoutes;

    public RankedAffinityServiceConfigurator(PathAddress address) {
        super(RankedAffinityResourceDefinition.Capability.AFFINITY, address);
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.delimiter = DELIMITER.resolveModelAttribute(context, model).asString();
        this.maxRoutes = MAX_ROUTES.resolveModelAttribute(context, model).asInt();
        return this;
    }

    @Override
    public RouteLocatorServiceConfiguratorFactory<C> get() {
        return new RankedRouteLocatorServiceConfiguratorFactory<>(this);
    }

    @Override
    public String getDelimiter() {
        return this.delimiter;
    }

    @Override
    public int getMaxRoutes() {
        return this.maxRoutes;
    }
}
