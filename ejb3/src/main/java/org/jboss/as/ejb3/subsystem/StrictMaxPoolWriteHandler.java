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

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.ejb3.component.pool.StrictMaxPoolConfigService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

/**
 * Handles the "write-attribute" operation for a strict-max-bean-instance-pool resource.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
class StrictMaxPoolWriteHandler extends AbstractWriteAttributeHandler<Void> {

    StrictMaxPoolWriteHandler(Collection<AttributeDefinition> attributes) {
        super(attributes);
    }

    @Override
    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                           ModelNode newValue, ModelNode currentValue, HandbackHolder<Void> handbackHolder) throws OperationFailedException {

        final ModelNode model = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
        applyModelToRuntime(context, operation, attributeName, model);

        return false;
    }

    private void applyModelToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode model) throws OperationFailedException {

        final String poolName = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)).getLastElement().getValue();
        final ServiceName serviceName = StrictMaxPoolConfigService.EJB_POOL_CONFIG_BASE_SERVICE_NAME.append(poolName);
        final ServiceRegistry registry = context.getServiceRegistry(true);
        ServiceController<?> sc = registry.getService(serviceName);
        if (sc != null) {
            StrictMaxPoolConfigService smpc = (StrictMaxPoolConfigService) sc.getService();
            if (smpc != null) {
                if (StrictMaxPoolResourceDefinition.MAX_POOL_SIZE.getName().equals(attributeName)) {
                    int maxPoolSize = StrictMaxPoolResourceDefinition.MAX_POOL_SIZE.resolveModelAttribute(context, model).asInt(-1);
                    smpc.setMaxPoolSize(maxPoolSize);
                } else if (StrictMaxPoolResourceDefinition.DERIVE_SIZE.getName().equals(attributeName)) {
                    StrictMaxPoolConfigService.Derive derive = StrictMaxPoolResourceDefinition.parseDeriveSize(context, model);
                    smpc.setDerive(derive);
                } else if (StrictMaxPoolResourceDefinition.INSTANCE_ACQUISITION_TIMEOUT.getName().equals(attributeName)) {
                    long timeout = StrictMaxPoolResourceDefinition.INSTANCE_ACQUISITION_TIMEOUT.resolveModelAttribute(context, model).asLong();
                    smpc.setTimeout(timeout);
                } else if (StrictMaxPoolResourceDefinition.INSTANCE_ACQUISITION_TIMEOUT_UNIT.getName().equals(attributeName)) {
                    String timeoutUnit = StrictMaxPoolResourceDefinition.INSTANCE_ACQUISITION_TIMEOUT_UNIT.resolveModelAttribute(context, model).asString();
                    smpc.setTimeoutUnit(TimeUnit.valueOf(timeoutUnit));
                }
            }
        }
    }

    @Override
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                         ModelNode valueToRestore, ModelNode valueToRevert, Void handback) throws OperationFailedException {
        final ModelNode restored = context.readResource(PathAddress.EMPTY_ADDRESS).getModel().clone();
        restored.get(attributeName).set(valueToRestore);
        applyModelToRuntime(context, operation, attributeName, restored);
    }
}
