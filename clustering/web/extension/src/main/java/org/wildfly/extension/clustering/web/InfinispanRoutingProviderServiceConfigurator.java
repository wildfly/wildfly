/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

import static org.wildfly.extension.clustering.web.InfinispanRoutingProviderResourceDefinition.Attribute.CACHE;
import static org.wildfly.extension.clustering.web.InfinispanRoutingProviderResourceDefinition.Attribute.CACHE_CONTAINER;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.web.service.routing.RoutingProvider;
import org.wildfly.extension.clustering.web.routing.infinispan.InfinispanRoutingConfiguration;
import org.wildfly.extension.clustering.web.routing.infinispan.InfinispanRoutingProvider;

/**
 * Service configurator for the Infinispan routing provider.
 * @author Paul Ferraro
 */
public class InfinispanRoutingProviderServiceConfigurator extends RoutingProviderServiceConfigurator implements InfinispanRoutingConfiguration {

    private volatile String containerName;
    private volatile String cacheName;

    public InfinispanRoutingProviderServiceConfigurator(PathAddress address) {
        super(address, InfinispanRoutingProviderResourceDefinition.Capability.INFINISPAN_ROUTING_PROVIDER.getServiceName(address));
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.containerName = CACHE_CONTAINER.resolveModelAttribute(context, model).asString();
        this.cacheName = CACHE.resolveModelAttribute(context, model).asStringOrNull();
        return super.configure(context, model);
    }

    @Override
    public RoutingProvider get() {
        return new InfinispanRoutingProvider(this);
    }

    @Override
    public String getContainerName() {
        return this.containerName;
    }

    @Override
    public String getCacheName() {
        return this.cacheName;
    }

    @Override
    public void accept(ConfigurationBuilder builder) {
        // Use configuration as is
    }
}
