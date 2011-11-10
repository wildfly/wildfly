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

import java.util.concurrent.TimeUnit;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.ejb3.cache.spi.BackingCacheEntryStoreConfig;
import org.jboss.as.ejb3.cache.spi.BackingCacheEntryStoreSourceService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

/**
 * @author Paul Ferraro
 */
public abstract class PassivationStoreWriteHandler<S extends BackingCacheEntryStoreConfig> extends AbstractWriteAttributeHandler<Void> {

    PassivationStoreWriteHandler(AttributeDefinition... attributes) {
        super(attributes);
    }

    @Override
    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode newValue, ModelNode currentValue, HandbackHolder<Void> handbackHolder) throws OperationFailedException {
        ModelNode model = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
        applyModelToRuntime(context, operation, attributeName, model);
        return false;
    }

    private void applyModelToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode model) throws OperationFailedException {
        String name = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)).getLastElement().getValue();
        ServiceName serviceName = BackingCacheEntryStoreSourceService.getServiceName(name);
        ServiceRegistry registry = context.getServiceRegistry(true);
        @SuppressWarnings("unchecked")
        ServiceController<S> service = (ServiceController<S>) registry.getService(serviceName);
        if (service != null) {
            S config = service.getValue();
            if (config != null) {
                AttributeDefinition maxSizeDefinition = this.getMaxSizeDefinition();
                if (maxSizeDefinition.getName().equals(attributeName)) {
                    int maxSize = maxSizeDefinition.resolveModelAttribute(context, model).asInt();
                    config.setMaxSize(maxSize);
                } else if (PassivationStoreResourceDefinition.IDLE_TIMEOUT.getName().equals(attributeName)) {
                    long timeout = PassivationStoreResourceDefinition.IDLE_TIMEOUT.resolveModelAttribute(context, model).asLong();
                    config.setIdleTimeout(timeout);
                } else if (PassivationStoreResourceDefinition.IDLE_TIMEOUT_UNIT.getName().equals(attributeName)) {
                    TimeUnit unit = TimeUnit.valueOf(PassivationStoreResourceDefinition.IDLE_TIMEOUT_UNIT.resolveModelAttribute(context, model).asString());
                    config.setIdleTimeoutUnit(unit);
                } else {
                    this.apply(config, context, attributeName, model);
                }
            }
        }
    }

    protected abstract AttributeDefinition getMaxSizeDefinition();

    protected abstract void apply(S config, OperationContext context, String attributeName, ModelNode model) throws OperationFailedException;

    @Override
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode valueToRestore, ModelNode valueToRevert, Void handback) throws OperationFailedException {
        ModelNode restored = context.readResource(PathAddress.EMPTY_ADDRESS).getModel().clone();
        restored.get(attributeName).set(valueToRestore);
        applyModelToRuntime(context, operation, attributeName, restored);
    }
}
