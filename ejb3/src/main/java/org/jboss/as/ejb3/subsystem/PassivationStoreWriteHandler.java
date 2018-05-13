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

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.ejb3.cache.distributable.DistributableCacheFactoryBuilder;
import org.jboss.as.ejb3.cache.distributable.DistributableCacheFactoryBuilderService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.service.PassiveServiceSupplier;

/**
 * @author Paul Ferraro
 */
public class PassivationStoreWriteHandler extends AbstractWriteAttributeHandler<Void> {

    private final AttributeDefinition maxSizeAttribute;

    /**
     * @param attributes the attributes associated with the passivation-store resource, starting with max-size
     */
    PassivationStoreWriteHandler(AttributeDefinition... attributes) {
        super(attributes);
        // The first one should be the max-size attribute
        this.maxSizeAttribute = attributes[0];
    }

    @Override
    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode newValue, ModelNode currentValue, HandbackHolder<Void> handbackHolder) throws OperationFailedException {
        ModelNode model = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
        this.applyModelToRuntime(context, operation, attributeName, model);
        return false;
    }

    private void applyModelToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode model) throws OperationFailedException {
        String name = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)).getLastElement().getValue();
        ServiceName serviceName = DistributableCacheFactoryBuilderService.getServiceName(name);
        DistributableCacheFactoryBuilder<?, ?> builder = new PassiveServiceSupplier<DistributableCacheFactoryBuilder<?, ?>>(context.getServiceRegistry(true), serviceName).get();
        if (builder != null) {
            if (this.maxSizeAttribute.getName().equals(attributeName)) {
                int maxSize = this.maxSizeAttribute.resolveModelAttribute(context, model).asInt();
                builder.getConfiguration().setMaxSize(maxSize);
            }
        }
    }

    @Override
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode valueToRestore, ModelNode valueToRevert, Void handback) throws OperationFailedException {
        ModelNode restored = context.readResource(PathAddress.EMPTY_ADDRESS).getModel().clone();
        restored.get(attributeName).set(valueToRestore);
        this.applyModelToRuntime(context, operation, attributeName, restored);
    }
}