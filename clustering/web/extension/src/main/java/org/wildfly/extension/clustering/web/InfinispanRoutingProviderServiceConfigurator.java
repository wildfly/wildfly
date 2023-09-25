/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
