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

package org.wildfly.extension.clustering.web;

import static org.wildfly.extension.clustering.web.RankedAffinityResourceDefinition.Attribute.DELIMITER;
import static org.wildfly.extension.clustering.web.RankedAffinityResourceDefinition.Attribute.MAX_ROUTES;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.web.infinispan.routing.RankedRouteLocatorServiceConfiguratorFactory;
import org.wildfly.clustering.web.infinispan.routing.RankedRoutingConfiguration;
import org.wildfly.clustering.web.infinispan.session.InfinispanSessionManagementConfiguration;
import org.wildfly.clustering.web.routing.RouteLocatorServiceConfiguratorFactory;

/**
 * @author Paul Ferraro
 */
public class RankedAffinityServiceConfigurator extends AffinityServiceConfigurator<InfinispanSessionManagementConfiguration> implements RankedRoutingConfiguration {

    private volatile String delimiter;
    private volatile int maxRoutes;

    public RankedAffinityServiceConfigurator(PathAddress address) {
        super(address);
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.delimiter = DELIMITER.resolveModelAttribute(context, model).asString();
        this.maxRoutes = MAX_ROUTES.resolveModelAttribute(context, model).asInt();
        return this;
    }

    @Override
    public RouteLocatorServiceConfiguratorFactory<InfinispanSessionManagementConfiguration> get() {
        return new RankedRouteLocatorServiceConfiguratorFactory(this);
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
