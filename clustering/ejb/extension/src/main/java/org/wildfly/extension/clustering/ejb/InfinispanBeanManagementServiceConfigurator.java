/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.ejb;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.ejb.bean.BeanManagementProvider;
import org.wildfly.clustering.ejb.infinispan.bean.InfinispanBeanManagementConfiguration;
import org.wildfly.clustering.ejb.infinispan.bean.InfinispanBeanManagementProvider;
import org.wildfly.clustering.service.ServiceConfigurator;

import static org.wildfly.extension.clustering.ejb.InfinispanBeanManagementResourceDefinition.Attribute.CACHE;
import static org.wildfly.extension.clustering.ejb.InfinispanBeanManagementResourceDefinition.Attribute.CACHE_CONTAINER;

/**
 * Service configurator for Infinispan bean management providers.
 *
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
public class InfinispanBeanManagementServiceConfigurator extends BeanManagementServiceConfigurator implements InfinispanBeanManagementConfiguration {

    private volatile String containerName;
    private volatile String cacheName;

    public InfinispanBeanManagementServiceConfigurator(PathAddress address) {
        super(address);
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.containerName = CACHE_CONTAINER.resolveModelAttribute(context, model).asString();
        this.cacheName = CACHE.resolveModelAttribute(context, model).asStringOrNull();
        return super.configure(context, model);
    }

    @Override
    public BeanManagementProvider apply(String name) {
        return new InfinispanBeanManagementProvider(name, this);
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
