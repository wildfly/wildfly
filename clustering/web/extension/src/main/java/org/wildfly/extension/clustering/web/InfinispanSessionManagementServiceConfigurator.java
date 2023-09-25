/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web;

import static org.wildfly.extension.clustering.web.InfinispanSessionManagementResourceDefinition.Attribute.CACHE;
import static org.wildfly.extension.clustering.web.InfinispanSessionManagementResourceDefinition.Attribute.CACHE_CONTAINER;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.web.infinispan.session.InfinispanSessionManagementConfiguration;
import org.wildfly.clustering.web.service.session.DistributableSessionManagementProvider;
import org.wildfly.extension.clustering.web.session.infinispan.InfinispanSessionManagementProvider;

/**
 * Service configurator for Infinispan session management providers.
 * @author Paul Ferraro
 */
public class InfinispanSessionManagementServiceConfigurator extends SessionManagementServiceConfigurator<InfinispanSessionManagementConfiguration<DeploymentUnit>> implements InfinispanSessionManagementConfiguration<DeploymentUnit> {

    private volatile String containerName;
    private volatile String cacheName;

    public InfinispanSessionManagementServiceConfigurator(PathAddress address) {
        super(address);
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.containerName = CACHE_CONTAINER.resolveModelAttribute(context, model).asString();
        this.cacheName = CACHE.resolveModelAttribute(context, model).asStringOrNull();
        return super.configure(context, model);
    }

    @Override
    public DistributableSessionManagementProvider<InfinispanSessionManagementConfiguration<DeploymentUnit>> get() {
        return new InfinispanSessionManagementProvider(this, this.getRouteLocatorServiceConfiguratorFactory());
    }

    @Override
    public String getContainerName() {
        return this.containerName;
    }

    @Override
    public String getCacheName() {
        return this.cacheName;
    }
}
