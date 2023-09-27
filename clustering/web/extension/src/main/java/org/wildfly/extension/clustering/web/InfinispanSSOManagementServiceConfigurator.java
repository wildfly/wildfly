/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web;

import static org.wildfly.extension.clustering.web.InfinispanSSOManagementResourceDefinition.Attribute.CACHE;
import static org.wildfly.extension.clustering.web.InfinispanSSOManagementResourceDefinition.Attribute.CACHE_CONTAINER;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.web.service.sso.DistributableSSOManagementProvider;
import org.wildfly.extension.clustering.web.sso.infinispan.InfinispanSSOManagementConfiguration;
import org.wildfly.extension.clustering.web.sso.infinispan.InfinispanSSOManagementProvider;

/**
 * Service configurator for Infinispan single sign-on management providers.
 * @author Paul Ferraro
 */
public class InfinispanSSOManagementServiceConfigurator extends SSOManagementServiceConfigurator implements InfinispanSSOManagementConfiguration {

    private volatile String containerName;
    private volatile String cacheName;

    public InfinispanSSOManagementServiceConfigurator(PathAddress address) {
        super(address);
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.containerName = CACHE_CONTAINER.resolveModelAttribute(context, model).asString();
        this.cacheName = CACHE.resolveModelAttribute(context, model).asStringOrNull();
        return this;
    }

    @Override
    public DistributableSSOManagementProvider get() {
        return new InfinispanSSOManagementProvider(this);
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
