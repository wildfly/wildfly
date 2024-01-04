/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.ejb3.component.stateful.cache.distributable.LegacyDistributableStatefulSessionBeanCacheProviderServiceConfigurator;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.wildfly.clustering.ejb.bean.LegacyBeanManagementConfiguration;

/**
 * @author Paul Ferraro
 */
@Deprecated
public class PassivationStoreAdd extends AbstractAddStepHandler {

    private final AttributeDefinition[] attributes;

    PassivationStoreAdd(AttributeDefinition... attributes) {
        this.attributes = attributes;
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (AttributeDefinition attr : this.attributes) {
            attr.validateAndSet(operation, model);
        }
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        int initialMaxSize = PassivationStoreResourceDefinition.MAX_SIZE.resolveModelAttribute(context, model).asInt();
        String containerName = PassivationStoreResourceDefinition.CACHE_CONTAINER.resolveModelAttribute(context, model).asString();
        ModelNode beanCacheNode = PassivationStoreResourceDefinition.BEAN_CACHE.resolveModelAttribute(context, model);
        String cacheName = beanCacheNode.isDefined() ? beanCacheNode.asString() : null;
        this.install(context, operation, initialMaxSize, containerName, cacheName);
    }

    protected void install(OperationContext context, ModelNode operation, final int maxSize, final String containerName, final String cacheName) {
        LegacyBeanManagementConfiguration config = new LegacyBeanManagementConfiguration() {
            @Override
            public String getContainerName() {
                return containerName;
            }

            @Override
            public String getCacheName() {
                return cacheName;
            }

            @Override
            public Integer getMaxActiveBeans() {
                return maxSize;
            }
        };
        new LegacyDistributableStatefulSessionBeanCacheProviderServiceConfigurator<>(context.getCurrentAddress(), config).build(context.getServiceTarget())
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
                .install();
    }
}
