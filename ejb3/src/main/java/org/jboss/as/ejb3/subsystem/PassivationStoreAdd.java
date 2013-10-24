/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.subsystem;

import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.ejb3.cache.distributable.DistributableCacheFactoryBuilderService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.wildfly.clustering.ejb.BeanManagerFactoryBuilderConfiguration;

/**
 * @author Paul Ferraro
 */
public class PassivationStoreAdd extends AbstractAddStepHandler {

    private final AttributeDefinition[] attributes;

    PassivationStoreAdd(AttributeDefinition... attributes) {
        this.attributes = attributes;
    }

    /**
     * Populate the <code>strictMaxPoolModel</code> from the <code>operation</code>
     *
     * @param operation the operation
     * @param model passivation-store ModelNode
     * @throws OperationFailedException
     */
    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (AttributeDefinition attr : this.attributes) {
            attr.validateAndSet(operation, model);
        }
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> serviceControllers) throws OperationFailedException {
        final String name = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.ADDRESS)).getLastElement().getValue();
        final int initialMaxSize = PassivationStoreResourceDefinition.MAX_SIZE.resolveModelAttribute(context, model).asInt();
        final String containerName = PassivationStoreResourceDefinition.CACHE_CONTAINER.resolveModelAttribute(context, model).asString();
        ModelNode beanCacheNode = PassivationStoreResourceDefinition.BEAN_CACHE.resolveModelAttribute(context, model);
        final String cacheName = beanCacheNode.isDefined() ? beanCacheNode.asString() : null;
        BeanManagerFactoryBuilderConfiguration config = new BeanManagerFactoryBuilderConfiguration() {
            private volatile int maxSize = initialMaxSize;

            @Override
            public String getContainerName() {
                return containerName;
            }

            @Override
            public String getCacheName() {
                return cacheName;
            }

            @Override
            public int getMaxSize() {
                return this.maxSize;
            }

            @Override
            public void setMaxSize(int size) {
                this.maxSize = size;
            }
        };
        ServiceBuilder<?> builder = new DistributableCacheFactoryBuilderService<>(name, config).build(context.getServiceTarget())
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
        ;
        serviceControllers.add(builder.install());
    }
}
